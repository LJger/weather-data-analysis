-- ============================================================
-- 气象数据管理与分析系统 - 完整模拟数据初始化脚本
-- 适用数据库: PostgreSQL
-- 目标 Schema: gra_project
--
-- 说明:
-- 1) 执行前请先执行 create_tables.sql
-- 2) 模拟账号首次写入时明文密码均为: 123456
-- 3) 本脚本按唯一键/主键做幂等写入，可重复执行
-- ============================================================

SET search_path TO gra_project, public;

CREATE EXTENSION IF NOT EXISTS pgcrypto;


-- ------------------------------------------------------------
-- 1. 模拟用户
-- ------------------------------------------------------------
INSERT INTO gra_project.sys_user (
    username,
    password_hash,
    role,
    email,
    phone,
    gender,
    age,
    region,
    organization,
    bio,
    avatar_url,
    created_at,
    updated_at
)
VALUES
    (
        'mock_admin',
        crypt('123456', gen_salt('bf', 10)),
        1,
        'mock.admin@weather.local',
        '13810001001',
        1,
        32,
        '北京市/北京市/海淀区',
        '气象数据管理中心',
        '模拟管理员账号，用于后台功能演示',
        '/uploads/avatars/mock-admin.png',
        NOW() - INTERVAL '40 days',
        NOW()
    ),
    (
        'meteor_alice',
        crypt('123456', gen_salt('bf', 10)),
        0,
        'alice@weather.local',
        '13810001002',
        2,
        26,
        '四川省/成都市/武侯区',
        '西南区域气象中心',
        '关注温湿度变化和站点数据质量',
        '/uploads/avatars/mock-user-alice.png',
        NOW() - INTERVAL '20 days',
        NOW()
    ),
    (
        'meteor_bob',
        crypt('123456', gen_salt('bf', 10)),
        0,
        'bob@weather.local',
        '13810001003',
        1,
        29,
        '上海市/上海市/徐汇区',
        '城市气象服务实验室',
        '负责降水过程分析和报表导出',
        '/uploads/avatars/mock-user-bob.png',
        NOW() - INTERVAL '15 days',
        NOW()
    ),
    (
        'data_chen',
        crypt('123456', gen_salt('bf', 10)),
        0,
        'chen@weather.local',
        '13810001004',
        1,
        24,
        '湖北省/武汉市/洪山区',
        '气象大数据应用组',
        '用于预测分析和空间分布展示的模拟用户',
        '/uploads/avatars/mock-user-chen.png',
        NOW() - INTERVAL '8 days',
        NOW()
    )
ON CONFLICT (username)
DO UPDATE SET
    role = EXCLUDED.role,
    email = EXCLUDED.email,
    phone = EXCLUDED.phone,
    gender = EXCLUDED.gender,
    age = EXCLUDED.age,
    region = EXCLUDED.region,
    organization = EXCLUDED.organization,
    bio = EXCLUDED.bio,
    avatar_url = EXCLUDED.avatar_url,
    updated_at = NOW();


-- ------------------------------------------------------------
-- 2. 模拟气象站
-- ------------------------------------------------------------
INSERT INTO gra_project.weather_stations (
    station_id,
    province,
    station_name,
    latitude,
    longitude,
    pressure_sensor_altitude,
    observation_altitude
)
VALUES
    ('54511', '北京市', '北京', 3993, 11628, 31.30, 54.70),
    ('58367', '上海市', '徐家汇', 3120, 12143, 4.50, 6.00),
    ('59287', '广东省', '广州', 2313, 11332, 41.00, 6.60),
    ('56294', '四川省', '成都', 3067, 10402, 506.10, 507.30),
    ('57494', '湖北省', '武汉', 3052, 11413, 23.00, 24.60),
    ('56038', '青海省', '冷湖', 3832, 9320, 2733.00, 2735.40),
    ('54398', '辽宁省', '丹东', 4005, 12433, 13.50, 14.80)
