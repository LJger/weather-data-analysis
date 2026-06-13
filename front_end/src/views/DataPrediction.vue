<template>
  <div class="prediction-page">
    <!-- ====== 顶部概览卡片 ====== -->
    <div class="stats-overview" v-loading="summaryLoading">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon data-icon"><el-icon><DataLine /></el-icon></div>
              <div class="stat-info">
                <div class="stat-number">{{ overviewStats.dataCount }}</div>
                <div class="stat-label">可用数据点</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon predict-icon"><el-icon><TrendCharts /></el-icon></div>
              <div class="stat-info">
                <div class="stat-number">{{ overviewStats.predictionCount }}</div>
                <div class="stat-label">本次已预测</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon warn-icon"><el-icon><Warning /></el-icon></div>
              <div class="stat-info">
                <div class="stat-number">{{ overviewStats.warningCount }}</div>
                <div class="stat-label">预警提示</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- ====== 预测配置 + 结果 ====== -->
    <el-row :gutter="20" class="main-content">
      <!-- 左侧：预测配置面板 -->
      <el-col :span="8">
        <el-card class="config-card">
          <template #header>
            <div class="card-header">
              <h3>预测配置</h3>
            </div>
          </template>

          <el-form :model="predictionConfig" label-width="100px" label-position="top">
            <!-- 要素选择 -->
            <el-form-item label="预测要素">
              <el-select
                v-model="predictionConfig.elementCode"
                placeholder="选择气象要素"
                style="width: 100%"
                @change="handleElementChange"
              >
                <el-option
                  v-for="item in elementOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>

            <!-- 数据概况提示 -->
            <div v-if="dataInfo.dataCount > 0" class="data-info-tip">
              <el-text type="info" size="small">
                共 {{ dataInfo.dataCount }} 条数据，时间范围：{{ dataInfo.minTime }} ~ {{ dataInfo.maxTime }}
              </el-text>
            </div>
            <div v-else-if="predictionConfig.elementCode && !dataInfoLoading" class="data-info-tip">
              <el-text type="warning" size="small">
                该要素暂无数据，请先到「数据采集」页面采集
              </el-text>
            </div>

            <!-- 站点选择 -->
            <el-form-item label="目标站点">
              <el-select
                v-model="predictionConfig.stationIds"
                placeholder="全部站点（可多选筛选）"
                multiple
                collapse-tags
                collapse-tags-tooltip
                clearable
                style="width: 100%"
              >
                <el-option
                  v-for="s in stationOptions"
                  :key="s.value"
                  :label="s.label"
                  :value="s.value"
                />
              </el-select>
            </el-form-item>

            <!-- 历史数据时间范围 -->
            <el-form-item label="历史数据范围">
              <el-date-picker
                v-model="predictionConfig.timeRange"
                type="datetimerange"
                value-format="YYYY-MM-DDTHH:mm:ss"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                style="width: 100%"
              />
            </el-form-item>

            <!-- 聚合粒度 -->
            <el-form-item label="时间粒度">
              <el-radio-group v-model="predictionConfig.granularity">
                <el-radio-button value="hour">按小时</el-radio-button>
                <el-radio-button value="day">按天</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <el-divider>算法设置</el-divider>

            <!-- 算法选择 -->
            <el-form-item label="预测算法">
              <el-radio-group v-model="predictionConfig.algorithm" class="algorithm-radio-group">
                <el-radio
                  v-for="algo in algorithmList"
                  :key="algo.code"
                  :value="algo.code"
                  class="algorithm-radio"
                >
                  <div class="algo-label">
                    <span class="algo-name">{{ algo.name }}</span>
                    <span class="algo-desc">{{ algo.description }}</span>
                  </div>
                </el-radio>
              </el-radio-group>
            </el-form-item>

            <!-- 动态算法参数 -->
            <template v-if="predictionConfig.algorithm === 'moving_average'">
              <el-form-item label="窗口大小">
                <el-input-number
                  v-model="predictionConfig.movingWindowSize"
                  :min="3"
                  :max="20"
                  :step="1"
                  style="width: 100%"
                />
              </el-form-item>
            </template>
            <template v-if="predictionConfig.algorithm === 'exponential_smoothing'">
              <el-form-item label="平滑系数 α">
                <el-slider
                  v-model="predictionConfig.smoothingAlpha"
                  :min="0.1"
                  :max="0.9"
                  :step="0.05"
                  show-input
                  :show-input-controls="false"
                />
              </el-form-item>
            </template>

            <!-- 预测步数 -->
            <el-form-item label="预测步数">
              <el-input-number
                v-model="predictionConfig.forecastSteps"
                :min="1"
                :max="48"
                :step="1"
                style="width: 160px"
              />
              <el-text type="info" size="small" style="margin-left: 8px">
                {{ forecastHint }}
              </el-text>
            </el-form-item>

            <!-- 预警阈值 -->
            <el-collapse class="threshold-collapse">
              <el-collapse-item title="预警阈值设置（可选）" name="threshold">
                <el-form-item label="上限阈值">
                  <el-input-number
                    v-model="predictionConfig.warningThresholdHigh"
                    :precision="2"
                    :step="1"
                    clearable
                    placeholder="不设置"
                    style="width: 100%"
                  />
                </el-form-item>
                <el-form-item label="下限阈值">
                  <el-input-number
                    v-model="predictionConfig.warningThresholdLow"
                    :precision="2"
                    :step="1"
                    clearable
                    placeholder="不设置"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-collapse-item>
            </el-collapse>

            <!-- 执行按钮 -->
            <el-form-item style="margin-top: 20px">
              <el-button
                type="primary"
                @click="runPrediction"
                :loading="predicting"
                :disabled="!canPredict"
                style="width: 100%"
                size="large"
              >
                {{ predicting ? '预测中...' : '执行预测' }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 右侧：预测结果展示 -->
      <el-col :span="16">
        <!-- 空状态 -->
        <el-card v-if="!hasResult" class="result-card empty-result">
          <el-empty description="配置参数后点击「执行预测」查看结果">
            <template #image>
              <el-icon class="empty-result-icon"><TrendCharts /></el-icon>
            </template>
          </el-empty>
        </el-card>

        <!-- 预测结果 -->
        <template v-else>
          <!-- 评估指标卡片 -->
          <el-row :gutter="16" class="metrics-row">
            <el-col :span="8">
              <el-card class="metric-card" shadow="hover">
                <div class="metric-value">{{ formatMetric(predictionResult?.metrics?.mae) }}</div>
                <div class="metric-label">MAE（平均绝对误差）</div>
              </el-card>
            </el-col>
            <el-col :span="8">
              <el-card class="metric-card" shadow="hover">
                <div class="metric-value">{{ formatMetric(predictionResult?.metrics?.rmse) }}</div>
                <div class="metric-label">RMSE（均方根误差）</div>
              </el-card>
            </el-col>
            <el-col :span="8">
              <el-card class="metric-card" shadow="hover">
                <div class="metric-value" :class="r2Class">
                  {{ formatMetric(predictionResult?.metrics?.r2) }}
                </div>
                <div class="metric-label">R²（决定系数）</div>
              </el-card>
            </el-col>
          </el-row>

          <!-- Tabs：图表 + 数据表 -->
          <el-card class="result-card">
            <el-tabs v-model="activeResultTab">
              <el-tab-pane label="预测图表" name="chart">
                <div ref="chartRef" class="prediction-chart"></div>
              </el-tab-pane>
              <el-tab-pane name="table">
                <template #label>
                  <span>
                    预测数据
                    <el-badge
                      v-if="predictionResult?.warnings?.length"
                      :value="predictionResult.warnings.length"
                      type="danger"
                      style="margin-left: 4px"
                    />
                  </span>
                </template>
                <el-table
                  :data="forecastTableData"
                  stripe
                  border
                  style="width: 100%"
                  :row-class-name="tableRowClassName"
                  max-height="500"
                >
                  <el-table-column prop="datetime" label="预测时间" width="180" />
                  <el-table-column prop="value" label="预测值" width="120" align="right">
                    <template #default="scope">
                      <span :class="{ 'warning-value': isWarningPoint(scope.row.datetime) }">
                        {{ scope.row.value }}
                      </span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="upper" label="置信上界" width="120" align="right" />
                  <el-table-column prop="lower" label="置信下界" width="120" align="right" />
                  <el-table-column label="预警" width="100" align="center">
                    <template #default="scope">
                      <el-tag
                        v-if="getWarningType(scope.row.datetime) === 'high'"
                        type="danger"
                        size="small"
                      >
                        超上限
                      </el-tag>
                      <el-tag
                        v-else-if="getWarningType(scope.row.datetime) === 'low'"
                        type="warning"
                        size="small"
                      >
                        低于下限
                      </el-tag>
                      <el-tag v-else type="success" size="small">正常</el-tag>
                    </template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>
            </el-tabs>
          </el-card>
        </template>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { DataLine, TrendCharts, Warning } from '@element-plus/icons-vue'
import http from '@/api/http'
import * as echarts from 'echarts'
import dayjs from 'dayjs'

// ========================= 类型定义 =========================

interface AlgorithmInfo {
  code: string
  name: string
  description: string
  params: Array<{
    key: string
    label: string
    type: string
    min?: number
    max?: number
    step?: number
    default?: number
  }>
}

interface DataPointDTO {
  datetime: string
  value: number | null
  upper?: number | null
  lower?: number | null
}

interface WarningPointDTO {
  datetime: string
  value: number
  threshold: number
  type: 'high' | 'low'
}

interface PredictionMetrics {
  mae: number | null
  rmse: number | null
  r2: number | null
}

interface PredictionResultDTO {
  success: boolean
  message: string
  algorithm: string
  elementCode: string
  historyData: DataPointDTO[]
  forecastData: DataPointDTO[]
  warnings: WarningPointDTO[]
  metrics: PredictionMetrics
}

interface FilterOptions {
  tasks: Array<{ taskId: number; taskName: string }>
  stations: Array<{ stationId: number; stationName: string }>
  elements: string[]
}

interface DataInfo {
  minTime: string | null
  maxTime: string | null
  dataCount: number
}

// ========================= 要素中文映射 =========================

const ELEMENT_NAME_MAP: Record<string, string> = {
  PRS: '气压 (hPa)',
  PRS_Sea: '海平面气压 (hPa)',
  PRS_Max: '最高气压 (hPa)',
  PRS_Min: '最低气压 (hPa)',
  TEM: '气温 (℃)',
  TEM_MAX: '最高气温 (℃)',
  TEM_MIN: '最低气温 (℃)',
  RHU: '相对湿度 (%)',
  RHU_Min: '最小相对湿度 (%)',
  VAP: '水汽压 (hPa)',
  PRE_3h: '3小时降水量 (mm)',
  WIN_S_MAX: '最大风速 (m/s)',
  WIN_S_Avg_2mi: '2分钟平均风速 (m/s)',
  WIN_S_Inst_Max: '极大风速 (m/s)',
  VIS: '水平能见度 (m)',
  CLO_Cov: '总云量 (%)',
}

// ========================= 响应式状态 =========================

const summaryLoading = ref(false)
const predicting = ref(false)
const dataInfoLoading = ref(false)
const hasResult = ref(false)
const activeResultTab = ref('chart')

const overviewStats = reactive({
  dataCount: 0,
  predictionCount: 0,
  warningCount: 0,
})

const predictionConfig = reactive({
  elementCode: '',
  stationIds: [] as number[],
  timeRange: null as string[] | null,
  granularity: 'hour',
  algorithm: 'linear_regression',
  forecastSteps: 8,
  movingWindowSize: 5,
  smoothingAlpha: 0.3,
  warningThresholdHigh: undefined as number | undefined,
  warningThresholdLow: undefined as number | undefined,
})

const dataInfo = reactive<DataInfo>({
  minTime: null,
  maxTime: null,
  dataCount: 0,
})

const elementOptions = ref<Array<{ value: string; label: string }>>([])
const stationOptions = ref<Array<{ value: number; label: string }>>([])
const algorithmList = ref<AlgorithmInfo[]>([])
const predictionResult = ref<PredictionResultDTO | null>(null)

// 图表
const chartRef = ref<HTMLDivElement>()
let chartInstance: echarts.ECharts | null = null

// ========================= 计算属性 =========================

const canPredict = computed(() => {
  return (
    predictionConfig.elementCode &&
    predictionConfig.timeRange &&
    predictionConfig.timeRange.length === 2 &&
    dataInfo.dataCount > 0 &&
    !predicting.value
  )
})

const forecastHint = computed(() => {
  const steps = predictionConfig.forecastSteps
  if (predictionConfig.granularity === 'hour') {
    return `≈ ${steps} 小时`
  }
  return `≈ ${steps} 天`
})

const forecastTableData = computed(() => {
  return (predictionResult.value?.forecastData ?? []).map((d) => ({
    datetime: d.datetime,
    value: d.value != null ? Number(d.value).toFixed(2) : '--',
    upper: d.upper != null ? Number(d.upper).toFixed(2) : '--',
    lower: d.lower != null ? Number(d.lower).toFixed(2) : '--',
  }))
})

const r2Class = computed(() => {
  const r2 = predictionResult.value?.metrics?.r2
  if (r2 == null) return ''
  if (r2 >= 0.8) return 'metric-good'
  if (r2 >= 0.5) return 'metric-ok'
  return 'metric-bad'
})

// ========================= 工具函数 =========================

function getCurrentUserId(): number | null {
  const stored = localStorage.getItem('userId')
  if (!stored) return null
  const parsed = Number(stored)
  return Number.isNaN(parsed) ? null : parsed
}

function formatMetric(value: number | null | undefined): string {
  if (value == null) return '--'
  return Number(value).toFixed(4)
}

function isWarningPoint(datetime: string): boolean {
  return (predictionResult.value?.warnings ?? []).some((w) => w.datetime === datetime)
}

function getWarningType(datetime: string): string | null {
  const w = (predictionResult.value?.warnings ?? []).find((item) => item.datetime === datetime)
  return w?.type ?? null
}

function tableRowClassName({ row }: { row: any }): string {
  if (isWarningPoint(row.datetime)) return 'warning-row'
  return ''
}

// ========================= 数据加载 =========================

const loadFilterOptions = async () => {
  const userId = getCurrentUserId()
  if (!userId) return
  try {
    const { data } = await http.get<FilterOptions>('/api/meteor-data/filter-options', {
      params: { userId },
    })
    // 构建要素选项
    const elements = data?.elements ?? []
    elementOptions.value = elements.map((code: string) => ({
      value: code,
      label: ELEMENT_NAME_MAP[code] ?? code,
    }))
    // 构建站点选项
    const stations = data?.stations ?? []
    stationOptions.value = stations.map((s: any) => ({
      value: s.stationId,
      label: s.stationName ? `${s.stationName} (${s.stationId})` : String(s.stationId),
    }))
    // 默认选中第一个要素
    if (!predictionConfig.elementCode && elementOptions.value.length > 0) {
      predictionConfig.elementCode = elementOptions.value[0]!.value
      await loadDataInfo()
    }
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '加载筛选选项失败')
  }
}

