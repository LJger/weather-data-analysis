package com.example.backend.dto;

import java.math.BigDecimal;

public record MeteorDataSpatialProvinceStat(
        String province,
        BigDecimal averageValue,
        BigDecimal minValue,
        BigDecimal maxValue,
        Long stationCount
) {
}

