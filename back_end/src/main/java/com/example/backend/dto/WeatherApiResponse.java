package com.example.backend.dto;

/**
 * 后端返回给前端的响应
 */
public record WeatherApiResponse(
    String requestId,
    String status,
    String message,
    WeatherApiRequest echo,
    String apiUrl,
    String apiResponse
) {}

