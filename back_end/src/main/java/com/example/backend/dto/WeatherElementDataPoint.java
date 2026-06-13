package com.example.backend.dto;

import java.math.BigDecimal;

/**
 * 气象要素数据点（用于地图可视化）
 */
public class WeatherElementDataPoint {
    
    private String stationId;
    private String stationName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal value;  // 要素值（温度/湿度/降水等）
    
    public WeatherElementDataPoint() {
    }
    
    public WeatherElementDataPoint(String stationId, String stationName, 
                                   BigDecimal latitude, BigDecimal longitude, 
                                   BigDecimal value) {
        this.stationId = stationId;
        this.stationName = stationName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.value = value;
    }
    
    // Getters and Setters
    public String getStationId() {
        return stationId;
    }
    
    public void setStationId(String stationId) {
        this.stationId = stationId;
    }
    
    public String getStationName() {
        return stationName;
    }
    
    public void setStationName(String stationName) {
        this.stationName = stationName;
    }
    
    public BigDecimal getLatitude() {
        return latitude;
    }
    
    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }
    
    public BigDecimal getLongitude() {
        return longitude;
    }
    
    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }
    
    public BigDecimal getValue() {
        return value;
    }
    
    public void setValue(BigDecimal value) {
        this.value = value;
    }
}
