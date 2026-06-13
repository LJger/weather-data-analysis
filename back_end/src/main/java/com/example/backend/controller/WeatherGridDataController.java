package com.example.backend.controller;

import com.example.backend.dto.WeatherGridDataResponse;
import com.example.backend.service.WeatherGridDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 气象网格化数据控制器
 * 用于GIS在线分析功能
 */
@RestController
@RequestMapping("/api/weather/grid-data")
public class WeatherGridDataController {

    private final WeatherGridDataService weatherGridDataService;

    public WeatherGridDataController(WeatherGridDataService weatherGridDataService) {
        this.weatherGridDataService = weatherGridDataService;
    }

    /**
     * 获取网格化气象数据
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param element 气象要素（temperature, precipitation, wind）
     * @param minLat 最小纬度
     * @param maxLat 最大纬度
     * @param minLon 最小经度
     * @param maxLon 最大经度
     * @return 网格化数据列表
     */
    @GetMapping
    public ResponseEntity<List<WeatherGridDataResponse>> getGridData(
            @RequestParam(required = false) OffsetDateTime startTime,
            @RequestParam(required = false) OffsetDateTime endTime,
            @RequestParam(required = false) String element,
            @RequestParam(required = false) Double minLat,
            @RequestParam(required = false) Double maxLat,
            @RequestParam(required = false) Double minLon,
            @RequestParam(required = false) Double maxLon
    ) {
        List<WeatherGridDataResponse> gridData = weatherGridDataService.getGridData(
                startTime, endTime, element, minLat, maxLat, minLon, maxLon
        );
        return ResponseEntity.ok(gridData);
    }

    /**
     * 根据经纬度查询特定位置的气象数据
     * 
     * @param lat 纬度
     * @param lon 经度
     * @param timestamp 时间戳
     * @return 该点的气象数据
     */
    @GetMapping("/point")
    public ResponseEntity<WeatherGridDataResponse.GridPoint> getPointData(
            @RequestParam Double lat,
            @RequestParam Double lon,
            @RequestParam OffsetDateTime timestamp
    ) {
        WeatherGridDataResponse.GridPoint point = weatherGridDataService.getPointData(lat, lon, timestamp);
        return ResponseEntity.ok(point);
    }
}
