# 气象数据管理与分析系统

基于 Vue 3、Spring Boot、PostgreSQL、Redis 和 HDFS 的气象数据管理与分析平台。系统面向普通用户提供气象数据采集、统计分析、GIS 可视化、数据预测和个性化配置；面向管理员提供用户治理、真实 HDFS 工作区管理和 Redis 缓存管理。

## 技术栈

| 层次 | 技术 |
|---|---|
| 前端 | Vue 3、TypeScript、Vue Router、Pinia、Element Plus、ECharts、百度地图 GL、Vite |
| 后端 | Spring Boot 3.5.7、Spring Security、JWT、MyBatis、Spring Mail、Apache POI |
| 数据库 | PostgreSQL，默认 schema 为 `gra_project` |
| 缓存 | Redis，缓存邮箱验证码、站点列表、省份列表等 |
| 分布式存储 | Hadoop HDFS 3.3.6，平台工作区默认为 `/weather-platform` |
| 构建与运行 | Java 17、Maven、Node.js 20.19+ |

## 当前核心功能

- 用户认证：注册、邮箱验证码、登录、JWT 鉴权、个人资料、头像上传、密码修改、账号注销。
- 角色权限：普通用户访问 `/dashboard/**`，管理员访问 `/admin/**` 和 `/api/storage/**`。
- 数据采集：创建采集任务，调用 CMA 气象开放接口，写入 `meteor_data`。
- 统计分析：概览、时间序列、相关性热力图、空间分布、明细分页查询。
- 报表导出：普通用户可导出 Word 分析报告和 Excel 明细，导出文件同时归档到 HDFS `/exports/reports/user-{用户ID}/`。
- GIS 可视化：百度地图展示站点、气象要素热力/散点数据、点选最近站点和气象详情。
- 数据预测：线性回归、移动平均、指数平滑，返回预测曲线、评估指标和阈值预警。
- 个性化服务：主题、默认要素、时间粒度、预测算法和分页偏好本地保存。
- 管理员用户管理：用户列表、统计、新增、编辑、角色调整、重置密码、删除。
- 管理员存储管理：真实 HDFS 浏览器、上传、下载、目录、重命名、删除、GIS 数据归档、GIS 表全量上传、文件副本/块数/块大小展示。
- Redis 缓存管理：查看 Redis 版本、连接数、运行时长、Key 列表、TTL 和按模式清理。

## 项目结构

```text
code/
├─ back_end/                         # Spring Boot 后端
│  ├─ src/main/java/com/example/backend/
│  │  ├─ config/                     # Security、JWT、Redis、HDFS、MyBatis 类型处理器
│  │  ├─ controller/                 # REST API
│  │  ├─ dto/                        # 请求/响应 DTO
│  │  ├─ entity/                     # 数据实体
│  │  ├─ mapper/                     # MyBatis Mapper 接口
│  │  ├─ service/                    # 业务逻辑
│  │  └─ util/                       # 安全上下文等工具类
│  ├─ src/main/resources/mapper/     # MyBatis XML
│  ├─ src/main/resources/sql/        # 建表与示例数据脚本
│  └─ pom.xml
├─ front_end/                        # Vue 3 前端
│  ├─ src/api/                       # Axios 封装
│  ├─ src/components/                # 布局、主题切换等公共组件
│  ├─ src/router/                    # 路由和前端权限守卫
│  ├─ src/stores/                    # Pinia Store
│  ├─ src/styles/                    # 全局主题样式
│  ├─ src/types/                     # TypeScript 类型
│  ├─ src/utils/                     # 前端工具函数
│  └─ src/views/                     # 页面视图
├─ docs/系统功能时序图.md
├─ 数据库表结构说明.md
├─ 技术路线与详细设计.md
└─ 前端界面设计说明.md
```

## 数据库表

当前建表脚本 `back_end/src/main/resources/sql/create_tables.sql` 创建 5 张核心业务表：

1. `sys_user`：系统用户、角色和个人资料。
2. `weather_stations`：气象站基础信息和经纬度。
3. `collection_tasks`：用户采集任务。
4. `meteor_data`：采集任务产出的用户气象要素数据。
5. `history_weather_data`：GIS 和历史气象查询使用的站点历史观测数据。

说明：旧的 `weather_data` 表和对应实体、Mapper 已删除，当前 GIS 管理端全量上传只导出 `weather_stations` 和 `history_weather_data`。

## HDFS 工作区约定

后端配置示例（实际配置见 `application.properties`）：

```properties
hdfs.uri=hdfs://YOUR_HDFS_HOST:8020
hdfs.user=YOUR_HDFS_USER
hdfs.base-path=/weather-platform
```

管理员页面所有 HDFS 路径都被限制在 `/weather-platform` 沙箱内。页面中的 `/raw-data` 实际对应 HDFS 的 `/weather-platform/raw-data`。

| 路径 | 用途 |
|---|---|
| `/raw-data` | 原始数据区 |
| `/raw-data/meteor` | 气象采集原始数据预留目录 |
| `/raw-data/history` | 历史气象数据预留目录 |
| `/raw-data/gis` | GIS 页面快照归档目录 |
| `/raw-data/gis/tables` | GIS 依赖表全量导出目录 |
| `/exports` | 用户报表和系统导出文件 |
| `/exports/reports/user-{id}` | 普通用户 Word/Excel 导出归档 |
| `/backups` | 备份文件 |
| `/temp` | 临时文件 |

## 快速启动

### 1. 初始化数据库

```bash
psql -U postgres -f back_end/src/main/resources/sql/create_tables.sql
```

如需导入示例历史气象数据，可继续执行仓库中的示例数据脚本。

### 2. 配置应用

复制配置模板并填入实际配置：

```bash
cd back_end/src/main/resources
cp application.properties.example application.properties
# 编辑 application.properties，填入数据库密码、JWT密钥、邮箱配置等
```

详细配置说明见 [SETUP.md](./SETUP.md)

### 3. 启动依赖

确保 PostgreSQL、Redis 和 HDFS 可用。默认配置使用：

- PostgreSQL：`localhost:5432/postgres`
- Redis：`localhost:6379`（根据实际情况配置密码）
- HDFS：配置在 `application.properties` 中

HDFS 首次使用前需要离开 SafeMode 并初始化平台目录：

```bash
hdfs dfsadmin -safemode leave
hdfs dfs -mkdir -p /weather-platform
hdfs dfs -chmod -R 777 /weather-platform
```

管理员页面也提供”初始化目录”按钮，会创建平台所需子目录。

### 4. 启动后端

```bash
cd back_end
mvn spring-boot:run
```

后端默认地址：`http://localhost:8080`

### 4. 启动前端

```bash
cd front_end
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`

## 常用验证命令

```bash
cd back_end
mvn clean test

cd ../front_end
npm run type-check
npm run build
```

## 账号与权限

- 普通用户：通过注册页面创建，注册时强制为 `role=0`。
- 管理员：由数据库或管理员后台创建/调整为 `role=1`。

## 相关文档

- `技术路线与详细设计.md`：系统技术路线、模块设计和 API 概览。
- `数据库表结构说明.md`：当前数据库表结构与关系。
- `前端界面设计说明.md`：前端页面结构、交互和主题系统。
- `docs/系统功能时序图.md`：主要业务流程 Mermaid 时序图。
- `front_end/README.md`：前端开发说明。
- `front_end/THEME_USAGE.md`：主题系统说明。
- `back_end/HELP.md`：后端开发和接口说明。

## License

本项目为毕业设计作品，仅供学习交流使用。