const loadSummary = async () => {
  const userId = getCurrentUserId()
  if (!userId) return
  summaryLoading.value = true
  try {
    const { data } = await http.get<any>('/api/meteor-data/summary', {
      params: { userId },
    })
    overviewStats.dataCount = data?.totalDataPoints ?? 0
  } catch {
    // 静默处理
  } finally {
    summaryLoading.value = false
  }
}

const loadAlgorithms = async () => {
  try {
    const { data } = await http.get<AlgorithmInfo[]>('/api/prediction/algorithms')
    algorithmList.value = data ?? []
  } catch {
    // 使用默认算法列表
    algorithmList.value = [
      {
        code: 'linear_regression',
        name: '线性回归',
        description: '基于最小二乘法拟合线性趋势',
        params: [],
      },
      {
        code: 'moving_average',
        name: '移动平均',
        description: '滑动窗口平均值预测',
        params: [],
      },
      {
        code: 'exponential_smoothing',
        name: '指数平滑',
        description: '加权指数衰减平滑预测',
        params: [],
      },
    ]
  }
}

const loadDataInfo = async () => {
  const userId = getCurrentUserId()
  if (!userId || !predictionConfig.elementCode) return
  dataInfoLoading.value = true
  try {
    const { data } = await http.get<DataInfo>('/api/prediction/data-info', {
      params: { userId, elementCode: predictionConfig.elementCode },
    })
    dataInfo.minTime = data?.minTime ? dayjs(data.minTime).format('YYYY-MM-DD HH:mm') : null
    dataInfo.maxTime = data?.maxTime ? dayjs(data.maxTime).format('YYYY-MM-DD HH:mm') : null
    dataInfo.dataCount = data?.dataCount ?? 0

    // 自动填充时间范围
    if (data?.minTime && data?.maxTime) {
      predictionConfig.timeRange = [
        dayjs(data.minTime).format('YYYY-MM-DDTHH:mm:ss'),
        dayjs(data.maxTime).format('YYYY-MM-DDTHH:mm:ss'),
      ]
    }
  } catch {
    dataInfo.minTime = null
    dataInfo.maxTime = null
    dataInfo.dataCount = 0
  } finally {
    dataInfoLoading.value = false
  }
}