ON CONFLICT (station_id)
DO UPDATE SET
    province = EXCLUDED.province,
    station_name = EXCLUDED.station_name,
    latitude = EXCLUDED.latitude,
    longitude = EXCLUDED.longitude,
    pressure_sensor_altitude = EXCLUDED.pressure_sensor_altitude,
    observation_altitude = EXCLUDED.observation_altitude;


-- ------------------------------------------------------------
-- 3. 模拟采集任务
--    使用固定 task_id，保证重复执行不会产生重复任务
-- ------------------------------------------------------------
WITH user_map AS (
    SELECT username, user_id::INTEGER AS user_id
    FROM gra_project.sys_user
    WHERE username IN ('meteor_alice', 'meteor_bob', 'data_chen')
)
INSERT INTO gra_project.collection_tasks (
    task_id,
    task_name,
    user_id,
    start_time,
    end_time,
    station_list,
    element_list,
    task_description,
    status,
    created_at,
    updated_at
)
SELECT
    v.task_id,
    v.task_name,
    user_map.user_id,
    v.start_time,
    v.end_time,
    v.station_list,
    v.element_list,
    v.task_description,
    v.status,
    v.created_at,
    NOW()
FROM (
    VALUES
        (
            900001::BIGINT,
            '华北温湿度采集任务',
            'meteor_alice',
            TIMESTAMPTZ '2026-05-28 00:00:00+08',
            TIMESTAMPTZ '2026-05-28 23:59:59+08',
            ARRAY[54511, 54398]::INTEGER[],
            ARRAY['TEM', 'RHU']::VARCHAR(20)[],
            '采集北京和丹东站点的温度、湿度模拟数据',
            2,
            NOW() - INTERVAL '6 days'
        ),
        (
            900002::BIGINT,
            '华东华南降水过程任务',
            'meteor_bob',
            TIMESTAMPTZ '2026-05-29 00:00:00+08',
            TIMESTAMPTZ '2026-05-29 23:59:59+08',
            ARRAY[58367, 59287]::INTEGER[],
            ARRAY['TEM', 'RHU', 'PRE_3h']::VARCHAR(20)[],
            '采集上海和广州站点的温度、湿度、3小时降水量',
            2,
            NOW() - INTERVAL '4 days'
        ),
        (
            900003::BIGINT,
            '西南华中预测样本任务',
            'data_chen',
            TIMESTAMPTZ '2026-05-30 00:00:00+08',
            TIMESTAMPTZ '2026-05-30 23:59:59+08',
            ARRAY[56294, 57494]::INTEGER[],
            ARRAY['TEM', 'RHU', 'PRE_3h']::VARCHAR(20)[],
            '为预测分析页面准备的成都和武汉模拟样本',
            1,
            NOW() - INTERVAL '2 days'
        )
) AS v (
    task_id,
    task_name,
    username,
    start_time,
    end_time,
    station_list,
    element_list,
    task_description,
    status,
    created_at
)
JOIN user_map ON user_map.username = v.username
ON CONFLICT (task_id)
DO UPDATE SET
    task_name = EXCLUDED.task_name,
    user_id = EXCLUDED.user_id,
    start_time = EXCLUDED.start_time,
    end_time = EXCLUDED.end_time,
    station_list = EXCLUDED.station_list,
    element_list = EXCLUDED.element_list,
    task_description = EXCLUDED.task_description,
    status = EXCLUDED.status,
    updated_at = NOW();


-- ------------------------------------------------------------
-- 4. 模拟采集明细数据
--    meteor_data.station_id 使用整数站号
-- ------------------------------------------------------------
INSERT INTO gra_project.meteor_data (
    id,
    user_id,
    task_id,
    station_id,
    element_code,
    datetime,
    value
)
SELECT
    v.id,
    collection_tasks.user_id,
    v.task_id,
    v.station_id,
    v.element_code,
    v.datetime,
    v.value
