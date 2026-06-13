package com.example.backend.controller;

import com.example.backend.dto.PredictionRequest;
import com.example.backend.dto.PredictionResponse;
import com.example.backend.service.PredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prediction")
public class PredictionController {

    @Autowired
    private PredictionService predictionService;

    /**
     * 执行预测
     */
    @PostMapping("/run")
    public ResponseEntity<PredictionResponse> runPrediction(@RequestBody PredictionRequest request) {
        PredictionResponse response = predictionService.predict(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取可用算法列表
     */
    @GetMapping("/algorithms")
    public ResponseEntity<List<Map<String, Object>>> getAlgorithms() {
        return ResponseEntity.ok(predictionService.getAlgorithms());
    }

    /**
     * 获取指定用户某要素的数据概况
     */
    @GetMapping("/data-info")
    public ResponseEntity<Map<String, Object>> getDataInfo(
            @RequestParam Long userId,
            @RequestParam String elementCode) {
        Map<String, Object> info = predictionService.getDataInfo(userId, elementCode);
        return ResponseEntity.ok(info);
    }
}
