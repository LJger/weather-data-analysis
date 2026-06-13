# 后端项目说明

本目录是气象数据管理与分析系统的 Spring Boot 后端工程。

## 1. 技术栈

- Java 17
- Spring Boot 3.5.7
- Spring Web
- Spring Security
- JWT
- MyBatis
- PostgreSQL
- Redis
- Hadoop HDFS Client 3.3.6
- Spring Mail
- Apache POI
- Apache Commons Math
- JUnit 5 / Mockito

## 2. 目录结构

```text
back_end/
├─ src/main/java/com/example/backend/
│  ├─ config/          # Security、JWT、Redis、HDFS、MyBatis TypeHandler
│  ├─ controller/      # REST Controller
│  ├─ dto/             # DTO
│  ├─ entity/          # 数据实体
│  ├─ exception/       # 全局异常处理
│  ├─ mapper/          # MyBatis Mapper 接口
│  ├─ service/         # 业务服务
│  └─ util/            # SecurityUtils 等工具
├─ src/main/resources/
│  ├─ mapper/          # MyBatis XML
│  ├─ sql/             # 建表和示例数据脚本
│  └─ application.properties
└─ src/test/java/      # 服务层测试
```

## 3. 配置项

当前开发配置位于 `src/main/resources/application.properties`。

### 数据库

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=123456
```

### Redis

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=123456
spring.data.redis.database=0
```

Redis 用途：

- 邮箱验证码。
- 验证码发送锁。
- 气象站列表缓存。
- 省份列表缓存。

### HDFS

```properties
hdfs.uri=hdfs://192.168.10.102:8020
hdfs.user=root
hdfs.base-path=/weather-platform
```

所有 HDFS 文件操作都会通过 `HdfsService.resolvePath()` 限制在 `/weather-platform` 下。

## 4. 主要 API

### 认证

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/auth/send-email-code` | 发送邮箱验证码 |
| POST | `/api/auth/register` | 注册普通用户 |
| POST | `/api/auth/login` | 登录 |
| GET | `/api/auth/profile` | 当前用户资料 |
| PUT | `/api/auth/profile` | 更新资料 |
| PUT | `/api/auth/password` | 修改密码 |
| POST | `/api/auth/avatar` | 上传头像 |
| DELETE | `/api/auth/account` | 注销账号 |

### 采集任务

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/collection-tasks` | 创建采集任务 |
| GET | `/api/collection-tasks` | 查询任务列表 |
| GET | `/api/collection-tasks/{taskId}` | 查询任务详情 |
| PATCH | `/api/collection-tasks/{taskId}/status` | 更新任务状态 |
| DELETE | `/api/collection-tasks/{taskId}` | 删除任务 |

