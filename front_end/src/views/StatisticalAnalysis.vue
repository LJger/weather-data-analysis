<template>
  <div class="statistical-analysis">
    <div class="stats-overview" v-loading="summaryLoading">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon total-data">
                <el-icon><DataLine /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ analysisStats.totalDataPoints }}</div>
                <div class="stat-label">数据点总数</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon analysis-tasks">
                <el-icon><Folder /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ analysisStats.taskCount }}</div>
                <div class="stat-label">关联任务数</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon stations">
                <el-icon><OfficeBuilding /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ analysisStats.stationCount }}</div>
                <div class="stat-label">覆盖站点数</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon elements">
                <el-icon><Odometer /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ analysisStats.elementCount }}</div>
                <div class="stat-label">覆盖要素数</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <div class="last-updated">
        最近更新时间：{{ lastUpdatedText }} · 异常记录估算：{{ analysisStats.anomalyCount }}
      </div>
    </div>

    <el-card class="analysis-type-card">
      <template #header>
        <div class="card-header">
          <h3>统计分析</h3>
          <el-button type="primary" :loading="analysisExporting" @click="exportAnalysisReport">
            <el-icon><Download /></el-icon>
            导出 Word
          </el-button>
        </div>
      </template>
      <el-tabs v-model="activeAnalysisType" @tab-change="handleAnalysisTypeChange">
        <el-tab-pane label="时间序列分析" name="timeSeries">
          <el-row :gutter="20" class="analysis-content">
            <el-col :span="8">
              <el-card class="config-card">
                <template #header>
                  <h4>时间序列配置</h4>
                </template>
                <el-form :model="timeSeriesConfig" label-width="90px">
                  <el-form-item label="时间范围">
                    <el-date-picker
                      v-model="timeSeriesConfig.timeRange"
                      type="datetimerange"
                      value-format="YYYY-MM-DDTHH:mm:ss[Z]"
                      range-separator="至"
                      start-placeholder="开始时间"
                      end-placeholder="结束时间"
                      style="width: 100%"
                    />
                  </el-form-item>
                  <el-form-item label="采集任务">
                    <el-select
                      v-model="timeSeriesConfig.taskIds"
                      multiple
                      collapse-tags
                      collapse-tags-tooltip
                      placeholder="全部任务"
                      style="width: 100%"
                    >
                      <el-option
                        v-for="task in filterOptions.tasks"
                        :key="task.taskId"
                        :label="task.taskName"
                        :value="task.taskId"
                      />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="覆盖站点">
                    <el-select
                      v-model="timeSeriesConfig.stationIds"
                      multiple
                      collapse-tags
                      collapse-tags-tooltip
                      placeholder="全部站点"
                      style="width: 100%"
                    >
                      <el-option
                        v-for="station in filterOptions.stations"
                        :key="station.stationId"
                        :label="`${station.stationName} (${station.stationId})`"
                        :value="station.stationId"
                      />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="气象要素">
                    <el-select
                      v-model="timeSeriesConfig.elementCode"
                      placeholder="请选择要素"
                      style="width: 100%"
                    >
                      <el-option
                        v-for="option in elementOptions"
                        :key="option.value"
                        :label="option.label"
                        :value="option.value"
                      />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="统计粒度">
                    <el-select v-model="timeSeriesConfig.granularity" style="width: 100%">
                      <el-option label="小时" value="hour" />
                      <el-option label="天" value="day" />
                      <el-option label="周" value="week" />
                      <el-option label="月" value="month" />
                    </el-select>
                  </el-form-item>
                  <el-form-item>
                    <el-button
                      type="primary"
                      :loading="timeSeriesLoading"
                      @click="runTimeSeriesAnalysis"
                    >
                      开始分析
                    </el-button>
                  </el-form-item>
                </el-form>
              </el-card>
            </el-col>
            <el-col :span="16">
              <el-card class="result-card" v-loading="timeSeriesLoading">
                <template #header>
                  <h4>时间序列结果</h4>
                </template>
                <div ref="timeSeriesChartRef" class="chart-container"></div>
              </el-card>
            </el-col>
          </el-row>
        </el-tab-pane>

        <el-tab-pane label="要素相关性分析" name="correlation">
          <el-row :gutter="20" class="analysis-content">
            <el-col :span="8">
              <el-card class="config-card">
                <template #header>
                  <h4>相关性配置</h4>
                </template>
                <el-form :model="correlationConfig" label-width="90px">
                  <el-form-item label="时间范围">
                    <el-date-picker
                      v-model="correlationConfig.timeRange"
                      type="datetimerange"
                      value-format="YYYY-MM-DDTHH:mm:ss[Z]"
                      range-separator="至"
                      start-placeholder="开始时间"
                      end-placeholder="结束时间"
                      style="width: 100%"
                    />
                  </el-form-item>
                  <el-form-item label="采集任务">
                    <el-select
                      v-model="correlationConfig.taskIds"
                      multiple
                      collapse-tags
                      collapse-tags-tooltip
                      placeholder="全部任务"
                      style="width: 100%"
                    >
                      <el-option
                        v-for="task in filterOptions.tasks"
                        :key="task.taskId"
                        :label="task.taskName"
                        :value="task.taskId"
                      />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="覆盖站点">
                    <el-select
                      v-model="correlationConfig.stationIds"
                      multiple
                      collapse-tags
                      collapse-tags-tooltip
                      placeholder="全部站点"
                      style="width: 100%"
                    >
                      <el-option
                        v-for="station in filterOptions.stations"
                        :key="station.stationId"
                        :label="`${station.stationName} (${station.stationId})`"
                        :value="station.stationId"
                      />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="气象要素">
                    <el-select
                      v-model="correlationConfig.elementCodes"
                      multiple
                      collapse-tags
                      collapse-tags-tooltip
                      placeholder="请选择至少两个要素"
                      style="width: 100%"
                    >
                      <el-option
                        v-for="option in elementOptions"
                        :key="option.value"
                        :label="option.label"
                        :value="option.value"
                      />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="统计粒度">
                    <el-select v-model="correlationConfig.granularity" style="width: 100%">
                      <el-option label="小时" value="hour" />
                      <el-option label="天" value="day" />
                      <el-option label="周" value="week" />
                      <el-option label="月" value="month" />
                    </el-select>
                  </el-form-item>
                  <el-form-item>
                    <el-button
                      type="primary"
                      :loading="correlationLoading"
                      @click="runCorrelationAnalysis"
                    >
                      计算相关性
                    </el-button>
                  </el-form-item>
                </el-form>
              </el-card>
            </el-col>
            <el-col :span="16">
              <el-card class="result-card" v-loading="correlationLoading">
                <template #header>
                  <h4>相关性热力图</h4>
                </template>
                <div ref="correlationChartRef" class="chart-container correlation-chart-container"></div>
              </el-card>
            </el-col>
          </el-row>
        </el-tab-pane>

        <el-tab-pane label="空间分析" name="spatial">
          <el-row :gutter="20" class="analysis-content">
            <el-col :span="7">
              <el-card class="config-card">
                <template #header>
                  <h4>空间分析配置</h4>
                </template>
                <el-form :model="spatialConfig" label-width="90px">
                  <el-form-item label="时间范围">
                    <el-date-picker
                      v-model="spatialConfig.timeRange"
                      type="datetimerange"
                      value-format="YYYY-MM-DDTHH:mm:ss[Z]"
                      range-separator="至"
                      start-placeholder="开始时间"
                      end-placeholder="结束时间"
                      style="width: 100%"
                    />
                  </el-form-item>
                  <el-form-item label="气象要素">
                    <el-select
                      v-model="spatialConfig.elementCode"
                      placeholder="请选择要素"
                      style="width: 100%"
                    >
                      <el-option
                        v-for="option in elementOptions"
                        :key="option.value"
                        :label="option.label"
                        :value="option.value"
                      />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="省份">
                    <el-select
                      v-model="spatialConfig.province"
                      clearable
                      filterable
                      placeholder="全国"
                      style="width: 100%"
                    >
                      <el-option
                        v-for="province in availableProvinces"
                        :key="province"
                        :label="province"
                        :value="province"
                      />
                    </el-select>
                  </el-form-item>
                  <el-form-item>
                    <div class="map-actions">
                      <el-button
                        type="primary"
                        :loading="spatialLoading"
                        @click="runSpatialAnalysis"
                      >
                        生成空间分布
                      </el-button>
                      <el-button v-if="spatialConfig.province" text @click="resetProvinceView"
                        >返回全国</el-button
                      >
                    </div>
                  </el-form-item>
                </el-form>
                <div class="map-summary" v-if="spatialSummaryText">
                  <strong>概览：</strong>{{ spatialSummaryText }}
                </div>
              </el-card>
            </el-col>
            <el-col :span="17">
              <el-card class="map-card" v-loading="spatialLoading">
                <template #header>
                  <div class="map-header">
                    <h4>空间分布图</h4>
                    <span class="map-header__subtitle">
                      {{ spatialConfig.province ? `${spatialConfig.province} 省份` : '全国' }} ·
                      {{ getElementLabel(spatialConfig.elementCode) }}
                    </span>
                  </div>
                </template>
                <div class="map-toolbar">
                  <span>提示：点击省份可下钻，点击站点查看具体值</span>
                  <el-button text size="small" @click="refreshSpatialChart">刷新视图</el-button>
                </div>
                <div ref="spatialChartRef" class="map-container"></div>
              </el-card>
            </el-col>
          </el-row>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-card class="data-table-card">
      <template #header>
        <div class="card-header">
          <h3>气象数据查询</h3>
          <el-button :loading="tableExporting" @click="exportTableRecords">
            <el-icon><Download /></el-icon>
            导出 Excel
          </el-button>
        </div>
      </template>
      <el-form :model="tableFilters" label-width="48px" class="table-filter-form">
        <div class="filter-grid">
          <el-form-item label="任务" class="filter-field">
            <el-select
              v-model="tableFilters.taskIds"
              multiple
              collapse-tags
              collapse-tags-tooltip
              placeholder="全部任务"
              style="width: 100%"
            >
              <el-option
                v-for="task in filterOptions.tasks"
                :key="task.taskId"
                :label="task.taskName"
                :value="task.taskId"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="站点" class="filter-field">
            <el-select
              v-model="tableFilters.stationIds"
              multiple
              collapse-tags
              collapse-tags-tooltip
              placeholder="全部站点"
              style="width: 100%"
            >
              <el-option
                v-for="station in filterOptions.stations"
                :key="station.stationId"
                :label="`${station.stationName} (${station.stationId})`"
                :value="station.stationId"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="要素" class="filter-field">
            <el-select
              v-model="tableFilters.elementCodes"
              multiple
              collapse-tags
              collapse-tags-tooltip
              placeholder="全部要素"
              style="width: 100%"
            >
              <el-option
                v-for="option in elementOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="时间范围" label-width="74px" class="filter-field time-range-field">
            <el-date-picker
              v-model="tableFilters.timeRange"
              type="datetimerange"
              value-format="YYYY-MM-DDTHH:mm:ss[Z]"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              style="width: 100%"
            />
          </el-form-item>

          <div class="filter-actions">
            <el-button type="primary" :loading="tableLoading" @click="handleTableFilterSearch">
              查询
            </el-button>
            <el-button @click="resetTableFilters">重置</el-button>
          </div>
        </div>
      </el-form>

      <el-table :data="tableRecords" v-loading="tableLoading" style="width: 100%" height="420px">
        <el-table-column prop="datetime" label="时间" min-width="220">
          <template #default="scope">
            {{ dayjs(scope.row.datetime).format('YYYY-MM-DD HH:mm') }}
          </template>
        </el-table-column>
        <el-table-column prop="stationName" label="站点" min-width="220">
          <template #default="scope">
            {{ scope.row.stationName }} ({{ scope.row.stationId }})
          </template>
        </el-table-column>
        <el-table-column prop="elementCode" label="要素" min-width="260">
          <template #default="scope">
            {{ getElementLabel(scope.row.elementCode) }}
          </template>
        </el-table-column>
        <el-table-column prop="value" label="数值" min-width="150">
          <template #default="scope">
            {{ formatValue(scope.row.value) }}
          </template>
        </el-table-column>
        <el-table-column prop="taskName" label="任务名称" width="180" />
      </el-table>

      <div class="table-pagination">
        <el-pagination
          background
          layout="total, prev, pager, next, sizes"
          :total="tablePagination.total"
          :page-size="tablePagination.size"
          :current-page="tablePagination.page"
          :page-sizes="[10, 20, 50, 100]"
          @current-change="handleTablePageChange"
          @size-change="handleTableSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { DataLine, Download, Folder, Odometer, OfficeBuilding } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import http from '@/api/http'

