-- ============================================================
-- 气象数据管理与分析系统 — PostgreSQL 建表脚本
-- Schema: gra_project
-- ============================================================

-- 创建 Schema（如不存在）
CREATE SCHEMA IF NOT EXISTS gra_project;

-- 设置默认搜索路径（可选，方便后续操作）
SET search_path TO gra_project, public;


-- ------------------------------------------------------------
-- 1. 用户表 sys_user
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS gra_project.sys_user (
    user_id       BIGSERIAL       PRIMARY KEY,
    username      VARCHAR(30)     NOT NULL UNIQUE,
    password_hash VARCHAR(100)    NOT NULL,
    role          SMALLINT        NOT NULL DEFAULT 0,  -- 0: 普通用户  1: 管理员
    email         VARCHAR(100)    UNIQUE,              -- 电子邮箱
    phone         VARCHAR(20)     UNIQUE,              -- 手机号
    gender        SMALLINT        DEFAULT 0,           -- 性别: 0=未知, 1=男, 2=女
    age           SMALLINT,                            -- 年龄
    region        VARCHAR(100),                        -- 所在地区（省/市）
    organization  VARCHAR(200),                        -- 所属机构/单位
    bio           TEXT,                                -- 个人简介
    avatar_url    VARCHAR(500),                        -- 头像URL
    created_at    TIMESTAMPTZ     DEFAULT NOW(),       -- 注册时间
    updated_at    TIMESTAMPTZ     DEFAULT NOW()        -- 信息更新时间
);

COMMENT ON TABLE  gra_project.sys_user               IS '系统用户表';
COMMENT ON COLUMN gra_project.sys_user.user_id       IS '用户ID，自增主键';
COMMENT ON COLUMN gra_project.sys_user.username      IS '用户名，全局唯一，2~30字符';
COMMENT ON COLUMN gra_project.sys_user.password_hash IS 'BCrypt 加密后的密码哈希';
COMMENT ON COLUMN gra_project.sys_user.role          IS '角色: 0=普通用户, 1=管理员';
COMMENT ON COLUMN gra_project.sys_user.email         IS '电子邮箱，全局唯一（可选）';
COMMENT ON COLUMN gra_project.sys_user.phone         IS '手机号，全局唯一（可选）';
COMMENT ON COLUMN gra_project.sys_user.gender        IS '性别: 0=未知, 1=男, 2=女';
COMMENT ON COLUMN gra_project.sys_user.age           IS '年龄';
COMMENT ON COLUMN gra_project.sys_user.region        IS '所在地区，如"北京市"或"广东省广州市"';
COMMENT ON COLUMN gra_project.sys_user.organization  IS '所属机构/单位名称';
COMMENT ON COLUMN gra_project.sys_user.bio           IS '个人简介（自由文本）';
COMMENT ON COLUMN gra_project.sys_user.created_at    IS '账号注册时间';
COMMENT ON COLUMN gra_project.sys_user.updated_at    IS '用户信息最后更新时间';


-- ------------------------------------------------------------
-- 2. 气象站信息表 weather_stations
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS gra_project.weather_stations (
    station_id               VARCHAR(20)     PRIMARY KEY,
    province                 VARCHAR(50)     NOT NULL,
    station_name             VARCHAR(100)    NOT NULL,
    latitude                 INTEGER         NOT NULL,   -- 纬度×100，如 3950 = 39.50°N
    longitude                INTEGER         NOT NULL,   -- 经度×100，如 11650 = 116.50°E
    pressure_sensor_altitude NUMERIC(8, 2),              -- 气压传感器海拔高度(m)
    observation_altitude     NUMERIC(8, 2)               -- 观测场海拔高度(m)
);

COMMENT ON TABLE  gra_project.weather_stations                          IS '全国气象站基础信息表';
COMMENT ON COLUMN gra_project.weather_stations.station_id               IS '气象站编号（国标站号），主键';
COMMENT ON COLUMN gra_project.weather_stations.province                 IS '所属省份';
COMMENT ON COLUMN gra_project.weather_stations.station_name             IS '气象站名称';
COMMENT ON COLUMN gra_project.weather_stations.latitude                 IS '纬度（整数，实际值=字段值/100，单位：度）';
COMMENT ON COLUMN gra_project.weather_stations.longitude                IS '经度（整数，实际值=字段值/100，单位：度）';
COMMENT ON COLUMN gra_project.weather_stations.pressure_sensor_altitude IS '气压传感器海拔高度（m）';
COMMENT ON COLUMN gra_project.weather_stations.observation_altitude     IS '观测场海拔高度（m）';


-- ------------------------------------------------------------
-- 3. 数据采集任务表 collection_tasks
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS gra_project.collection_tasks (
    task_id          BIGSERIAL        PRIMARY KEY,
    task_name        VARCHAR(255)     NOT NULL,
    user_id          INTEGER          NOT NULL,
    start_time       TIMESTAMPTZ      NOT NULL,
    end_time         TIMESTAMPTZ      NOT NULL,
    station_list     INTEGER[]        NOT NULL,           -- 采集站点ID数组
    element_list     VARCHAR(20)[]    NOT NULL,           -- 气象要素代码数组
    task_description TEXT,
    status           INTEGER          NOT NULL DEFAULT 0, -- 0:待执行 1:运行中 2:成功 3:失败
    created_at       TIMESTAMPTZ      DEFAULT NOW(),
    updated_at       TIMESTAMPTZ      DEFAULT NOW(),

    CONSTRAINT fk_collection_tasks_user
        FOREIGN KEY (user_id) REFERENCES gra_project.sys_user (user_id)
        ON DELETE CASCADE
);

