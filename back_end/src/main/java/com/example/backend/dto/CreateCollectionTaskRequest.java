package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.List;

public record CreateCollectionTaskRequest(
        @NotBlank(message = "任务名称不能为空")
        String taskName,

        @NotNull(message = "用户ID不能为空")
        Integer userId,

        @NotNull(message = "开始时间不能为空")
        OffsetDateTime startTime,

        @NotNull(message = "结束时间不能为空")
        OffsetDateTime endTime,

        @NotEmpty(message = "站点列表不能为空")
        List<Integer> stationList,

        @NotEmpty(message = "要素列表不能为空")
        List<String> elementList,

        String taskDescription
) {
}


