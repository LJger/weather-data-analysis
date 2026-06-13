package com.example.backend.service;

import com.example.backend.dto.WeatherGridDataResponse;
import com.example.backend.dto.WeatherGridDataResponse.GridPoint;
import com.example.backend.entity.MeteorData;
import com.example.backend.entity.WeatherStation;
import com.example.backend.mapper.MeteorDataMapper;
import com.example.backend.mapper.WeatherStationMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 气象网格化数据服务
 */
@Service
public class WeatherGridDataService {

    private final MeteorDataMapper meteorDataMapper;
    private final WeatherStationMapper weatherStationMapper;

    public WeatherGridDataService(
            MeteorDataMapper meteorDataMapper,
            WeatherStationMapper weatherStationMapper) {
        this.meteorDataMapper = meteorDataMapper;
        this.weatherStationMapper = weatherStationMapper;
    }

    /**
     * 获取网格化气象数据
     * 将站点数据转换为网格化显示
     */
    public List<WeatherGridDataResponse> getGridData(
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            String element,
            Double minLat,
            Double maxLat,
            Double minLon,
            Double maxLon
    ) {
        // 设置默认值 - 最近24小时
        if (startTime == null) {
            startTime = OffsetDateTime.now().minusDays(1).withMinute(0).withSecond(0).withNano(0);
        }
        if (endTime == null) {
            endTime = OffsetDateTime.now().withMinute(0).withSecond(0).withNano(0);
        }
        
        // 查询时间范围内的所有气象数据
        List<MeteorData> meteorDataList = meteorDataMapper.findByDatetimeBetween(startTime, endTime);
        
        // 如果没有数据，返回模拟数据
        if (meteorDataList.isEmpty()) {
            return generateMockGridData(startTime, endTime);
        }
        
        // 获取所有气象站信息
        Map<Integer, WeatherStation> stationMap = new HashMap<>();
        List<WeatherStation> stations = weatherStationMapper.findAll();
        for (WeatherStation station : stations) {
            try {
                stationMap.put(Integer.parseInt(station.getStationId()), station);
            } catch (NumberFormatException e) {
                // 忽略无法转换的站点ID
            }
        }
        
        // 按时间分组
        Map<OffsetDateTime, List<MeteorData>> dataByTime = meteorDataList.stream()
                .collect(Collectors.groupingBy(MeteorData::getDatetime));
        
        List<WeatherGridDataResponse> result = new ArrayList<>();
        
        // 为每个时间戳创建网格数据
        for (Map.Entry<OffsetDateTime, List<MeteorData>> entry : dataByTime.entrySet()) {
            OffsetDateTime timestamp = entry.getKey();
            List<MeteorData> dataAtTime = entry.getValue();
            
            // 按站点分组
            Map<Integer, Map<String, BigDecimal>> stationDataMap = new HashMap<>();
            
            for (MeteorData data : dataAtTime) {
                Integer stationId = data.getStationId();
                stationDataMap.putIfAbsent(stationId, new HashMap<>());
                stationDataMap.get(stationId).put(data.getElementCode(), data.getValue());
            }
            
            // 创建网格点
            List<GridPoint> points = new ArrayList<>();
            
            for (Map.Entry<Integer, Map<String, BigDecimal>> stationEntry : stationDataMap.entrySet()) {
                Integer stationId = stationEntry.getKey();
                WeatherStation station = stationMap.get(stationId);
                
                if (station != null) {
                    // 将经纬度从整数转换为度（除以100）
                    Double lat = station.getLatitude() / 100.0;
                    Double lon = station.getLongitude() / 100.0;
                    
                    // 应用地理范围过滤
                    if (minLat != null && lat < minLat) continue;
                    if (maxLat != null && lat > maxLat) continue;
                    if (minLon != null && lon < minLon) continue;
                    if (maxLon != null && lon > maxLon) continue;
                    
                    GridPoint point = new GridPoint(lat, lon);
                    Map<String, BigDecimal> elementValues = stationEntry.getValue();
                    
                    // 根据请求的要素类型设置数据
                    if (element == null || element.contains("temperature")) {
                        BigDecimal temp = elementValues.get("TEM");
                        if (temp != null) {
                            point.setTemperature(temp.doubleValue());
                        }
                    }
                    
                    if (element == null || element.contains("precipitation")) {
                        BigDecimal precip = elementValues.get("PRE_1h");
                        if (precip != null) {
                            point.setPrecipitation(precip.doubleValue());
                        }
                    }
                    
                    if (element == null || element.contains("wind")) {
                        BigDecimal windSpeed = elementValues.get("WIN_S_Avg_2mi");
                        BigDecimal windDir = elementValues.get("WIN_D_Avg_2mi");
                        if (windSpeed != null) {
                            point.setWindSpeed(windSpeed.doubleValue());
                        }
                        if (windDir != null) {
                            point.setWindDirection(windDir.doubleValue());
                        }
                    }
                    
                    points.add(point);
                }
            }
            
            if (!points.isEmpty()) {
                result.add(new WeatherGridDataResponse(timestamp, points));
            }
        }
        
        // 如果处理后仍没有数据，返回模拟数据
        if (result.isEmpty()) {
            return generateMockGridData(startTime, endTime);
        }
        
        // 按时间排序
        result.sort(Comparator.comparing(WeatherGridDataResponse::getTimestamp));
        
        return result;
    }