interface MeteorDataSummary {
  totalDataPoints: number
  taskCount: number
  stationCount: number
  elementCount: number
  anomalyCount: number
  lastUpdated?: string
}

interface MeteorDataTimeSeriesPoint {
  timestamp: string
  average: number | null
  minimum: number | null
  maximum: number | null
}

interface MeteorDataTableRecord {
  id: number
  taskId: number
  taskName: string
  stationId: number
  stationName: string
  elementCode: string
  datetime: string
  value: number | null
}

interface MeteorDataCorrelationCell {
  xElement: string
  yElement: string
  coefficient: number
  effectiveSamples: number
}

interface MeteorDataCorrelationResponse {
  elementCodes: string[]
  matrix: MeteorDataCorrelationCell[]
  granularity: string
}

interface MeteorDataTablePageResponse {
  records: MeteorDataTableRecord[]
  totalElements: number
  page: number
  size: number
}

interface SpatialProvinceStat {
  province: string
  averageValue: number | null
  minValue: number | null
  maxValue: number | null
  stationCount: number
}

interface SpatialStationPoint {
  stationId: number
  stationName: string
  province: string
  latitude: number
  longitude: number
  value: number | null
}

interface SpatialMapResponse {
  provinces: SpatialProvinceStat[]
  stations: SpatialStationPoint[]
  availableProvinces: string[]
  currentProvince?: string
  startTime?: string
  endTime?: string
  elementCode?: string
}

