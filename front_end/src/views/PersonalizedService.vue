<template>
  <div class="personalized-service">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>系统设置</h2>
      <p class="page-desc">查看数据概览，维护界面偏好与默认分析参数</p>
    </div>

    <!-- 数据概览卡片 -->
    <el-row :gutter="16" class="overview-row">
      <el-col :xs="12" :sm="6" v-for="card in overviewCards" :key="card.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" :style="{ backgroundColor: card.bgColor }">
              <el-icon :size="28" color="#fff"><component :is="card.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ card.value }}</div>
              <div class="stat-label">{{ card.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最新数据 + 数据分布 -->
    <el-row :gutter="16" class="content-row">
      <el-col :span="15">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span><el-icon><Clock /></el-icon> 我的最新数据</span>
              <el-button size="small" text type="primary" @click="loadLatestData">
                <el-icon><Refresh /></el-icon> 刷新
              </el-button>
            </div>
          </template>
          <el-table :data="latestData" v-loading="loadingLatest" stripe size="small" max-height="330">
            <el-table-column prop="elementCode" label="要素" width="110">
              <template #default="{ row }">
                <el-tag size="small" :type="getElementTagType(row.elementCode)">
                  {{ getElementName(row.elementCode) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="stationId" label="站点ID" width="80" align="center" />
            <el-table-column prop="value" label="观测值" width="120" align="right">
              <template #default="{ row }">
                <span class="data-value">{{ row.value }}</span>
                <span class="data-unit">{{ getElementUnit(row.elementCode) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="datetime" label="观测时间" min-width="170">
              <template #default="{ row }">{{ formatDatetime(row.datetime) }}</template>
            </el-table-column>
            <el-table-column prop="taskId" label="任务" width="70" align="center" />
          </el-table>
          <el-empty v-if="!loadingLatest && latestData.length === 0" description="暂无数据，请先在数据采集页面创建任务" :image-size="60" />
        </el-card>
      </el-col>

      <el-col :span="9">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><DataAnalysis /></el-icon> 要素数据分布</span>
            </div>
          </template>
          <div ref="chartRef" class="distribution-chart"></div>
          <el-empty v-if="!loadingDistribution && distributionData.length === 0" description="暂无数据" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 界面设置 + 分析偏好 -->
    <el-row :gutter="16" class="content-row settings-row">
      <!-- 界面设置 + 快捷操作 -->
      <el-col :xs="24" :lg="11">
        <el-card shadow="hover" class="settings-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><Brush /></el-icon> 界面设置</span>
              <el-button size="small" text type="warning" @click="handleResetTheme">重置默认</el-button>
            </div>
          </template>
          <el-form label-width="82px" size="small" class="appearance-form">
            <el-form-item label="主色">
              <el-color-picker v-model="themeStore.currentTheme.primaryColor" @change="onThemeFieldChange" />
            </el-form-item>
            <el-form-item label="背景">
              <el-radio-group v-model="themeStore.currentTheme.backgroundMode" @change="onThemeFieldChange">
                <el-radio-button value="light">浅色</el-radio-button>
                <el-radio-button value="dark">深色</el-radio-button>
                <el-radio-button value="auto">跟随系统</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="字体大小">
              <el-slider v-model="themeStore.currentTheme.fontSize" :min="12" :max="20" :step="1" show-stops @change="onThemeFieldChange" style="width: 200px" />
              <span class="slider-val">{{ themeStore.currentTheme.fontSize }}px</span>
            </el-form-item>
            <el-form-item label="圆角大小">
              <el-slider v-model="themeStore.currentTheme.borderRadius" :min="0" :max="12" :step="2" @change="onThemeFieldChange" style="width: 200px" />
              <span class="slider-val">{{ themeStore.currentTheme.borderRadius }}px</span>
            </el-form-item>
          </el-form>
          <el-divider />
          <div class="section-label">快捷操作</div>
          <div class="quick-actions">
            <el-button type="primary" plain @click="goTo('/dashboard/data-collection')">
              <el-icon><FolderAdd /></el-icon> 数据采集
            </el-button>
            <el-button type="success" plain @click="goTo('/dashboard/data-analysis/statistical')">
              <el-icon><TrendCharts /></el-icon> 统计分析
            </el-button>
            <el-button type="warning" plain @click="goTo('/dashboard/data-prediction')">
              <el-icon><DataLine /></el-icon> 数据预测
            </el-button>
            <el-button plain @click="goTo('/dashboard/data-analysis/gis-map')">
              <el-icon><MapLocation /></el-icon> 气象地图
            </el-button>
          </div>
        </el-card>
      </el-col>

      <!-- 分析偏好 -->
      <el-col :xs="24" :lg="13">
        <el-card shadow="hover" class="settings-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><Setting /></el-icon> 分析偏好</span>
              <div>
                <el-button size="small" text type="warning" @click="resetPreferences">重置</el-button>
                <el-button size="small" text type="primary" @click="savePreferences">保存</el-button>
              </div>
            </div>
          </template>
          <el-form :model="preferences" label-width="100px" size="small">
            <el-form-item label="默认要素">
              <el-select v-model="preferences.defaultElement" placeholder="选择默认分析要素" clearable style="width: 100%">
                <el-option v-for="el in availableElements" :key="el" :label="getElementName(el)" :value="el" />
              </el-select>
            </el-form-item>
            <el-form-item label="时间粒度">
              <el-radio-group v-model="preferences.defaultGranularity">
                <el-radio-button value="hour">小时</el-radio-button>
                <el-radio-button value="day">天</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="预测算法">
              <el-select v-model="preferences.defaultAlgorithm" style="width: 100%">
                <el-option label="线性回归" value="linear_regression" />
                <el-option label="移动平均" value="moving_average" />
                <el-option label="指数平滑" value="exponential_smoothing" />
              </el-select>
            </el-form-item>
            <el-form-item label="预测步数">
              <el-input-number v-model="preferences.defaultForecastSteps" :min="1" :max="48" />
            </el-form-item>
            <el-form-item label="数据分页">
              <el-select v-model="preferences.defaultPageSize" style="width: 100%">
                <el-option label="每页 10 条" :value="10" />
                <el-option label="每页 20 条" :value="20" />
                <el-option label="每页 50 条" :value="50" />
              </el-select>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  DataLine, DataAnalysis, TrendCharts,
  Clock, Refresh, Brush, Setting,
  FolderAdd, MapLocation, OfficeBuilding, Odometer, Folder,
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import http from '@/api/http'
import { useThemeStore } from '@/stores/theme'

const router = useRouter()
const themeStore = useThemeStore()
const userId = localStorage.getItem('userId')

/* ============ 数据概览 ============ */
interface SummaryData {
  totalDataPoints: number
  taskCount: number
  stationCount: number
  elementCount: number
  lastUpdated: string | null
}

const summaryData = ref<SummaryData>({
  totalDataPoints: 0,
  taskCount: 0,
  stationCount: 0,
  elementCount: 0,
  lastUpdated: null,
})

const overviewCards = computed(() => [
  { label: '数据总量', value: summaryData.value.totalDataPoints, icon: DataLine, bgColor: '#1d4f91' },
  { label: '站点数', value: summaryData.value.stationCount, icon: OfficeBuilding, bgColor: '#2f855a' },
  { label: '要素种类', value: summaryData.value.elementCount, icon: Odometer, bgColor: '#b7791f' },
  { label: '采集任务', value: summaryData.value.taskCount, icon: Folder, bgColor: '#52616f' },
])

async function loadSummary() {
  if (!userId) return
  try {
    const { data } = await http.get('/api/meteor-data/summary', { params: { userId } })
    summaryData.value = data
  } catch {
    console.warn('加载数据概览失败')
  }
}

/* ============ 最新数据 ============ */
interface LatestRecord {
  id: number
  userId: number
  taskId: number
  stationId: number
  elementCode: string
  datetime: string
  value: number
}

const latestData = ref<LatestRecord[]>([])
const loadingLatest = ref(false)

async function loadLatestData() {
  if (!userId) return
  loadingLatest.value = true
  try {
    const { data } = await http.get('/api/meteor-data/latest', {
      params: { userId, limit: 20 },
    })
    latestData.value = data
  } catch {
    ElMessage.error('加载最新数据失败')
  } finally {
    loadingLatest.value = false
  }
}

/* ============ 要素数据分布（饼图） ============ */
interface DistributionItem {
  elementCode: string
  count: number
}

const distributionData = ref<DistributionItem[]>([])
const loadingDistribution = ref(false)
const chartRef = ref<HTMLDivElement>()
let chartInstance: echarts.ECharts | null = null

async function loadDistribution() {
  if (!userId) return
  loadingDistribution.value = true
  try {
    const { data } = await http.get('/api/meteor-data/element-distribution', {
      params: { userId },
    })
    distributionData.value = data
    await nextTick()
    renderChart()
  } catch {
    console.warn('加载要素分布失败')
  } finally {
    loadingDistribution.value = false
  }
}

function renderChart() {
  if (!chartRef.value || distributionData.value.length === 0) return
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }
  const pieData = distributionData.value.map((d) => ({
    name: getElementName(d.elementCode),
    value: d.count,
  }))
  chartInstance.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} 条 ({d}%)' },
    legend: { bottom: 0, type: 'scroll', textStyle: { fontSize: 11 } },
    series: [
      {
        type: 'pie',
        radius: ['35%', '60%'],
        center: ['50%', '42%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        emphasis: {
          label: { show: true, fontSize: 13, fontWeight: 'bold' },
        },
        data: pieData,
      },
    ],
  })
}

/* ============ 可用要素列表 ============ */
const availableElements = ref<string[]>([])

async function loadElements() {
  if (!userId) return
  try {
    const { data } = await http.get('/api/meteor-data/elements', { params: { userId } })
    availableElements.value = data
  } catch {
    console.warn('加载要素列表失败')
  }
}

/* ============ 要素名称/单位映射 ============ */
const elementNameMap: Record<string, string> = {
  TEM: '气温', TEM_Max: '最高气温', TEM_Min: '最低气温',
  PRE_1h: '1小时降水', PRE_3h: '3小时降水', PRE_6h: '6小时降水', PRE_24h: '24小时降水',
  RHU: '相对湿度', RHU_Min: '最小湿度',
  PRS: '气压', PRS_Sea: '海平面气压', PRS_Max: '最高气压', PRS_Min: '最低气压',
  WIN_S_Avg_2mi: '2分钟平均风速', WIN_S_Max: '最大风速', WIN_S_Inst_Max: '极大风速',
  WIN_D_Avg_2mi: '2分钟平均风向', WIN_D_S_Max: '最大风速风向', WIN_D_INST_Max: '极大风速风向',
  VIS: '能见度', CLO_Cov: '总云量', CLO_Cov_Low: '低云量',
  VAP: '水汽压', WEP_Now: '天气现象',
}

const elementUnitMap: Record<string, string> = {
  TEM: '℃', TEM_Max: '℃', TEM_Min: '℃',
  PRE_1h: 'mm', PRE_3h: 'mm', PRE_6h: 'mm', PRE_24h: 'mm',
  RHU: '%', RHU_Min: '%',
  PRS: 'hPa', PRS_Sea: 'hPa', PRS_Max: 'hPa', PRS_Min: 'hPa',
  WIN_S_Avg_2mi: 'm/s', WIN_S_Max: 'm/s', WIN_S_Inst_Max: 'm/s',
  WIN_D_Avg_2mi: '°', WIN_D_S_Max: '°', WIN_D_INST_Max: '°',
  VIS: 'm', CLO_Cov: '%', CLO_Cov_Low: '%',
  VAP: 'hPa',
}

const elementTagTypes: Record<string, '' | 'success' | 'warning' | 'danger' | 'info'> = {
  TEM: 'danger', PRE_1h: 'primary' as '', PRE_3h: '' as '', RHU: 'success',
  PRS: 'info', WIN_S_Avg_2mi: 'warning', VIS: 'info', CLO_Cov: '',
}

function getElementName(code: string): string {
  return elementNameMap[code] || code
}

function getElementUnit(code: string): string {
  return elementUnitMap[code] || ''
}

function getElementTagType(code: string): '' | 'success' | 'warning' | 'danger' | 'info' {
  return elementTagTypes[code] || ''
}

function formatDatetime(dt: string): string {
  if (!dt) return '-'
  return dayjs(dt).format('YYYY-MM-DD HH:mm')
}

/* ============ 界面设置 ============ */
function onThemeFieldChange() {
  themeStore.applyTheme({ ...themeStore.currentTheme })
}

function handleResetTheme() {
  themeStore.resetTheme()
  ElMessage.success('已重置为默认主题')
}

/* ============ 分析偏好（localStorage） ============ */
interface UserPreferences {
  defaultElement: string
  defaultGranularity: string
  defaultAlgorithm: string
  defaultForecastSteps: number
  defaultPageSize: number
}

const defaultPreferences: UserPreferences = {
  defaultElement: '',
  defaultGranularity: 'hour',
  defaultAlgorithm: 'linear_regression',
  defaultForecastSteps: 8,
  defaultPageSize: 20,
}

const preferences = reactive<UserPreferences>({ ...defaultPreferences })

function loadPreferences() {
  try {
    const saved = localStorage.getItem('user-analysis-preferences')
    if (saved) {
      Object.assign(preferences, { ...defaultPreferences, ...JSON.parse(saved) })
    }
  } catch {
    console.warn('加载偏好设置失败')
  }
}

function savePreferences() {
  try {
    localStorage.setItem('user-analysis-preferences', JSON.stringify({ ...preferences }))
    ElMessage.success('偏好设置已保存')
  } catch {
    ElMessage.error('保存失败')
  }
}

function resetPreferences() {
  Object.assign(preferences, { ...defaultPreferences })
  localStorage.removeItem('user-analysis-preferences')
  ElMessage.success('已重置偏好设置')
}

/* ============ 快捷导航 ============ */
function goTo(path: string) {
  router.push(path)
}

/* ============ 窗口resize ============ */
function handleResize() {
  chartInstance?.resize()
}

/* ============ 生命周期 ============ */
onMounted(async () => {
  loadPreferences()
  // 并行加载数据
  await Promise.all([loadSummary(), loadLatestData(), loadDistribution(), loadElements()])
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
  chartInstance = null
})
</script>

<style scoped>
.personalized-service {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 4px;
  font-size: 22px;
  color: var(--el-text-color-primary);
}

.page-desc {
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

/* 概览卡片 */
.overview-row {
  margin-bottom: 16px;
}

.stat-card {
  cursor: default;
}

.stat-card :deep(.el-card__body) {
  padding: 18px 16px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 14px;
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.2;
  color: var(--el-text-color-primary);
}

.stat-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}

/* 内容行 */
.content-row {
  margin-bottom: 16px;
}

.settings-row {
  align-items: stretch;
}

.settings-row :deep(.el-col) {
  display: flex;
}

.settings-card {
  width: 100%;
}

.settings-card :deep(.el-card__body) {
  padding: 22px 24px;
}

/* 卡片头部 */
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 15px;
  font-weight: 600;
}

.card-header span {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 数据表格 */
.data-value {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.data-unit {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  margin-left: 3px;
}

/* 饼图 */
.chart-card :deep(.el-card__body) {
  padding: 12px 16px 8px;
}

.distribution-chart {
  width: 100%;
  height: 310px;
}

/* 界面设置 */
.appearance-form {
  max-width: 560px;
}

.section-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-regular);
  margin: 4px 0 12px;
}

.slider-val {
  margin-left: 10px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  min-width: 36px;
  display: inline-block;
}

/* 快捷操作 */
.quick-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(140px, 1fr));
  gap: 12px;
}

.quick-actions .el-button {
  width: 100%;
  min-width: 0;
  margin-left: 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .personalized-service {
    padding: 12px;
  }
  .overview-row .el-col {
    margin-bottom: 12px;
  }
  .content-row .el-col {
    margin-bottom: 12px;
  }
  .settings-card :deep(.el-card__body) {
    padding: 18px 16px;
  }
  .quick-actions {
    grid-template-columns: 1fr;
  }
}
</style>
