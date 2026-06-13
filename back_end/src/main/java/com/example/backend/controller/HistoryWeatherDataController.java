package com.example.backend.controller;

import com.example.backend.dto.WeatherElementDataResponse;
import com.example.backend.service.HistoryWeatherDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/weather")
public class HistoryWeatherDataController {
    
    @Autowired
    private HistoryWeatherDataService historyWeatherDataService;
    
    /**
     * 获取气象要素数据（用于GIS可视化）
     * @param element 要素类型: temperature, humidity, precipitation
     * @param observationTime 观测时间（可选，不传则使用最新时间）
     * @return 气象要素数据点列表
     */
    @GetMapping("/element-data")
    public ResponseEntity<WeatherElementDataResponse> getElementData(
            @RequestParam String element,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
            LocalDateTime observationTime) {
        
        WeatherElementDataResponse response = historyWeatherDataService
            .getElementData(element, observationTime);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取可用的观测时间列表
     * @return 观测时间列表（降序）
     */
    @GetMapping("/observation-times")
    public ResponseEntity<List<LocalDateTime>> getObservationTimes() {
        List<LocalDateTime> times = historyWeatherDataService.getAvailableObservationTimes();
        return ResponseEntity.ok(times);
    }
    
    /**
     * 获取最新的观测时间
     * @return 最新观测时间
     */
    @GetMapping("/latest-observation-time")
    public ResponseEntity<LocalDateTime> getLatestObservationTime() {
        LocalDateTime latestTime = historyWeatherDataService.getLatestObservationTime();
        return ResponseEntity.ok(latestTime);
    }
    
    /**
     * 获取指定站点的气象数据
     * @param stationId 站点ID
     * @param observationTime 观测时间（可选，不传则使用最新时间）
     * @return 站点的气象数据（温度、湿度、降水量）
     */
    @GetMapping("/station/{stationId}/data")
    public ResponseEntity<?> getStationData(
            @PathVariable String stationId,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
            LocalDateTime observationTime) {
        
        var data = historyWeatherDataService.getStationData(stationId, observationTime);
        return ResponseEntity.ok(data);
    }
}
