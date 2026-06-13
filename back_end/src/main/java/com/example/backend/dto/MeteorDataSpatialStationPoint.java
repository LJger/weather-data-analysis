package com.example.backend.dto;

import java.math.BigDecimal;

public record MeteorDataSpatialStationPoint(
        Integer stationId,
        String stationName,
        String province,
        Double latitude,
        Double longitude,
        BigDecimal value
) {
}

