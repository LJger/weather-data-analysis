package com.example.backend.service;

import com.example.backend.dto.WeatherElementDataResponse;
import com.example.backend.dto.WeatherStationResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 将 GIS 页面使用的气象展示数据归档到 HDFS。
 */
@Service
public class GisWeatherHdfsArchiveService {

    private static final Set<String> SUPPORTED_ELEMENTS = Set.of("temperature", "humidity", "precipitation");
    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final WeatherStationService weatherStationService;
    private final HistoryWeatherDataService historyWeatherDataService;
    private final HdfsService hdfsService;
    private final ObjectMapper objectMapper;

    public GisWeatherHdfsArchiveService(WeatherStationService weatherStationService,
                                        HistoryWeatherDataService historyWeatherDataService,
                                        HdfsService hdfsService,
                                        ObjectMapper objectMapper) {
        this.weatherStationService = weatherStationService;
        this.historyWeatherDataService = historyWeatherDataService;
        this.hdfsService = hdfsService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> archiveSnapshot(String element, LocalDateTime observationTime) {
        String normalizedElement = normalizeElement(element);
        List<WeatherStationResponse> stations = weatherStationService.listAllStations();
        WeatherElementDataResponse elementData = historyWeatherDataService.getElementData(normalizedElement, observationTime);
        LocalDateTime resolvedObservationTime = elementData.getObservationTime();
        String fileTime = (resolvedObservationTime == null ? LocalDateTime.now() : resolvedObservationTime)
                .format(FILE_TIME_FORMATTER);
        String targetPath = "/raw-data/gis/weather-gis-" + normalizedElement + "-" + fileTime + ".json";

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("snapshotType", "GIS_WEATHER_DISPLAY");
        snapshot.put("source", "weather-map-view");
        snapshot.put("element", normalizedElement);
        snapshot.put("observationTime", resolvedObservationTime);
        snapshot.put("generatedAt", LocalDateTime.now());
        snapshot.put("stations", stations);
        snapshot.put("elementData", elementData);

        String hdfsPath = hdfsService.writeText(toJson(snapshot), targetPath);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "GIS 气象展示数据已写入 HDFS");
        result.put("element", normalizedElement);
        result.put("observationTime", resolvedObservationTime);
        result.put("stationCount", stations.size());
        result.put("dataPointCount", elementData.getTotalPoints());
        result.put("hdfsPath", hdfsPath);
        return result;
    }

    private String normalizeElement(String element) {
        String normalized = (element == null || element.isBlank())
                ? "temperature"
                : element.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_ELEMENTS.contains(normalized)) {
            throw new IllegalArgumentException("不支持的 GIS 气象要素: " + element);
        }
        return normalized;
    }

    private String toJson(Map<String, Object> snapshot) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化 GIS 气象展示数据失败: " + e.getMessage(), e);
        }
    }
}
