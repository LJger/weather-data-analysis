package com.example.backend.service;

import com.example.backend.dto.WeatherApiRequest;
import com.example.backend.entity.MeteorData;
import com.example.backend.mapper.MeteorDataMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class WeatherApiService {

    private static final Logger log = LoggerFactory.getLogger(WeatherApiService.class);
    private static final DateTimeFormatter CMA_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.CHINA);

    // CMA API 账号信息
    private static final String CMA_API_USER_ID = "xxx";
    private static final String CMA_API_PASSWORD = "xxx";
    private static final String CMA_API_BASE_URL = "http://api.data.cma.cn:8090/api";

    private final RestTemplate restTemplate;
    private final MeteorDataMapper meteorDataMapper;
    private final ObjectMapper objectMapper;

    public WeatherApiService(
            RestTemplate restTemplate,
            MeteorDataMapper meteorDataMapper,
            ObjectMapper objectMapper
    ) {
        this.restTemplate = restTemplate;
        this.meteorDataMapper = meteorDataMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 调用 CMA 气象数据 API
     *
     * @param request 前端请求参数
     * @return API 返回的 JSON 数据（字符串形式）
     */
    public String callWeatherApi(WeatherApiRequest request) {
        // 构建完整的 API URL
        URI uri = buildApiUri(request);

        // 打印访问链接
        log.info("=== 气象 API 访问链接 ===");
        log.info(uri.toString());
        log.info("========================");

        try {
            // 调用 API
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);

            String responseBody = response.getBody();

            // 打印返回数据
            log.info("=== 气象 API 返回数据 ===");
            log.info("HTTP 状态码: {}", response.getStatusCode());
            log.info("返回内容: {}", responseBody);
            log.info("========================");

            persistMeteorologicalData(responseBody, request);

            return responseBody;
        } catch (Exception e) {
            log.error("调用气象 API 失败", e);
            throw new RuntimeException("调用气象 API 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建 CMA API 的完整 URL
     *
     * 根据 API 文档：
     * http://api.data.cma.cn:8090/api?userId=<帐号>&pwd=<密码>&dataFormat=json
     * &interfaceId=getSurfEleByTimeRangeAndStaID&dataCode=SURF_CHN_MUL_HOR_3H
     * &timeRange=<时间范围>&staIDs=<台站列表>&elements=Station_Id_C,Year,Mon,Day,Hour,<要素列表>
     */
    private URI buildApiUri(WeatherApiRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(CMA_API_BASE_URL)
                .queryParam("userId", CMA_API_USER_ID)
                .queryParam("pwd", CMA_API_PASSWORD)
                .queryParam("dataFormat", "json")
                .queryParam("interfaceId", "getSurfEleByTimeRangeAndStaID")
                .queryParam("dataCode", "SURF_CHN_MUL_HOR_3H")
                .queryParam("timeRange", encodeParam(request.timeRange()))
                .queryParam("staIDs", joinStaIDs(request.staIDs()));

        // 构建 elements 参数：Station_Id_C,Year,Mon,Day,Hour,<要素列表>
        String elements = buildElementsString(request.elements());
        builder.queryParam("elements", encodeParam(elements));

        // 先在 builder 中完成编码，然后直接构建 URI，防止 RestTemplate 再次对字符串进行编码导致参数失真
        return builder.encode().build(true).toUri();
    }

    private String encodeParam(String value) {
        return UriUtils.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * 将站点 ID 列表用逗号连接
     */
    private String joinStaIDs(List<String> staIDs) {
        return String.join(",", staIDs);
    }

    /**
     * 构建 elements 参数字符串
     * 格式：Station_Id_C,Year,Mon,Day,Hour,<要素列表>
     */
    private String buildElementsString(List<String> elements) {
        StringBuilder sb = new StringBuilder();
        // 添加默认字段
        sb.append("Station_Id_C,Year,Mon,Day,Hour");
        // 添加用户选择的要素
        if (elements != null && !elements.isEmpty()) {
            sb.append(",").append(String.join(",", elements));
        }
        return sb.toString();
    }

    @Transactional
    private void persistMeteorologicalData(String responseBody, WeatherApiRequest request) {
        if (request == null || request.taskId() == null || request.userId() == null) {
            log.info("未提供 taskId 或 userId，跳过气象要素入库。");
            return;
        }
        if (responseBody == null || responseBody.isBlank()) {
            log.warn("API 返回为空，跳过入库。");
            return;
        }
        if (request.elements() == null || request.elements().isEmpty()) {
            log.warn("未提供要素列表，无法写入气象数据。");
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (!"0".equals(root.path("returnCode").asText())) {
                log.warn("API 返回非成功状态(returnCode={}), 跳过入库。", root.path("returnCode").asText());
                return;
            }
            JsonNode dataSet = root.path("DS");
            if (!dataSet.isArray() || dataSet.size() == 0) {
                log.info("API 返回 DS 为空，跳过入库。");
                return;
            }

            meteorDataMapper.deleteByTaskId(request.taskId());

            List<MeteorData> rows = new ArrayList<>();
            for (JsonNode node : dataSet) {
                Integer stationId = parseInteger(node.path("Station_Id_C"));
                OffsetDateTime datetime = resolveDateTime(node);
                if (stationId == null || datetime == null) {
                    continue;
                }
                for (String elementCode : request.elements()) {
                    if (elementCode == null) {
                        continue;
                    }
                    BigDecimal value = parseBigDecimal(node.path(elementCode));
                    if (value == null) {
                        continue;
                    }
                    MeteorData meteorData = new MeteorData();
                    meteorData.setUserId(request.userId());
                    meteorData.setTaskId(request.taskId());
                    meteorData.setStationId(stationId);
                    meteorData.setElementCode(elementCode);
                    meteorData.setDatetime(datetime);
                    meteorData.setValue(value.setScale(2, RoundingMode.HALF_UP));
                    rows.add(meteorData);
                }
            }

            if (!rows.isEmpty()) {
                meteorDataMapper.insertBatch(rows);
                log.info("任务 {} 成功写入 {} 条气象要素记录。", request.taskId(), rows.size());
            } else {
                log.info("任务 {} 解析后无有效气象要素可写入。", request.taskId());
            }
        } catch (Exception e) {
            log.error("解析或写入气象要素失败。", e);
            throw new RuntimeException("写入气象数据失败: " + e.getMessage(), e);
        }
    }

    private BigDecimal parseBigDecimal(JsonNode node) {
        String value = parseText(node);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            log.debug("无法将值 {} 解析为 BigDecimal", value, e);
            return null;
        }
    }

    private Integer parseInteger(JsonNode node) {
        String value = parseText(node);
        if (value == null) {
            return null;
        }
        try {
            BigDecimal decimal = new BigDecimal(value);
            return decimal.intValue();
        } catch (NumberFormatException e) {
            log.debug("无法将值 {} 解析为 Integer", value, e);
            return null;
        }
    }

    private String parseText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String text = node.asText().trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) {
            return null;
        }
        return text;
    }

    private OffsetDateTime resolveDateTime(JsonNode node) {
        String datetimeText = parseText(node.path("Datetime"));
        if (datetimeText != null) {
            return parseDateTime(datetimeText);
        }

        Integer year = parseInteger(node.path("Year"));
        Integer month = parseInteger(node.path("Mon"));
        Integer day = parseInteger(node.path("Day"));
        Integer hour = parseInteger(node.path("Hour"));

        if (year == null || month == null || day == null) {
            return null;
        }

        try {
            LocalDateTime localDateTime = LocalDateTime.of(
                    year,
                    month,
                    day,
                    hour == null ? 0 : hour,
                    0
            );
            return OffsetDateTime.of(localDateTime, ZoneOffset.ofHours(8));
        } catch (Exception e) {
            log.warn("无法根据拆分的日期字段解析气象数据时间: year={}, month={}, day={}, hour={}",
                    year, month, day, hour, e);
            return null;
        }
    }

    private OffsetDateTime parseDateTime(String value) {
        if (value == null) {
            return null;
        }
        try {
            return OffsetDateTime.of(
                    java.time.LocalDateTime.parse(value, CMA_DATE_TIME_FORMATTER),
                    ZoneOffset.UTC
            );
        } catch (DateTimeParseException e) {
            log.warn("无法解析气象数据时间：{}", value, e);
            return null;
        }
    }
}

