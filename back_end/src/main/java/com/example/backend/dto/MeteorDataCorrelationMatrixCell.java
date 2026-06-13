package com.example.backend.dto;

public record MeteorDataCorrelationMatrixCell(
        String xElement,
        String yElement,
        double coefficient,
        int effectiveSamples
) {
}