interface FilterOptions {
  tasks: Array<{ taskId: number; taskName: string }>
  stations: Array<{ stationId: number; stationName: string }>
  elements: string[]
}

interface TimeSeriesConfig {
  timeRange: string[]
  elementCode: string
  granularity: 'hour' | 'day' | 'week' | 'month'
  stationIds: number[]
  taskIds: number[]
}

interface TimeSeriesStationPoint {
  timestamp: string
  value: number | null
}

interface TimeSeriesStationSeries {
  stationId: number
  stationName: string
  points: TimeSeriesStationPoint[]
}

interface CorrelationConfig {
  timeRange: string[]
  elementCodes: string[]
  stationIds: number[]
  taskIds: number[]
  granularity: 'hour' | 'day' | 'week' | 'month'
}

// 默认时间范围：2025年11月1日0点至2025年11月30日0点
const DEFAULT_START_TIME = '2025-11-01T00:00:00Z'
const DEFAULT_END_TIME = '2025-11-30T00:00:00Z'
const DEFAULT_TIME_RANGE_HOURS = 24 // 保留用于其他计算
const ELEMENT_NAME_MAP: Record<string, string> = {
  PRS: '气压 (PRS)',
  PRS_Sea: '海平面气压 (PRS_Sea)',
  PRS_Max: '最高气压 (PRS_Max)',
  PRS_Min: '最低气压 (PRS_Min)',
  TEM: '气温 (TEM)',
  TEM_MAX: '最高气温 (TEM_MAX)',
  TEM_MIN: '最低气温 (TEM_MIN)',
  RHU: '相对湿度 (RHU)',
  RHU_Min: '最小相对湿度 (RHU_Min)',
  VAP: '水汽压 (VAP)',
  PRE_3h: '3小时降水量 (PRE_3h)',
  WIN_D_INST_Max: '极大风速风向 (WIN_D_INST_Max)',
  WIN_S_MAX: '最大风速 (WIN_S_MAX)',
  WIN_D_S_Max: '最大风速风向 (WIN_D_S_Max)',
  WIN_S_Avg_2mi: '2分钟平均风速 (WIN_S_Avg_2mi)',
  WIN_D_Avg_2mi: '2分钟平均风向 (WIN_D_Avg_2mi)',
  WEP_Now: '现在天气 (WEP_Now)',
  WIN_S_Inst_Max: '极大风速 (WIN_S_Inst_Max)',
  VIS: '水平能见度 (VIS)',
  CLO_Cov: '总云量 (CLO_Cov)',
  CLO_Cov_Low: '低云量 (CLO_Cov_Low)',
  CLO_COV_LM: '云量 (CLO_COV_LM)',
  Datetime: '资料时间 (Datetime)',
}

const PROVINCE_MAP_KEY: Record<string, string> = {
  北京: 'beijing',
  天津: 'tianjin',
  河北: 'hebei',
  山西: 'shanxi',
  内蒙古: 'neimenggu',
  辽宁: 'liaoning',
  吉林: 'jilin',
  黑龙江: 'heilongjiang',
  上海: 'shanghai',
  江苏: 'jiangsu',
  浙江: 'zhejiang',
  安徽: 'anhui',
  福建: 'fujian',
  江西: 'jiangxi',
  山东: 'shandong',
  河南: 'henan',
  湖北: 'hubei',
  湖南: 'hunan',
  广东: 'guangdong',
  广西: 'guangxi',
  海南: 'hainan',
  重庆: 'chongqing',
  四川: 'sichuan',
  贵州: 'guizhou',
  云南: 'yunnan',
  西藏: 'xizang',
  陕西: 'shaanxi',
  甘肃: 'gansu',
  青海: 'qinghai',
  宁夏: 'ningxia',
  新疆: 'xinjiang',
  香港: 'xianggang',
  澳门: 'aomen',
  台湾: 'taiwan',
}

const MAP_CODE_MAP: Record<string, string> = {
  china: '100000_full',
  beijing: '110000_full',
  tianjin: '120000_full',
  hebei: '130000_full',
  shanxi: '140000_full',
  neimenggu: '150000_full',
  liaoning: '210000_full',
  jilin: '220000_full',
  heilongjiang: '230000_full',
  shanghai: '310000_full',
  jiangsu: '320000_full',
  zhejiang: '330000_full',
  anhui: '340000_full',
  fujian: '350000_full',
  jiangxi: '360000_full',
  shandong: '370000_full',
  henan: '410000_full',
  hubei: '420000_full',
  hunan: '430000_full',
  guangdong: '440000_full',
  guangxi: '450000_full',
  hainan: '460000_full',
  chongqing: '500000_full',
  sichuan: '510000_full',
  guizhou: '520000_full',
  yunnan: '530000_full',
  xizang: '540000_full',
  shaanxi: '610000_full',
  gansu: '620000_full',
  qinghai: '630000_full',
  ningxia: '640000_full',
  xinjiang: '650000_full',
  taiwan: '710000_full',
  xianggang: '810000_full',
  aomen: '820000_full',
}

const geoJsonCache = new Map<string, any>()
const DEFAULT_MAP_KEY = 'china'
const MAP_BASE_URL = 'https://geo.datav.aliyun.com/areas_v3/bound'

const activeAnalysisType = ref('timeSeries')
const summaryLoading = ref(false)
const timeSeriesLoading = ref(false)
const correlationLoading = ref(false)
const tableLoading = ref(false)
const spatialLoading = ref(false)
const analysisExporting = ref(false)
const tableExporting = ref(false)

const currentUserId = ref<number | null>(getCurrentUserId())
const timeSeriesSeries = ref<TimeSeriesStationSeries[]>([])
const correlationElements = ref<string[]>([])
const correlationMatrix = ref<Array<{ x: string; y: string; value: number }>>([])
const tableRecords = ref<MeteorDataTableRecord[]>([])
const spatialData = ref<SpatialMapResponse | null>(null)
const availableProvinces = ref<string[]>([])

const tablePagination = reactive({
  page: 1,
  size: 20,
  total: 0,
})

const tableFilters = reactive({
  taskIds: [] as number[],
  stationIds: [] as number[],
  elementCodes: [] as string[],
  timeRange: [] as string[],
})

const filterOptions = reactive<FilterOptions>({
  tasks: [],
  stations: [],
  elements: [],
})

const elementOptions = computed(() =>
  filterOptions.elements.map((code) => ({
    value: code,
    label: ELEMENT_NAME_MAP[code] ?? code,
  })),
)

const analysisStats = reactive<MeteorDataSummary>({
  totalDataPoints: 0,
  taskCount: 0,
  stationCount: 0,
  elementCount: 0,
  anomalyCount: 0,
  lastUpdated: undefined,
})

const lastUpdatedText = computed(() => {
  if (!analysisStats.lastUpdated) {
    return '—'
  }
  return dayjs(analysisStats.lastUpdated).format('YYYY-MM-DD HH:mm:ss')
})

