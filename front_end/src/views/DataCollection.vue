<template>
  <div class="data-collection">
    <!-- 任务概览统计 -->
    <div class="stats-overview">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon total-tasks"><el-icon><DataLine /></el-icon></div>
              <div class="stat-info">
                <div class="stat-number">{{ taskStats.totalTasks }}</div>
                <div class="stat-label">总任务数</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon running-tasks"><el-icon><Loading /></el-icon></div>
              <div class="stat-info">
                <div class="stat-number">{{ taskStats.runningTasks }}</div>
                <div class="stat-label">运行中</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon success-tasks"><el-icon><CircleCheck /></el-icon></div>
              <div class="stat-info">
                <div class="stat-number">{{ taskStats.successTasks }}</div>
                <div class="stat-label">成功完成</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon failed-tasks"><el-icon><CircleClose /></el-icon></div>
              <div class="stat-info">
                <div class="stat-number">{{ taskStats.failedTasks }}</div>
                <div class="stat-label">失败任务</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 任务可视化图表 -->
    <el-row :gutter="20" class="charts-section">
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <h3>任务状态分布</h3>
          </template>
          <div ref="statusChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <h3>任务执行趋势</h3>
          </template>
          <div ref="trendChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 创建采集任务 -->
    <el-card class="create-task-card">
      <template #header>
        <h3>创建采集任务</h3>
      </template>

      <el-form
        :model="taskForm"
        :rules="taskRules"
        ref="taskFormRef"
        label-position="top"
        class="task-form"
      >
        <div class="form-grid">
          <el-form-item label="任务名称" prop="name" class="grid-item">
            <el-input v-model="taskForm.name" placeholder="请输入任务名称" />
          </el-form-item>

          <el-form-item label="采集时间范围" prop="timeRange" class="grid-item">
            <el-date-picker
              v-model="taskForm.timeRange"
              type="datetimerange"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              range-separator="至"
              format="YYYY-MM-DD HH:00"
              value-format="YYYY-MM-DD HH:00:00"
              @change="handleTimeRangeChange"
              style="width: 100%"
            />
          </el-form-item>

          <el-form-item label="采集地点" prop="location.stationIds" class="grid-item grid-span-3">
            <el-cascader
              class="location-cascader"
              v-model="taskForm.location.stationIds"
              :options="cascaderOptions"
              :props="cascaderProps"
              placeholder="请选择省份与站点（可多选，最多30个站点）"
              collapse-tags
              collapse-tags-tooltip
              :max-collapse-tags="2"
              filterable
              clearable
              @change="handleStationSelection"
            />
          </el-form-item>
        </div>

        <el-form-item
          v-if="allSelectedStations.length > 0"
          label="已选站点"
          class="section-card"
        >
          <div class="selected-stations-header">
            <span>已展示 {{ displayedStations.length }}/{{ allSelectedStations.length }} 个站点</span>
            <el-button
              v-if="allSelectedStations.length > MAX_VISIBLE_STATIONS"
              type="primary"
              link
              size="small"
              @click="showAllStations = !showAllStations"
            >
              {{ showAllStations ? '折叠' : '展开全部' }}
            </el-button>
          </div>
          <div class="selected-stations-info">
            <div
              v-for="station in displayedStations"
              :key="station.stationId"
              class="station-info-card"
            >
              <el-descriptions :column="2" border size="small" class="station-info">
                <el-descriptions-item label="站点名">{{ station.stationName }}</el-descriptions-item>
                <el-descriptions-item label="区站号">{{ station.stationId }}</el-descriptions-item>
                <el-descriptions-item label="经度">{{ station.longitude }}</el-descriptions-item>
                <el-descriptions-item label="纬度">{{ station.latitude }}</el-descriptions-item>
                <el-descriptions-item label="压力传感器高程">
                  {{ station.pressureSensorAltitude || '—' }}
                </el-descriptions-item>
                <el-descriptions-item label="观测场高程">
                  {{ station.observationAltitude || '—' }}
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </div>
          <div
            v-if="!showAllStations && hiddenStationsCount > 0"
            class="selected-stations-hint"
          >
            还有 {{ hiddenStationsCount }} 个站点已折叠
          </div>
        </el-form-item>

        <el-form-item label="气象要素" prop="elements" class="section-card stacked-section">
          <el-checkbox-group v-model="taskForm.elements" class="element-groups">
            <div
              v-for="category in meteorologicalCategories"
              :key="category.label"
              class="element-category"
            >
              <h4>{{ category.label }}</h4>
              <div class="element-list">
                <el-checkbox
                  v-for="item in category.items"
                  :key="item.code"
                  :label="item.code"
                >
                  {{ item.name }}
                  <span v-if="item.unit" class="element-unit">({{ item.unit }})</span>
                </el-checkbox>
              </div>
            </div>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="任务描述" prop="description" class="section-card">
          <el-input
            v-model="taskForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入任务描述"
          />
        </el-form-item>

        <el-form-item label="API 调用" class="section-card actions-row">
          <el-button type="primary" @click="simulateApiRequest" :loading="apiRequestLoading">
            使用当前参数模拟调用
          </el-button>
          <el-button @click="resetApiResponse">清空回执</el-button>
        </el-form-item>

        <el-form-item v-if="apiRequestResponse" label="回执结果" class="section-card">
          <pre class="api-response__content">{{ JSON.stringify(apiRequestResponse, null, 2) }}</pre>
        </el-form-item>

        <el-form-item class="form-actions">
          <el-button type="primary" @click="createTask" :loading="creating"> 创建任务 </el-button>
          <el-button @click="resetForm">重置</el-button>
          <el-button type="success" @click="showTemplateDialog = true"> 使用模板 </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 任务列表 -->
    <el-card class="task-list-card">
      <template #header>
        <div class="table-header">
          <h3>任务列表</h3>
          <div class="header-actions">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索任务名称或描述"
              style="width: 300px; margin-right: 10px"
              clearable
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button @click="refreshTasks">
              <el-icon><Refresh /></el-icon>
              刷新
          </el-button>
          </div>
        </div>
      </template>

      <el-table
        :data="filteredTasks"
        style="width: 100%"
        v-loading="loading"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="任务ID" width="80" />
        <el-table-column prop="name" label="任务名称" width="180" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusColor(scope.row.status)">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="progress" label="进度" width="120">
          <template #default="scope">
            <el-progress
              :percentage="scope.row.progress"
              :color="getProgressColor(scope.row.progress)"
              :show-text="false"
            />
            <span class="progress-text">{{ scope.row.progress }}%</span>
          </template>
        </el-table-column>
        <el-table-column prop="elements" label="采集要素" width="260">
          <template #default="scope">
            <el-tag
              v-for="element in getVisibleElements(scope.row.elements)"
              :key="element"
              size="small"
              style="margin-right: 5px; margin-bottom: 4px"
            >
              {{ getElementName(element) }}
            </el-tag>
            <el-popover
              v-if="getHiddenElementsCount(scope.row.elements) > 0"
              placement="top"
              trigger="hover"
            >
              <template #reference>
                <el-tag
                  size="small"
                  type="info"
                  style="cursor: pointer; margin-bottom: 4px"
                >
                  +{{ getHiddenElementsCount(scope.row.elements) }} 更多
                </el-tag>
              </template>
              <div class="more-elements">
                <el-tag
                  v-for="element in scope.row.elements.slice(MAX_ELEMENT_TAGS)"
                  :key="element"
                  size="small"
                  style="margin: 0 5px 5px 0"
                >
                  {{ getElementName(element) }}
                </el-tag>
              </div>
            </el-popover>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="采集地点" width="150" />
        <el-table-column prop="createTime" label="创建时间" width="150" />
        <el-table-column prop="updateTime" label="更新时间" width="150" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="viewTask(scope.row)">查看</el-button>
            <el-button
              size="small"
              :type="scope.row.status === '运行中' ? 'warning' : 'success'"
              @click="toggleTaskStatus(scope.row)"
            >
              {{ scope.row.status === '运行中' ? '暂停' : '启动' }}
            </el-button>
            <el-button size="small" type="danger" @click="deleteTask(scope.row)"> 删除 </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="totalTasks"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 任务详情对话框 -->
    <el-dialog v-model="showTaskDialog" title="任务详情" width="800px">
      <div v-if="selectedTask" class="task-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="任务名称">{{ selectedTask.name }}</el-descriptions-item>
          <el-descriptions-item label="任务状态">
            <el-tag :type="getStatusColor(selectedTask.status)">{{ selectedTask.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="进度">{{ selectedTask.progress }}%</el-descriptions-item>
          <el-descriptions-item label="采集地点">{{ selectedTask.location }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ selectedTask.startTime || '—' }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ selectedTask.endTime || '—' }}</el-descriptions-item>
          <el-descriptions-item label="数据源">{{ selectedTask.dataSource }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{
            selectedTask.createTime
          }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{
            selectedTask.updateTime
          }}</el-descriptions-item>
          <el-descriptions-item label="任务描述" :span="2">{{
            selectedTask.description
          }}</el-descriptions-item>
        </el-descriptions>

        <div class="task-elements">
          <h4>采集要素</h4>
          <el-tag
            v-for="element in selectedTask.elements"
            :key="element"
            style="margin-right: 10px; margin-bottom: 10px"
          >
            {{ getElementName(element) }}
          </el-tag>
        </div>

        <div class="task-log" v-if="selectedTask.logs">
          <h4>执行日志</h4>
          <el-scrollbar height="200px">
            <div class="log-content">
              <div
                v-for="(log, index) in selectedTask.logs"
                :key="index"
                class="log-item"
                :class="log.level"
              >
                <span class="log-time">{{ log.time }}</span>
                <span class="log-message">{{ log.message }}</span>
              </div>
            </div>
          </el-scrollbar>
        </div>
      </div>
    </el-dialog>

    <!-- 任务模板对话框 -->
    <el-dialog v-model="showTemplateDialog" title="任务模板" width="600px">
      <div class="template-list">
        <el-card
          v-for="template in taskTemplates"
          :key="template.id"
          class="template-card"
          shadow="hover"
          @click="useTemplate(template)"
        >
          <div class="template-content">
            <h4>{{ template.name }}</h4>
            <p>{{ template.description }}</p>
            <div class="template-elements">
              <el-tag
                v-for="element in template.elements"
                :key="element"
                size="small"
                style="margin-right: 5px"
              >
                {{ getElementName(element) }}
              </el-tag>
            </div>
          </div>
        </el-card>
      </div>
    </el-dialog>

    <!-- 批量操作对话框 -->
    <el-dialog v-model="showBatchDialog" title="批量操作" width="400px">
      <p>已选择 {{ selectedTasks.length }} 个任务</p>
      <el-form label-width="100px">
        <el-form-item label="操作类型">
          <el-select v-model="batchAction" style="width: 100%">
            <el-option label="批量启动" value="start" />
            <el-option label="批量暂停" value="pause" />
            <el-option label="批量删除" value="delete" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showBatchDialog = false">取消</el-button>
        <el-button type="primary" @click="executeBatchAction" :loading="batchProcessing">
          执行操作
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { CascaderOption, CascaderProps, FormInstance, FormRules } from 'element-plus'
import * as echarts from 'echarts'
import http from '@/api/http'
import dayjs from 'dayjs'

