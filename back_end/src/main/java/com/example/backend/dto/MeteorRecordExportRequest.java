package com.example.backend.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record MeteorRecordExportRequest(
        List<Long> taskIds,
        List<Integer> stationIds,
        List<String> elementCodes,
        OffsetDateTime startTime,
        OffsetDateTime endTime
) {
}