const spatialSummaryText = computed(() => {
  if (
    !spatialData.value ||
    !spatialData.value.provinces ||
    spatialData.value.provinces.length === 0
  ) {
    return ''
  }
  const valid = spatialData.value.provinces.filter((item) => item.averageValue !== null)
  if (valid.length === 0) {
    return ''
  }
  const sorted = [...valid].sort((a, b) => (b.averageValue ?? 0) - (a.averageValue ?? 0))
  const top = sorted[0]
  const bottom = sorted[sorted.length - 1]
  if (!top) return ''
  const topText = `${top.province} 平均值 ${formatValue(top.averageValue)}`
  if (!bottom || bottom === top) {
    return `${topText}，覆盖站点 ${top.stationCount} 个`
  }
  return `${topText}，最低 ${bottom.province} ${formatValue(bottom.averageValue)}`
})

const timeSeriesConfig = reactive<TimeSeriesConfig>({
  timeRange: [DEFAULT_START_TIME, DEFAULT_END_TIME],
  elementCode: '',
  granularity: 'hour',
  stationIds: [],
  taskIds: [],
})

const correlationConfig = reactive<CorrelationConfig>({
  timeRange: [DEFAULT_START_TIME, DEFAULT_END_TIME],
  elementCodes: [],
  stationIds: [],
  taskIds: [],
  granularity: 'hour',
})

const spatialConfig = reactive({
  timeRange: [DEFAULT_START_TIME, DEFAULT_END_TIME],
  elementCode: '',
  province: '',
})

const timeSeriesChartRef = ref()
const timeSeriesChartInstance = ref<echarts.ECharts | null>(null)
const correlationChartRef = ref()
const correlationChartInstance = ref<echarts.ECharts | null>(null)
const spatialChartRef = ref()
const spatialChartInstance = ref<echarts.ECharts | null>(null)
const resizeCleanupHandlers: Array<() => void> = []

function getCurrentUserId(): number | null {
  const stored = localStorage.getItem('userId')
  if (!stored) return null
  const parsed = Number(stored)
  return Number.isNaN(parsed) ? null : parsed
}

const ensureLoggedIn = () => {
  if (currentUserId.value) {
    return true
  }
  ElMessage.error('请先登录后再执行分析')
  return false
}

const getElementLabel = (code: string) => {
  return ELEMENT_NAME_MAP[code] ?? code
}

const formatValue = (value: number | null | undefined) => {
  if (value === null || value === undefined) {
    return '--'
  }
  return Number(value).toFixed(2)
}

const loadSummary = async () => {
  if (!ensureLoggedIn()) return
  summaryLoading.value = true
  try {
    const { data } = await http.get<MeteorDataSummary>('/api/meteor-data/summary', {
      params: { userId: currentUserId.value },
    })
    analysisStats.totalDataPoints = data?.totalDataPoints ?? 0
    analysisStats.taskCount = data?.taskCount ?? 0
    analysisStats.stationCount = data?.stationCount ?? 0
    analysisStats.elementCount = data?.elementCount ?? 0
    analysisStats.anomalyCount = data?.anomalyCount ?? 0
    analysisStats.lastUpdated = data?.lastUpdated
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '统计信息获取失败'
    ElMessage.error(message)
  } finally {
    summaryLoading.value = false
  }
}

const loadFilterOptions = async () => {
  if (!ensureLoggedIn()) return
  try {
    const { data } = await http.get<FilterOptions>('/api/meteor-data/filter-options', {
      params: { userId: currentUserId.value },
    })
    filterOptions.tasks = data?.tasks ?? []
    filterOptions.stations = data?.stations ?? []
    filterOptions.elements = data?.elements ?? []

    const firstElement = elementOptions.value[0]
    if (!timeSeriesConfig.elementCode && firstElement) {
      timeSeriesConfig.elementCode = firstElement.value
    }
    if (correlationConfig.elementCodes.length === 0 && elementOptions.value.length >= 2) {
      correlationConfig.elementCodes = elementOptions.value.slice(0, 2).map((item) => item.value)
    }
    if (!spatialConfig.elementCode && firstElement) {
      spatialConfig.elementCode = firstElement.value
    }
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '过滤条件加载失败')
  }
}

const resolveTimeRange = (range: string[] | undefined): [string, string] => {
  if (!range || range.length !== 2 || !range[0] || !range[1]) {
    // 使用默认时间范围：2025年11月1日至11月30日
    return [DEFAULT_START_TIME, DEFAULT_END_TIME]
  }
  const [rawStart, rawEnd] = range
  return [dayjs(rawStart).toISOString(), dayjs(rawEnd).toISOString()]
}

const appendArrayParam = (
  params: URLSearchParams,
  key: string,
  values?: Array<number | string>,
) => {
  if (!values || values.length === 0) {
    return
  }
  values.forEach((value) => params.append(key, String(value)))
}

const downloadBlob = (blob: Blob, fallbackFilename: string, contentDisposition?: string) => {
  const filename = resolveDownloadFilename(contentDisposition) || fallbackFilename
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

const resolveDownloadFilename = (contentDisposition?: string) => {
  if (!contentDisposition) return ''
  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1])
  }
  const asciiMatch = contentDisposition.match(/filename="?([^";]+)"?/i)
  return asciiMatch?.[1] ?? ''
}

const resolveBlobErrorMessage = async (error: any, fallback: string) => {
  const data = error?.response?.data
  if (data instanceof Blob) {
    const text = await data.text()
    try {
      const parsed = JSON.parse(text)
      return parsed?.message || fallback
    } catch {
      return text || fallback
    }
  }
  return error?.response?.data?.message || error?.message || fallback
}

const getActiveChartImage = () => {
  const chart =
    activeAnalysisType.value === 'timeSeries'
      ? timeSeriesChartInstance.value
      : activeAnalysisType.value === 'correlation'
        ? correlationChartInstance.value
        : spatialChartInstance.value
  return (
    chart?.getDataURL({
      type: 'png',
      pixelRatio: 2,
      backgroundColor: '#fff',
    }) ?? ''
  )
}

const buildAnalysisExportPayload = (chartImageBase64: string) => {
  const analysisType = activeAnalysisType.value
  if (analysisType === 'timeSeries') {
    const [start, end] = resolveTimeRange(timeSeriesConfig.timeRange)
    const stationIds =
      timeSeriesConfig.stationIds.length > 0
        ? timeSeriesConfig.stationIds
        : filterOptions.stations.slice(0, 5).map((station) => station.stationId)
    return {
      analysisType,
      chartImageBase64,
      filters: {
        taskIds: timeSeriesConfig.taskIds,
        stationIds,
        elementCodes: [timeSeriesConfig.elementCode],
        elementCode: timeSeriesConfig.elementCode,
        startTime: start,
        endTime: end,
        granularity: timeSeriesConfig.granularity,
      },
    }
  }
  if (analysisType === 'correlation') {
    const [start, end] = resolveTimeRange(correlationConfig.timeRange)
    return {
      analysisType,
      chartImageBase64,
      filters: {
        taskIds: correlationConfig.taskIds,
        stationIds: correlationConfig.stationIds,
        elementCodes: correlationConfig.elementCodes,
        startTime: start,
        endTime: end,
        granularity: correlationConfig.granularity,
      },
    }
  }
  const [start, end] = resolveTimeRange(spatialConfig.timeRange)
  return {
    analysisType,
    chartImageBase64,
    filters: {
      elementCodes: [spatialConfig.elementCode],
      elementCode: spatialConfig.elementCode,
      startTime: start,
      endTime: end,
      province: spatialConfig.province,
    },
  }
}

