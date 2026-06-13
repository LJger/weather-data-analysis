package com.example.backend.controller;

import com.example.backend.dto.WeatherApiRequest;
import com.example.backend.dto.WeatherApiResponse;
import com.example.backend.service.WeatherApiService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/weather")
public class WeatherApiController {

    private static final Logger log = LoggerFactory.getLogger(WeatherApiController.class);

    private final WeatherApiService weatherApiService;

    public WeatherApiController(WeatherApiService weatherApiService) {
        this.weatherApiService = weatherApiService;
    }

    @PostMapping("/simulate")
    public ResponseEntity<WeatherApiResponse> simulate(@Valid @RequestBody WeatherApiRequest request) {
        log.info("收到气象 API 访问请求: {}", request);

        String requestId = UUID.randomUUID().toString();
        String apiUrl = "";
        String apiResponse = "";
        String status = "SUCCESS";
        String message = "API 访问成功";

        try {
            // 调用真实的气象 API
            apiResponse = weatherApiService.callWeatherApi(request);
            
            // 从 service 中获取 URL（为了简化，这里重新构建一次用于显示）
            // 实际上 URL 已经在 service 中打印了
            apiUrl = buildApiUrlForDisplay(request);
            
            log.info("气象 API 访问成功，请求ID: {}", requestId);
        } catch (Exception e) {
            log.error("气象 API 访问失败", e);
            status = "ERROR";
            message = "API 访问失败: " + e.getMessage();
            apiResponse = e.getMessage();
        }

        WeatherApiResponse response = new WeatherApiResponse(
            requestId,
            status,
            message,
            request,
            apiUrl,
            apiResponse
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 构建 API URL 用于显示（仅用于返回给前端展示）
     * 注意：这里构建的 URL 不包含密码（安全考虑）
     */
    private String buildApiUrlForDisplay(WeatherApiRequest request) {
        StringBuilder url = new StringBuilder("http://api.data.cma.cn:8090/api");
        url.append("?userId=763640616157owbdB");
        url.append("&pwd=***"); // 隐藏密码
        url.append("&dataFormat=json");
        url.append("&interfaceId=getSurfEleByTimeRangeAndStaID");
        url.append("&dataCode=SURF_CHN_MUL_HOR_3H");
        url.append("&timeRange=").append(request.timeRange());
        url.append("&staIDs=").append(String.join(",", request.staIDs()));
        
        // 构建 elements
        StringBuilder elements = new StringBuilder("Station_Id_C,Year,Mon,Day,Hour");
        if (request.elements() != null && !request.elements().isEmpty()) {
            elements.append(",").append(String.join(",", request.elements()));
        }
        url.append("&elements=").append(elements);
        
        return url.toString();
    }
}

