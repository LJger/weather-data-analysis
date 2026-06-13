package com.example.backend.service;

import com.example.backend.dto.MeteorDataCorrelationMatrixCell;
import com.example.backend.dto.MeteorDataCorrelationRequest;
import com.example.backend.dto.MeteorDataCorrelationResponse;
import com.example.backend.dto.MeteorDataFilterOptionsResponse;
import com.example.backend.dto.MeteorDataRecordResponse;
import com.example.backend.dto.MeteorDataSpatialPoint;
import com.example.backend.dto.MeteorDataSpatialProvinceStat;
import com.example.backend.dto.MeteorDataSpatialResponse;
import com.example.backend.dto.MeteorDataSpatialStationPoint;
import com.example.backend.dto.MeteorDataStationOption;
import com.example.backend.dto.MeteorDataSummaryResponse;
import com.example.backend.dto.MeteorDataTablePageResponse;
import com.example.backend.dto.MeteorDataTableRecordResponse;
import com.example.backend.dto.MeteorDataTaskOption;
import com.example.backend.dto.MeteorDataTimeSeriesPoint;
import com.example.backend.entity.MeteorData;
import com.example.backend.entity.WeatherStation;
import com.example.backend.mapper.CollectionTaskMapper;
import com.example.backend.mapper.MeteorDataMapper;
import com.example.backend.mapper.WeatherStationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class MeteorDataService {

    private static final int DEFAULT_TIME_RANGE_HOURS = 24;
    private static final Map<String, Range> ELEMENT_SAFE_RANGE = buildElementRanges();

    private final MeteorDataMapper meteorDataMapper;
    private final CollectionTaskMapper collectionTaskMapper;
    private final WeatherStationMapper weatherStationMapper;

    public MeteorDataService(MeteorDataMapper meteorDataMapper,
                             CollectionTaskMapper collectionTaskMapper,
                             WeatherStationMapper weatherStationMapper) {
        this.meteorDataMapper = meteorDataMapper;
        this.collectionTaskMapper = collectionTaskMapper;
        this.weatherStationMapper = weatherStationMapper;
    }

    @Transactional(readOnly = true)
    public MeteorDataSummaryResponse loadSummary(Integer userId) {
        long total = meteorDataMapper.countByUserId(userId);
        long taskCount = meteorDataMapper.countDistinctTaskIdByUserId(userId);
        long stationCount = meteorDataMapper.countDistinctStationIdByUserId(userId);
        long elementCount = meteorDataMapper.countDistinctElementCodeByUserId(userId);
        long anomalyCount = estimateAnomalyCount(userId);
        MeteorData latest = meteorDataMapper.findTop1ByUserIdOrderByDatetimeDesc(userId);
        OffsetDateTime lastUpdated = latest == null ? null : latest.getDatetime();

        return new MeteorDataSummaryResponse(
                total,
                taskCount,
                stationCount,
                elementCount,
                anomalyCount,
                lastUpdated
        );
    }

    @Transactional(readOnly = true)
    public List<MeteorDataTimeSeriesPoint> loadTimeSeries(Integer userId,
                                                          String elementCode,
                                                          OffsetDateTime startTime,
                                                          OffsetDateTime endTime,
                                                          String granularity,
                                                          List<Integer> stationIds,
                                                          List<Long> taskIds) {
        if (elementCode == null || elementCode.isBlank()) {
            throw new IllegalArgumentException("elementCode 不能为空");
        }
        OffsetDateTime end = Optional.ofNullable(endTime).orElse(OffsetDateTime.now());
        OffsetDateTime start = Optional.ofNullable(startTime).orElse(end.minusHours(DEFAULT_TIME_RANGE_HOURS));

        String normalizedGranularity = normalizeGranularity(granularity);

        List<Map<String, Object>> rows = meteorDataMapper.aggregateTimeSeries(
                userId,
                elementCode,
                start,
                end,
                normalizedGranularity,
                toIntegerArray(stationIds),
                toLongArray(taskIds)
        );

        List<MeteorDataTimeSeriesPoint> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            OffsetDateTime bucket = toOffsetDateTime(row.get("bucket"));
            BigDecimal avg = toBigDecimal(row.get("avg_value"));
            BigDecimal min = toBigDecimal(row.get("min_value"));
            BigDecimal max = toBigDecimal(row.get("max_value"));
            result.add(new MeteorDataTimeSeriesPoint(bucket, avg, min, max));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<MeteorDataRecordResponse> loadLatestRecords(Integer userId, Integer limit, String elementCode) {
        int size = Optional.ofNullable(limit).orElse(50);
        size = Math.min(Math.max(size, 1), 500);

        List<MeteorData> records;
        if (elementCode != null && !elementCode.isBlank()) {
            records = meteorDataMapper.findTopNByUserIdAndElementCodeOrderByDatetimeDesc(userId, elementCode, 200);
        } else {
            records = meteorDataMapper.findTopNByUserIdOrderByDatetimeDesc(userId, 200);
        }

        records = records.stream()
                .limit(size)
                .toList();

        return records.stream()
                .map(m -> new MeteorDataRecordResponse(
                        m.getId(),
                        m.getUserId(),
                        m.getTaskId(),
                        m.getStationId(),
                        m.getElementCode(),
                        m.getDatetime(),
                        m.getValue()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> listElementCodes(Integer userId) {
        return meteorDataMapper.findDistinctElementCodesByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<MeteorDataSpatialPoint> loadSpatialAverages(Integer userId,
                                                            String elementCode,
                                                            OffsetDateTime startTime,
                                                            OffsetDateTime endTime) {
        OffsetDateTime end = Optional.ofNullable(endTime).orElse(OffsetDateTime.now());
        OffsetDateTime start = Optional.ofNullable(startTime).orElse(end.minusHours(DEFAULT_TIME_RANGE_HOURS));

        List<Map<String, Object>> rows = meteorDataMapper.aggregateSpatialByStation(userId, elementCode, start, end);
        List<MeteorDataSpatialPoint> points = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            Integer stationId = toInteger(row.get("stationId"));
            BigDecimal avgValue = toBigDecimal(row.get("avgValue"));
            if (stationId != null && avgValue != null) {
                points.add(new MeteorDataSpatialPoint(stationId, avgValue));
            }
        }
        return points;
    }

    @Transactional(readOnly = true)
    public MeteorDataSpatialResponse loadSpatialMap(Integer userId,
                                                    String elementCode,
                                                    OffsetDateTime startTime,
                                                    OffsetDateTime endTime,
                                                    String province) {
        if (elementCode == null || elementCode.isBlank()) {
            throw new IllegalArgumentException("elementCode 不能为空");
        }
        OffsetDateTime end = Optional.ofNullable(endTime).orElse(OffsetDateTime.now());
        OffsetDateTime start = Optional.ofNullable(startTime).orElse(end.minusHours(DEFAULT_TIME_RANGE_HOURS));
        String provinceFilter = (province == null || province.isBlank()) ? null : province.trim();

        List<Map<String, Object>> provinceRows = meteorDataMapper.aggregateProvinceSpatialStats(userId, elementCode, start, end);
        List<MeteorDataSpatialProvinceStat> provinceStats = provinceRows.stream()
                .map(row -> new MeteorDataSpatialProvinceStat(
                        Objects.toString(row.get("province"), "未知"),
                        toBigDecimal(row.get("avg_value")),
                        toBigDecimal(row.get("min_value")),
                        toBigDecimal(row.get("max_value")),
                Optional.ofNullable(toLong(row.get("station_count")))
                    .orElse(Optional.ofNullable(toLong(row.get("count"))).orElse(0L))
                ))
                .toList();

        List<Map<String, Object>> stationRows = meteorDataMapper.aggregateStationSpatialStats(userId, elementCode, start, end, provinceFilter);
        List<MeteorDataSpatialStationPoint> stationPoints = stationRows.stream()
                .map(row -> new MeteorDataSpatialStationPoint(
                Optional.ofNullable(toInteger(row.get("station_id")))
                    .orElse(toInteger(row.get("stationId"))),
                        Objects.toString(row.get("station_name"), "-"),
                        Objects.toString(row.get("province"), "未知"),
                normalizeCoordinate(Optional.ofNullable(toInteger(row.get("latitude")))
                    .orElse(toInteger(row.get("lat")))),
                normalizeCoordinate(Optional.ofNullable(toInteger(row.get("longitude")))
                    .orElse(toInteger(row.get("lon")))),
                        toBigDecimal(row.get("avg_value"))
                ))
                .filter(point -> point.latitude() != null && point.longitude() != null && point.stationId() != null)
                .toList();

        List<String> availableProvinces = weatherStationMapper.findDistinctProvinces();
        return new MeteorDataSpatialResponse(
                provinceStats,
                stationPoints,
                availableProvinces,
                provinceFilter,
                start,
                end,
                elementCode
        );
    }

    @Transactional(readOnly = true)
    public MeteorDataFilterOptionsResponse loadFilterOptions(Integer userId) {
        List<MeteorDataTaskOption> taskOptions = collectionTaskMapper.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(task -> new MeteorDataTaskOption(task.getTaskId(), task.getTaskName()))
                .toList();

        List<Integer> stationIds = meteorDataMapper.findDistinctStationIdsByUserId(userId);
        Map<Integer, String> stationNameMap = resolveStationNames(stationIds);
        List<MeteorDataStationOption> stationOptions = stationIds.stream()
                .map(id -> new MeteorDataStationOption(id, stationNameMap.getOrDefault(id, "站点" + id)))
                .toList();

        List<String> elements = meteorDataMapper.findDistinctElementCodesByUserId(userId);
        return new MeteorDataFilterOptionsResponse(taskOptions, stationOptions, elements);
    }

    @Transactional(readOnly = true)
    public MeteorDataCorrelationResponse calculateCorrelation(MeteorDataCorrelationRequest request) {
        if (request == null || request.userId() == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        List<String> elements = Optional.ofNullable(request.elementCodes())
                .filter(list -> !list.isEmpty())
                .orElseThrow(() -> new IllegalArgumentException("请至少选择两个要素"));
        if (elements.size() < 2) {
            throw new IllegalArgumentException("相关性分析需要至少两个要素");
        }

        OffsetDateTime end = Optional.ofNullable(request.endTime()).orElse(OffsetDateTime.now());
        OffsetDateTime start = Optional.ofNullable(request.startTime()).orElse(end.minusHours(DEFAULT_TIME_RANGE_HOURS));
        String granularity = normalizeGranularity(request.granularity());

        List<Map<String, Object>> rows = meteorDataMapper.aggregateCorrelationSeries(
                request.userId(),
                elements.toArray(String[]::new),
                start,
                end,
                granularity,
                toIntegerArray(request.stationIds()),
                toLongArray(request.taskIds())
        );

        Map<String, Map<OffsetDateTime, BigDecimal>> seriesByElement = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String element = row.get("element_code") == null ? null : row.get("element_code").toString();
            OffsetDateTime bucket = toOffsetDateTime(row.get("bucket"));
            BigDecimal avgValue = toBigDecimal(row.get("avg_value"));
            if (element == null || bucket == null || avgValue == null) {
                continue;
            }
            seriesByElement.computeIfAbsent(element, key -> new TreeMap<>())
                    .put(bucket, avgValue);
        }

        List<MeteorDataCorrelationMatrixCell> matrix = new ArrayList<>();
        for (String x : elements) {
            Map<OffsetDateTime, BigDecimal> xSeries = seriesByElement.getOrDefault(x, Map.of());
            for (String y : elements) {
                Map<OffsetDateTime, BigDecimal> ySeries = seriesByElement.getOrDefault(y, Map.of());
                CorrelationComputationResult result = computeCorrelation(xSeries, ySeries);
                matrix.add(new MeteorDataCorrelationMatrixCell(x, y, result.coefficient(), result.effectiveSamples()));
            }
        }

        return new MeteorDataCorrelationResponse(elements, matrix, granularity);
    }

    @Transactional(readOnly = true)
    public MeteorDataTablePageResponse queryRecords(Integer userId,
                                                    List<Long> taskIds,
                                                    List<Integer> stationIds,
                                                    List<String> elementCodes,
                                                    OffsetDateTime startTime,
                                                    OffsetDateTime endTime,
                                                    int page,
                                                    int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 500));
        int offset = safePage * safeSize;

        List<MeteorData> content = meteorDataMapper.searchRecords(
                userId,
                taskIds,
                stationIds,
                elementCodes,
                startTime,
                endTime,
                offset,
                safeSize
        );
        long totalElements = meteorDataMapper.countByFilters(
                userId,
                taskIds,
                stationIds,
                elementCodes,
                startTime,
                endTime
        );

        Map<Long, String> taskNameMap = collectionTaskMapper.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .collect(Collectors.toMap(
                        task -> task.getTaskId(),
                        task -> task.getTaskName(),
                        (left, right) -> left
                ));

        List<Integer> stationIdList = content
                .stream()
                .map(MeteorData::getStationId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Integer, String> stationNameMap = resolveStationNames(stationIdList);

        List<MeteorDataTableRecordResponse> records = content.stream()
                .map(record -> new MeteorDataTableRecordResponse(
                        record.getId(),
                        record.getTaskId(),
                        taskNameMap.getOrDefault(record.getTaskId(), "-"),
                        record.getStationId(),
                        stationNameMap.getOrDefault(record.getStationId(), "站点" + record.getStationId()),
                        record.getElementCode(),
                        record.getDatetime(),
                        record.getValue()
                ))
                .toList();

        return new MeteorDataTablePageResponse(records, totalElements, safePage, safeSize);
    }

    @Transactional(readOnly = true)
    public long countRecordsByFilters(Integer userId,
                                      List<Long> taskIds,
                                      List<Integer> stationIds,
                                      List<String> elementCodes,
                                      OffsetDateTime startTime,
                                      OffsetDateTime endTime) {
        return meteorDataMapper.countByFilters(userId, taskIds, stationIds, elementCodes, startTime, endTime);
    }

    @Transactional(readOnly = true)
    public List<MeteorDataTableRecordResponse> queryRecordsForExport(Integer userId,
                                                                     List<Long> taskIds,
                                                                     List<Integer> stationIds,
                                                                     List<String> elementCodes,
                                                                     OffsetDateTime startTime,
                                                                     OffsetDateTime endTime,
                                                                     int limit) {
        int safeLimit = Math.max(1, limit);
        List<MeteorData> content = meteorDataMapper.searchRecords(
                userId,
                taskIds,
                stationIds,
                elementCodes,
                startTime,
                endTime,
                0,
                safeLimit
        );

        Map<Long, String> taskNameMap = collectionTaskMapper.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .collect(Collectors.toMap(
                        task -> task.getTaskId(),
                        task -> task.getTaskName(),
                        (left, right) -> left
                ));

        List<Integer> stationIdList = content
                .stream()
                .map(MeteorData::getStationId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Integer, String> stationNameMap = resolveStationNames(stationIdList);

        return content.stream()
                .map(record -> new MeteorDataTableRecordResponse(
                        record.getId(),
                        record.getTaskId(),
                        taskNameMap.getOrDefault(record.getTaskId(), "-"),
                        record.getStationId(),
                        stationNameMap.getOrDefault(record.getStationId(), "绔欑偣" + record.getStationId()),
                        record.getElementCode(),
                        record.getDatetime(),
                        record.getValue()
                ))
                .toList();
    }

    private long estimateAnomalyCount(Integer userId) {
        List<MeteorData> recent = meteorDataMapper.findTopNByUserIdOrderByDatetimeDesc(userId, 1000);
        return recent.stream()
                .filter(this::isAnomaly)
                .count();
    }

    private boolean isAnomaly(MeteorData meteorData) {
        Range range = ELEMENT_SAFE_RANGE.get(meteorData.getElementCode());
        if (range == null || meteorData.getValue() == null) {
            return false;
        }
        BigDecimal value = meteorData.getValue();
        return value.compareTo(range.min()) < 0 || value.compareTo(range.max()) > 0;
    }

    private static Map<String, Range> buildElementRanges() {
        Map<String, Range> ranges = new HashMap<>();
        ranges.put("TEM", new Range(BigDecimal.valueOf(-40), BigDecimal.valueOf(50)));
        ranges.put("RHU", new Range(BigDecimal.ZERO, BigDecimal.valueOf(100)));
        ranges.put("PRS", new Range(BigDecimal.valueOf(850), BigDecimal.valueOf(1080)));
        ranges.put("PRS_Sea", new Range(BigDecimal.valueOf(850), BigDecimal.valueOf(1080)));
        ranges.put("WIN_S_MAX", new Range(BigDecimal.ZERO, BigDecimal.valueOf(60)));
        ranges.put("PRE_3h", new Range(BigDecimal.ZERO, BigDecimal.valueOf(200)));
        return ranges;
    }

    private String normalizeGranularity(String granularity) {
        if (granularity == null) {
            return "hour";
        }
        return switch (granularity.toLowerCase(Locale.ROOT)) {
            case "hour", "day", "week", "month" -> granularity.toLowerCase(Locale.ROOT);
            default -> "hour";
        };
    }

    private Integer[] toIntegerArray(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return new Integer[0];
        }
        return values.toArray(Integer[]::new);
    }

    private Long[] toLongArray(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return new Long[0];
        }
        return values.toArray(Long[]::new);
    }

    private Map<Integer, String> resolveStationNames(List<Integer> stationIds) {
        if (stationIds == null || stationIds.isEmpty()) {
            return Map.of();
        }
        List<String> idStrings = stationIds.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .toList();
        List<WeatherStation> stations = weatherStationMapper.findAllByIds(idStrings);
        Map<Integer, String> mapping = new HashMap<>();
        for (WeatherStation station : stations) {
            try {
                Integer key = Integer.valueOf(station.getStationId());
                mapping.put(key, station.getStationName());
            } catch (NumberFormatException ignored) {
            }
        }
        return mapping;
    }

    private CorrelationComputationResult computeCorrelation(Map<OffsetDateTime, BigDecimal> xSeries,
                                                            Map<OffsetDateTime, BigDecimal> ySeries) {
        if (xSeries.isEmpty() || ySeries.isEmpty()) {
            return new CorrelationComputationResult(Double.NaN, 0);
        }
        Set<OffsetDateTime> overlap = new HashSet<>(xSeries.keySet());
        overlap.retainAll(ySeries.keySet());
        if (overlap.size() < 2) {
            return new CorrelationComputationResult(Double.NaN, overlap.size());
        }

        double[] xs = new double[overlap.size()];
        double[] ys = new double[overlap.size()];
        int index = 0;
        for (OffsetDateTime bucket : overlap) {
            xs[index] = xSeries.get(bucket).doubleValue();
            ys[index] = ySeries.get(bucket).doubleValue();
            index++;
        }

        double meanX = mean(xs);
        double meanY = mean(ys);
        double numerator = 0;
        double denominatorLeft = 0;
        double denominatorRight = 0;
        for (int i = 0; i < xs.length; i++) {
            double diffX = xs[i] - meanX;
            double diffY = ys[i] - meanY;
            numerator += diffX * diffY;
            denominatorLeft += diffX * diffX;
            denominatorRight += diffY * diffY;
        }
        double denominator = Math.sqrt(denominatorLeft * denominatorRight);
        double coefficient = denominator == 0 ? Double.NaN : numerator / denominator;
        return new CorrelationComputationResult(coefficient, overlap.size());
    }

    private double mean(double[] values) {
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        return values.length == 0 ? 0 : sum / values.length;
    }

    private OffsetDateTime toOffsetDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime;
        }
        if (value instanceof java.time.Instant instant) {
            return instant.atOffset(ZoneOffset.UTC);
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant().atOffset(ZoneOffset.UTC);
        }
        throw new IllegalArgumentException("无法转换为 OffsetDateTime: " + value.getClass());
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (value instanceof String str) {
            String trimmed = str.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            try {
                return new BigDecimal(trimmed);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        throw new IllegalArgumentException("无法转换为 BigDecimal: " + value.getClass());
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str) {
            String trimmed = str.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            try {
                return Integer.valueOf(trimmed);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String str) {
            String trimmed = str.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            try {
                return Long.valueOf(trimmed);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private Double normalizeCoordinate(Integer raw) {
        if (raw == null) {
            return null;
        }
        double asDouble = raw.doubleValue();
        if (Math.abs(asDouble) > 180) {
            return asDouble / 100.0;
        }
        return asDouble;
    }

    private record Range(BigDecimal min, BigDecimal max) {
    }

    private record CorrelationComputationResult(double coefficient, int effectiveSamples) {
    }

    /**
     * 按要素分组统计用户的数据量分布
     */
    public List<Map<String, Object>> loadElementDistribution(Integer userId) {
        List<Map<String, Object>> rows = meteorDataMapper.countGroupByElementCode(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new HashMap<>();
            Object elementCode = row.get("elementCode");
            if (elementCode == null) {
                elementCode = row.get("element_code");
            }
            item.put("elementCode", elementCode);
            item.put("count", Optional.ofNullable(toLong(row.get("count"))).orElse(0L));
            result.add(item);
        }
        return result;
    }
}