const handleElementChange = async () => {
  await loadDataInfo()
}

// ========================= 执行预测 =========================

const runPrediction = async () => {
  const userId = getCurrentUserId()
  if (!userId) {
    ElMessage.error('请先登录')
    return
  }
  if (!predictionConfig.elementCode) {
    ElMessage.warning('请选择预测要素')
    return
  }
  if (!predictionConfig.timeRange || predictionConfig.timeRange.length !== 2) {
    ElMessage.warning('请选择历史数据时间范围')
    return
  }

  predicting.value = true
  try {
    const requestBody = {
      userId,
      elementCode: predictionConfig.elementCode,
      stationIds: predictionConfig.stationIds.length > 0 ? predictionConfig.stationIds : null,
      taskIds: null,
      algorithm: predictionConfig.algorithm,
      historyStartTime: predictionConfig.timeRange[0],
      historyEndTime: predictionConfig.timeRange[1],
      forecastSteps: predictionConfig.forecastSteps,
      granularity: predictionConfig.granularity,
      movingWindowSize: predictionConfig.movingWindowSize,
      smoothingAlpha: predictionConfig.smoothingAlpha,
      warningThresholdHigh: predictionConfig.warningThresholdHigh ?? null,
      warningThresholdLow: predictionConfig.warningThresholdLow ?? null,
    }

    const { data } = await http.post<PredictionResultDTO>('/api/prediction/run', requestBody)

    if (!data.success) {
      ElMessage.error(data.message || '预测失败')
      return
    }

    predictionResult.value = data
    hasResult.value = true
    overviewStats.predictionCount++
    overviewStats.warningCount = data.warnings?.length ?? 0

    ElMessage.success('预测完成')

    // 渲染图表
    await nextTick()
    renderChart()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || '预测请求失败')
  } finally {
    predicting.value = false
  }
}