COMMENT ON TABLE  gra_project.collection_tasks                  IS '数据采集任务表';
COMMENT ON COLUMN gra_project.collection_tasks.task_id          IS '任务ID，自增主键';
COMMENT ON COLUMN gra_project.collection_tasks.task_name        IS '任务名称';
COMMENT ON COLUMN gra_project.collection_tasks.user_id          IS '创建用户ID，关联 sys_user';
COMMENT ON COLUMN gra_project.collection_tasks.start_time       IS '采集数据起始时间';
COMMENT ON COLUMN gra_project.collection_tasks.end_time         IS '采集数据结束时间';
COMMENT ON COLUMN gra_project.collection_tasks.station_list     IS '采集站点ID数组（PostgreSQL 原生数组类型）';
COMMENT ON COLUMN gra_project.collection_tasks.element_list     IS '气象要素代码数组，如 {TEM,RHU,PRE_3h}';
COMMENT ON COLUMN gra_project.collection_tasks.task_description IS '任务描述（可选）';
COMMENT ON COLUMN gra_project.collection_tasks.status           IS '任务状态: 0=待执行, 1=运行中, 2=成功, 3=失败';
COMMENT ON COLUMN gra_project.collection_tasks.created_at       IS '记录创建时间';
COMMENT ON COLUMN gra_project.collection_tasks.updated_at       IS '记录最后更新时间';

-- 按用户ID建索引，加速任务列表查询
CREATE INDEX IF NOT EXISTS idx_collection_tasks_user_id
    ON gra_project.collection_tasks (user_id);


-- ------------------------------------------------------------
-- 4. 气象要素数据表 meteor_data
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS gra_project.meteor_data (
    id           BIGSERIAL        PRIMARY KEY,
    user_id      INTEGER          NOT NULL,
    task_id      BIGINT           NOT NULL,
    station_id   INTEGER          NOT NULL,
    element_code VARCHAR(20)      NOT NULL,   -- 要素代码，如 TEM、RHU、PRE_3h
    datetime     TIMESTAMPTZ      NOT NULL,   -- 观测时刻
    value        NUMERIC(8, 2)    NOT NULL,   -- 要素观测值

    CONSTRAINT fk_meteor_data_user
        FOREIGN KEY (user_id) REFERENCES gra_project.sys_user (user_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_meteor_data_task
        FOREIGN KEY (task_id) REFERENCES gra_project.collection_tasks (task_id)
        ON DELETE CASCADE
);

COMMENT ON TABLE  gra_project.meteor_data              IS '气象要素采集数据表（采集任务产出的原始数据）';
COMMENT ON COLUMN gra_project.meteor_data.id           IS '记录ID，自增主键';
COMMENT ON COLUMN gra_project.meteor_data.user_id      IS '所属用户ID';
COMMENT ON COLUMN gra_project.meteor_data.task_id      IS '来源采集任务ID';
COMMENT ON COLUMN gra_project.meteor_data.station_id   IS '气象站ID（整数形式）';
COMMENT ON COLUMN gra_project.meteor_data.element_code IS '气象要素代码（如 TEM=气温, RHU=相对湿度, PRE_3h=3小时降水）';
COMMENT ON COLUMN gra_project.meteor_data.datetime     IS '要素观测时刻（含时区）';
COMMENT ON COLUMN gra_project.meteor_data.value        IS '要素观测值，精度 NUMERIC(8,2)';

-- 常用查询索引
CREATE INDEX IF NOT EXISTS idx_meteor_data_user_id
    ON gra_project.meteor_data (user_id);

CREATE INDEX IF NOT EXISTS idx_meteor_data_task_id
    ON gra_project.meteor_data (task_id);

CREATE INDEX IF NOT EXISTS idx_meteor_data_datetime
    ON gra_project.meteor_data (datetime);

CREATE INDEX IF NOT EXISTS idx_meteor_data_user_elem_dt
    ON gra_project.meteor_data (user_id, element_code, datetime);


-- ------------------------------------------------------------
-- 5. 历史气象观测数据表 history_weather_data
--    联合主键：station_id + observation_time
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS gra_project.history_weather_data (
    station_id        VARCHAR(20)     NOT NULL,  -- 站点编号
    observation_time  TIMESTAMP       NOT NULL,  -- 观测时刻（不含时区，存为 UTC）
    temperature       NUMERIC(6, 2),             -- 气温（℃）
    relative_humidity NUMERIC(5, 2),             -- 相对湿度（%）
    precipitation_3h  NUMERIC(8, 4),             -- 3小时降水量（mm）

    PRIMARY KEY (station_id, observation_time),

    CONSTRAINT fk_history_weather_station
        FOREIGN KEY (station_id) REFERENCES gra_project.weather_stations (station_id)
        ON DELETE CASCADE
);

COMMENT ON TABLE  gra_project.history_weather_data                  IS '历史气象观测数据表（简化版，联合主键）';
COMMENT ON COLUMN gra_project.history_weather_data.station_id       IS '气象站编号，关联 weather_stations';
COMMENT ON COLUMN gra_project.history_weather_data.observation_time IS '观测时刻';
COMMENT ON COLUMN gra_project.history_weather_data.temperature      IS '气温（℃）';
COMMENT ON COLUMN gra_project.history_weather_data.relative_humidity IS '相对湿度（%）';
COMMENT ON COLUMN gra_project.history_weather_data.precipitation_3h IS '3小时降水量（mm）';

CREATE INDEX IF NOT EXISTS idx_history_weather_obs_time
    ON gra_project.history_weather_data (observation_time);
