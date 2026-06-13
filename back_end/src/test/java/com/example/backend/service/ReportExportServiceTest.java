package com.example.backend.service;

import com.example.backend.dto.AnalysisReportExportRequest;
import com.example.backend.dto.MeteorDataFilterOptionsResponse;
import com.example.backend.dto.MeteorDataStationOption;
import com.example.backend.dto.MeteorDataTableRecordResponse;
import com.example.backend.dto.MeteorDataTaskOption;
import com.example.backend.dto.MeteorDataTimeSeriesPoint;
import com.example.backend.dto.MeteorRecordExportRequest;
import com.example.backend.dto.ReportFilterSummary;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportExportServiceTest {

    @Test
    void archivesAnalysisDocxExportToHdfsReportsDirectory() throws Exception {
        MeteorDataService meteorDataService = mock(MeteorDataService.class);
        HdfsService hdfsService = mock(HdfsService.class);
        ReportExportService service = new ReportExportService(meteorDataService, hdfsService);
        Integer userId = 7;
        ReportFilterSummary filters = new ReportFilterSummary(
                List.of(1L),
                List.of(57415),
                List.of("TEM"),
                "TEM",
                OffsetDateTime.parse("2025-12-27T00:00:00+08:00"),
                OffsetDateTime.parse("2025-12-28T00:00:00+08:00"),
                "hour",
                null
        );

        when(meteorDataService.loadFilterOptions(userId)).thenReturn(filterOptions());
        when(meteorDataService.loadTimeSeries(
                userId,
                "TEM",
                filters.startTime(),
                filters.endTime(),
                filters.granularity(),
                filters.stationIds(),
                filters.taskIds()
        )).thenReturn(List.of(
                new MeteorDataTimeSeriesPoint(filters.startTime(), BigDecimal.valueOf(5.2), BigDecimal.valueOf(4.8), BigDecimal.valueOf(6.1)),
                new MeteorDataTimeSeriesPoint(filters.endTime(), BigDecimal.valueOf(6.4), BigDecimal.valueOf(5.9), BigDecimal.valueOf(7.0))
        ));
        when(hdfsService.uploadFile(any(InputStream.class), anyString())).thenReturn("/weather-platform/exports/reports/user-7/analysis/report.docx");

        byte[] content = service.exportAnalysisReport(userId, new AnalysisReportExportRequest("timeSeries", filters, ""));

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        verify(hdfsService).uploadFile(any(InputStream.class), pathCaptor.capture());
        assertThat(content).isNotEmpty();
        assertThat(pathCaptor.getValue())
                .startsWith("/exports/reports/user-7/analysis/weather-analysis-report-")
                .endsWith(".docx");
    }

    @Test
    void archivesRecordsXlsxExportToHdfsReportsDirectory() throws Exception {
        MeteorDataService meteorDataService = mock(MeteorDataService.class);
        HdfsService hdfsService = mock(HdfsService.class);
        ReportExportService service = new ReportExportService(meteorDataService, hdfsService);
        Integer userId = 7;
        MeteorRecordExportRequest request = new MeteorRecordExportRequest(
                List.of(1L),
                List.of(57415),
                List.of("TEM"),
                OffsetDateTime.parse("2025-12-27T00:00:00+08:00"),
                OffsetDateTime.parse("2025-12-28T00:00:00+08:00")
        );

        when(meteorDataService.countRecordsByFilters(
                userId,
                request.taskIds(),
                request.stationIds(),
                request.elementCodes(),
                request.startTime(),
                request.endTime()
        )).thenReturn(1L);
        when(meteorDataService.queryRecordsForExport(
                userId,
                request.taskIds(),
                request.stationIds(),
                request.elementCodes(),
                request.startTime(),
                request.endTime(),
                100_000
        )).thenReturn(List.of(new MeteorDataTableRecordResponse(
                1L,
                1L,
                "测试任务",
                57415,
                "广安",
                "TEM",
                request.startTime(),
                BigDecimal.valueOf(5.2)
        )));
        when(meteorDataService.loadFilterOptions(userId)).thenReturn(filterOptions());
        when(hdfsService.uploadFile(any(InputStream.class), anyString())).thenReturn("/weather-platform/exports/reports/user-7/records/records.xlsx");

        byte[] content = service.exportRecordsExcel(userId, request);

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        verify(hdfsService).uploadFile(any(InputStream.class), pathCaptor.capture());
        assertThat(content).isNotEmpty();
        assertThat(pathCaptor.getValue())
                .startsWith("/exports/reports/user-7/records/weather-data-records-")
                .endsWith(".xlsx");
    }

    private MeteorDataFilterOptionsResponse filterOptions() {
        return new MeteorDataFilterOptionsResponse(
                List.of(new MeteorDataTaskOption(1L, "测试任务")),
                List.of(new MeteorDataStationOption(57415, "广安")),
                List.of("TEM")
        );
    }
}