FROM (
    VALUES
        (900001::BIGINT, 900001::BIGINT, 54511, 'TEM', TIMESTAMPTZ '2026-05-28 08:00:00+08', 24.60::NUMERIC(8, 2)),
        (900002::BIGINT, 900001::BIGINT, 54511, 'RHU', TIMESTAMPTZ '2026-05-28 08:00:00+08', 52.00::NUMERIC(8, 2)),
        (900003::BIGINT, 900001::BIGINT, 54511, 'TEM', TIMESTAMPTZ '2026-05-28 14:00:00+08', 30.20::NUMERIC(8, 2)),
        (900004::BIGINT, 900001::BIGINT, 54511, 'RHU', TIMESTAMPTZ '2026-05-28 14:00:00+08', 38.00::NUMERIC(8, 2)),
        (900005::BIGINT, 900001::BIGINT, 54398, 'TEM', TIMESTAMPTZ '2026-05-28 08:00:00+08', 19.40::NUMERIC(8, 2)),
        (900006::BIGINT, 900001::BIGINT, 54398, 'RHU', TIMESTAMPTZ '2026-05-28 08:00:00+08', 68.00::NUMERIC(8, 2)),

        (900007::BIGINT, 900002::BIGINT, 58367, 'TEM', TIMESTAMPTZ '2026-05-29 08:00:00+08', 25.80::NUMERIC(8, 2)),
        (900008::BIGINT, 900002::BIGINT, 58367, 'RHU', TIMESTAMPTZ '2026-05-29 08:00:00+08', 76.00::NUMERIC(8, 2)),
        (900009::BIGINT, 900002::BIGINT, 58367, 'PRE_3h', TIMESTAMPTZ '2026-05-29 08:00:00+08', 2.40::NUMERIC(8, 2)),
        (900010::BIGINT, 900002::BIGINT, 58367, 'TEM', TIMESTAMPTZ '2026-05-29 14:00:00+08', 28.10::NUMERIC(8, 2)),
        (900011::BIGINT, 900002::BIGINT, 59287, 'TEM', TIMESTAMPTZ '2026-05-29 08:00:00+08', 29.60::NUMERIC(8, 2)),
        (900012::BIGINT, 900002::BIGINT, 59287, 'RHU', TIMESTAMPTZ '2026-05-29 08:00:00+08', 81.00::NUMERIC(8, 2)),
        (900013::BIGINT, 900002::BIGINT, 59287, 'PRE_3h', TIMESTAMPTZ '2026-05-29 08:00:00+08', 6.80::NUMERIC(8, 2)),

        (900014::BIGINT, 900003::BIGINT, 56294, 'TEM', TIMESTAMPTZ '2026-05-30 08:00:00+08', 22.40::NUMERIC(8, 2)),
        (900015::BIGINT, 900003::BIGINT, 56294, 'RHU', TIMESTAMPTZ '2026-05-30 08:00:00+08', 84.00::NUMERIC(8, 2)),
        (900016::BIGINT, 900003::BIGINT, 56294, 'PRE_3h', TIMESTAMPTZ '2026-05-30 08:00:00+08', 1.20::NUMERIC(8, 2)),
        (900017::BIGINT, 900003::BIGINT, 57494, 'TEM', TIMESTAMPTZ '2026-05-30 08:00:00+08', 26.70::NUMERIC(8, 2)),
        (900018::BIGINT, 900003::BIGINT, 57494, 'RHU', TIMESTAMPTZ '2026-05-30 08:00:00+08', 73.00::NUMERIC(8, 2)),
        (900019::BIGINT, 900003::BIGINT, 57494, 'PRE_3h', TIMESTAMPTZ '2026-05-30 08:00:00+08', 0.60::NUMERIC(8, 2))
) AS v (
    id,
    task_id,
    station_id,
    element_code,
    datetime,
    value
)
JOIN gra_project.collection_tasks ON collection_tasks.task_id = v.task_id
ON CONFLICT (id)
DO UPDATE SET
    user_id = EXCLUDED.user_id,
    task_id = EXCLUDED.task_id,
    station_id = EXCLUDED.station_id,
    element_code = EXCLUDED.element_code,
    datetime = EXCLUDED.datetime,
    value = EXCLUDED.value;