// 定义接口类型
interface CollectionTask {
  id: number
  name: string
  status: string
  statusCode: number
  progress: number
  location: string
  dataSource: string
  createTime: string
  updateTime: string
  description: string
  elements: string[]
  startTime?: string
  endTime?: string
  logs: Array<{ time: string; level: string; message: string }>
}

interface BackendCollectionTask {
  taskId: number
  taskName: string
  userId: number
  startTime: string
  endTime: string
  stationList: number[]
  elementList: string[]
  taskDescription: string
  status: number
  createdAt: string
  updatedAt: string
}

const STATUS_TEXT_MAP: Record<number, string> = {
  0: '失败',
  1: '已完成',
  2: '运行中',
  3: '已暂停',
}

const STATUS_PROGRESS_MAP: Record<number, number> = {
  0: 0,
  1: 100,
  2: 50,
  3: 25,
}

interface TaskTemplate {
  id: number
  name: string
  description: string
  elements: string[]
}

interface WeatherStation {
  stationId: string
  stationName: string
  latitude: number
  longitude: number
  province: string
  pressureSensorAltitude?: string | null
  observationAltitude?: string | null
}

interface MeteorologicalElement {
  code: string
  name: string
  unit?: string
}

interface MeteorologicalCategory {
  label: string
  items: MeteorologicalElement[]
}

