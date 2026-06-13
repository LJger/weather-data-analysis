package com.example.backend.entity;

import java.math.BigDecimal;

/**
 * 气象站信息实体 对应表 gra_project.weather_stations
 */
public class WeatherStation {

    private String province;

    private String stationId;

    private String stationName;

    private Integer latitude;

    private Integer longitude;

    private BigDecimal pressureSensorAltitude;

    private BigDecimal observationAltitude;

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

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

    public Integer getLatitude() {
        return latitude;
    }

    public void setLatitude(Integer latitude) {
        this.latitude = latitude;
    }

    public Integer getLongitude() {
        return longitude;
    }

    public void setLongitude(Integer longitude) {
        this.longitude = longitude;
    }

    public BigDecimal getPressureSensorAltitude() {
        return pressureSensorAltitude;
    }

    public void setPressureSensorAltitude(BigDecimal pressureSensorAltitude) {
        this.pressureSensorAltitude = pressureSensorAltitude;
    }

    public BigDecimal getObservationAltitude() {
        return observationAltitude;
    }

    public void setObservationAltitude(BigDecimal observationAltitude) {
        this.observationAltitude = observationAltitude;
    }
}