const buildRecordExportPayload = () => {
  const payload: Record<string, unknown> = {
    taskIds: tableFilters.taskIds,
    stationIds: tableFilters.stationIds,
    elementCodes: tableFilters.elementCodes,
  }
  if (tableFilters.timeRange && tableFilters.timeRange.length === 2) {
    const [start, end] = resolveTimeRange(tableFilters.timeRange)
    payload.startTime = start
    payload.endTime = end
  }
  return payload
}

const refreshActiveAnalysisForExport = async () => {
  if (activeAnalysisType.value === 'timeSeries') {
    return runTimeSeriesAnalysis()
  }
  if (activeAnalysisType.value === 'correlation') {
    return runCorrelationAnalysis()
  }
  if (activeAnalysisType.value === 'spatial') {
    return runSpatialAnalysis()
  }
  ElMessage.warning('未知分析类型，无法导出')
  return false
}

const exportAnalysisReport = async () => {
  if (!ensureLoggedIn()) return
  analysisExporting.value = true
  try {
    const refreshed = await refreshActiveAnalysisForExport()
    if (!refreshed) return
    await nextTick()
    const payload = buildAnalysisExportPayload(getActiveChartImage())
    const response = await http.post<Blob>('/api/meteor-data/export/analysis-docx', payload, {
      responseType: 'blob',
      timeout: 60000,
    })
    downloadBlob(
      response.data,
      `weather-analysis-${activeAnalysisType.value}-${dayjs().format('YYYYMMDDHHmmss')}.docx`,
      String(response.headers['content-disposition'] ?? ''),
    )
    ElMessage.success('Word 报告导出完成')
  } catch (error: any) {
    ElMessage.error(await resolveBlobErrorMessage(error, 'Word 报告导出失败'))
  } finally {
    analysisExporting.value = false
  }
}

const exportTableRecords = async () => {
  if (!ensureLoggedIn()) return
  tableExporting.value = true
  try {
    const response = await http.post<Blob>(
      '/api/meteor-data/export/records-xlsx',
      buildRecordExportPayload(),
      {
        responseType: 'blob',
        timeout: 60000,
      },
    )
    downloadBlob(
      response.data,
      `weather-records-${dayjs().format('YYYYMMDDHHmmss')}.xlsx`,
      String(response.headers['content-disposition'] ?? ''),
    )
    ElMessage.success('Excel 数据导出完成')
  } catch (error: any) {
    ElMessage.error(await resolveBlobErrorMessage(error, 'Excel 数据导出失败'))
  } finally {
    tableExporting.value = false
  }
}

const normalizeProvinceLabel = (province?: string) => {
  if (!province) return ''
  return province
    .replace(/(省|市|壮族自治区|回族自治区|维吾尔自治区|自治区|特别行政区)/g, '')
    .trim()
}

const resolveProvinceMapKey = (province?: string) => {
  if (!province) {
    return DEFAULT_MAP_KEY
  }
  const normalized = normalizeProvinceLabel(province)
  return PROVINCE_MAP_KEY[normalized] ?? DEFAULT_MAP_KEY
}

const ensureGeoMap = async (mapKey: string) => {
  if (echarts.getMap(mapKey)) {
    return
  }
  const cached = geoJsonCache.get(mapKey)
  if (cached) {
    echarts.registerMap(mapKey, cached)
    return
  }
  const code = MAP_CODE_MAP[mapKey] ?? MAP_CODE_MAP[DEFAULT_MAP_KEY]
  if (!code) {
    throw new Error(`缺少 ${mapKey} 的地图编码`)
  }
  const response = await fetch(`${MAP_BASE_URL}/${code}.json`)
  if (!response.ok) {
    throw new Error(`地图数据加载失败（${response.status}）`)
  }
  const geoJson = await response.json()
  geoJsonCache.set(mapKey, geoJson)
  echarts.registerMap(mapKey, geoJson)
}

const matchAvailableProvince = (targetName: string) => {
  const normalizedTarget = normalizeProvinceLabel(targetName)
  return (
    availableProvinces.value.find((item) => normalizeProvinceLabel(item) === normalizedTarget) ?? ''
  )
}

const runTimeSeriesAnalysis = async () => {
  if (!ensureLoggedIn()) return false
  if (!timeSeriesConfig.elementCode) {
    ElMessage.warning('请选择气象要素')
    return false
  }
  const resolvedStations =
    timeSeriesConfig.stationIds.length > 0
      ? timeSeriesConfig.stationIds
      : filterOptions.stations.slice(0, 5).map((station) => station.stationId)
  if (!resolvedStations.length) {
    ElMessage.warning('暂无可分析的站点，请先配置站点数据')
    return false
  }
  timeSeriesLoading.value = true
  try {
    const [start, end] = resolveTimeRange(timeSeriesConfig.timeRange)
    const stationSeries = await Promise.all(
      resolvedStations.map(async (stationId) => {
        const params = new URLSearchParams()
        params.append('userId', String(currentUserId.value))
        params.append('elementCode', timeSeriesConfig.elementCode)
        params.append('startTime', start)
        params.append('endTime', end)
        params.append('granularity', timeSeriesConfig.granularity)
        appendArrayParam(params, 'stationIds', [stationId])
        appendArrayParam(params, 'taskIds', timeSeriesConfig.taskIds)

        const { data } = await http.get<MeteorDataTimeSeriesPoint[]>(
          '/api/meteor-data/time-series',
          {
            params,
          },
        )

        const stationName =
          filterOptions.stations.find((item) => item.stationId === stationId)?.stationName ||
          `站点 ${stationId}`

        return {
          stationId,
          stationName,
          points:
            data?.map((point) => ({
              timestamp: point.timestamp,
              value: point.average,
            })) ?? [],
        }
      }),
    )

    timeSeriesSeries.value = stationSeries
    if (timeSeriesSeries.value.every((series) => series.points.length === 0)) {
      timeSeriesChartInstance.value?.clear()
      ElMessage.warning('所选站点在当前时间范围内暂无数据')
      return true
    }
    if (!timeSeriesChartInstance.value) {
      initTimeSeriesChart()
    } else {
      renderTimeSeriesChart()
    }
    ElMessage.success('时间序列分析完成')
    return true
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '时间序列分析失败'
    ElMessage.error(message)
    return false
  } finally {
    timeSeriesLoading.value = false
  }
}