// 响应式数据
const loading = ref(false)
const creating = ref(false)
const apiRequestLoading = ref(false)
const batchProcessing = ref(false)
const showTaskDialog = ref(false)
const showTemplateDialog = ref(false)
const showBatchDialog = ref(false)
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const totalTasks = ref(0)
const selectedTasks = ref<CollectionTask[]>([])
const selectedTask = ref<CollectionTask | null>(null)
const batchAction = ref('')

const taskFormRef = ref<FormInstance>()
const stations = ref<WeatherStation[]>([])
const cascaderOptions = ref<CascaderOption[]>([])
const showAllStations = ref(false)
const MAX_VISIBLE_STATIONS = 3

// 任务表单
const taskForm = reactive({
  name: '',
  location: {
    stationIds: [] as string[],
  },
  elements: [] as string[],
  dataSource: 'weather_station',
  description: '',
  timeRange: [] as string[],
})

const stationMap = computed<Record<string, WeatherStation>>(() => {
  return stations.value.reduce((map, station) => {
    map[station.stationId] = station
    return map
  }, {} as Record<string, WeatherStation>)
})

const allSelectedStations = computed<WeatherStation[]>(() => {
  if (!taskForm.location.stationIds || taskForm.location.stationIds.length === 0) {
    return []
  }
  return taskForm.location.stationIds
    .map((id) => stationMap.value[id])
    .filter((station): station is WeatherStation => Boolean(station))
})

const displayedStations = computed<WeatherStation[]>(() => {
  const stationsList = allSelectedStations.value
  if (showAllStations.value) {
    return stationsList
  }
  return stationsList.slice(0, MAX_VISIBLE_STATIONS)
})

const hiddenStationsCount = computed(() => {
  if (showAllStations.value) {
    return 0
  }
  return Math.max(allSelectedStations.value.length - MAX_VISIBLE_STATIONS, 0)
})

watch(
  () => allSelectedStations.value.length,
  (newCount) => {
    if (newCount <= MAX_VISIBLE_STATIONS) {
      showAllStations.value = false
    }
  },
)

// 图表引用
const statusChartRef = ref()
const trendChartRef = ref()
let statusChart: echarts.ECharts | null = null
let trendChart: echarts.ECharts | null = null

// 任务统计数据（由真实任务数据计算得到）
const taskStats = reactive({
  totalTasks: 0,
  runningTasks: 0,
  successTasks: 0,
  failedTasks: 0,
})

const apiRequestResponse = ref<Record<string, any> | null>(null)