// ========================= ECharts 图表渲染 =========================

const renderChart = () => {
  if (!chartRef.value || !predictionResult.value) return

  if (chartInstance) {
    chartInstance.dispose()
  }
  chartInstance = echarts.init(chartRef.value)

  const result = predictionResult.value
  const historyData = result.historyData ?? []
  const forecastData = result.forecastData ?? []
  const warnings = result.warnings ?? []

  // 构建 X 轴和 Y 轴数据
  const historyX = historyData.map((d) => d.datetime)
  const historyY = historyData.map((d) => d.value)

  const forecastX = forecastData.map((d) => d.datetime)
  const forecastY = forecastData.map((d) => d.value)
  const forecastUpper = forecastData.map((d) => d.upper)
  const forecastLower = forecastData.map((d) => d.lower)

  // 连接最后一个历史点与预测起点
  const bridgeY = historyY.length > 0 ? [historyY[historyY.length - 1]] : []

  const allX = [...historyX, ...forecastX]
  const elementLabel = ELEMENT_NAME_MAP[result.elementCode] ?? result.elementCode
  const algorithmName =
    algorithmList.value.find((a) => a.code === result.algorithm)?.name ?? result.algorithm

  // 预警标记点
  const markPointData = warnings.map((w) => ({
    name: w.type === 'high' ? '超上限' : '低于下限',
    coord: [w.datetime, w.value] as [string, number],
    value: w.type === 'high' ? '⬆' : '⬇',
    itemStyle: { color: w.type === 'high' ? '#F56C6C' : '#E6A23C' },
  }))

  // 阈值标线
  const markLineData: any[] = []
  if (predictionConfig.warningThresholdHigh != null) {
    markLineData.push({
      yAxis: predictionConfig.warningThresholdHigh,
      label: { formatter: '上限', position: 'end' },
      lineStyle: { color: '#F56C6C', type: 'dashed', width: 2 },
    })
  }
  if (predictionConfig.warningThresholdLow != null) {
    markLineData.push({
      yAxis: predictionConfig.warningThresholdLow,
      label: { formatter: '下限', position: 'end' },
      lineStyle: { color: '#E6A23C', type: 'dashed', width: 2 },
    })
  }

  const option: echarts.EChartsOption = {
    title: {
      text: `${elementLabel} — ${algorithmName} 预测`,
      left: 'center',
      textStyle: { fontSize: 16 },
    },
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        if (!Array.isArray(params)) return ''
        let html = `<b>${params[0].axisValue}</b><br/>`
        params.forEach((p: any) => {
          if (p.value != null && p.value !== '-') {
            html += `${p.marker} ${p.seriesName}: ${Number(p.value).toFixed(2)}<br/>`
          }
        })
        return html
      },
    },
    legend: {
      top: 36,
      data: ['历史数据', '预测数据', '置信上界', '置信下界'],
    },
    grid: {
      left: 60,
      right: 40,
      bottom: 80,
      top: 80,
    },
    xAxis: {
      type: 'category',
      data: allX,
      axisLabel: {
        rotate: 30,
        fontSize: 10,
        formatter: (val: string) => (val.length > 16 ? val.substring(5, 16) : val),
      },
      boundaryGap: false,
    },
    yAxis: {
      type: 'value',
      name: elementLabel,
      nameTextStyle: { fontSize: 12 },
    },
    dataZoom: [
      { type: 'slider', bottom: 10, height: 25 },
      { type: 'inside' },
    ],
    series: [
      // 历史数据系列
      {
        name: '历史数据',
        type: 'line',
        data: [...historyY, ...forecastX.map(() => '-')],
        smooth: true,
        lineStyle: { width: 2, color: '#409EFF' },
        itemStyle: { color: '#409EFF' },
        symbol: 'circle',
        symbolSize: 3,
      },
      // 预测数据系列（含桥接点）
      {
        name: '预测数据',
        type: 'line',
        data: [...historyX.slice(0, -1).map(() => '-'), bridgeY[0], ...forecastY],
        smooth: true,
        lineStyle: { width: 2, color: '#E6A23C', type: 'dashed' },
        itemStyle: { color: '#E6A23C' },
        symbol: 'diamond',
        symbolSize: 5,
        markPoint:
          markPointData.length > 0
            ? { data: markPointData, symbolSize: 30, label: { show: true, fontSize: 14 } }
            : undefined,
        markLine:
          markLineData.length > 0 ? { data: markLineData, silent: true } : undefined,
      },
      // 置信区间上界
      {
        name: '置信上界',
        type: 'line',
        data: [...historyX.map(() => '-'), ...forecastUpper],
        lineStyle: { width: 0 },
        itemStyle: { color: 'transparent' },
        symbol: 'none',
        areaStyle: { color: 'rgba(230, 162, 60, 0.15)' },
        stack: 'confidence',
      },
      // 置信区间下界
      {
        name: '置信下界',
        type: 'line',
        data: [...historyX.map(() => '-'), ...forecastLower],
        lineStyle: { width: 1, type: 'dotted', color: 'rgba(230, 162, 60, 0.4)' },
        itemStyle: { color: 'transparent' },
        symbol: 'none',
        areaStyle: { color: 'rgba(255, 255, 255, 1)' },
        stack: 'confidence',
      },
    ],
  }

  chartInstance.setOption(option)
}