const runCorrelationAnalysis = async () => {
  if (!ensureLoggedIn()) return false
  if (correlationConfig.elementCodes.length < 2) {
    ElMessage.warning('请至少选择两个要素')
    return false
  }
  correlationLoading.value = true
  try {
    const [start, end] = resolveTimeRange(correlationConfig.timeRange)
    const payload = {
      userId: currentUserId.value,
      elementCodes: correlationConfig.elementCodes,
      taskIds: correlationConfig.taskIds,
      stationIds: correlationConfig.stationIds,
      startTime: start,
      endTime: end,
      granularity: correlationConfig.granularity,
    }
    const { data } = await http.post<MeteorDataCorrelationResponse>(
      '/api/meteor-data/correlation',
      payload,
    )
    correlationElements.value = data?.elementCodes ?? []
    correlationMatrix.value =
      data?.matrix?.map((cell) => ({
        x: cell.xElement,
        y: cell.yElement,
        value: Number.isFinite(cell.coefficient) ? Number(cell.coefficient.toFixed(3)) : 0,
      })) ?? []
    renderCorrelationChart()
    ElMessage.success('相关性分析完成')
    return true
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '相关性分析失败'
    ElMessage.error(message)
    return false
  } finally {
    correlationLoading.value = false
  }
}

const runSpatialAnalysis = async () => {
  if (!ensureLoggedIn()) return false
  if (!spatialConfig.elementCode) {
    ElMessage.warning('请选择气象要素')
    return false
  }
  spatialLoading.value = true
  try {
    const [start, end] = resolveTimeRange(spatialConfig.timeRange)
    const params = new URLSearchParams()
    params.append('userId', String(currentUserId.value))
    params.append('elementCode', spatialConfig.elementCode)
    params.append('startTime', start)
    params.append('endTime', end)
    if (spatialConfig.province) {
      params.append('province', spatialConfig.province)
    }

    const { data } = await http.get<SpatialMapResponse>('/api/meteor-data/spatial-map', {
      params,
    })
    spatialData.value = data ?? null
    availableProvinces.value = data?.availableProvinces ?? []
    if (!spatialConfig.province && data?.currentProvince) {
      spatialConfig.province = data.currentProvince
    }
    await nextTick()
    initSpatialChart()
    await renderSpatialChart()
    ElMessage.success('空间分析完成')
    return true
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '空间分析失败'
    ElMessage.error(message)
    return false
  } finally {
    spatialLoading.value = false
  }
}

const resetProvinceView = () => {
  if (spatialConfig.province) {
    spatialConfig.province = ''
    runSpatialAnalysis()
  }
}

const refreshSpatialChart = () => {
  void renderSpatialChart()
}

const fetchTableRecords = async () => {
  if (!ensureLoggedIn()) return
  tableLoading.value = true
  try {
    const params = new URLSearchParams()
    params.append('userId', String(currentUserId.value))
    params.append('page', String(tablePagination.page - 1))
    params.append('size', String(tablePagination.size))
    appendArrayParam(params, 'taskIds', tableFilters.taskIds)
    appendArrayParam(params, 'stationIds', tableFilters.stationIds)
    appendArrayParam(params, 'elementCodes', tableFilters.elementCodes)

    if (tableFilters.timeRange && tableFilters.timeRange.length === 2) {
      const [start, end] = resolveTimeRange(tableFilters.timeRange)
      params.append('startTime', start)
      params.append('endTime', end)
    }

    const { data } = await http.get<MeteorDataTablePageResponse>('/api/meteor-data/records', {
      params,
    })

    tableRecords.value = data?.records ?? []
    tablePagination.total = data?.totalElements ?? 0
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || '数据列表加载失败'
    ElMessage.error(message)
  } finally {
    tableLoading.value = false
  }
}

const handleTableFilterSearch = () => {
  tablePagination.page = 1
  fetchTableRecords()
}

const resetTableFilters = () => {
  tableFilters.taskIds = []
  tableFilters.stationIds = []
  tableFilters.elementCodes = []
  tableFilters.timeRange = []
  tablePagination.page = 1
  fetchTableRecords()
}

const handleTablePageChange = (page: number) => {
  tablePagination.page = page
  fetchTableRecords()
}

const handleTableSizeChange = (size: number) => {
  tablePagination.size = size
  tablePagination.page = 1
  fetchTableRecords()
}

const handleAnalysisTypeChange = (type: string) => {
  activeAnalysisType.value = type
  nextTick(() => {
    if (type === 'timeSeries') {
      initTimeSeriesChart()
    } else if (type === 'correlation') {
      initCorrelationChart()
    } else if (type === 'spatial') {
      initSpatialChart()
    }
  })
}

const initTimeSeriesChart = () => {
  const container = timeSeriesChartRef.value
  if (!container) return
  if (container.clientWidth === 0 || container.clientHeight === 0) {
    setTimeout(initTimeSeriesChart, 100)
    return
  }
  if (!timeSeriesChartInstance.value) {
    timeSeriesChartInstance.value = echarts.init(container)
    const resizeHandler = () => timeSeriesChartInstance.value?.resize()
    window.addEventListener('resize', resizeHandler)
    resizeCleanupHandlers.push(() => window.removeEventListener('resize', resizeHandler))
  }
  renderTimeSeriesChart()
}

const renderTimeSeriesChart = () => {
  if (!timeSeriesChartInstance.value) return
  if (timeSeriesSeries.value.length === 0) {
    timeSeriesChartInstance.value.clear()
    return
  }

  const timelineSet = new Set<string>()
  timeSeriesSeries.value.forEach((series) => {
    series.points.forEach((point) => {
      if (point.timestamp) {
        timelineSet.add(point.timestamp)
      }
    })
  })
  const timeline = Array.from(timelineSet).sort((a, b) => dayjs(a).valueOf() - dayjs(b).valueOf())
  if (timeline.length === 0) {
    timeSeriesChartInstance.value.clear()
    return
  }
  const categories = timeline.map((ts) => dayjs(ts).format('MM-DD HH:mm'))

  const allValues: number[] = []
  timeSeriesSeries.value.forEach((series) => {
    series.points.forEach((point) => {
      if (point.value !== null && point.value !== undefined) {
        allValues.push(Number(point.value))
      }
    })
  })
  const minValue = allValues.length ? Math.min(...allValues) : 0
  const maxValue = allValues.length ? Math.max(...allValues) : 1

  const seriesOptions = timeSeriesSeries.value.map((series) => {
    const pointMap = new Map(series.points.map((point) => [point.timestamp, point.value]))
    const data = timeline.map((ts) => {
      const value = pointMap.get(ts)
      return value !== null && value !== undefined ? Number(value) : null
    })
    return {
      name: series.stationName,
      type: 'line',
      data,
      smooth: true,
      showSymbol: true,
      connectNulls: true,
      symbolSize: (val: any) => {
        const value = Array.isArray(val) ? (val[1] ?? val[0]) : val
        if (!Number.isFinite(value)) return 6
        if (!allValues.length || maxValue === minValue) {
          return 12
        }
        const normalized = (Number(value) - minValue) / (maxValue - minValue)
        return 8 + normalized * 12
      },
      label: {
        show: true,
        position: 'top',
        formatter: (params: any) => {
          const value = Array.isArray(params.value)
            ? (params.value[1] ?? params.value[0])
            : params.value
          if (value === null || value === undefined) return ''
          return Number(value).toFixed(2)
        },
      },
    }
  })

  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any[]) => {
        if (!params || params.length === 0) {
          return ''
        }
        const header = params[0].axisValueLabel
        const lines = params
          .map((item) => {
            const value = Array.isArray(item.value) ? (item.value[1] ?? item.value[0]) : item.value
            if (value === null || value === undefined) {
              return `${item.marker}${item.seriesName}：--`
            }
            return `${item.marker}${item.seriesName}：${Number(value).toFixed(2)}`
          })
          .join('<br/>')
        return `${header}<br/>${lines}`
      },
    },
    legend: {
      type: 'scroll',
      data: timeSeriesSeries.value.map((series) => series.stationName),
    },
    xAxis: { type: 'category', boundaryGap: false, data: categories },
    yAxis: { type: 'value', scale: true },
    series: seriesOptions,
  }
  timeSeriesChartInstance.value.setOption(option, true)
}

