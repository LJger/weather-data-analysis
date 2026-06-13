package com.example.backend.service;

import com.example.backend.entity.HistoryWeatherData;
import com.example.backend.entity.WeatherStation;
import com.example.backend.mapper.HistoryWeatherDataMapper;
import com.example.backend.mapper.WeatherStationMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将 GIS 分析相关数据库表全量导出到 HDFS。
 */
@Service
public class GisWeatherTableHdfsExportService {

    private static final DateTimeFormatter BATCH_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final WeatherStationMapper weatherStationMapper;
    private final HistoryWeatherDataMapper historyWeatherDataMapper;
    private final HdfsService hdfsService;
    private final ObjectMapper objectMapper;

    public GisWeatherTableHdfsExportService(WeatherStationMapper weatherStationMapper,
                                            HistoryWeatherDataMapper historyWeatherDataMapper,
                                            HdfsService hdfsService,
                                            ObjectMapper objectMapper) {
        this.weatherStationMapper = weatherStationMapper;
        this.historyWeatherDataMapper = historyWeatherDataMapper;
        this.hdfsService = hdfsService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> exportAllTables() {
        LocalDateTime exportedAt = LocalDateTime.now();
        String batchPath = "/raw-data/gis/tables/export-" + exportedAt.format(BATCH_TIME_FORMATTER);

        List<WeatherStation> stations = weatherStationMapper.findAll();
        List<HistoryWeatherData> historyWeatherData = historyWeatherDataMapper.findAll();

        String stationsPath = writeTable(batchPath, "weather_stations", stations);
        String historyPath = writeTable(batchPath, "history_weather_data", historyWeatherData);

        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("exportType", "GIS_WEATHER_TABLES_FULL");
        manifest.put("exportedAt", exportedAt);
        manifest.put("batchPath", batchPath);
        manifest.put("tables", List.of(
                tableManifest("gra_project.weather_stations", stations.size(), stationsPath),
                tableManifest("gra_project.history_weather_data", historyWeatherData.size(), historyPath)
        ));
        String manifestPath = hdfsService.writeText(toJson(manifest), batchPath + "/manifest.json");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "GIS 气象分析相关数据库表已全量写入 HDFS");
        result.put("batchPath", batchPath);
        result.put("weatherStations", stations.size());
        result.put("historyWeatherData", historyWeatherData.size());
        result.put("files", Map.of(
                "weatherStations", stationsPath,
                "historyWeatherData", historyPath,
                "manifest", manifestPath
        ));
        return result;
    }

    private String writeTable(String batchPath, String tableName, Object rows) {
        Map<String, Object> tableExport = new LinkedHashMap<>();
        tableExport.put("table", "gra_project." + tableName);
        tableExport.put("rows", rows);
        return hdfsService.writeText(toJson(tableExport), batchPath + "/" + tableName + ".json");
    }

    private Map<String, Object> tableManifest(String tableName, int rowCount, String hdfsPath) {
        Map<String, Object> table = new LinkedHashMap<>();
        table.put("table", tableName);
        table.put("rowCount", rowCount);
        table.put("hdfsPath", hdfsPath);
        return table;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化 GIS 表数据失败: " + e.getMessage(), e);
        }
    }
}
