package com.example.backend.dto;

import java.util.List;

public class PredictionResponse {

    private boolean success;
    private String message;
    private String algorithm;
    private String elementCode;
    private List<DataPoint> historyData;
    private List<DataPoint> forecastData;
    private List<WarningPoint> warnings;
    private PredictionMetrics metrics;

    public PredictionResponse() {}

    public PredictionResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    // ===== 内部类 =====

    public static class DataPoint {
        private String datetime;
        private Double value;
        private Double upper; // 置信上界
        private Double lower; // 置信下界

        public DataPoint() {}

        public DataPoint(String datetime, Double value) {
            this.datetime = datetime;
            this.value = value;
        }

        public DataPoint(String datetime, Double value, Double upper, Double lower) {
            this.datetime = datetime;
            this.value = value;
            this.upper = upper;
            this.lower = lower;
        }

        public String getDatetime() { return datetime; }
        public void setDatetime(String datetime) { this.datetime = datetime; }
        public Double getValue() { return value; }
        public void setValue(Double value) { this.value = value; }
        public Double getUpper() { return upper; }
        public void setUpper(Double upper) { this.upper = upper; }
        public Double getLower() { return lower; }
        public void setLower(Double lower) { this.lower = lower; }
    }

    public static class WarningPoint {
        private String datetime;
        private Double value;
        private Double threshold;
        private String type; // "high" 或 "low"

        public WarningPoint() {}

        public WarningPoint(String datetime, Double value, Double threshold, String type) {
            this.datetime = datetime;
            this.value = value;
            this.threshold = threshold;
            this.type = type;
        }

        public String getDatetime() { return datetime; }
        public void setDatetime(String datetime) { this.datetime = datetime; }
        public Double getValue() { return value; }
        public void setValue(Double value) { this.value = value; }
        public Double getThreshold() { return threshold; }
        public void setThreshold(Double threshold) { this.threshold = threshold; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    public static class PredictionMetrics {
        private Double mae;
        private Double rmse;
        private Double r2;

        public PredictionMetrics() {}

        public PredictionMetrics(Double mae, Double rmse, Double r2) {
            this.mae = mae;
            this.rmse = rmse;
            this.r2 = r2;
        }

        public Double getMae() { return mae; }
        public void setMae(Double mae) { this.mae = mae; }
        public Double getRmse() { return rmse; }
        public void setRmse(Double rmse) { this.rmse = rmse; }
        public Double getR2() { return r2; }
        public void setR2(Double r2) { this.r2 = r2; }
    }

    // ===== Getters & Setters =====

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

    public String getElementCode() { return elementCode; }
    public void setElementCode(String elementCode) { this.elementCode = elementCode; }

    public List<DataPoint> getHistoryData() { return historyData; }
    public void setHistoryData(List<DataPoint> historyData) { this.historyData = historyData; }

    public List<DataPoint> getForecastData() { return forecastData; }
    public void setForecastData(List<DataPoint> forecastData) { this.forecastData = forecastData; }

    public List<WarningPoint> getWarnings() { return warnings; }
    public void setWarnings(List<WarningPoint> warnings) { this.warnings = warnings; }

    public PredictionMetrics getMetrics() { return metrics; }
    public void setMetrics(PredictionMetrics metrics) { this.metrics = metrics; }
}
