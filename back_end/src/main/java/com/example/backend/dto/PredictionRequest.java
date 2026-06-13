package com.example.backend.dto;

import java.util.List;

public class PredictionRequest {

    private Long userId;
    private String elementCode;
    private List<Integer> stationIds;
    private List<Long> taskIds;
    private String algorithm; // linear_regression / moving_average / exponential_smoothing
    private String historyStartTime;
    private String historyEndTime;
    private Integer forecastSteps; // 预测步数（每步=1个粒度单位，默认8）
    private String granularity;   // hour / day，默认 hour
    private Integer movingWindowSize; // 移动平均窗口（默认5）
    private Double smoothingAlpha;    // 指数平滑系数（默认0.3）
    private Double warningThresholdHigh; // 预警上阈值
    private Double warningThresholdLow;  // 预警下阈值

    public PredictionRequest() {}

    // ===== Getters & Setters =====

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getElementCode() { return elementCode; }
    public void setElementCode(String elementCode) { this.elementCode = elementCode; }

    public List<Integer> getStationIds() { return stationIds; }
    public void setStationIds(List<Integer> stationIds) { this.stationIds = stationIds; }

    public List<Long> getTaskIds() { return taskIds; }
    public void setTaskIds(List<Long> taskIds) { this.taskIds = taskIds; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

    public String getHistoryStartTime() { return historyStartTime; }
    public void setHistoryStartTime(String historyStartTime) { this.historyStartTime = historyStartTime; }

    public String getHistoryEndTime() { return historyEndTime; }
    public void setHistoryEndTime(String historyEndTime) { this.historyEndTime = historyEndTime; }

    public Integer getForecastSteps() { return forecastSteps; }
    public void setForecastSteps(Integer forecastSteps) { this.forecastSteps = forecastSteps; }

    public String getGranularity() { return granularity; }
    public void setGranularity(String granularity) { this.granularity = granularity; }

    public Integer getMovingWindowSize() { return movingWindowSize; }
    public void setMovingWindowSize(Integer movingWindowSize) { this.movingWindowSize = movingWindowSize; }

    public Double getSmoothingAlpha() { return smoothingAlpha; }
    public void setSmoothingAlpha(Double smoothingAlpha) { this.smoothingAlpha = smoothingAlpha; }

    public Double getWarningThresholdHigh() { return warningThresholdHigh; }
    public void setWarningThresholdHigh(Double warningThresholdHigh) { this.warningThresholdHigh = warningThresholdHigh; }

    public Double getWarningThresholdLow() { return warningThresholdLow; }
    public void setWarningThresholdLow(Double warningThresholdLow) { this.warningThresholdLow = warningThresholdLow; }
}
