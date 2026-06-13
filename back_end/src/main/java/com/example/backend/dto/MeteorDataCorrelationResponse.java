package com.example.backend.dto;

import java.util.List;

public record MeteorDataCorrelationResponse(
        List<String> elementCodes,
        List<MeteorDataCorrelationMatrixCell> matrix,
        String granularity
) {
}

