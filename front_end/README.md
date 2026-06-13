# 前端项目说明

本目录是气象数据管理与分析系统的 Vue 3 前端工程。

## 技术栈

- Vue 3
- TypeScript
- Vite 7
- Vue Router
- Pinia
- Element Plus
- ECharts
- 百度地图 GL / MapVGL
- Axios
- Day.js

## 目录结构

```text
front_end/
├─ src/
│  ├─ api/              # Axios 封装
│  ├─ components/       # 公共组件和布局
│  ├─ data/             # 静态地区数据
│  ├─ router/           # 路由和权限守卫
│  ├─ stores/           # Pinia 状态
│  ├─ styles/           # 全局主题样式
│  ├─ types/            # TypeScript 类型
│  ├─ utils/            # 工具函数
│  └─ views/            # 页面
├─ THEME_USAGE.md       # 主题系统说明
└─ package.json
```

## 页面路由

### 公共页面

- `/welcome`：欢迎页。
- `/login`：登录页。
- `/register`：注册页。

### 普通用户

- `/dashboard/profile`：个人信息。
- `/dashboard/data-collection`：数据采集。
- `/dashboard/data-analysis/statistical`：统计分析和报表导出。
- `/dashboard/data-analysis/gis-map`：GIS 地图分析。
- `/dashboard/data-prediction`：数据预测。
- `/dashboard/personalized-service`：个性化服务和主题偏好。

### 管理员

- `/admin/user-management`：用户管理。
- `/admin/storage-management`：HDFS 工作区和 Redis 缓存管理。

## 权限逻辑

路由守卫读取本地存储：

- `token`：JWT。
- `role`：`0` 为普通用户，`1` 为管理员。

规则：

- 未登录访问业务页会跳转到 `/login`。
- 管理员访问普通用户页面会跳转到 `/admin`。
- 普通用户访问管理员页面会跳转到 `/dashboard`。

## API 约定

HTTP 实例位于 `src/api/http.ts`：

- 自动附加 `Authorization: Bearer <token>`。
- 401 时清理登录态并跳转登录页。
- 统一处理后端错误消息。

## 关键页面说明

### 统计分析

`src/views/StatisticalAnalysis.vue`

- 调用 `/api/meteor-data/**` 获取统计、时序、相关性、空间和明细数据。
- 支持导出 Word 分析报告和 Excel 明细。
- 导出文件由浏览器下载，同时后端会归档到 HDFS。

### GIS 地图

`src/views/WeatherMapView.vue`

- 展示气象站位置。
- 支持温度、湿度、降水要素展示。
- 支持地图点选查询最近站点。

### 管理员存储管理

`src/views/admin/StorageManagement.vue`

- HDFS：真实文件浏览、上传、下载、新建目录、重命名、删除、初始化目录、GIS 数据归档、GIS 表数据上传。
- 文件元数据：副本数、块大小、估算块数、HDFS 占用、Owner、Group、权限。
- Redis：状态总览、Key 查询、TTL 展示、按模式清理。

## 开发命令

```bash
npm install
npm run dev
npm run type-check
npm run build
```

## 环境

默认开发服务：

```text
http://localhost:5173
```

后端 API 默认：

```text
http://localhost:8080
```

如需修改代理或 API 地址，请检查 `src/api/http.ts` 和 Vite 配置。

## 主题系统

主题系统说明见 `THEME_USAGE.md`。

文档更新时间：2026-05-16。
