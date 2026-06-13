package com.example.backend.controller;

import com.example.backend.dto.MeteorDataCorrelationRequest;
import com.example.backend.dto.MeteorDataCorrelationResponse;
import com.example.backend.dto.MeteorDataFilterOptionsResponse;
import com.example.backend.dto.AnalysisReportExportRequest;
import com.example.backend.dto.MeteorRecordExportRequest;
import com.example.backend.dto.MeteorDataRecordResponse;
import com.example.backend.dto.MeteorDataSpatialPoint;
import com.example.backend.dto.MeteorDataSpatialResponse;
import com.example.backend.dto.MeteorDataSummaryResponse;
import com.example.backend.dto.MeteorDataTablePageResponse;
import com.example.backend.dto.MeteorDataTimeSeriesPoint;
import com.example.backend.service.MeteorDataService;
import com.example.backend.service.ReportExportService;
import com.example.backend.util.SecurityUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meteor-data")
public class MeteorDataController {

    private final MeteorDataService meteorDataService;
    private final ReportExportService reportExportService;

    public MeteorDataController(MeteorDataService meteorDataService,
                                ReportExportService reportExportService) {
        this.meteorDataService = meteorDataService;
        this.reportExportService = reportExportService;
    }

    @GetMapping("/summary")
    public ResponseEntity<MeteorDataSummaryResponse> getSummary(@RequestParam Integer userId) {
        return ResponseEntity.ok(meteorDataService.loadSummary(userId));
    }

    @GetMapping("/time-series")
    public ResponseEntity<List<MeteorDataTimeSeriesPoint>> getTimeSeries(
            @RequestParam Integer userId,
            @RequestParam String elementCode,
            @RequestParam(required = false) OffsetDateTime startTime,
            @RequestParam(required = false) OffsetDateTime endTime,
            @RequestParam(defaultValue = "hour") String granularity,
            @RequestParam(required = false) List<Integer> stationIds,
            @RequestParam(required = false) List<Long> taskIds
    ) {
        return ResponseEntity.ok(
                meteorDataService.loadTimeSeries(userId, elementCode, startTime, endTime, granularity, stationIds, taskIds)
        );
    }

    @GetMapping("/latest")
    public ResponseEntity<List<MeteorDataRecordResponse>> getLatest(
            @RequestParam Integer userId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String elementCode
    ) {
        return ResponseEntity.ok(meteorDataService.loadLatestRecords(userId, limit, elementCode));
    }

    @GetMapping("/elements")
    public ResponseEntity<List<String>> listElements(@RequestParam Integer userId) {
        return ResponseEntity.ok(meteorDataService.listElementCodes(userId));
    }

    @GetMapping("/spatial")
    public ResponseEntity<List<MeteorDataSpatialPoint>> getSpatialAverages(
            @RequestParam Integer userId,
            @RequestParam String elementCode,
            @RequestParam(required = false) OffsetDateTime startTime,
            @RequestParam(required = false) OffsetDateTime endTime
    ) {
        return ResponseEntity.ok(
                meteorDataService.loadSpatialAverages(userId, elementCode, startTime, endTime)
        );
    }

    @GetMapping("/spatial-map")
    public ResponseEntity<MeteorDataSpatialResponse> getSpatialMap(
            @RequestParam Integer userId,
            @RequestParam String elementCode,
            @RequestParam(required = false) OffsetDateTime startTime,
            @RequestParam(required = false) OffsetDateTime endTime,
            @RequestParam(required = false) String province
    ) {
        return ResponseEntity.ok(
                meteorDataService.loadSpatialMap(userId, elementCode, startTime, endTime, province)
        );
    }

    @GetMapping("/filter-options")
    public ResponseEntity<MeteorDataFilterOptionsResponse> getFilterOptions(@RequestParam Integer userId) {
        return ResponseEntity.ok(meteorDataService.loadFilterOptions(userId));
    }

    @PostMapping("/correlation")
    public ResponseEntity<MeteorDataCorrelationResponse> runCorrelation(@RequestBody MeteorDataCorrelationRequest request) {
        return ResponseEntity.ok(meteorDataService.calculateCorrelation(request));
    }

    @GetMapping("/records")
    public ResponseEntity<MeteorDataTablePageResponse> queryRecords(
            @RequestParam Integer userId,
            @RequestParam(required = false) List<Long> taskIds,
            @RequestParam(required = false) List<Integer> stationIds,
            @RequestParam(required = false) List<String> elementCodes,
            @RequestParam(required = false) OffsetDateTime startTime,
            @RequestParam(required = false) OffsetDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                meteorDataService.queryRecords(userId, taskIds, stationIds, elementCodes, startTime, endTime, page, size)
        );
    }

    @GetMapping("/element-distribution")
    public ResponseEntity<List<Map<String, Object>>> getElementDistribution(@RequestParam Integer userId) {
        return ResponseEntity.ok(meteorDataService.loadElementDistribution(userId));
    }

    @PostMapping("/export/analysis-docx")
    public ResponseEntity<byte[]> exportAnalysisDocx(@RequestBody AnalysisReportExportRequest request) throws Exception {
        Integer userId = SecurityUtils.requireCurrentUserId().intValue();
        byte[] content = reportExportService.exportAnalysisReport(userId, request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"weather-analysis-report.docx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(content);
    }

    @PostMapping("/export/records-xlsx")
    public ResponseEntity<byte[]> exportRecordsXlsx(@RequestBody MeteorRecordExportRequest request) throws Exception {
        Integer userId = SecurityUtils.requireCurrentUserId().intValue();
        byte[] content = reportExportService.exportRecordsExcel(userId, request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"weather-data-records.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }
}
