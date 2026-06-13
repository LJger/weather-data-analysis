package com.example.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(
        @NotNull(message = "状态值不能为空")
        @Min(value = 0, message = "状态值非法")
        @Max(value = 3, message = "状态值非法")
        Integer status
) {
}