const initCorrelationChart = () => {
  const container = correlationChartRef.value
  if (!container) return
  if (container.clientWidth === 0 || container.clientHeight === 0) {
    setTimeout(initCorrelationChart, 100)
    return
  }
  if (!correlationChartInstance.value) {
    correlationChartInstance.value = echarts.init(container)
    const resizeHandler = () => correlationChartInstance.value?.resize()
    window.addEventListener('resize', resizeHandler)
    resizeCleanupHandlers.push(() => window.removeEventListener('resize', resizeHandler))
  }
  renderCorrelationChart()
}

const renderCorrelationChart = () => {
  if (!correlationChartInstance.value) return
  if (correlationElements.value.length === 0) {
    correlationChartInstance.value.clear()
    return
  }

  const labelIndex = new Map<string, number>()
  correlationElements.value.forEach((code, index) => labelIndex.set(code, index))
  const displayLabels = correlationElements.value.map((code) => getElementLabel(code))

  const heatmapData = correlationMatrix.value
    .map((cell) => {
      const xIndex = labelIndex.get(cell.x)
      const yIndex = labelIndex.get(cell.y)
      if (xIndex === undefined || yIndex === undefined) {
        return null
      }
      return [xIndex, yIndex, cell.value]
    })
    .filter((item): item is [number, number, number] => item !== null)

  const option = {
    tooltip: {
      position: 'top',
      formatter: (params: any) => {
        const value = params.value?.[2] ?? 0
        return `${displayLabels[params.value[1]]} vs ${displayLabels[params.value[0]]}<br/>相关系数：${value.toFixed(3)}`
      },
    },
    grid: {
      top: 36,
      left: 170,
      right: 48,
      bottom: 94,
    },
    xAxis: {
      type: 'category',
      data: displayLabels,
      splitArea: { show: true },
      axisLabel: {
        interval: 0,
        margin: 18,
      },
    },
    yAxis: {
      type: 'category',
      data: displayLabels,
      splitArea: { show: true },
      axisLabel: {
        margin: 10,
      },
    },
    visualMap: {
      min: -1,
      max: 1,
      calculable: false,
      orient: 'horizontal',
      left: 'center',
      bottom: 18,
      itemWidth: 12,
      itemHeight: 220,
      text: ['1', '-1'],
      textGap: 10,
      dimension: 2,
    },
    series: [
      {
        name: '相关性',
        type: 'heatmap',
        data: heatmapData,
        label: { show: true, formatter: (params: any) => Number(params.value[2]).toFixed(2) },
      },
    ],
  }
  correlationChartInstance.value.setOption(option, true)
}

const initSpatialChart = () => {
  const container = spatialChartRef.value
  if (!container) return
  if (container.clientWidth === 0 || container.clientHeight === 0) {
    setTimeout(initSpatialChart, 100)
    return
  }
  if (!spatialChartInstance.value) {
    spatialChartInstance.value = echarts.init(container)
    const resizeHandler = () => spatialChartInstance.value?.resize()
    window.addEventListener('resize', resizeHandler)
    resizeCleanupHandlers.push(() => {
      window.removeEventListener('resize', resizeHandler)
    })
    spatialChartInstance.value.on('click', handleSpatialChartClick)
  }
  void renderSpatialChart()
}

const renderSpatialChart = async () => {
  if (!spatialChartInstance.value) return
  const mapKey = resolveProvinceMapKey(spatialConfig.province)
  try {
    await ensureGeoMap(mapKey)
  } catch (error: any) {
    ElMessage.error(error?.message || '地图数据加载失败')
    return
  }

  if (!spatialData.value) {
    spatialChartInstance.value.clear()
    return
  }

  const provinces = spatialData.value.provinces ?? []
  const targetProvince = normalizeProvinceLabel(spatialConfig.province)
  const provinceSeries = (
    targetProvince
      ? provinces.filter((item) => normalizeProvinceLabel(item.province) === targetProvince)
      : provinces
  ).map((item) => ({
    name: item.province,
    value: item.averageValue ?? null,
    stationCount: item.stationCount ?? 0,
    minValue: item.minValue ?? null,
    maxValue: item.maxValue ?? null,
  }))

  const mapSeriesData = provinceSeries.map((item) => ({
    ...item,
    rawValue: item.value,
    value: item.value ?? 0,
  }))

  const provinceDataMap = new Map(
    mapSeriesData.map((item) => [normalizeProvinceLabel(item.name), item]),
  )

  const stationList = spatialData.value.stations ?? []
  const stationSeries = stationList
    .filter((station) => {
      if (station.latitude === null || station.latitude === undefined) return false
      if (station.longitude === null || station.longitude === undefined) return false
      if (!targetProvince) return true
      return normalizeProvinceLabel(station.province) === targetProvince
    })
    .map((station) => ({
      name: `${station.stationName} (${station.stationId})`,
      value: [station.longitude, station.latitude, station.value ?? null],
      province: station.province,
    }))

  const numericValues: number[] = []
  provinceSeries.forEach((item) => {
    if (item.value !== null && item.value !== undefined) {
      numericValues.push(Number(item.value))
    }
  })
  stationSeries.forEach((item) => {
    if (item.value[2] !== null && item.value[2] !== undefined) {
      numericValues.push(Number(item.value[2]))
    }
  })

  const visualMin = numericValues.length ? Math.min(...numericValues) : 0
  let visualMax = numericValues.length ? Math.max(...numericValues) : 1
  if (visualMax === visualMin) {
    visualMax = visualMin + 1
  }

  const computeSymbolSize = (val: number | null | undefined) => {
    if (!Number.isFinite(val) || visualMax - visualMin <= 0) {
      return 12
    }
    return 10 + ((Number(val) - visualMin) / (visualMax - visualMin)) * 25
  }

  const getValueColor = (val: number | null | undefined) => {
    if (!Number.isFinite(val) || visualMax - visualMin <= 0) {
      return '#4fa0ff'
    }
    const ratio = Math.max(0, Math.min(1, (Number(val) - visualMin) / (visualMax - visualMin)))
    const low: [number, number, number] = [212, 244, 255]
    const middle: [number, number, number] = [79, 160, 255]
    const high: [number, number, number] = [11, 83, 193]
    const segment = ratio < 0.5 ? 0 : 1
    const localRatio = segment === 0 ? ratio * 2 : (ratio - 0.5) * 2
    const start = segment === 0 ? low : middle
    const end = segment === 0 ? middle : high
    const rgb = start.map((channel, index) => {
      const endChannel = end[index] ?? channel
      return Math.round(channel + (endChannel - channel) * localRatio)
    })
    return `rgb(${rgb[0]}, ${rgb[1]}, ${rgb[2]})`
  }

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        if (params.seriesType === 'map') {
          const datum = provinceDataMap.get(normalizeProvinceLabel(params.name))
          if (!datum || datum.rawValue === null) {
            return `${params.name}<br/>暂无数据`
          }
          return `${params.name}<br/>平均值：${formatValue(datum.rawValue)}<br/>站点数：${datum.stationCount}`
        }
        if (params.seriesType === 'effectScatter') {
          const value = params.value?.[2]
          if (value === null || value === undefined) {
            return `${params.name}<br/>暂无数据`
          }
          return `${params.name}<br/>数值：${formatValue(value)}`
        }
        return ''
      },
    },
    visualMap: {
      min: visualMin,
      max: visualMax,
      seriesIndex: 0,
      calculable: true,
      orient: 'vertical',
      right: 20,
      bottom: 30,
      text: ['高', '低'],
      inRange: {
        color: ['#d4f4ff', '#4fa0ff', '#0b53c1'],
      },
    },
    geo: {
      map: mapKey,
      roam: true,
      scaleLimit: { min: 0.8, max: 8 },
      zoom: spatialConfig.province ? 1.2 : 1,
      label: {
        show: !spatialConfig.province,
        color: '#555',
      },
      itemStyle: {
        borderColor: '#83b5ff',
        borderWidth: 0.8,
      },
      emphasis: {
        label: { color: '#000' },
        itemStyle: { areaColor: '#ffd28c' },
      },
    },
    series: [
      {
        name: '省份平均值',
        type: 'map',
        map: mapKey,
        geoIndex: 0,
        data: mapSeriesData,
        emphasis: {
          label: { show: true },
        },
      },
      {
        name: '站点值',
        type: 'effectScatter',
        coordinateSystem: 'geo',
        data: stationSeries,
        symbolSize: (val: any) => computeSymbolSize(val[2]),
        encode: { value: 2 },
        rippleEffect: { scale: 3, brushType: 'stroke' },
        itemStyle: {
          color: (params: any) => getValueColor(params.value?.[2]),
        },
        tooltip: {
          formatter: (params: any) => {
            const value = params.value?.[2]
            if (value === null || value === undefined) {
              return `${params.name}<br/>暂无数据`
            }
            return `${params.name}<br/>数值：${formatValue(value)}`
          },
        },
      },
    ],
  }
  spatialChartInstance.value.setOption(option, true)
}

