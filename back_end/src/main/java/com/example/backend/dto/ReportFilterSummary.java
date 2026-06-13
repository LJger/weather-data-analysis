package com.example.backend.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ReportFilterSummary(
        List<Long> taskIds,
        List<Integer> stationIds,
        List<String> elementCodes,
        String elementCode,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String granularity,
        String province
) {
}
