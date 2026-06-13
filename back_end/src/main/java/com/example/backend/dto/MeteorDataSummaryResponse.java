package com.example.backend.dto;

import java.time.OffsetDateTime;

public record MeteorDataSummaryResponse(
        long totalDataPoints,
        long taskCount,
        long stationCount,
        long elementCount,
        long anomalyCount,
        OffsetDateTime lastUpdated
) {
}









