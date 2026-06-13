package com.example.backend.service;

import com.example.backend.dto.PredictionResponse;
import com.example.backend.dto.PredictionResponse.DataPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Python 微服务预测接口（预留扩展）
 * 需要配置 prediction.python.enabled=true 才会启用
 * Python 端需实现 POST /predict 接口
 */
@Service
@ConditionalOnProperty(name = "prediction.python.enabled", havingValue = "true")
public class PythonPredictionService {

    private final RestTemplate restTemplate;

    @Value("${prediction.python.url:http://localhost:5000}")
    private String pythonServiceUrl;

    public PythonPredictionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 调用 Python 微服务执行预测
     *
     * @param algorithm    算法类型 (arima / lstm / random_forest)
     * @param historyData  历史数据点列表
     * @param forecastSteps 预测步数
     * @return 预测响应
     */
    public PredictionResponse predictWithPython(String algorithm, List<DataPoint> historyData, int forecastSteps) {
        try {
            String url = pythonServiceUrl + "/predict";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("algorithm", algorithm);
            requestBody.put("history_data", historyData);
            requestBody.put("forecast_steps", forecastSteps);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<PredictionResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, PredictionResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                return new PredictionResponse(false, "Python 预测服务返回异常");
            }
        } catch (Exception e) {
            return new PredictionResponse(false, "Python 预测服务不可用: " + e.getMessage());
        }
    }

    /**
     * 检查 Python 服务是否可用
     */
    public boolean isAvailable() {
        try {
            String url = pythonServiceUrl + "/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}