    /**
     * 根据经纬度查询特定位置的气象数据
     * 使用最近邻插值
     */
    public GridPoint getPointData(Double lat, Double lon, OffsetDateTime timestamp) {
        // 查询该时间戳附近的数据
        OffsetDateTime startTime = timestamp.minusHours(1);
        OffsetDateTime endTime = timestamp.plusHours(1);
        
        List<MeteorData> nearbyData = meteorDataMapper.findByDatetimeBetween(startTime, endTime);
        
        // 获取所有气象站
        Map<Integer, WeatherStation> stationMap = new HashMap<>();
        List<WeatherStation> stations = weatherStationMapper.findAll();
        for (WeatherStation station : stations) {
            try {
                stationMap.put(Integer.parseInt(station.getStationId()), station);
            } catch (NumberFormatException e) {
                // 忽略
            }
        }
        
        // 找到最近的站点
        Integer nearestStationId = null;
        double minDistance = Double.MAX_VALUE;
        
        for (WeatherStation station : stations) {
            double stationLat = station.getLatitude() / 100.0;
            double stationLon = station.getLongitude() / 100.0;
            double distance = calculateDistance(lat, lon, stationLat, stationLon);
            
            if (distance < minDistance) {
                minDistance = distance;
                try {
                    nearestStationId = Integer.parseInt(station.getStationId());
                } catch (NumberFormatException e) {
                    // 忽略
                }
            }
        }
        
        if (nearestStationId == null) {
            return null;
        }
        
        // 获取该站点的数据
        Integer finalNearestStationId = nearestStationId;
        List<MeteorData> stationData = nearbyData.stream()
                .filter(d -> d.getStationId().equals(finalNearestStationId))
                .collect(Collectors.toList());
        
        if (stationData.isEmpty()) {
            return null;
        }
        
        WeatherStation station = stationMap.get(nearestStationId);
        GridPoint point = new GridPoint(
                station.getLatitude() / 100.0,
                station.getLongitude() / 100.0
        );
        
        // 设置各要素值
        for (MeteorData data : stationData) {
            switch (data.getElementCode()) {
                case "TEM":
                    point.setTemperature(data.getValue().doubleValue());
                    break;
                case "PRE_1h":
                    point.setPrecipitation(data.getValue().doubleValue());
                    break;
                case "WIN_S_Avg_2mi":
                    point.setWindSpeed(data.getValue().doubleValue());
                    break;
                case "WIN_D_Avg_2mi":
                    point.setWindDirection(data.getValue().doubleValue());
                    break;
            }
        }
        
        return point;
    }

    /**
     * 计算两点之间的距离（简化的欧几里得距离）
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        return Math.sqrt(Math.pow(lat2 - lat1, 2) + Math.pow(lon2 - lon1, 2));
    }

    /**
     * 生成模拟网格数据用于演示
     */
    private List<WeatherGridDataResponse> generateMockGridData(OffsetDateTime startTime, OffsetDateTime endTime) {
        List<WeatherGridDataResponse> result = new ArrayList<>();
        
        // 生成24小时的数据
        OffsetDateTime currentTime = startTime;
        while (currentTime.isBefore(endTime) || currentTime.equals(endTime)) {
            List<GridPoint> points = new ArrayList<>();
            
            // 生成网格点（中国范围：纬度18-54，经度73-135，间隔2度）
            for (double lat = 20.0; lat <= 50.0; lat += 2.0) {
                for (double lon = 75.0; lon <= 135.0; lon += 2.0) {
                    GridPoint point = new GridPoint(lat, lon);
                    
                    // 生成模拟数据
                    double hourFactor = currentTime.getHour() / 24.0;
                    double latFactor = (lat - 20.0) / 30.0;
                    
                    // 温度：-10°C 到 35°C，随时间和纬度变化
                    point.setTemperature(-10.0 + Math.random() * 45.0 + Math.sin(hourFactor * Math.PI) * 10.0 - latFactor * 15.0);
                    
                    // 降水：0-50mm
                    point.setPrecipitation(Math.random() * 50.0);
                    
                    // 风速：0-15 m/s
                    point.setWindSpeed(Math.random() * 15.0);
                    
                    // 风向：0-360度
                    point.setWindDirection(Math.random() * 360.0);
                    
                    points.add(point);
                }
            }
            
            result.add(new WeatherGridDataResponse(currentTime, points));
            currentTime = currentTime.plusHours(1);
            
            // 限制最多24个时间点
            if (result.size() >= 24) {
                break;
            }
        }
        
        return result;
    }
}