### 气象分析

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/meteor-data/summary` | 数据概览 |
| GET | `/api/meteor-data/time-series` | 时间序列 |
| GET | `/api/meteor-data/latest` | 最新数据 |
| GET | `/api/meteor-data/elements` | 要素列表 |
| GET | `/api/meteor-data/spatial` | 空间均值 |
| GET | `/api/meteor-data/spatial-map` | 空间地图数据 |
| GET | `/api/meteor-data/filter-options` | 筛选选项 |
| POST | `/api/meteor-data/correlation` | 相关性矩阵 |
| GET | `/api/meteor-data/records` | 明细分页 |
| GET | `/api/meteor-data/element-distribution` | 要素分布 |
| POST | `/api/meteor-data/export/analysis-docx` | 导出 Word 分析报告 |
| POST | `/api/meteor-data/export/records-xlsx` | 导出 Excel 明细 |

普通用户报表导出会同时上传到 HDFS：

```text
/weather-platform/exports/reports/user-{id}/analysis/*.docx
/weather-platform/exports/reports/user-{id}/records/*.xlsx
```

### GIS 和历史气象

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/weather/stations/provinces` | 省份列表 |
| GET | `/api/weather/stations` | 按省份查站点 |
| GET | `/api/weather/stations/all` | 全部站点 |
| GET | `/api/weather/element-data` | 要素空间数据 |
| GET | `/api/weather/observation-times` | 可选观测时间 |
| GET | `/api/weather/latest-observation-time` | 最新观测时间 |
| GET | `/api/weather/station/{stationId}/data` | 站点详情数据 |
| GET | `/api/weather/grid-data` | 网格数据 |
| GET | `/api/weather/grid-data/point` | 点位网格查询 |

### 预测

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/prediction/algorithms` | 算法列表 |
| GET | `/api/prediction/data-info` | 数据范围 |
| POST | `/api/prediction/run` | 执行预测 |

### 管理员用户

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/admin/users` | 用户列表 |
| GET | `/api/admin/users/statistics` | 用户统计 |
| POST | `/api/admin/users` | 新增用户 |
| PUT | `/api/admin/users/{id}` | 编辑用户 |
| GET | `/api/admin/users/{id}` | 用户详情 |
| PATCH | `/api/admin/users/{id}/role` | 修改角色 |
| PATCH | `/api/admin/users/{id}/reset-password` | 重置密码 |
| DELETE | `/api/admin/users/{id}` | 删除用户 |

### 管理员存储

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/storage/overview` | HDFS + Redis 总览 |
| GET | `/api/storage/files` | 浏览 HDFS 目录 |
| POST | `/api/storage/files/upload` | 上传文件 |
| GET | `/api/storage/files/download` | 下载文件 |
| POST | `/api/storage/files/mkdir` | 新建目录 |
| POST | `/api/storage/files/rename` | 重命名 |
| DELETE | `/api/storage/files` | 删除 |
| GET | `/api/storage/files/summary` | 目录统计 |
| POST | `/api/storage/init` | 初始化 HDFS 平台目录 |
| POST | `/api/storage/gis/archive` | 归档 GIS 页面数据 |
| POST | `/api/storage/gis/export-tables` | 导出 GIS 依赖表到 HDFS |
| GET | `/api/storage/cache/overview` | Redis 状态 |
| GET | `/api/storage/cache/keys` | 查询 Redis Key |
| DELETE | `/api/storage/cache` | 清理 Redis Key |

## 5. HDFS 目录

平台根目录：`/weather-platform`

| 子目录 | 用途 |
|---|---|
| `/raw-data` | 原始数据 |
| `/raw-data/meteor` | 气象采集原始数据预留 |
| `/raw-data/history` | 历史数据预留 |
| `/raw-data/gis` | GIS 快照 |
| `/raw-data/gis/tables` | GIS 表数据导出 |
| `/exports` | 导出文件 |
| `/exports/reports` | 普通用户报表 |
| `/backups` | 备份 |
| `/temp` | 临时文件 |

## 6. 数据库

建表脚本：`src/main/resources/sql/create_tables.sql`

当前核心表：

- `sys_user`
- `weather_stations`
- `collection_tasks`
- `meteor_data`
- `history_weather_data`

旧表 `weather_data` 已删除，当前代码不再包含对应实体和 Mapper。

## 7. 测试

```bash
mvn test
mvn clean test
```

当前服务层测试覆盖：

- HDFS 目录统计和文件元数据。
- GIS 快照归档。
- GIS 表数据导出。
- 普通用户报表归档到 HDFS。

## 8. 启动

```bash
mvn spring-boot:run
```

默认地址：

```text
http://localhost:8080
```

## 9. 注意事项

1. 生产环境不要直接使用开发环境中的数据库、Redis、邮箱和 JWT 配置。
2. HDFS 写入前确认 NameNode 可访问，且不处于 SafeMode。
3. 管理员 HDFS 页面只管理 `/weather-platform`，不会直接开放 HDFS 根目录。
4. Redis 管理页支持按模式删除 Key，操作前需要确认模式范围。

文档更新时间：2026-05-16。
