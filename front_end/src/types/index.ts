/**
 * 通用 API 类型定义
 */

// 用户相关
export interface User {
  userId: number
  username: string
  email: string | null
  phone: string | null
  gender: number | null // 0:未知 1:男 2:女
  age: number | null
  region: string | null
  organization: string | null
  bio: string | null
  avatarUrl: string | null
  role: number // 0:普通用户 1:管理员
  createdAt: string
  updatedAt: string
}

export interface AuthResponse {
  success: boolean
  message: string
  token?: string
  userId?: number
  username?: string
  role?: number
  email?: string
  phone?: string
  gender?: number
  age?: number
  region?: string
  organization?: string
  bio?: string
  avatarUrl?: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  email?: string
  emailCode?: string
  phone?: string
  gender?: number
  age?: number
  region?: string
  organization?: string
  bio?: string
  userType?: string
}

export interface UpdateProfileRequest {
  email?: string
  phone?: string
  gender?: number
  age?: number
  region?: string
  organization?: string
  bio?: string
}

// 采集任务
export interface CollectionTask {
  taskId: number
  userId: number
  taskName: string
  stationIds: number[]
  elementCodes: string[]
  startTime: string
  endTime: string
  status: string
  createdAt: string
  recordCount?: number
}

export interface CreateCollectionTaskRequest {
  userId: number
  taskName: string
  stationIds: number[]
  elementCodes: string[]
  startTime: string
  endTime: string
}

// 气象站
export interface WeatherStation {
  stationId: string
  stationName: string
  province: string
  latitude: number
  longitude: number
  altitude: number
}

// 气象数据记录
export interface MeteorDataRecord {
  id: number
  taskId: number
  stationId: number
  stationName: string
  elementCode: string
  datetime: string
  value: number
}

// 统计分析
export interface MeteorDataSummary {
  totalRecords: number
  taskCount: number
  stationCount: number
  anomalyCount: number
}

export interface TimeSeriesPoint {
  datetime: string
  value: number
  count?: number
}

export interface SpatialPoint {
  stationId: number
  stationName: string
  lat: number
  lng: number
  avg: number
  count: number
}

export interface CorrelationResult {
  element1: string
  element2: string
  coefficient: number
  sampleCount: number
}

export interface FilterOptions {
  tasks: { taskId: number; taskName: string }[]
  stations: { stationId: number; stationName: string }[]
  elements: string[]
}

// 预测
export interface PredictionRequest {
  userId: number
  elementCode: string
  stationId?: number
  algorithm: string
  forecastSteps: number
}

export interface PredictionResult {
  algorithm: string
  forecast: { datetime: string; value: number }[]
  historical: { datetime: string; value: number }[]
  metrics: {
    mae: number
    rmse: number
    r2: number
  }
}

// 分页
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

// 管理员
export interface UserStatistics {
  totalUsers: number
  adminCount: number
  normalCount: number
  genderDistribution: {
    male: number
    female: number
    unknown: number
  }
}
