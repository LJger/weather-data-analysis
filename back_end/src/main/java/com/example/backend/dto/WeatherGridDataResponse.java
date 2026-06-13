package com.example.backend.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 网格化气象数据响应DTO
 */
public class WeatherGridDataResponse {
    
    private OffsetDateTime timestamp;
    private List<GridPoint> points;
    
    public WeatherGridDataResponse() {
    }
    
    public WeatherGridDataResponse(OffsetDateTime timestamp, List<GridPoint> points) {
        this.timestamp = timestamp;
        this.points = points;
    }
    
    public OffsetDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public List<GridPoint> getPoints() {
        return points;
    }
    
    public void setPoints(List<GridPoint> points) {
        this.points = points;
    }
    
    /**
     * 网格点数据
     */
    public static class GridPoint {
        private Double lat;
        private Double lon;
        private Double temperature;
        private Double precipitation;
        private Double windSpeed;
        private Double windDirection;
        
        public GridPoint() {
        }
        
        public GridPoint(Double lat, Double lon) {
            this.lat = lat;
            this.lon = lon;
        }
        
        public Double getLat() {
            return lat;
        }
        
        public void setLat(Double lat) {
            this.lat = lat;
        }
        
        public Double getLon() {
            return lon;
        }
        
        public void setLon(Double lon) {
            this.lon = lon;
        }
        
        public Double getTemperature() {
            return temperature;
        }
        
        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }
        
        public Double getPrecipitation() {
            return precipitation;
        }
        
        public void setPrecipitation(Double precipitation) {
            this.precipitation = precipitation;
        }
        
        public Double getWindSpeed() {
            return windSpeed;
        }
        
        public void setWindSpeed(Double windSpeed) {
            this.windSpeed = windSpeed;
        }
        
        public Double getWindDirection() {
            return windDirection;
        }
        
        public void setWindDirection(Double windDirection) {
            this.windDirection = windDirection;
        }
    }
}
