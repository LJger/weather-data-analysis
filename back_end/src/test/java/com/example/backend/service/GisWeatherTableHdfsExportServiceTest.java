package com.example.backend.service;

import com.example.backend.entity.HistoryWeatherData;
import com.example.backend.entity.WeatherStation;
import com.example.backend.mapper.HistoryWeatherDataMapper;
import com.example.backend.mapper.WeatherStationMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GisWeatherTableHdfsExportServiceTest {

    @Test
    void exportsAllGisRelatedDatabaseTablesToHdfsBatchDirectory() {
        WeatherStationMapper weatherStationMapper = mock(WeatherStationMapper.class);
        HistoryWeatherDataMapper historyWeatherDataMapper = mock(HistoryWeatherDataMapper.class);
        HdfsService hdfsService = mock(HdfsService.class);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        GisWeatherTableHdfsExportService service = new GisWeatherTableHdfsExportService(
                weatherStationMapper, historyWeatherDataMapper, hdfsService, objectMapper);

        WeatherStation station = new WeatherStation();
        station.setStationId("57415");
        station.setStationName("广安");
        HistoryWeatherData historyData = new HistoryWeatherData();
        historyData.setStationId("57415");
        historyData.setObservationTime(LocalDateTime.of(2025, 12, 27, 0, 0));

        when(weatherStationMapper.findAll()).thenReturn(List.of(station));
        when(historyWeatherDataMapper.findAll()).thenReturn(List.of(historyData));
        when(hdfsService.writeText(anyString(), anyString())).thenAnswer(invocation ->
                "/weather-platform" + invocation.getArgument(1, String.class));

        Map<String, Object> result = service.exportAllTables();

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        verify(hdfsService, times(3)).writeText(anyString(), pathCaptor.capture());
        assertThat(pathCaptor.getAllValues()).anySatisfy(path -> assertThat(path).endsWith("/weather_stations.json"));
        assertThat(pathCaptor.getAllValues()).anySatisfy(path -> assertThat(path).endsWith("/history_weather_data.json"));
        assertThat(pathCaptor.getAllValues()).noneSatisfy(path -> assertThat(path).endsWith("/weather_data.json"));
        assertThat(pathCaptor.getAllValues()).anySatisfy(path -> assertThat(path).endsWith("/manifest.json"));

        assertThat(result)
                .containsEntry("success", true)
                .containsEntry("weatherStations", 1)
                .containsEntry("historyWeatherData", 1);
        assertThat(result).doesNotContainKey("weatherData");
        assertThat(result.get("batchPath").toString()).startsWith("/raw-data/gis/tables/export-");
    }
}
