package com.example.backend.service;

import com.example.backend.dto.WeatherElementDataPoint;
import com.example.backend.dto.WeatherElementDataResponse;
import com.example.backend.dto.WeatherStationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GisWeatherHdfsArchiveServiceTest {

    @Test
    void archivesCurrentGisWeatherSnapshotToHdfs() throws Exception {
        WeatherStationService stationService = mock(WeatherStationService.class);
        HistoryWeatherDataService historyService = mock(HistoryWeatherDataService.class);
        HdfsService hdfsService = mock(HdfsService.class);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        GisWeatherHdfsArchiveService service = new GisWeatherHdfsArchiveService(
                stationService, historyService, hdfsService, objectMapper);

        LocalDateTime observationTime = LocalDateTime.of(2025, 12, 27, 0, 0);
        WeatherElementDataResponse elementData = new WeatherElementDataResponse();
        elementData.setElement("temperature");
        elementData.setObservationTime(observationTime);
        elementData.setDataPoints(List.of(new WeatherElementDataPoint(
                "57415", "广安", new BigDecimal("30.52"), new BigDecimal("106.64"), new BigDecimal("5.3"))));
        elementData.setMinValue(new BigDecimal("5.3"));
        elementData.setMaxValue(new BigDecimal("5.3"));
        elementData.setTotalPoints(1);

        when(stationService.listAllStations()).thenReturn(List.of(new WeatherStationResponse(
                "57415", "广安", 30.52, 106.64, "四川", "320.0", "310.0")));
        when(historyService.getElementData("temperature", observationTime)).thenReturn(elementData);
        when(hdfsService.writeText(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("/weather-platform/raw-data/gis/weather-gis-temperature-20251227000000.json");

        Map<String, Object> result = service.archiveSnapshot("temperature", observationTime);

        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        verify(hdfsService).writeText(contentCaptor.capture(), pathCaptor.capture());

        assertThat(pathCaptor.getValue()).isEqualTo("/raw-data/gis/weather-gis-temperature-20251227000000.json");
        assertThat(contentCaptor.getValue())
                .contains("\"snapshotType\" : \"GIS_WEATHER_DISPLAY\"")
                .contains("\"stations\"")
                .contains("\"elementData\"")
                .contains("\"广安\"");
        assertThat(result)
                .containsEntry("success", true)
                .containsEntry("element", "temperature")
                .containsEntry("stationCount", 1)
                .containsEntry("dataPointCount", 1)
                .containsEntry("hdfsPath", "/weather-platform/raw-data/gis/weather-gis-temperature-20251227000000.json");
    }
}
