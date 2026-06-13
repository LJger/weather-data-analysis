package com.example.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MeteorDataTimeSeriesPoint(
        OffsetDateTime timestamp,
        BigDecimal average,
        BigDecimal minimum,
        BigDecimal maximum
) {
}