const handleSpatialChartClick = (params: any) => {
  if (params?.seriesType === 'map' && params.name) {
    const matched = matchAvailableProvince(params.name)
    if (matched && matched !== spatialConfig.province) {
      spatialConfig.province = matched
      runSpatialAnalysis()
    }
  }
}

const bootstrapAnalysis = async () => {
  if (!ensureLoggedIn()) return
  await loadFilterOptions()
  await loadSummary()
  await runTimeSeriesAnalysis()
  await runCorrelationAnalysis()
  await runSpatialAnalysis()
  await fetchTableRecords()
}

onMounted(() => {
  initTimeSeriesChart()
  initCorrelationChart()
  initSpatialChart()
  bootstrapAnalysis()
})

onUnmounted(() => {
  resizeCleanupHandlers.forEach((cleanup) => cleanup())
  timeSeriesChartInstance.value?.dispose()
  correlationChartInstance.value?.dispose()
  spatialChartInstance.value?.dispose()
})
</script>

<style scoped>
.statistical-analysis {
  padding: 0;
}

.stats-overview {
  margin-bottom: 20px;
}

.last-updated {
  margin-top: 12px;
  color: #7f8c8d;
  font-size: 0.9rem;
}

.stat-card {
  border: 1px solid var(--el-border-color-light);
  box-shadow: 0 1px 4px rgba(25, 55, 90, 0.08);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 15px;
}

.stat-icon {
  font-size: 1.8rem;
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: #fff;
}

.stat-icon.total-data {
  background: #1d4f91;
}

.stat-icon.analysis-tasks {
  background: #2f855a;
}

.stat-icon.stations {
  background: #b7791f;
}

.stat-icon.elements {
  background: #3d78b2;
}

.stat-number {
  font-size: 2rem;
  font-weight: 600;
  color: #2c3e50;
  line-height: 1;
}

.stat-label {
  color: #7f8c8d;
  font-size: 0.9rem;
  margin-top: 5px;
}

.analysis-type-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.card-header h3 {
  margin: 0;
}

.analysis-content {
  padding: 20px 0;
}

.config-card,
.result-card {
  height: 500px;
}

.chart-container {
  height: 420px;
  width: 100%;
}

.correlation-chart-container {
  height: 450px;
}

.map-card {
  height: 520px;
}

.map-container {
  height: 430px;
  width: 100%;
}

.map-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.85rem;
  color: #666;
  margin-bottom: 8px;
}

.map-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.map-summary {
  margin-top: 16px;
  font-size: 0.9rem;
  color: #4f5b66;
  line-height: 1.5;
}

.map-header {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.map-header__subtitle {
  font-size: 0.85rem;
  color: #9b9b9b;
}

.data-table-card {
  margin-bottom: 20px;
}

.table-filter-form {
  margin-bottom: 14px;
  padding: 16px;
  background: #f8fafc;
  border: 1px solid #e6edf5;
  border-radius: 8px;
}

.filter-grid {
  display: grid;
  grid-template-columns:
    minmax(220px, 1fr)
    minmax(220px, 1fr)
    minmax(220px, 1fr)
    minmax(360px, 1.45fr)
    auto;
  gap: 16px 18px;
  align-items: start;
}

.filter-field {
  margin-bottom: 0;
  min-width: 0;
}

.time-range-field {
  min-width: 360px;
}

.filter-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  align-items: center;
  align-self: start;
  min-height: 32px;
}

.filter-actions .el-button {
  margin-left: 0;
}

.table-pagination {
  margin-top: 15px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .table-filter-form {
    padding: 14px;
  }

  .filter-grid {
    grid-template-columns: 1fr;
  }

  .time-range-field {
    min-width: 0;
  }

  .filter-actions {
    justify-content: flex-start;
  }

  .config-card,
  .result-card {
    height: auto;
    margin-bottom: 16px;
  }

  .chart-container {
    height: 320px;
  }

  .correlation-chart-container {
    height: 360px;
  }
}

@media (max-width: 1500px) {
  .filter-grid {
    grid-template-columns: repeat(2, minmax(260px, 1fr));
  }

  .time-range-field {
    min-width: 0;
  }

  .filter-actions {
    justify-content: flex-end;
  }
}

@media (max-width: 900px) {
  .filter-grid {
    grid-template-columns: 1fr;
  }

  .filter-actions {
    justify-content: flex-start;
  }
}
</style>
