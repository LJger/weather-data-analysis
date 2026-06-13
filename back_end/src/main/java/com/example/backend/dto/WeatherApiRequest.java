package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 用于承载前端模拟调用气象数据 API 的参数
 */
public record WeatherApiRequest(
    @NotEmpty(message = "请至少选择一个站点")
    @Size(min = 1, max = 30, message = "站点数量必须在1-30个之间")
    List<String> staIDs,

    @NotBlank(message = "时间范围不能为空")
    String timeRange,

    @NotEmpty(message = "请至少选择一个气象要素")
    @Size(max = 64, message = "气象要素数量不能超过64个")
    List<String> elements,

    String dataSource,

    String frequency,

    String remarks,

    Long taskId,

    Integer userId
) {}