-- ------------------------------------------------------------
-- 5. 模拟历史观测数据
-- ------------------------------------------------------------
INSERT INTO gra_project.history_weather_data (
    station_id,
    observation_time,
    temperature,
    relative_humidity,
    precipitation_3h
)
VALUES
    ('54511', TIMESTAMP '2026-05-28 08:00:00', 24.60, 52.00, 0.0000),
    ('54511', TIMESTAMP '2026-05-28 14:00:00', 30.20, 38.00, 0.0000),
    ('54511', TIMESTAMP '2026-05-28 20:00:00', 25.10, 49.00, 0.2000),
    ('58367', TIMESTAMP '2026-05-29 08:00:00', 25.80, 76.00, 2.4000),
    ('58367', TIMESTAMP '2026-05-29 14:00:00', 28.10, 70.00, 1.1000),
    ('58367', TIMESTAMP '2026-05-29 20:00:00', 24.70, 82.00, 4.6000),
    ('59287', TIMESTAMP '2026-05-29 08:00:00', 29.60, 81.00, 6.8000),
    ('59287', TIMESTAMP '2026-05-29 14:00:00', 31.40, 74.00, 3.2000),
    ('59287', TIMESTAMP '2026-05-29 20:00:00', 27.80, 88.00, 8.5000),
    ('56294', TIMESTAMP '2026-05-30 08:00:00', 22.40, 84.00, 1.2000),
    ('56294', TIMESTAMP '2026-05-30 14:00:00', 25.50, 69.00, 0.4000),
    ('56294', TIMESTAMP '2026-05-30 20:00:00', 21.90, 87.00, 2.1000),
    ('57494', TIMESTAMP '2026-05-30 08:00:00', 26.70, 73.00, 0.6000),
    ('57494', TIMESTAMP '2026-05-30 14:00:00', 30.30, 62.00, 0.0000),
    ('57494', TIMESTAMP '2026-05-30 20:00:00', 26.10, 78.00, 1.7000),
    ('56038', TIMESTAMP '2025-12-21 03:00:00', -7.50, 30.00, 0.0000),
    ('56038', TIMESTAMP '2025-12-21 06:00:00', 5.90, 10.00, 0.0000),
    ('54398', TIMESTAMP '2025-12-21 03:00:00', 0.20, 30.00, 0.0000),
    ('54398', TIMESTAMP '2025-12-21 06:00:00', 3.60, 17.00, 0.0000)
ON CONFLICT (station_id, observation_time)
DO UPDATE SET
    temperature = EXCLUDED.temperature,
    relative_humidity = EXCLUDED.relative_humidity,
    precipitation_3h = EXCLUDED.precipitation_3h;


-- ------------------------------------------------------------
-- 6. 修正显式主键写入后的序列位置
-- ------------------------------------------------------------
SELECT setval(
    pg_get_serial_sequence('gra_project.collection_tasks', 'task_id'),
    GREATEST((SELECT COALESCE(MAX(task_id), 1) FROM gra_project.collection_tasks), 1),
    true
);

SELECT setval(
    pg_get_serial_sequence('gra_project.meteor_data', 'id'),
    GREATEST((SELECT COALESCE(MAX(id), 1) FROM gra_project.meteor_data), 1),
    true
);


-- ------------------------------------------------------------
-- 7. 导入结果检查
-- ------------------------------------------------------------
SELECT 'sys_user' AS table_name, COUNT(*) AS row_count
FROM gra_project.sys_user
WHERE username IN ('mock_admin', 'meteor_alice', 'meteor_bob', 'data_chen')
UNION ALL
SELECT 'weather_stations', COUNT(*)
FROM gra_project.weather_stations
WHERE station_id IN ('54511', '58367', '59287', '56294', '57494', '56038', '54398')
UNION ALL
SELECT 'collection_tasks', COUNT(*)
FROM gra_project.collection_tasks
WHERE task_id BETWEEN 900001 AND 900003
UNION ALL
SELECT 'meteor_data', COUNT(*)
FROM gra_project.meteor_data
WHERE id BETWEEN 900001 AND 900019
UNION ALL
SELECT 'history_weather_data', COUNT(*)
FROM gra_project.history_weather_data
WHERE station_id IN ('54511', '58367', '59287', '56294', '57494', '56038', '54398');
