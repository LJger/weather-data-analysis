package com.example.backend.service;

import com.example.backend.dto.WeatherStationResponse;
import com.example.backend.entity.WeatherStation;
import com.example.backend.mapper.WeatherStationMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class WeatherStationService {

    private static final Logger log = LoggerFactory.getLogger(WeatherStationService.class);

    private final WeatherStationMapper weatherStationMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /** Redis key 前缀 */
    private static final String CACHE_ALL_STATIONS = "weather:stations:all";
    private static final String CACHE_PROVINCES = "weather:stations:provinces";
    private static final String CACHE_PROVINCE_PREFIX = "weather:stations:province:";
    /** 缓存过期时间：24 小时（站点数据极少变动） */
    private static final long CACHE_TTL_HOURS = 24;

    public WeatherStationService(WeatherStationMapper weatherStationMapper,
                                  StringRedisTemplate redisTemplate,
                                  ObjectMapper objectMapper) {
        this.weatherStationMapper = weatherStationMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public List<String> listProvinces() {
        try {
            String cached = redisTemplate.opsForValue().get(CACHE_PROVINCES);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<List<String>>() {});
            }
        } catch (Exception e) {
            log.warn("Redis 读取省份缓存失败，回退到数据库", e);
        }

        List<String> provinces = weatherStationMapper.findDistinctProvinces();

        try {
            redisTemplate.opsForValue().set(CACHE_PROVINCES,
                    objectMapper.writeValueAsString(provinces), CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Redis 写入省份缓存失败", e);
        }
        return provinces;
    }

    public List<WeatherStationResponse> listStationsByProvince(String province) {
        String cacheKey = CACHE_PROVINCE_PREFIX + province;
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached,
                        new TypeReference<List<WeatherStationResponse>>() {});
            }
        } catch (Exception e) {
            log.warn("Redis 读取省份站点缓存失败，回退到数据库", e);
        }

        List<WeatherStationResponse> result = weatherStationMapper.findByProvinceOrderByStationNameAsc(province)
                .stream().map(this::toResponse).toList();

        try {
            redisTemplate.opsForValue().set(cacheKey,
                    objectMapper.writeValueAsString(result), CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Redis 写入省份站点缓存失败", e);
        }
        return result;
    }

    public List<WeatherStationResponse> listAllStations() {
        try {
            String cached = redisTemplate.opsForValue().get(CACHE_ALL_STATIONS);
            if (cached != null) {
                return objectMapper.readValue(cached,
                        new TypeReference<List<WeatherStationResponse>>() {});
            }
        } catch (Exception e) {
            log.warn("Redis 读取全量站点缓存失败，回退到数据库", e);
        }

        List<WeatherStationResponse> result = weatherStationMapper.findAll().stream()
                .map(this::toResponse).toList();

        try {
            redisTemplate.opsForValue().set(CACHE_ALL_STATIONS,
                    objectMapper.writeValueAsString(result), CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Redis 写入全量站点缓存失败", e);
        }
        return result;
    }

    private WeatherStationResponse toResponse(WeatherStation station) {
        // 转换经纬度：如果绝对值大于180，则除以100（度分秒格式），否则直接使用
        Integer lat = station.getLatitude();
        Integer lon = station.getLongitude();
        
        Double normalizedLat = normalizeCoordinate(lat);
        Double normalizedLon = normalizeCoordinate(lon);
        
        return new WeatherStationResponse(
            station.getStationId(),
            station.getStationName(),
            normalizedLat,
            normalizedLon,
            station.getProvince(),
            station.getPressureSensorAltitude() == null ? null : station.getPressureSensorAltitude().toPlainString(),
            station.getObservationAltitude() == null ? null : station.getObservationAltitude().toPlainString()
        );
    }

    private Double normalizeCoordinate(Integer raw) {
        if (raw == null) {
            return null;
        }
        double asDouble = raw.doubleValue();
        // 如果绝对值大于180，说明是度分秒格式（如3950表示39.50度），需要除以100
        if (Math.abs(asDouble) > 180) {
            return asDouble / 100.0;
        }
        return asDouble;
    }
}

