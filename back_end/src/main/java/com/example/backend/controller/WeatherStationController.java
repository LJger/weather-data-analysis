package com.example.backend.controller;

import com.example.backend.dto.WeatherStationResponse;
import com.example.backend.service.WeatherStationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/weather/stations")
public class WeatherStationController {

    private final WeatherStationService weatherStationService;

    public WeatherStationController(WeatherStationService weatherStationService) {
        this.weatherStationService = weatherStationService;
    }

    @GetMapping("/provinces")
    public ResponseEntity<List<String>> listProvinces() {
        return ResponseEntity.ok(weatherStationService.listProvinces());
    }

    @GetMapping
    public ResponseEntity<List<WeatherStationResponse>> listStations(@RequestParam String province) {
        return ResponseEntity.ok(weatherStationService.listStationsByProvince(province));
    }

    @GetMapping("/all")
    public ResponseEntity<List<WeatherStationResponse>> listAllStations() {
        return ResponseEntity.ok(weatherStationService.listAllStations());
    }
}