const meteorologicalCategories: MeteorologicalCategory[] = [
  {
    label: '观测要素',
    items: [
      { code: 'PRS', name: '气压', unit: '百帕' },
      { code: 'PRS_Sea', name: '海平面气压', unit: '百帕' },
      { code: 'PRS_Max', name: '最高气压', unit: '百帕' },
      { code: 'PRS_Min', name: '最低气压', unit: '百帕' },
      { code: 'TEM', name: '温度/气温', unit: '摄氏度(℃)' },
      { code: 'TEM_MAX', name: '最高气温', unit: '摄氏度(℃)' },
      { code: 'TEM_MIN', name: '最低气温', unit: '摄氏度(℃)' },
      { code: 'RHU', name: '相对湿度', unit: '百分率' },
      { code: 'RHU_Min', name: '最小相对湿度', unit: '百分率' },
      { code: 'VAP', name: '水汽压', unit: '百帕' },
      { code: 'PRE_3h', name: '3小时降水量', unit: '毫米' },
      { code: 'WIN_D_INST_Max', name: '极大风速的风向(角度)', unit: '字符' },
      { code: 'WIN_S_MAX', name: '最大风速', unit: '米/秒' },
      { code: 'WIN_D_S_Max', name: '最大风速的风向(角度)', unit: '度' },
      { code: 'WIN_S_Avg_2mi', name: '2分钟平均风速', unit: '米/秒' },
      { code: 'WIN_D_Avg_2mi', name: '2分钟平均风向(角度)', unit: '度' },
      { code: 'WEP_Now', name: '现在天气' },
      { code: 'WIN_S_Inst_Max', name: '极大风速', unit: '米/秒' },
      { code: 'VIS', name: '水平能见度(人工)', unit: '米' },
      { code: 'CLO_Cov', name: '总云量', unit: '百分率' },
      { code: 'CLO_Cov_Low', name: '低云量', unit: '百分率' },
      { code: 'CLO_COV_LM', name: '云量(低云或中云)', unit: '百分率' },
      { code: 'Datetime', name: '资料时间' },
    ],
  },
]

const meteorologicalElementMap: Record<string, string> = meteorologicalCategories.reduce(
  (map, category) => {
    category.items.forEach((item) => {
      map[item.code] = item.unit ? `${item.name}(${item.unit})` : item.name
    })
    return map
  },
  {} as Record<string, string>,
)

// 任务列表（从后端加载）
const tasks = ref<CollectionTask[]>([])

// 任务模板（模拟数据）
const taskTemplates = ref<TaskTemplate[]>([
  {
    id: 1,
    name: '标准气象站采集',
    description: '采集标准气象站的所有基础气象要素',
    elements: [
      'PRS',
      'PRS_Sea',
      'TEM',
      'RHU',
      'WIN_S_MAX',
      'PRE_3h',
    ],
  },
  {
    id: 2,
    name: '空气质量监测',
    description: '专门用于空气质量监测的采集任务',
    elements: ['PRS', 'PRS_Min', 'PRS_Max', 'TEM', 'TEM_MAX', 'TEM_MIN', 'RHU', 'VIS'],
  },
  {
    id: 3,
    name: '海洋气象观测',
    description: '适用于海洋气象观测的采集任务',
    elements: ['PRS_Sea', 'TEM', 'RHU', 'WIN_S_MAX', 'WIN_D_S_Max', 'WIN_S_Inst_Max', 'VAP'],
  },
])

// 表单验证规则
const taskRules: FormRules = {
  name: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  'location.stationIds': [
    {
      required: true,
      trigger: 'change',
      validator: (_rule, value, callback) => {
        if (!value || value.length === 0) {
          callback(new Error('请至少选择一个站点'))
        } else if (value.length > 30) {
          callback(new Error('最多只能选择30个站点'))
        } else {
          callback()
        }
      },
    },
  ],
  elements: [{ required: true, message: '请选择至少一个气象要素', trigger: 'change' }],
  timeRange: [{ required: true, message: '请选择采集时间范围', trigger: 'change' }],
}

const handleTimeRangeChange = (value: string[] | null) => {
  if (!value || value.length !== 2) {
    taskForm.timeRange = []
    return
  }
  // 存储为 YYYY-MM-DD HH:00:00 格式（用于显示和表单验证）
  taskForm.timeRange = value.map((time) =>
    dayjs(time).minute(0).second(0).millisecond(0).format('YYYY-MM-DD HH:00:00'),
  )
}

// 将时间格式转换为 API 要求的格式：[YYYYMMDDHHMISS,YYYYMMDDHHMISS]
const formatTimeRangeForApi = (timeRange: string[]): string => {
  if (!timeRange || timeRange.length !== 2) {
    return ''
  }
  const formatted = timeRange.map((time) =>
    dayjs(time).format('YYYYMMDDHHmmss'),
  )
  return `[${formatted.join(',')}]`
}

// 计算属性
const filteredTasks = computed(() => {
  if (!searchKeyword.value) return tasks.value
  return tasks.value.filter(
    (task) =>
      task.name.includes(searchKeyword.value) || task.description.includes(searchKeyword.value),
  )
})

// 工具方法
const getStatusColor = (status: string) => {
  const colorMap: Record<string, string> = {
    运行中: 'success',
    已完成: 'primary',
    已暂停: 'warning',
    失败: 'danger',
  }
  return colorMap[status] || 'default'
}

const getProgressColor = (progress: number) => {
  if (progress < 30) return '#f56c6c'
  if (progress < 70) return '#e6a23c'
  return '#67c23a'
}

const getElementName = (element: string) => {
  return meteorologicalElementMap[element] || element
}

// 任务列表中要素展示最多显示的标签数量
const MAX_ELEMENT_TAGS = 3

const getVisibleElements = (elements: string[]) => {
  if (!Array.isArray(elements)) return []
  return elements.slice(0, MAX_ELEMENT_TAGS)
}

const getHiddenElementsCount = (elements: string[]) => {
  if (!Array.isArray(elements)) return 0
  return Math.max(elements.length - MAX_ELEMENT_TAGS, 0)
}

