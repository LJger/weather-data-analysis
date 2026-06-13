-- 历史气象数据表导入脚本
-- 用于导入示例数据到 gra_project.history_weather_data 表

-- 插入示例数据（基于用户提供的数据）
INSERT INTO gra_project.history_weather_data 
(station_id, observation_time, temperature, relative_humidity, precipitation_3h) 
VALUES
-- 站点 56038 的数据
('56038', '2025-12-21 03:00:00', -7.50, 30.00, 0.0000),
('56038', '2025-12-21 06:00:00', 5.90, 10.00, 0.0000),
('56038', '2025-12-21 09:00:00', 6.70, 9.00, 0.0000),
('56038', '2025-12-21 12:00:00', -1.20, 18.00, 0.0000),
('56038', '2025-12-21 15:00:00', 1.40, 18.00, 0.0000),
('56038', '2025-12-21 18:00:00', -2.90, 57.00, 0.0000),
('56038', '2025-12-21 21:00:00', -3.20, 59.00, 0.0000),

-- 站点 54398 的数据（用户附件中的数据）
('54398', '2025-12-21 03:00:00', 0.20, 30.00, 0.0000),
('54398', '2025-12-21 06:00:00', 3.60, 17.00, 0.0000),
('54398', '2025-12-21 09:00:00', 1.10, 25.00, 0.0000),
('54398', '2025-12-21 12:00:00', -3.30, 40.00, 0.0000),
('54398', '2025-12-21 15:00:00', -1.20, 36.00, 0.0000),
('54398', '2025-12-21 18:00:00', -2.90, 57.00, 0.0000),
('54398', '2025-12-21 21:00:00', -3.20, 59.00, 0.0000)

ON CONFLICT (station_id, observation_time) DO NOTHING;

-- 查询验证
SELECT 
    station_id,
    COUNT(*) as record_count,
    MIN(observation_time) as earliest_time,
    MAX(observation_time) as latest_time,
    AVG(temperature) as avg_temp,
    AVG(relative_humidity) as avg_humidity,
    SUM(precipitation_3h) as total_precipitation
FROM gra_project.history_weather_data
GROUP BY station_id
ORDER BY station_id;

-- 查询最新观测时间
SELECT MAX(observation_time) as latest_observation_time
FROM gra_project.history_weather_data;

-- 查询所有可用的观测时间
SELECT DISTINCT observation_time
FROM gra_project.history_weather_data
ORDER BY observation_time DESC;
