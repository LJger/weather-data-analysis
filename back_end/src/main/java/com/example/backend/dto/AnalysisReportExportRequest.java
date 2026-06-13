package com.example.backend.dto;

public record AnalysisReportExportRequest(
        String analysisType,
        ReportFilterSummary filters,
        String chartImageBase64
) {
}