const mergeStations = (newStations: WeatherStation[]) => {
  const stationMap = new Map(stations.value.map((station) => [station.stationId, station]))
  newStations.forEach((station) => {
    stationMap.set(station.stationId, station)
  })
  stations.value = Array.from(stationMap.values())
}

const fetchProvinces = async () => {
  try {
    const { data } = await http.get('/api/weather/stations/provinces')
    const provinceList = Array.isArray(data) ? data : []
    cascaderOptions.value = provinceList.map((province) => ({
      value: province,
      label: province,
      leaf: false,
    }))
    return cascaderOptions.value
  } catch (error) {
    ElMessage.error('获取省份信息失败，请稍后重试')
    return []
  }
}

const fetchStations = async (province: string) => {
  if (!province) return []
  try {
    const { data } = await http.get('/api/weather/stations', { params: { province } })
    const fetchedStations = Array.isArray(data) ? data : []
    mergeStations(fetchedStations)
    return fetchedStations
  } catch (error) {
    ElMessage.error('获取站点信息失败，请稍后重试')
    return []
  }
}

const loadLocationNodes: CascaderProps['lazyLoad'] = async (node, resolve) => {
  if (node.level === 0) {
    const roots = cascaderOptions.value.length > 0 ? cascaderOptions.value : await fetchProvinces()
    resolve(roots)
    return
  }

  if (node.level === 1) {
    const province = node.value as string
    const stationList = await fetchStations(province)
    const children: CascaderOption[] = stationList.map((station) => ({
      value: station.stationId,
      label: `${station.stationName} (${station.stationId})`,
      leaf: true,
    }))
    resolve(children)
    return
  }

  resolve([])
}

const cascaderProps: CascaderProps = {
  value: 'value',
  label: 'label',
  children: 'children',
  multiple: true,
  emitPath: false,
  checkStrictly: false,
  lazy: true,
  lazyLoad: loadLocationNodes,
}

const handleStationSelection = (stationIds: string[]) => {
  if (stationIds.length > 30) {
    ElMessage.warning('最多只能选择30个站点')
    taskForm.location.stationIds = stationIds.slice(0, 30)
  }
}

const resetApiResponse = () => {
  apiRequestResponse.value = null
}

const simulateApiRequest = async () => {
  if (!taskForm.location.stationIds || taskForm.location.stationIds.length === 0) {
    ElMessage.warning('请至少选择一个站点后再调用接口')
    return
  }

  if (taskForm.location.stationIds.length > 30) {
    ElMessage.warning('最多只能选择30个站点')
    return
  }

  if (taskForm.elements.length === 0) {
    ElMessage.warning('请至少选择一个气象要素')
    return
  }

  if (taskForm.timeRange.length !== 2) {
    ElMessage.warning('请先选择完整的采集时间范围')
    return
  }

  apiRequestLoading.value = true
  try {
    const payload = {
      staIDs: taskForm.location.stationIds,
      timeRange: formatTimeRangeForApi(taskForm.timeRange),
      elements: taskForm.elements,
      dataSource: taskForm.dataSource || 'weather_station',
      frequency: 'hourly',
      remarks: taskForm.description,
    }
    const { data } = await http.post('/api/weather/simulate', payload)
    apiRequestResponse.value = data
    ElMessage.success(data?.message || '模拟访问成功')
  } catch (error) {
    ElMessage.error('模拟访问失败，请稍后再试')
  } finally {
    apiRequestLoading.value = false
  }
}

// 计算执行趋势数据（按月份统计完成/失败任务数）
const getTrendSeriesData = () => {
  const completedPerMonth = Array(12).fill(0)
  const failedPerMonth = Array(12).fill(0)

  tasks.value.forEach((task) => {
    // 使用创建时间所在月份作为统计维度
    const monthIndex = dayjs(task.createTime).month() // 0-11
    if (isNaN(monthIndex)) return

    if (task.statusCode === 1) {
      completedPerMonth[monthIndex] += 1
    } else if (task.statusCode === 0) {
      failedPerMonth[monthIndex] += 1
    }
  })

  return { completedPerMonth, failedPerMonth }
}

// 根据真实数据更新图表
const updateCharts = () => {
  if (!statusChart || !trendChart) return

  // 状态分布饼图
  statusChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)',
    },
    legend: {
      orient: 'vertical',
      left: 'left',
    },
    series: [
      {
        name: '任务状态',
        type: 'pie',
        radius: '50%',
        data: [
          { value: taskStats.runningTasks, name: '运行中' },
          { value: taskStats.successTasks, name: '已完成' },
          {
            value:
              taskStats.totalTasks -
              taskStats.runningTasks -
              taskStats.successTasks -
              taskStats.failedTasks,
            name: '已暂停',
          },
          { value: taskStats.failedTasks, name: '失败' },
        ],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)',
          },
        },
      },
    ],
  })

  // 执行趋势折线图
  const { completedPerMonth, failedPerMonth } = getTrendSeriesData()
  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
    },
    xAxis: {
      type: 'category',
      data: [
        '1月',
        '2月',
        '3月',
        '4月',
        '5月',
        '6月',
        '7月',
        '8月',
        '9月',
        '10月',
        '11月',
        '12月',
      ],
    },
    yAxis: {
      type: 'value',
    },
    series: [
      {
        name: '完成任务数',
        type: 'line',
        data: completedPerMonth,
        smooth: true,
        itemStyle: {
          color: '#67c23a',
        },
      },
      {
        name: '失败任务数',
        type: 'line',
        data: failedPerMonth,
        smooth: true,
        itemStyle: {
          color: '#f56c6c',
        },
      },
    ],
  })
}