// ========================= 生命周期 =========================

const handleResize = () => {
  chartInstance?.resize()
}

onMounted(async () => {
  await Promise.all([loadFilterOptions(), loadSummary(), loadAlgorithms()])
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
  chartInstance = null
})

// 切换到图表 tab 时重新 resize，确保正确渲染
watch(activeResultTab, (val) => {
  if (val === 'chart') {
    nextTick(() => chartInstance?.resize())
  }
})
</script>

<style scoped>
.prediction-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 0 32px;
}

/* ====== 概览卡片 ====== */
.stats-overview {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 8px;
  transition: border-color 0.2s;
}

.stat-card:hover {
  border-color: var(--el-color-primary-light-7);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 4px 0;
}

.stat-icon {
  font-size: 24px;
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: #fff;
}

.data-icon {
  background: #1d4f91;
}
.predict-icon {
  background: #2f855a;
}
.warn-icon {
  background: #b7791f;
}

.empty-result-icon {
  font-size: 56px;
  color: var(--el-color-primary-light-5);
}

.stat-info {
  flex: 1;
}

.stat-number {
  font-size: 28px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.stat-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}

/* ====== 主体区域 ====== */
.main-content {
  min-height: 600px;
}

.config-card {
  position: sticky;
  top: 20px;
}

