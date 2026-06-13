package com.example.backend.dto;

public record WeatherStationResponse(
    String stationId,
    String stationName,
    Double latitude,
    Double longitude,
    String province,
    String pressureSensorAltitude,
    String observationAltitude
) {}

