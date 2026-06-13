package com.example.backend.service;

import com.example.backend.dto.WeatherElementDataPoint;
import com.example.backend.dto.WeatherElementDataResponse;
import com.example.backend.entity.HistoryWeatherData;
import com.example.backend.entity.WeatherStation;
import com.example.backend.mapper.HistoryWeatherDataMapper;
import com.example.backend.mapper.WeatherStationMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class HistoryWeatherDataService {

    private static final Logger log = LoggerFactory.getLogger(HistoryWeatherDataService.class);

    private final HistoryWeatherDataMapper historyWeatherDataMapper;
    private final WeatherStationMapper weatherStationMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /** Redis 缓存 key */
    private static final String CACHE_OBS_TIMES = "weather:history:obs-times";
    private static final String CACHE_LATEST_OBS = "weather:history:latest-obs";
    /** 观测时间列表缓存 1 小时 */
    private static final long OBS_CACHE_TTL_MINUTES = 60;

    public HistoryWeatherDataService(HistoryWeatherDataMapper historyWeatherDataMapper,
                                      WeatherStationMapper weatherStationMapper,
                                      StringRedisTemplate redisTemplate,
                                      ObjectMapper objectMapper) {
        this.historyWeatherDataMapper = historyWeatherDataMapper;
        this.weatherStationMapper = weatherStationMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 获取气象要素数据（用于GIS可视化）
     */
    public WeatherElementDataResponse getElementData(
            String element, 
            LocalDateTime observationTime) {
        
        // 如果未指定时间，使用最新时间
        if (observationTime == null) {
            observationTime = historyWeatherDataMapper.findLatestObservationTime();
        }

        if (observationTime == null) {
            WeatherElementDataResponse emptyResponse = new WeatherElementDataResponse();
            emptyResponse.setElement(element);
            emptyResponse.setObservationTime(null);
            emptyResponse.setDataPoints(List.of());
            emptyResponse.setMinValue(null);
            emptyResponse.setMaxValue(null);
            emptyResponse.setTotalPoints(0);
            return emptyResponse;
        }
        
        // 查询该时间的所有数据
        List<HistoryWeatherData> weatherDataList = 
            historyWeatherDataMapper.findByObservationTime(observationTime);
        if (weatherDataList == null) {
            weatherDataList = List.of();
        }
        
        // 获取所有站点信息
        List<WeatherStation> allStations = weatherStationMapper.findAll();
        if (allStations == null) {
            allStations = List.of();
        }
        Map<String, WeatherStation> stationMap = allStations.stream()
            .collect(Collectors.toMap(WeatherStation::getStationId, s -> s, (left, right) -> left));
        
        // 转换为数据点
        List<WeatherElementDataPoint> dataPoints = new ArrayList<>();
        BigDecimal minValue = null;
        BigDecimal maxValue = null;
        
        for (HistoryWeatherData data : weatherDataList) {
            WeatherStation station = stationMap.get(data.getStationId());
            if (station == null) {
                continue;  // 跳过没有站点信息的数据
            }
            
            BigDecimal value = getElementValue(data, element);
            if (value == null) {
                continue;  // 跳过空值
            }
            
            // 更新最大最小值
            if (minValue == null || value.compareTo(minValue) < 0) {
                minValue = value;
            }
            if (maxValue == null || value.compareTo(maxValue) > 0) {
                maxValue = value;
            }
            
            WeatherElementDataPoint point = new WeatherElementDataPoint(
                data.getStationId(),
                station.getStationName(),
                new BigDecimal(station.getLatitude()).divide(new BigDecimal("100")),
                new BigDecimal(station.getLongitude()).divide(new BigDecimal("100")),
                value
            );
            dataPoints.add(point);
        }
        
        // 构建响应
        WeatherElementDataResponse response = new WeatherElementDataResponse();
        response.setElement(element);
        response.setObservationTime(observationTime);
        response.setDataPoints(dataPoints);
        response.setMinValue(minValue);
        response.setMaxValue(maxValue);
        response.setTotalPoints(dataPoints.size());
        
        return response;
    }
    
    /**
     * 获取可用的观测时间列表（Redis 缓存 1 小时）
     */
    public List<LocalDateTime> getAvailableObservationTimes() {
        try {
            String cached = redisTemplate.opsForValue().get(CACHE_OBS_TIMES);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<List<LocalDateTime>>() {});
            }
        } catch (Exception e) {
            log.warn("Redis 读取观测时间缓存失败", e);
        }

        List<LocalDateTime> times = historyWeatherDataMapper.findDistinctObservationTimes();

        try {
            redisTemplate.opsForValue().set(CACHE_OBS_TIMES,
                    objectMapper.writeValueAsString(times), OBS_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis 写入观测时间缓存失败", e);
        }
        return times;
    }
    
    /**
     * 获取最新的观测时间（Redis 缓存 1 小时）
     */
    public LocalDateTime getLatestObservationTime() {
        try {
            String cached = redisTemplate.opsForValue().get(CACHE_LATEST_OBS);
            if (cached != null) {
                return objectMapper.readValue(cached, LocalDateTime.class);
            }
        } catch (Exception e) {
            log.warn("Redis 读取最新观测时间缓存失败", e);
        }

        LocalDateTime latest = historyWeatherDataMapper.findLatestObservationTime();

        if (latest != null) {
            try {
                redisTemplate.opsForValue().set(CACHE_LATEST_OBS,
                        objectMapper.writeValueAsString(latest), OBS_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.warn("Redis 写入最新观测时间缓存失败", e);
            }
        }
        return latest;
    }
    
    /**
     * 根据要素类型获取值
     */
    private BigDecimal getElementValue(HistoryWeatherData data, String element) {
        switch (element) {
            case "temperature":
                return data.getTemperature();
            case "humidity":
                return data.getRelativeHumidity();
            case "precipitation":
                return data.getPrecipitation3h();
            default:
                return null;
        }
    }
    
    /**
     * 获取指定站点的气象数据
     */
    public Map<String, Object> getStationData(String stationId, LocalDateTime observationTime) {
        // 如果未指定时间，使用最新时间
        if (observationTime == null) {
            observationTime = historyWeatherDataMapper.findLatestObservationTime();
        }

        if (observationTime == null) {
            Map<String, Object> empty = new java.util.HashMap<>();
            empty.put("temperature", null);
            empty.put("humidity", null);
            empty.put("precipitation", null);
            empty.put("observationTime", null);
            return empty;
        }
        
        // 查询该站点在指定时间的数据
        List<HistoryWeatherData> dataList = 
            historyWeatherDataMapper.findByObservationTime(observationTime);
        if (dataList == null) {
            dataList = List.of();
        }
        
        HistoryWeatherData stationData = dataList.stream()
            .filter(d -> d.getStationId().equals(stationId))
            .findFirst()
            .orElse(null);
        
        // 构建响应
        Map<String, Object> result = new java.util.HashMap<>();
        
        if (stationData != null) {
            result.put("temperature", stationData.getTemperature());
            result.put("humidity", stationData.getRelativeHumidity());
            result.put("precipitation", stationData.getPrecipitation3h());
            result.put("observationTime", stationData.getObservationTime());
        } else {
            result.put("temperature", null);
            result.put("humidity", null);
            result.put("precipitation", null);
            result.put("observationTime", observationTime);
        }
        
        return result;
    }
}