.config-card .card-header h3 {
  margin: 0;
  font-size: 16px;
}

.data-info-tip {
  margin: -8px 0 12px;
  padding: 6px 10px;
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
  line-height: 1.5;
}

/* ====== 算法选择 ====== */
.algorithm-radio-group {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 10px;
  width: 100%;
}

.algorithm-radio {
  display: flex !important;
  align-items: flex-start !important;
  width: 100%;
  margin-right: 0 !important;
  height: auto !important;
  padding: 2px 0;
  white-space: normal;
}

.algorithm-radio :deep(.el-radio__input) {
  flex: 0 0 16px;
  margin-top: 3px;
}

.algorithm-radio :deep(.el-radio__label) {
  flex: 1;
  min-width: 0;
  padding-left: 10px;
  line-height: 1.4;
}

.algo-label {
  display: grid;
  gap: 3px;
  width: 100%;
}

.algo-name {
  font-weight: 600;
  font-size: 14px;
  line-height: 1.35;
}

.algo-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
}

/* ====== 阈值折叠 ====== */
.threshold-collapse {
  margin-bottom: 8px;
  border: none;
}

.threshold-collapse :deep(.el-collapse-item__header) {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  height: 36px;
  line-height: 36px;
}

/* ====== 结果区 ====== */
.result-card {
  border-radius: 12px;
}

.empty-result {
  min-height: 500px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.prediction-chart {
  width: 100%;
  height: 450px;
}

/* ====== 评估指标 ====== */
.metrics-row {
  margin-bottom: 16px;
}

.metric-card {
  text-align: center;
  border-radius: 10px;
  padding: 8px 0;
}

.metric-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  font-family: 'Courier New', monospace;
}

.metric-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

.metric-good {
  color: #67c23a;
}
.metric-ok {
  color: #e6a23c;
}
.metric-bad {
  color: #f56c6c;
}

/* ====== 预警样式 ====== */
:deep(.warning-row) {
  background-color: rgba(245, 108, 108, 0.08) !important;
}

.warning-value {
  color: #f56c6c;
  font-weight: 600;
}

/* ====== 响应式 ====== */
@media (max-width: 1200px) {
  .main-content > .el-col:first-child {
    margin-bottom: 16px;
  }
}
</style>
