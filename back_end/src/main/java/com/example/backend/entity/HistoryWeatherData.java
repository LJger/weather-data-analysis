package com.example.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 历史气象观测数据实体 对应表 gra_project.history_weather_data
 */
public class HistoryWeatherData {

    private String stationId;

    private LocalDateTime observationTime;

    private BigDecimal temperature;

    private BigDecimal relativeHumidity;

    private BigDecimal precipitation3h;
    
    // Getters and Setters
    public String getStationId() {
        return stationId;
    }
    
    public void setStationId(String stationId) {
        this.stationId = stationId;
    }
    
    public LocalDateTime getObservationTime() {
        return observationTime;
    }
    
    public void setObservationTime(LocalDateTime observationTime) {
        this.observationTime = observationTime;
    }
    
    public BigDecimal getTemperature() {
        return temperature;
    }
    
    public void setTemperature(BigDecimal temperature) {
        this.temperature = temperature;
    }
    
    public BigDecimal getRelativeHumidity() {
        return relativeHumidity;
    }
    
    public void setRelativeHumidity(BigDecimal relativeHumidity) {
        this.relativeHumidity = relativeHumidity;
    }
    
    public BigDecimal getPrecipitation3h() {
        return precipitation3h;
    }
    
    public void setPrecipitation3h(BigDecimal precipitation3h) {
        this.precipitation3h = precipitation3h;
    }
}