// 初始化图表
const initCharts = () => {
  nextTick(() => {
    if (statusChartRef.value && !statusChart) {
      statusChart = echarts.init(statusChartRef.value)
    }
    if (trendChartRef.value && !trendChart) {
      trendChart = echarts.init(trendChartRef.value)
    }

    if (statusChart && trendChart) {
      updateCharts()

      // 响应式调整
      window.addEventListener('resize', () => {
        statusChart && statusChart.resize()
        trendChart && trendChart.resize()
      })
    }
  })
}

// 方法
const handleSelectionChange = (selection: CollectionTask[]) => {
  selectedTasks.value = selection
  if (selection.length > 0) {
    showBatchDialog.value = true
  }
}

const handleSizeChange = (val: number) => {
  pageSize.value = val
}

const handleCurrentChange = (val: number) => {
  currentPage.value = val
}

const createTask = async () => {
  if (!taskFormRef.value) return

  await taskFormRef.value.validate(async (valid) => {
    if (valid) {
      creating.value = true
      try {
        // 获取用户ID
        const userIdStr = localStorage.getItem('userId')
        if (!userIdStr) {
          ElMessage.error('用户未登录，请先登录')
          return
        }
        const userId = parseInt(userIdStr, 10)

        // 验证时间范围
        if (!taskForm.timeRange || taskForm.timeRange.length !== 2) {
          ElMessage.error('请选择完整的采集时间范围')
          return
        }

        // 验证站点列表
        if (!taskForm.location.stationIds || taskForm.location.stationIds.length === 0) {
          ElMessage.error('请至少选择一个站点')
          return
        }

        // 验证要素列表
        if (!taskForm.elements || taskForm.elements.length === 0) {
          ElMessage.error('请至少选择一个气象要素')
          return
        }

        // 将站点ID从字符串转换为整数
        const stationList = taskForm.location.stationIds.map((id) => parseInt(id, 10))

        // 将时间格式转换为ISO 8601格式（后端期望的OffsetDateTime格式）
        const startTime = dayjs(taskForm.timeRange[0]).toISOString()
        const endTime = dayjs(taskForm.timeRange[1]).toISOString()

        // 构建请求数据
        const requestData = {
          taskName: taskForm.name,
          userId: userId,
          startTime: startTime,
          endTime: endTime,
          stationList: stationList,
          elementList: taskForm.elements,
          taskDescription: taskForm.description || null,
        }

        // 调用后端API创建任务
        const { data } = await http.post('/api/collection-tasks', requestData)

        ElMessage.success('任务创建成功')
        
        // 刷新任务列表
        await loadTasks()
        
        resetForm()
      } catch (error: any) {
        console.error('创建任务失败:', error)
        const errorMsg = error?.response?.data?.message || error?.message || '任务创建失败，请重试'
        ElMessage.error(errorMsg)
      } finally {
        creating.value = false
      }
    }
  })
}

const resetForm = () => {
  taskForm.name = ''
  taskForm.location.stationIds = []
  taskForm.elements = []
  taskForm.dataSource = 'weather_station'
  taskForm.description = ''
  taskForm.timeRange = []
  stations.value = []
  apiRequestResponse.value = null
}

const viewTask = (task: any) => {
  selectedTask.value = task
  showTaskDialog.value = true
}

const toggleTaskStatus = async (task: any) => {
  const action = task.status === '运行中' ? '暂停' : '启动'
  const newStatus = task.status === '运行中' ? 3 : 2 // 3=暂停, 2=运行中
  try {
    await ElMessageBox.confirm(`确定要${action}任务 ${task.name} 吗？`, '确认操作')
    
    // 调用后端API更新任务状态
    await http.patch(`/api/collection-tasks/${task.id}/status`, {
      status: newStatus,
    })
    
    ElMessage.success(`任务${action}成功`)
    await loadTasks() // 刷新任务列表
  } catch (error: any) {
    if (error?.response?.status !== 400) {
      // 400可能是用户取消操作
      const errorMsg = error?.response?.data?.message || error?.message || '操作失败'
      ElMessage.error(errorMsg)
    }
  }
}

const deleteTask = async (task: any) => {
  try {
    await ElMessageBox.confirm(`确定要删除任务 ${task.name} 吗？此操作不可撤销！`, '确认删除', {
      type: 'error',
    })
    
    // 调用后端API删除任务
    await http.delete(`/api/collection-tasks/${task.id}`)
    
    ElMessage.success('任务删除成功')
    await loadTasks() // 刷新任务列表
  } catch (error: any) {
    if (error?.response?.status !== 400) {
      // 400可能是用户取消操作
      const errorMsg = error?.response?.data?.message || error?.message || '删除失败'
      ElMessage.error(errorMsg)
    }
  }
}

const useTemplate = (template: TaskTemplate) => {
  taskForm.elements = [...template.elements]
  showTemplateDialog.value = false
  ElMessage.success(`已应用模板：${template.name}`)
}

