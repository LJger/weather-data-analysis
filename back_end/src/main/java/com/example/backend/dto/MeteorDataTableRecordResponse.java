package com.example.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MeteorDataTableRecordResponse(
        Long id,
        Long taskId,
        String taskName,
        Integer stationId,
        String stationName,
        String elementCode,
        OffsetDateTime datetime,
        BigDecimal value
) {
}

