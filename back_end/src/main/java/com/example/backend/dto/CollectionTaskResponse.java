package com.example.backend.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record CollectionTaskResponse(
        Long taskId,
        String taskName,
        Integer userId,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        List<Integer> stationList,
        List<String> elementList,
        String taskDescription,
        Integer status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}


