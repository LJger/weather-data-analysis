package com.example.backend.service;

import com.example.backend.dto.PredictionRequest;
import com.example.backend.dto.PredictionResponse;
import com.example.backend.dto.PredictionResponse.DataPoint;
import com.example.backend.dto.PredictionResponse.PredictionMetrics;
import com.example.backend.dto.PredictionResponse.WarningPoint;
import com.example.backend.mapper.MeteorDataMapper;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PredictionService {

    @Autowired
    private MeteorDataMapper meteorDataMapper;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MIN_DATA_POINTS = 10;

    /**
     * 执行预测的主入口
     */
    public PredictionResponse predict(PredictionRequest request) {
        // 1. 参数校验与默认值填充
        if (request.getUserId() == null || request.getElementCode() == null || request.getElementCode().isBlank()) {
            return new PredictionResponse(false, "用户ID和要素代码不能为空");
        }
        String algorithm = request.getAlgorithm() != null ? request.getAlgorithm() : "linear_regression";
        int forecastSteps = request.getForecastSteps() != null ? request.getForecastSteps() : 8;
        String granularity = request.getGranularity() != null ? request.getGranularity() : "hour";
        int windowSize = request.getMovingWindowSize() != null ? request.getMovingWindowSize() : 5;
        double alpha = request.getSmoothingAlpha() != null ? request.getSmoothingAlpha() : 0.3;

        // 2. 解析时间范围
        OffsetDateTime startTime;
        OffsetDateTime endTime;
        try {
            startTime = parseDateTime(request.getHistoryStartTime());
            endTime = parseDateTime(request.getHistoryEndTime());
        } catch (Exception e) {
            return new PredictionResponse(false, "时间格式错误，应为 yyyy-MM-dd HH:mm:ss 或 ISO 格式");
        }

        // 3. 查询历史数据（使用聚合时间序列）
        Integer[] stationIds = request.getStationIds() != null
                ? request.getStationIds().toArray(new Integer[0])
                : new Integer[0];
        Long[] taskIds = request.getTaskIds() != null
                ? request.getTaskIds().toArray(new Long[0])
                : new Long[0];

        List<Map<String, Object>> rawTimeSeries = meteorDataMapper.aggregateTimeSeries(
                request.getUserId().intValue(),
                request.getElementCode(),
                startTime, endTime,
                granularity,
                stationIds, taskIds
        );

        if (rawTimeSeries == null || rawTimeSeries.size() < MIN_DATA_POINTS) {
            return new PredictionResponse(false,
                    "历史数据不足，至少需要 " + MIN_DATA_POINTS + " 个数据点，当前仅有 "
                            + (rawTimeSeries == null ? 0 : rawTimeSeries.size()) + " 个");
        }

        // 4. 转换为时间戳+值的列表
        List<double[]> tsData = new ArrayList<>(); // [epochMillis, value]
        List<String> tsLabels = new ArrayList<>();
        for (Map<String, Object> row : rawTimeSeries) {
            Object timeBucket = row.get("bucket");
            BigDecimal avgValue = row.get("avg_value") instanceof BigDecimal ? (BigDecimal) row.get("avg_value")
                    : new BigDecimal(row.get("avg_value").toString());
            OffsetDateTime dt;
            if (timeBucket instanceof java.sql.Timestamp ts) {
                dt = ts.toInstant().atOffset(ZoneOffset.ofHours(8));
            } else if (timeBucket instanceof OffsetDateTime odt) {
                dt = odt;
            } else {
                dt = OffsetDateTime.parse(timeBucket.toString());
            }
            tsData.add(new double[]{dt.toInstant().toEpochMilli(), avgValue.doubleValue()});
            tsLabels.add(dt.format(FMT));
        }

        // 5. 根据算法类型执行预测
        PredictionResult result;
        switch (algorithm) {
            case "moving_average":
                result = predictMovingAverage(tsData, windowSize, forecastSteps);
                break;
            case "exponential_smoothing":
                result = predictExponentialSmoothing(tsData, alpha, forecastSteps);
                break;
            case "linear_regression":
            default:
                result = predictLinearRegression(tsData, forecastSteps);
                break;
        }

        // 6. 计算预测时间点的时间标签
        double lastTime = tsData.get(tsData.size() - 1)[0];
        double timeStep = estimateTimeStep(tsData);
        List<String> forecastLabels = new ArrayList<>();
        for (int i = 1; i <= forecastSteps; i++) {
            long futureMillis = (long) (lastTime + i * timeStep);
            OffsetDateTime futureTime = OffsetDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(futureMillis), ZoneOffset.ofHours(8));
            forecastLabels.add(futureTime.format(FMT));
        }

        // 7. 构建历史数据点
        List<DataPoint> historyPoints = new ArrayList<>();
        for (int i = 0; i < tsData.size(); i++) {
            historyPoints.add(new DataPoint(tsLabels.get(i), tsData.get(i)[1]));
        }

        // 8. 构建预测数据点（含置信区间）
        List<DataPoint> forecastPoints = new ArrayList<>();
        for (int i = 0; i < forecastSteps; i++) {
            forecastPoints.add(new DataPoint(
                    forecastLabels.get(i),
                    round(result.predictions[i]),
                    round(result.upperBounds[i]),
                    round(result.lowerBounds[i])
            ));
        }

        // 9. 检查预警
        List<WarningPoint> warnings = checkWarnings(forecastPoints, request);

        // 10. 计算评估指标（用最后20%历史数据做验证）
        PredictionMetrics metrics = calculateMetrics(tsData, algorithm, windowSize, alpha);

        // 11. 组装响应
        PredictionResponse response = new PredictionResponse();
        response.setSuccess(true);
        response.setMessage("预测完成");
        response.setAlgorithm(algorithm);
        response.setElementCode(request.getElementCode());
        response.setHistoryData(historyPoints);
        response.setForecastData(forecastPoints);
        response.setWarnings(warnings);
        response.setMetrics(metrics);
        return response;
    }

    /**
     * 获取某用户某要素的数据概况
     */
    public Map<String, Object> getDataInfo(Long userId, String elementCode) {
        List<Map<String, Object>> rangeResult = meteorDataMapper.findDataTimeRange(userId.intValue(), elementCode);
        Map<String, Object> info = new HashMap<>();
        if (rangeResult != null && !rangeResult.isEmpty()) {
            Map<String, Object> row = rangeResult.get(0);
            Object minTime = firstNonNull(row.get("minTime"), row.get("min_time"));
            Object maxTime = firstNonNull(row.get("maxTime"), row.get("max_time"));
            Object dataCount = firstNonNull(row.get("dataCount"), row.get("data_count"));

            info.put("minTime", minTime != null ? minTime.toString() : null);
            info.put("maxTime", maxTime != null ? maxTime.toString() : null);
            info.put("dataCount", toLong(dataCount));
        } else {
            info.put("minTime", null);
            info.put("maxTime", null);
            info.put("dataCount", 0);
        }
        return info;
    }

    /**
     * 返回可用算法列表
     */
    public List<Map<String, Object>> getAlgorithms() {
        List<Map<String, Object>> algorithms = new ArrayList<>();

        Map<String, Object> lr = new LinkedHashMap<>();
        lr.put("code", "linear_regression");
        lr.put("name", "线性回归");
        lr.put("description", "基于最小二乘法拟合线性趋势，适合趋势稳定的气象要素预测");
        lr.put("params", List.of());
        algorithms.add(lr);

        Map<String, Object> ma = new LinkedHashMap<>();
        ma.put("code", "moving_average");
        ma.put("name", "移动平均");
        ma.put("description", "通过滑动窗口平均值预测未来趋势，适合短期平稳序列预测");
        Map<String, Object> maParam = new LinkedHashMap<>();
        maParam.put("key", "movingWindowSize");
        maParam.put("label", "窗口大小");
        maParam.put("type", "number");
        maParam.put("min", 3);
        maParam.put("max", 20);
        maParam.put("default", 5);
        ma.put("params", List.of(maParam));
        algorithms.add(ma);

        Map<String, Object> es = new LinkedHashMap<>();
        es.put("code", "exponential_smoothing");
        es.put("name", "指数平滑");
        es.put("description", "通过加权指数衰减进行平滑预测，近期数据权重更高，适合带趋势的序列");
        Map<String, Object> esParam = new LinkedHashMap<>();
        esParam.put("key", "smoothingAlpha");
        esParam.put("label", "平滑系数 α");
        esParam.put("type", "slider");
        esParam.put("min", 0.1);
        esParam.put("max", 0.9);
        esParam.put("step", 0.05);
        esParam.put("default", 0.3);
        es.put("params", List.of(esParam));
        algorithms.add(es);

        return algorithms;
    }

    // ========================= 预测算法实现 =========================

    /**
     * 线性回归预测
     * 使用 Apache Commons Math SimpleRegression
     */
    private PredictionResult predictLinearRegression(List<double[]> data, int forecastSteps) {
        SimpleRegression regression = new SimpleRegression();

        // 以第一个时间点为基准，归一化 x 轴
        double baseTime = data.get(0)[0];
        for (double[] point : data) {
            regression.addData(point[0] - baseTime, point[1]);
        }

        double slope = regression.getSlope();
        double intercept = regression.getIntercept();
        double timeStep = estimateTimeStep(data);

        // 计算残差标准差
        double sumSquaredResiduals = 0;
        for (double[] point : data) {
            double predicted = slope * (point[0] - baseTime) + intercept;
            sumSquaredResiduals += Math.pow(point[1] - predicted, 2);
        }
        double residualStd = Math.sqrt(sumSquaredResiduals / (data.size() - 2));

        double lastTime = data.get(data.size() - 1)[0];
        double[] predictions = new double[forecastSteps];
        double[] upper = new double[forecastSteps];
        double[] lower = new double[forecastSteps];

        for (int i = 0; i < forecastSteps; i++) {
            double futureX = lastTime + (i + 1) * timeStep - baseTime;
            double pred = slope * futureX + intercept;
            // 置信区间随步数增大（95% 置信度 ≈ 1.96σ，额外乘以步数平方根）
            double margin = 1.96 * residualStd * Math.sqrt(1 + 1.0 / data.size()
                    + Math.pow(futureX - meanX(data, baseTime), 2) / sumSqDevX(data, baseTime));
            predictions[i] = pred;
            upper[i] = pred + margin;
            lower[i] = pred - margin;
        }

        return new PredictionResult(predictions, upper, lower);
    }

    /**
     * 移动平均预测
     */
    private PredictionResult predictMovingAverage(List<double[]> data, int windowSize, int forecastSteps) {
        windowSize = Math.min(windowSize, data.size());

        // 取最后 windowSize 个数据的均值和标准差
        List<Double> windowValues = new ArrayList<>();
        for (int i = data.size() - windowSize; i < data.size(); i++) {
            windowValues.add(data.get(i)[1]);
        }

        double mean = windowValues.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double std = Math.sqrt(windowValues.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average().orElse(0));

        double[] predictions = new double[forecastSteps];
        double[] upper = new double[forecastSteps];
        double[] lower = new double[forecastSteps];

        // 迭代预测：每步使用前 windowSize 个值的平均
        List<Double> extendedValues = new ArrayList<>(windowValues);
        for (int i = 0; i < forecastSteps; i++) {
            // 取最后 windowSize 个值的均值
            List<Double> recent = extendedValues.subList(
                    Math.max(0, extendedValues.size() - windowSize), extendedValues.size());
            double pred = recent.stream().mapToDouble(Double::doubleValue).average().orElse(mean);

            // 置信区间随步数增大
            double uncertainty = 1.96 * std * Math.sqrt(1.0 + (double) i / windowSize);
            predictions[i] = pred;
            upper[i] = pred + uncertainty;
            lower[i] = pred - uncertainty;

            extendedValues.add(pred);
        }

        return new PredictionResult(predictions, upper, lower);
    }

    /**
     * 指数平滑（单指数 + 趋势的Holt方法）
     */
    private PredictionResult predictExponentialSmoothing(List<double[]> data, double alpha, int forecastSteps) {
        double beta = 0.1; // 趋势平滑系数

        // 初始化
        double level = data.get(0)[1];
        double trend = data.size() > 1 ? data.get(1)[1] - data.get(0)[1] : 0;

        // 拟合历史数据
        List<Double> fittedValues = new ArrayList<>();
        fittedValues.add(level);
        double sumSquaredError = 0;

        for (int i = 1; i < data.size(); i++) {
            double actual = data.get(i)[1];
            double prevLevel = level;

            // Holt 双参数更新
            level = alpha * actual + (1 - alpha) * (prevLevel + trend);
            trend = beta * (level - prevLevel) + (1 - beta) * trend;

            double fitted = level + trend;
            fittedValues.add(fitted);
            sumSquaredError += Math.pow(actual - fitted, 2);
        }

        double residualStd = Math.sqrt(sumSquaredError / Math.max(1, data.size() - 2));

        // 预测
        double[] predictions = new double[forecastSteps];
        double[] upper = new double[forecastSteps];
        double[] lower = new double[forecastSteps];

        for (int i = 0; i < forecastSteps; i++) {
            double pred = level + (i + 1) * trend;
            // 置信区间随步数递增
            double margin = 1.96 * residualStd * Math.sqrt(1 + (i + 1) * alpha * alpha);
            predictions[i] = pred;
            upper[i] = pred + margin;
            lower[i] = pred - margin;
        }

        return new PredictionResult(predictions, upper, lower);
    }

    // ========================= 辅助方法 =========================

    /**
     * 估算时间步长（毫秒），取中位数间隔
     */
    private double estimateTimeStep(List<double[]> data) {
        if (data.size() < 2) return 3 * 3600 * 1000; // 默认3小时

        List<Double> diffs = new ArrayList<>();
        for (int i = 1; i < data.size(); i++) {
            diffs.add(data.get(i)[0] - data.get(i - 1)[0]);
        }
        diffs.sort(Double::compareTo);
        return diffs.get(diffs.size() / 2); // 中位数
    }

    /**
     * 计算评估指标（用最后20%数据做验证）
     */
    private PredictionMetrics calculateMetrics(List<double[]> data, String algorithm, int windowSize, double alpha) {
        int validationSize = Math.max(2, data.size() / 5); // 20%
        int trainSize = data.size() - validationSize;
        if (trainSize < MIN_DATA_POINTS) {
            return new PredictionMetrics(null, null, null);
        }

        List<double[]> trainData = data.subList(0, trainSize);
        List<double[]> validData = data.subList(trainSize, data.size());

        PredictionResult result;
        switch (algorithm) {
            case "moving_average":
                result = predictMovingAverage(trainData, windowSize, validationSize);
                break;
            case "exponential_smoothing":
                result = predictExponentialSmoothing(trainData, alpha, validationSize);
                break;
            case "linear_regression":
            default:
                result = predictLinearRegression(trainData, validationSize);
                break;
        }

        // 计算 MAE, RMSE, R²
        double sumAbsError = 0, sumSquaredError = 0;
        double sumSquaredActual = 0;
        double meanActual = validData.stream().mapToDouble(d -> d[1]).average().orElse(0);

        for (int i = 0; i < validationSize; i++) {
            double actual = validData.get(i)[1];
            double predicted = result.predictions[i];
            double error = actual - predicted;

            sumAbsError += Math.abs(error);
            sumSquaredError += error * error;
            sumSquaredActual += Math.pow(actual - meanActual, 2);
        }

        double mae = sumAbsError / validationSize;
        double rmse = Math.sqrt(sumSquaredError / validationSize);
        double r2 = sumSquaredActual > 0 ? 1.0 - sumSquaredError / sumSquaredActual : 0;

        return new PredictionMetrics(round(mae), round(rmse), round(r2));
    }

    /**
     * 检查预测值是否超阈值
     */
    private List<WarningPoint> checkWarnings(List<DataPoint> forecastPoints, PredictionRequest request) {
        List<WarningPoint> warnings = new ArrayList<>();
        Double high = request.getWarningThresholdHigh();
        Double low = request.getWarningThresholdLow();

        for (DataPoint point : forecastPoints) {
            if (high != null && point.getValue() != null && point.getValue() > high) {
                warnings.add(new WarningPoint(point.getDatetime(), point.getValue(), high, "high"));
            }
            if (low != null && point.getValue() != null && point.getValue() < low) {
                warnings.add(new WarningPoint(point.getDatetime(), point.getValue(), low, "low"));
            }
        }
        return warnings;
    }

    /**
     * 解析时间字符串
     */
    private OffsetDateTime parseDateTime(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) {
            throw new IllegalArgumentException("时间字符串不能为空");
        }
        // 1. 带时区偏移的 ISO 格式，如 2025-11-01T00:00:00+08:00
        try {
            return OffsetDateTime.parse(timeStr);
        } catch (Exception ignored) {}
        // 2. ISO LocalDateTime 格式（无时区），如 2025-11-01T00:00:00
        try {
            return java.time.LocalDateTime.parse(timeStr).atOffset(ZoneOffset.ofHours(8));
        } catch (Exception ignored) {}
        // 3. "yyyy-MM-dd HH:mm:ss" 格式
        try {
            return java.time.LocalDateTime.parse(timeStr, FMT).atOffset(ZoneOffset.ofHours(8));
        } catch (Exception ignored) {}
        throw new IllegalArgumentException("时间格式错误: " + timeStr);
    }

    private Object firstNonNull(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = value.toString();
        if (text == null || text.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    /** X轴均值 */
    private double meanX(List<double[]> data, double baseTime) {
        return data.stream().mapToDouble(d -> d[0] - baseTime).average().orElse(0);
    }

    /** X轴偏差平方和 */
    private double sumSqDevX(List<double[]> data, double baseTime) {
        double mx = meanX(data, baseTime);
        return data.stream().mapToDouble(d -> Math.pow(d[0] - baseTime - mx, 2)).sum();
    }

    /** 四舍五入保留2位小数 */
    private Double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /**
     * 内部预测结果容器
     */
    private static class PredictionResult {
        final double[] predictions;
        final double[] upperBounds;
        final double[] lowerBounds;

        PredictionResult(double[] predictions, double[] upperBounds, double[] lowerBounds) {
            this.predictions = predictions;
            this.upperBounds = upperBounds;
            this.lowerBounds = lowerBounds;
        }
    }
}
