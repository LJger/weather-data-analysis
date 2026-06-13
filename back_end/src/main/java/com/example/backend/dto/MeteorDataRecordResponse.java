package com.example.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MeteorDataRecordResponse(
        Long id,
        Integer userId,
        Long taskId,
        Integer stationId,
        String elementCode,
        OffsetDateTime datetime,
        BigDecimal value
) {
}