// 从后端加载任务列表
const loadTasks = async () => {
  loading.value = true
  try {
    const userIdStr = localStorage.getItem('userId')
    if (!userIdStr) {
      ElMessage.warning('用户未登录')
      return
    }
    const userId = parseInt(userIdStr, 10)

    const { data } = await http.get('/api/collection-tasks', {
      params: { userId },
    })

    // 将后端数据转换为前端显示格式
    tasks.value = (data || []).map((task: BackendCollectionTask) => {
      // 生成地点标签：显示站点数量或站点ID列表
      let locationLabel = '未选择站点'
      if (task.stationList && task.stationList.length > 0) {
        // 尝试从已加载的站点中查找站点名称
        const foundStations = task.stationList
          .map((stationId) => {
            const station = stations.value.find((s) => s.stationId === String(stationId))
            return station ? `${station.province} - ${station.stationName}` : null
          })
          .filter((name): name is string => name !== null)
        
        if (foundStations.length > 0) {
          locationLabel = foundStations.join(', ')
        } else {
          // 如果找不到站点名称，显示站点ID和数量
          locationLabel = `${task.stationList.length}个站点 (${task.stationList.slice(0, 3).join(', ')}${task.stationList.length > 3 ? '...' : ''})`
        }
      }

      return {
        id: task.taskId,
        name: task.taskName,
        status: STATUS_TEXT_MAP[task.status] || '未知',
        statusCode: task.status,
        progress: STATUS_PROGRESS_MAP[task.status] || 0,
        location: locationLabel,
        dataSource: 'weather_station',
        createTime: dayjs(task.createdAt).format('YYYY-MM-DD HH:mm:ss'),
        updateTime: dayjs(task.updatedAt).format('YYYY-MM-DD HH:mm:ss'),
        description: task.taskDescription || '',
        elements: task.elementList || [],
        startTime: dayjs(task.startTime).format('YYYY-MM-DD HH:mm:ss'),
        endTime: dayjs(task.endTime).format('YYYY-MM-DD HH:mm:ss'),
        logs: [
          {
            time: dayjs(task.createdAt).format('YYYY-MM-DD HH:mm:ss'),
            level: 'info',
            message: '任务创建成功',
          },
        ],
      } as CollectionTask
    })

    // 更新统计数据（基于真实任务数据）
    taskStats.totalTasks = tasks.value.length
    taskStats.runningTasks = tasks.value.filter((t) => t.statusCode === 2).length
    taskStats.successTasks = tasks.value.filter((t) => t.statusCode === 1).length
    taskStats.failedTasks = tasks.value.filter((t) => t.statusCode === 0).length

    // 任务列表和统计更新后，刷新图表
    updateCharts()
  } catch (error: any) {
    console.error('加载任务列表失败:', error)
    ElMessage.error('加载任务列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const refreshTasks = async () => {
  await loadTasks()
  ElMessage.success('任务列表已刷新')
}

const executeBatchAction = async () => {
  if (selectedTasks.value.length === 0) return

  batchProcessing.value = true
  try {
    // 模拟API调用
    await new Promise((resolve) => setTimeout(resolve, 1500))

    switch (batchAction.value) {
      case 'start':
        selectedTasks.value.forEach((task) => {
          task.status = '运行中'
          task.updateTime = new Date().toLocaleString()
        })
        ElMessage.success('批量启动成功')
        break
      case 'pause':
        selectedTasks.value.forEach((task) => {
          task.status = '已暂停'
          task.updateTime = new Date().toLocaleString()
        })
        ElMessage.success('批量暂停成功')
        break
      case 'delete':
        selectedTasks.value.forEach((task) => {
          const index = tasks.value.findIndex((t) => t.id === task.id)
          if (index > -1) tasks.value.splice(index, 1)
        })
        ElMessage.success('批量删除成功')
        break
    }

    showBatchDialog.value = false
    selectedTasks.value = []
  } catch (error) {
    ElMessage.error('批量操作失败，请重试')
  } finally {
    batchProcessing.value = false
  }
}

// 生命周期
onMounted(() => {
  initCharts()
  fetchProvinces()
  loadTasks() // 加载任务列表
})
</script>

<style scoped>
.data-collection {
  padding: 0;
}

.stats-overview {
  margin-bottom: 20px;
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
  font-size: 1.75rem;
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: #fff;
}

.stat-icon.total-tasks {
  background: #1d4f91;
}

.stat-icon.running-tasks {
  background: #2f855a;
}

.stat-icon.success-tasks {
  background: #3d78b2;
}

.stat-icon.failed-tasks {
  background: #d64545;
}

.stat-info {
  flex: 1;
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

.charts-section {
  margin-bottom: 20px;
}

.chart-card {
  height: 400px;
}

.chart-container {
  height: 300px;
  width: 100%;
}

.create-task-card,
.task-list-card {
  margin-bottom: 20px;
  border-radius: 16px;
  border: none;
  box-shadow: 0 15px 40px rgba(15, 23, 42, 0.08);
}

.create-task-card :deep(.el-card__header) {
  border-bottom: none;
  padding-bottom: 0;
}

.task-form {
  padding: 10px 0 10px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.task-form .el-form-item {
  margin-bottom: 0;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px 24px;
  padding: 10px 20px 0;
}

.grid-item {
  background: #f9fbff;
  border-radius: 12px;
  padding: 12px 18px;
  border: 1px solid rgba(99, 102, 241, 0.15);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.4);
}

.grid-item :deep(.el-form-item__content) {
  margin-left: 0 !important;
}

.grid-item :deep(.el-form-item__label) {
  line-height: 20px;
  font-weight: 500;
  color: #334155;
}

.grid-span-2 {
  grid-column: span 2;
}

.grid-span-3 {
  grid-column: span 3;
}

@media (max-width: 768px) {
  .grid-span-2 {
    grid-column: span 1;
  }
  
  .grid-span-3 {
    grid-column: span 1;
  }
}

.section-card {
  margin: 0 20px;
  padding: 12px 18px;
  background: #f5f7fb;
  border-radius: 14px;
  border: 1px solid #e5e9f2;
}

.section-card :deep(.el-form-item__label) {
  font-weight: 600;
  color: #1e293b;
}

.section-card :deep(.el-form-item__content) {
  margin-left: 0 !important;
}

.stacked-section {
  padding-bottom: 4px;
}

.actions-row :deep(.el-form-item__content) {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.location-cascader {
  width: 100%;
}

.location-cascader :deep(.el-input__wrapper) {
  flex-wrap: wrap;
  min-height: 44px;
  padding: 8px 40px 8px 12px;
}

.location-cascader :deep(.el-input__inner) {
  flex: 1;
  width: 100%;
  min-width: 280px;
  text-overflow: clip;
  white-space: nowrap;
  overflow: visible;
}

.location-cascader :deep(.el-input__inner::placeholder) {
  text-overflow: clip;
  white-space: nowrap;
}

.location-cascader :deep(.el-select__tags) {
  width: calc(100% - 30px);
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin: 0;
  max-width: none;
}

.location-cascader :deep(.el-select__tags-text) {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
}

.location-cascader :deep(.el-tag) {
  display: inline-flex;
  align-items: center;
  margin: 0;
  max-width: 280px;
  white-space: normal;
  word-break: break-word;
}

.location-cascader :deep(.el-input__suffix) {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
}

@media (max-width: 900px) {
  .location-cascader {
    max-width: 100%;
  }
  
  .location-cascader :deep(.el-input__inner) {
    min-width: 200px;
  }
  
  .location-cascader :deep(.el-tag) {
    max-width: 180px;
  }
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 10px 20px 0;
}

.api-tip {
  margin-bottom: 0;
}

.api-response__content {
  margin: 0;
  white-space: pre-wrap;
  font-family: 'Fira Code', Consolas, monospace;
  font-size: 0.85rem;
  color: #34495e;
  background: #f8f9fa;
  border-radius: 8px;
  padding: 15px;
  border: 1px solid #e4e7ed;
}

.station-info {
  width: 100%;
  background: #f7f9fc;
  border-radius: 10px;
  padding: 10px;
}

.selected-stations-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-weight: 500;
  color: #1f2937;
}

.selected-stations-info {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.station-info-card {
  flex: 1 1 calc(33.33% - 8px);
  min-width: 260px;
  background: #f7f9fc;
  border-radius: 8px;
  padding: 12px;
  border: 1px solid #e4e7ed;
}

.selected-stations-hint {
  margin-top: 10px;
  font-size: 0.85rem;
  color: #6b7280;
}

.element-groups {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.element-category {
  padding: 12px 16px;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  background: #fafbff;
  box-shadow: inset 0 6px 12px rgba(102, 126, 234, 0.07);
}

.element-category h4 {
  margin: 0 0 10px 0;
  color: #2c3e50;
}

.element-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 20px;
}

.element-unit {
  color: #7f8c8d;
  font-size: 0.85rem;
  margin-left: 4px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table-header h3 {
  margin: 0;
  color: #2c3e50;
}

.header-actions {
  display: flex;
  align-items: center;
}

.progress-text {
  margin-left: 10px;
  font-size: 0.9rem;
  color: #7f8c8d;
}

.pagination-container {
  margin-top: 20px;
  text-align: right;
}

.task-detail {
  padding: 20px 0;
}

.task-elements {
  margin: 20px 0;
}

.task-elements h4 {
  margin-bottom: 10px;
  color: #2c3e50;
}

.task-log {
  margin-top: 20px;
}

.task-log h4 {
  margin-bottom: 10px;
  color: #2c3e50;
}

.log-content {
  background: #f8f9fa;
  border-radius: 4px;
  padding: 10px;
}

.log-item {
  display: flex;
  gap: 10px;
  margin-bottom: 5px;
  font-family: monospace;
  font-size: 0.9rem;
}

.log-item.info {
  color: #409eff;
}

.log-item.warning {
  color: #e6a23c;
}

.log-item.error {
  color: #f56c6c;
}

.log-time {
  color: #7f8c8d;
  min-width: 150px;
}

.template-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 15px;
}

.template-card {
  cursor: pointer;
  transition: all 0.3s ease;
}

.template-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.template-content h4 {
  margin: 0 0 10px 0;
  color: #2c3e50;
}

.template-content p {
  margin: 0 0 15px 0;
  color: #7f8c8d;
  font-size: 0.9rem;
}

.template-elements {
  margin-top: 10px;
}

.more-elements {
  max-width: 360px;
  max-height: 220px;
  overflow-y: auto;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .stats-overview .el-col {
    margin-bottom: 10px;
  }

  .table-header {
    flex-direction: column;
    gap: 15px;
    align-items: stretch;
  }

  .header-actions {
    justify-content: space-between;
  }

  .chart-card {
    height: 300px;
  }

  .chart-container {
    height: 200px;
  }

  .template-list {
    grid-template-columns: 1fr;
  }
}
</style>
