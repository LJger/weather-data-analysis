package com.example.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 气象要素数据响应（用于GIS可视化）
 */
public class WeatherElementDataResponse {
    
    private String element;  // 要素类型：temperature, humidity, precipitation
    private LocalDateTime observationTime;
    private List<WeatherElementDataPoint> dataPoints;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private int totalPoints;
    
    public WeatherElementDataResponse() {
    }
    
    // Getters and Setters
    public String getElement() {
        return element;
    }
    
    public void setElement(String element) {
        this.element = element;
    }
    
    public LocalDateTime getObservationTime() {
        return observationTime;
    }
    
    public void setObservationTime(LocalDateTime observationTime) {
        this.observationTime = observationTime;
    }
    
    public List<WeatherElementDataPoint> getDataPoints() {
        return dataPoints;
    }
    
    public void setDataPoints(List<WeatherElementDataPoint> dataPoints) {
        this.dataPoints = dataPoints;
    }
    
    public BigDecimal getMinValue() {
        return minValue;
    }
    
    public void setMinValue(BigDecimal minValue) {
        this.minValue = minValue;
    }
    
    public BigDecimal getMaxValue() {
        return maxValue;
    }
    
    public void setMaxValue(BigDecimal maxValue) {
        this.maxValue = maxValue;
    }
    
    public int getTotalPoints() {
        return totalPoints;
    }
    
    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }
}
