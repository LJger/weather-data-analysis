package com.example.backend.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record MeteorDataCorrelationRequest(
        Integer userId,
        List<String> elementCodes,
        List<Long> taskIds,
        List<Integer> stationIds,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String granularity
) {
}

