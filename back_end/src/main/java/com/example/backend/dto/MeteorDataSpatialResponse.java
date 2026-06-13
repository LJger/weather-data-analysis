package com.example.backend.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record MeteorDataSpatialResponse(
        List<MeteorDataSpatialProvinceStat> provinces,
        List<MeteorDataSpatialStationPoint> stations,
        List<String> availableProvinces,
        String currentProvince,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String elementCode
) {
}

