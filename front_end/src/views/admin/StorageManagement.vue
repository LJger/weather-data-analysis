<template>
  <div class="storage-management">
    <section class="page-header">
      <div>
        <div class="eyebrow">Admin Storage</div>
        <h2>存储管理</h2>
        <p>
          管理 HDFS 工作区 {{ basePathDisplay }} 内的目录与文件，同时保留 Redis 缓存状态、Key 查询和按模式清理能力。
        </p>
      </div>
      <div class="page-actions">
        <el-tag :type="hdfsStatusTag.type" effect="plain">{{ hdfsStatusTag.label }}</el-tag>
        <el-tag type="success" effect="plain">Redis 实时</el-tag>
        <el-button :icon="Refresh" @click="refreshAll()" :loading="pageLoading">刷新全部</el-button>
      </div>
    </section>

    <el-row :gutter="16" class="stats-overview">
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon total-storage">
              <el-icon><Coin /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ hdfsOverview.totalFormatted || '-' }}</div>
              <div class="stat-label">HDFS 总容量</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon used-storage">
              <el-icon><PieChart /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ hdfsOverview.usedFormatted || '-' }}</div>
              <div class="stat-label">HDFS 已用</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon available-storage">
              <el-icon><CircleCheckFilled /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ hdfsOverview.availableFormatted || '-' }}</div>
              <div class="stat-label">HDFS 可用</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon redis-memory">
              <el-icon><Monitor /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ redisOverview.usedMemory || '-' }}</div>
              <div class="stat-label">Redis 内存</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-tabs v-model="activeTab" class="storage-tabs">
      <el-tab-pane label="HDFS 工作区" name="hdfs">
        <el-card class="panel-card">
          <template #header>
            <div class="panel-header">
              <div>
                <h3>HDFS 文件浏览器</h3>
                <span>所有操作限定在 {{ basePathDisplay }} 下，页面路径 / 对应 HDFS {{ basePathDisplay }}。</span>
              </div>
              <div class="header-actions">
                <el-button :icon="Refresh" @click="refreshHdfs()" :loading="hdfsLoading">刷新 HDFS</el-button>
                <el-button type="warning" plain :icon="Connection" @click="initHdfsWorkspace" :loading="initLoading">
                  初始化目录
                </el-button>
                <el-button type="primary" plain :icon="DataLine" @click="openGisArchiveDialog">
                  归档GIS数据
                </el-button>
                <el-button type="success" plain :icon="Upload" @click="exportGisTables" :loading="gisTableExportLoading">
                  上传GIS表数据
                </el-button>
                <el-button type="primary" :icon="Plus" @click="openMkdirDialog">新建目录</el-button>
                <el-upload
                  :auto-upload="false"
                  :show-file-list="false"
                  :on-change="handleUploadChange"
                >
                  <el-button type="success" :icon="Upload" :loading="uploading">上传文件</el-button>
                </el-upload>
              </div>
            </div>
          </template>

          <el-alert
            v-if="hdfsError"
            class="storage-alert"
            :title="hdfsError"
            type="error"
            show-icon
            :closable="false"
          />
          <el-alert
            v-else-if="hdfsOverview.safeMode === true"
            class="storage-alert"
            title="HDFS 当前处于 SafeMode，上传、新建、重命名和删除可能失败。请在 Hadoop 节点执行 hdfs dfsadmin -safemode leave。"
            type="warning"
            show-icon
            :closable="false"
          />
          <el-alert
            v-else-if="platformInitialized === false"
            class="storage-alert"
            :title="`平台目录 ${basePathDisplay} 尚未初始化，请先点击“初始化目录”。`"
            type="warning"
            show-icon
            :closable="false"
          />

          <div class="capacity-strip">
            <div class="capacity-copy">
              <span>HDFS 空间使用率</span>
              <strong>{{ hdfsUsagePercent }}%</strong>
            </div>
            <el-progress
              :percentage="hdfsUsagePercent"
              :stroke-width="12"
              :color="getUsageColor(hdfsUsagePercent)"
            />
          </div>

          <div class="browser-toolbar">
            <el-button :icon="ArrowUp" :disabled="currentPath === '/'" @click="goParent">上级目录</el-button>
            <div class="path-bar">
              <span class="path-label">当前目录</span>
              <el-breadcrumb separator="/" class="path-breadcrumb">
                <el-breadcrumb-item>
                  <button class="breadcrumb-button" @click="openPath('/')">{{ basePathDisplay }}</button>
                </el-breadcrumb-item>
                <el-breadcrumb-item v-for="item in breadcrumbItems" :key="item.path">
                  <button class="breadcrumb-button" @click="openPath(item.path)">{{ item.name }}</button>
                </el-breadcrumb-item>
              </el-breadcrumb>
            </div>
          </div>

          <div class="directory-strip">
            <div>
              <span>完整路径</span>
              <strong>{{ currentFullPathDisplay }}</strong>
            </div>
            <div>
              <span>目录</span>
              <strong>{{ folderCount }}</strong>
            </div>
            <div>
              <span>文件</span>
              <strong>{{ fileCount }}</strong>
            </div>
            <div>
              <span>当前对象</span>
              <strong>{{ hdfsFiles.length }}</strong>
            </div>
          </div>

          <el-table
            class="hdfs-file-table"
            :data="hdfsFiles"
            row-key="path"
            style="width: 100%"
            v-loading="hdfsLoading"
            empty-text="当前目录为空或尚未初始化"
          >
            <el-table-column type="expand" width="44">
              <template #default="{ row }">
                <div class="hdfs-meta-grid">
                  <div>
                    <span>副本数</span>
                    <strong>{{ row.isDirectory ? '-' : formatHdfsMeta(row.replication) }}</strong>
                  </div>
                  <div>
                    <span>块大小</span>
                    <strong>{{ row.isDirectory ? '-' : formatHdfsMeta(row.blockSizeFormatted) }}</strong>
                  </div>
                  <div>
                    <span>估算块数</span>
                    <strong>{{ row.isDirectory ? '-' : formatHdfsMeta(row.blockCount) }}</strong>
                  </div>
                  <div>
                    <span>HDFS 占用</span>
                    <strong>{{ formatHdfsMeta(row.spaceConsumedFormatted || row.sizeFormatted) }}</strong>
                  </div>
                  <div>
                    <span>文件数</span>
                    <strong>{{ formatHdfsMeta(row.fileCount ?? (row.isDirectory ? 0 : 1)) }}</strong>
                  </div>
                  <div>
                    <span>子目录数</span>
                    <strong>{{ row.isDirectory ? formatHdfsMeta(row.directoryCount) : '-' }}</strong>
                  </div>
                  <div>
                    <span>Group</span>
                    <strong>{{ formatHdfsMeta(row.group) }}</strong>
                  </div>
                  <div>
                    <span>权限</span>
                    <strong>{{ formatHdfsMeta(row.permission) }}</strong>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="名称" min-width="300">
              <template #default="{ row }">
                <button class="file-name-button" @click="row.isDirectory ? openPath(toWorkspacePath(row.path)) : undefined">
                  <span class="file-icon" :class="{ directory: row.isDirectory }">
                    <el-icon>
                      <FolderOpened v-if="row.isDirectory" />
                      <Document v-else />
                    </el-icon>
                  </span>
                  <span class="file-copy">
                    <strong>{{ row.name || '/' }}</strong>
                    <small>{{ toWorkspacePath(row.path) }}</small>
                  </span>
                </button>
              </template>
            </el-table-column>
            <el-table-column prop="type" label="类型" width="110">
              <template #default="{ row }">
                <el-tag class="file-type-tag" :type="row.isDirectory ? 'primary' : 'info'" effect="plain">
                  {{ row.isDirectory ? '目录' : '文件' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="sizeFormatted" label="大小" width="120" />
            <el-table-column label="HDFS占用" width="130">
              <template #default="{ row }">
                {{ row.spaceConsumedFormatted || row.sizeFormatted || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="副本" width="80">
              <template #default="{ row }">
                {{ row.isDirectory ? '-' : formatHdfsMeta(row.replication) }}
              </template>
            </el-table-column>
            <el-table-column label="块数" width="80">
              <template #default="{ row }">
                {{ row.isDirectory ? '-' : formatHdfsMeta(row.blockCount) }}
              </template>
            </el-table-column>
            <el-table-column prop="permission" label="权限" width="100" />
            <el-table-column prop="owner" label="Owner" width="110" />
            <el-table-column prop="modificationTime" label="修改时间" min-width="160" />
            <el-table-column label="操作" width="260" fixed="right" align="right">
              <template #default="{ row }">
                <div class="row-actions">
                  <el-button v-if="!row.isDirectory" size="small" :icon="Download" @click="downloadFile(row)">
                    下载
                  </el-button>
                  <el-button size="small" :icon="Edit" @click="openRenameDialog(row)">重命名</el-button>
                  <el-button size="small" type="danger" plain :icon="Delete" @click="deleteHdfsItem(row)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="Redis 缓存" name="redis">
        <el-card class="panel-card">
          <template #header>
            <div class="panel-header compact">
              <div>
                <h3>Redis 缓存</h3>
                <span>读取 Redis 状态和 Key 列表，支持按模式清理。</span>
              </div>
              <el-button size="small" :icon="Refresh" @click="refreshRedisData()" :loading="redisLoading">
                刷新
              </el-button>
            </div>
          </template>

          <el-alert
            v-if="redisError"
            class="storage-alert"
            :title="redisError"
            type="error"
            show-icon
            :closable="false"
          />

          <div class="cache-summary">
            <div>
              <span>Redis 版本</span>
              <strong>{{ redisOverview.version || '-' }}</strong>
            </div>
            <div>
              <span>连接数</span>
              <strong>{{ redisOverview.connectedClients ?? '-' }}</strong>
            </div>
            <div>
              <span>运行时长</span>
              <strong>{{ formatUptime(redisOverview.uptime) }}</strong>
            </div>
            <div>
              <span>匹配 Key</span>
              <strong>{{ cacheKeys.length }}</strong>
            </div>
          </div>

          <div class="cache-toolbar">
            <el-input
              v-model="cachePattern"
              clearable
              placeholder="Key 模式，如 weather:*"
              @keyup.enter="loadCacheKeys"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button @click="loadCacheKeys" :loading="cacheLoading">查询</el-button>
            <el-button type="danger" :icon="Delete" @click="clearCache" :loading="clearingCache">
              清理
            </el-button>
          </div>

          <el-table
            :data="cacheKeys"
            size="small"
            row-key="key"
            max-height="420"
            v-loading="cacheLoading"
            empty-text="暂无匹配 Key"
          >
            <el-table-column prop="key" label="Key" min-width="260" show-overflow-tooltip />
            <el-table-column prop="type" label="类型" width="100">
              <template #default="{ row }">
                <el-tag size="small" effect="plain">{{ row.type || 'unknown' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="ttlSeconds" label="TTL" width="120">
              <template #default="{ row }">{{ formatTtl(row.ttlSeconds) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="showMkdirDialog" title="新建 HDFS 目录" width="520px">
      <el-form :model="mkdirForm" label-width="96px">
        <el-form-item label="当前目录">
          <el-input :model-value="currentPath" disabled />
        </el-form-item>
        <el-form-item label="目录名称">
          <el-input v-model="mkdirForm.name" placeholder="例如 monthly-report" @keyup.enter="createDirectory" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showMkdirDialog = false">取消</el-button>
        <el-button type="primary" @click="createDirectory" :loading="savingDirectory">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showGisArchiveDialog" title="归档 GIS 气象展示数据" width="560px">
      <el-form :model="gisArchiveForm" label-width="108px">
        <el-form-item label="气象要素">
          <el-select v-model="gisArchiveForm.element" style="width: 100%">
            <el-option label="温度" value="temperature" />
            <el-option label="相对湿度" value="humidity" />
            <el-option label="降水量" value="precipitation" />
          </el-select>
        </el-form-item>
        <el-form-item label="观测时间">
          <el-date-picker
            v-model="gisArchiveForm.observationTime"
            type="datetime"
            clearable
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="不选则使用最新观测时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-alert
          title="归档内容包含 GIS 页面使用的全量站点列表和所选气象要素数据，写入 /weather-platform/raw-data/gis。"
          type="info"
          show-icon
          :closable="false"
        />
      </el-form>
      <template #footer>
        <el-button @click="showGisArchiveDialog = false">取消</el-button>
        <el-button type="primary" @click="archiveGisWeatherData" :loading="gisArchiveLoading">
          写入 HDFS
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showRenameDialog" title="重命名 HDFS 对象" width="520px">
      <el-form :model="renameForm" label-width="96px">
        <el-form-item label="原名称">
          <el-input :model-value="renameForm.oldName" disabled />
        </el-form-item>
        <el-form-item label="新名称">
          <el-input v-model="renameForm.newName" @keyup.enter="renameHdfsItem" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRenameDialog = false">取消</el-button>
        <el-button type="primary" @click="renameHdfsItem" :loading="renaming">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowUp,
  CircleCheckFilled,
  Coin,
  Connection,
  DataLine,
  Delete,
  Document,
  Download,
  Edit,
  FolderOpened,
  Monitor,
  PieChart,
  Plus,
  Refresh,
  Search,
  Upload,
} from '@element-plus/icons-vue'
import http from '@/api/http'

interface HdfsOverview {
  connected?: boolean
  uri?: string
  basePath?: string
  safeMode?: boolean | null
  basePathExists?: boolean
  totalStorage?: number
  usedStorage?: number
  availableStorage?: number
  totalFormatted?: string
  usedFormatted?: string
  availableFormatted?: string
  usagePercent?: number
  error?: string
}

interface DirectorySummary {
  path?: string
  length?: number
  lengthFormatted?: string
  fileCount?: number
  directoryCount?: number
  spaceConsumed?: number
  spaceConsumedFormatted?: string
}

interface StorageOverviewResponse {
  hdfs?: HdfsOverview
  redis?: RedisOverview
  basePath?: string
  platformInitialized?: boolean
  directories?: Record<string, DirectorySummary>
}

interface HdfsFileItem {
  name: string
  path: string
  isDirectory: boolean
  type: string
  size: number
  sizeFormatted: string
  replication: number
  blockSize?: number
  blockSizeFormatted?: string
  blockCount?: number
  spaceConsumed?: number
  spaceConsumedFormatted?: string
  fileCount?: number
  directoryCount?: number
  modificationTime: string
  owner: string
  group: string
  permission: string
}

interface HdfsFilesResponse {
  path: string
  files: HdfsFileItem[]
  total: number
}

interface RedisOverview {
  usedMemory?: string
  connectedClients?: string | number
  totalKeys?: string | number
  uptime?: string | number
  version?: string
  error?: string
}

interface CacheKeyInfo {
  key: string
  ttlSeconds: number | null
  type: string
}

interface CacheKeysResponse {
  pattern: string
  keys?: string[]
  items?: CacheKeyInfo[]
  count: number
}

type HdfsActionItem = Pick<HdfsFileItem, 'name' | 'path' | 'isDirectory'>

const activeTab = ref<'hdfs' | 'redis'>('hdfs')
const hdfsLoading = ref(false)
const pageLoading = ref(false)
const initLoading = ref(false)
const uploading = ref(false)
const savingDirectory = ref(false)
const gisArchiveLoading = ref(false)
const gisTableExportLoading = ref(false)
const renaming = ref(false)
const redisLoading = ref(false)
const cacheLoading = ref(false)
const clearingCache = ref(false)
const hdfsError = ref('')
const redisError = ref('')
const showMkdirDialog = ref(false)
const showGisArchiveDialog = ref(false)
const showRenameDialog = ref(false)
const currentPath = ref('/')
const hdfsOverview = ref<HdfsOverview>({})
const platformInitialized = ref<boolean | null>(null)
const hdfsFiles = ref<HdfsFileItem[]>([])
const directorySummaries = ref<Record<string, DirectorySummary>>({})
const redisOverview = ref<RedisOverview>({})
const cacheKeys = ref<CacheKeyInfo[]>([])
const cachePattern = ref('*')

const mkdirForm = reactive({ name: '' })
const gisArchiveForm = reactive({
  element: 'temperature',
  observationTime: '',
})
const renameForm = reactive({
  oldPath: '',
  oldName: '',
  newName: '',
  isDirectory: false,
})

const basePathDisplay = computed(() => {
  return hdfsOverview.value.basePath || '/weather-platform'
})

const hdfsUsagePercent = computed(() => {
  const usage = Number(hdfsOverview.value.usagePercent)
  return Number.isFinite(usage) ? Math.min(Math.max(Math.round(usage), 0), 100) : 0
})

const hdfsStatusTag = computed<{ type: 'success' | 'warning' | 'danger' | 'info'; label: string }>(() => {
  if (hdfsOverview.value.connected === false || hdfsOverview.value.error || hdfsError.value) {
    return { type: 'danger', label: 'HDFS 异常' }
  }
  if (hdfsOverview.value.safeMode === true) {
    return { type: 'warning', label: 'SafeMode' }
  }
  if (platformInitialized.value === false) {
    return { type: 'warning', label: '待初始化' }
  }
  if (hdfsOverview.value.connected === true) {
    return { type: 'success', label: 'HDFS 在线' }
  }
  return { type: 'info', label: 'HDFS 未检测' }
})

const breadcrumbItems = computed(() => {
  if (currentPath.value === '/') return []
  const parts = currentPath.value.split('/').filter(Boolean)
  return parts.map((name, index) => ({
    name,
    path: `/${parts.slice(0, index + 1).join('/')}`,
  }))
})

const currentFullPathDisplay = computed(() => {
  if (currentPath.value === '/') return basePathDisplay.value
  return `${basePathDisplay.value}${currentPath.value}`
})

const folderCount = computed(() => hdfsFiles.value.filter((item) => item.isDirectory).length)

const fileCount = computed(() => hdfsFiles.value.filter((item) => !item.isDirectory).length)

const refreshAll = async () => {
  pageLoading.value = true
  try {
    await Promise.all([refreshHdfs(false), refreshRedisData(false)])
  } finally {
    pageLoading.value = false
  }
}

const refreshHdfs = async (showMessage = true) => {
  hdfsLoading.value = true
  hdfsError.value = ''
  try {
    await loadStorageOverview()
    await loadHdfsFiles(currentPath.value)
    if (showMessage) {
      ElMessage.success('HDFS 数据已刷新')
    }
  } catch (error: any) {
    hdfsError.value = error?.response?.data?.message || error?.message || 'HDFS 数据加载失败'
    if (showMessage) {
      ElMessage.error(hdfsError.value)
    }
  } finally {
    hdfsLoading.value = false
  }
}

const loadStorageOverview = async () => {
  const { data } = await http.get<StorageOverviewResponse>('/api/storage/overview')
  hdfsOverview.value = data.hdfs || {}
  redisOverview.value = data.redis || redisOverview.value
  directorySummaries.value = data.directories || {}
  platformInitialized.value =
    typeof data.platformInitialized === 'boolean'
      ? data.platformInitialized
      : Boolean(data.hdfs?.basePathExists)
}

const loadHdfsFiles = async (path: string) => {
  const safePath = normalizeWorkspacePath(path)
  const { data } = await http.get<HdfsFilesResponse>('/api/storage/files', {
    params: { path: safePath },
  })
  currentPath.value = normalizeWorkspacePath(data.path || safePath)
  hdfsFiles.value = (data.files || []).map((item) => ({
    ...item,
    path: toWorkspacePath(item.path),
  }))
}

const openPath = async (path: string) => {
  await loadHdfsFiles(path)
}

const goParent = async () => {
  if (currentPath.value === '/') return
  const parts = currentPath.value.split('/').filter(Boolean)
  parts.pop()
  await openPath(parts.length ? `/${parts.join('/')}` : '/')
}

const openMkdirDialog = () => {
  mkdirForm.name = ''
  showMkdirDialog.value = true
}

const createDirectory = async () => {
  const name = mkdirForm.name.trim()
  if (!isValidName(name)) {
    ElMessage.warning('目录名称只能包含字母、数字、中文、下划线、短横线和点')
    return
  }
  savingDirectory.value = true
  try {
    await http.post('/api/storage/files/mkdir', null, {
      params: { path: joinPath(currentPath.value, name) },
    })
    showMkdirDialog.value = false
    ElMessage.success('目录创建成功')
    await refreshHdfs(false)
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || '目录创建失败')
  } finally {
    savingDirectory.value = false
  }
}

const openGisArchiveDialog = () => {
  showGisArchiveDialog.value = true
}

const archiveGisWeatherData = async () => {
  gisArchiveLoading.value = true
  try {
    const params: Record<string, string> = {
      element: gisArchiveForm.element,
    }
    if (gisArchiveForm.observationTime) {
      params.observationTime = gisArchiveForm.observationTime
    }
    const { data } = await http.post('/api/storage/gis/archive', null, { params })
    showGisArchiveDialog.value = false
    ElMessage.success(`GIS 数据已写入 HDFS：${data?.hdfsPath || '/raw-data/gis'}`)
    await refreshHdfs(false)
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || 'GIS 数据写入 HDFS 失败')
  } finally {
    gisArchiveLoading.value = false
  }
}

const exportGisTables = async () => {
  try {
    await ElMessageBox.confirm(
      '将全量导出 weather_stations、history_weather_data 两张 GIS 查询表到 HDFS，数据量较大时可能需要等待一段时间。是否继续？',
      '上传 GIS 表数据',
      { type: 'warning' },
    )
    gisTableExportLoading.value = true
    const { data } = await http.post('/api/storage/gis/export-tables')
    ElMessage.success(`GIS 表数据已写入 HDFS：${data?.batchPath || '/raw-data/gis/tables'}`)
    await openPath('/raw-data/gis/tables')
    await refreshHdfs(false)
  } catch (error: any) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(error?.response?.data?.message || error?.message || 'GIS 表数据上传 HDFS 失败')
  } finally {
    gisTableExportLoading.value = false
  }
}

const handleUploadChange = async (uploadFile: any) => {
  const rawFile = uploadFile?.raw
  if (!rawFile) return
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', rawFile)
    formData.append('targetDir', currentPath.value)
    await http.post('/api/storage/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    ElMessage.success('文件上传成功')
    await refreshHdfs(false)
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || '文件上传失败')
  } finally {
    uploading.value = false
  }
}

const downloadFile = async (item: HdfsActionItem) => {
  try {
    const response = await http.get<Blob>('/api/storage/files/download', {
      params: { path: toWorkspacePath(item.path) },
      responseType: 'blob',
    })
    const blob = response.data
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = item.name
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || '文件下载失败')
  }
}

const openRenameDialog = (item: HdfsActionItem) => {
  renameForm.oldPath = toWorkspacePath(item.path)
  renameForm.oldName = item.name
  renameForm.newName = item.name
  renameForm.isDirectory = item.isDirectory
  showRenameDialog.value = true
}

const renameHdfsItem = async () => {
  const newName = renameForm.newName.trim()
  if (!isValidName(newName)) {
    ElMessage.warning('新名称只能包含字母、数字、中文、下划线、短横线和点')
    return
  }
  const dstPath = joinPath(getParentPath(renameForm.oldPath), newName)
  renaming.value = true
  try {
    await http.post('/api/storage/files/rename', null, {
      params: {
        srcPath: renameForm.oldPath,
        dstPath,
      },
    })
    showRenameDialog.value = false
    ElMessage.success('重命名成功')
    await refreshHdfs(false)
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || '重命名失败')
  } finally {
    renaming.value = false
  }
}

const deleteHdfsItem = async (item: HdfsActionItem) => {
  const workspacePath = toWorkspacePath(item.path)
  try {
    await ElMessageBox.confirm(
      `确定删除 ${workspacePath} 吗？${item.isDirectory ? '目录会递归删除。' : ''}`,
      '确认删除',
      { type: 'warning' },
    )
    await http.delete('/api/storage/files', {
      params: {
        path: workspacePath,
        recursive: item.isDirectory,
      },
    })
    ElMessage.success('删除成功')
    await refreshHdfs(false)
  } catch (error: any) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(error?.response?.data?.message || error?.message || '删除失败')
  }
}

const initHdfsWorkspace = async () => {
  initLoading.value = true
  try {
    await http.post('/api/storage/init')
    ElMessage.success('HDFS 平台目录初始化完成')
    await refreshHdfs(false)
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || 'HDFS 平台目录初始化失败')
  } finally {
    initLoading.value = false
  }
}

const loadRedisOverview = async () => {
  const { data } = await http.get<RedisOverview>('/api/storage/cache/overview')
  redisOverview.value = data
}

const loadCacheKeys = async () => {
  cacheLoading.value = true
  try {
    const pattern = cachePattern.value.trim() || '*'
    const { data } = await http.get<CacheKeysResponse>('/api/storage/cache/keys', {
      params: { pattern },
    })
    cacheKeys.value = normalizeCacheKeys(data)
  } finally {
    cacheLoading.value = false
  }
}

const refreshRedisData = async (showMessage = true) => {
  redisLoading.value = true
  redisError.value = ''
  try {
    await Promise.all([loadRedisOverview(), loadCacheKeys()])
    if (showMessage) {
      ElMessage.success('Redis 数据已刷新')
    }
  } catch (error: any) {
    redisError.value = error?.response?.data?.message || error?.message || 'Redis 数据加载失败'
    if (showMessage) {
      ElMessage.error(redisError.value)
    }
  } finally {
    redisLoading.value = false
  }
}

const normalizeCacheKeys = (data: CacheKeysResponse): CacheKeyInfo[] => {
  if (Array.isArray(data.items)) {
    return data.items.map((item) => ({
      key: item.key,
      ttlSeconds: item.ttlSeconds,
      type: item.type || 'unknown',
    }))
  }
  if (Array.isArray(data.keys)) {
    return data.keys.map((key) => ({ key, ttlSeconds: null, type: 'unknown' }))
  }
  return []
}

const formatHdfsMeta = (value: unknown) => {
  if (value === null || value === undefined || value === '') return '-'
  return String(value)
}

const clearCache = async () => {
  const pattern = cachePattern.value.trim() || '*'
  try {
    await ElMessageBox.confirm(`确定清理匹配 ${pattern} 的 Redis Key 吗？`, '确认清理', {
      type: 'warning',
    })
    clearingCache.value = true
    const { data } = await http.delete('/api/storage/cache', {
      params: { pattern },
    })
    ElMessage.success(`已清理 ${data?.deletedCount ?? 0} 个 Redis Key`)
    await refreshRedisData(false)
  } catch (error: any) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(error?.response?.data?.message || error?.message || 'Redis 清理失败')
  } finally {
    clearingCache.value = false
  }
}

const formatTtl = (ttl: number | null) => {
  if (ttl === null || ttl === undefined) return '-'
  if (ttl === -2) return '已过期'
  if (ttl === -1) return '永久'
  if (ttl < 60) return `${ttl}s`
  if (ttl < 3600) return `${Math.floor(ttl / 60)}m ${ttl % 60}s`
  return `${Math.floor(ttl / 3600)}h ${Math.floor((ttl % 3600) / 60)}m`
}

const formatUptime = (value: RedisOverview['uptime']) => {
  const seconds = Number(value)
  if (!Number.isFinite(seconds) || seconds <= 0) return '-'
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  if (days > 0) return `${days}d ${hours}h`
  if (hours > 0) return `${hours}h ${minutes}m`
  return `${minutes}m`
}

const getUsageColor = (usage: number) => {
  if (usage < 60) return '#2f9e6d'
  if (usage < 82) return '#d99025'
  return '#d84d4d'
}

const normalizeWorkspacePath = (path: string) => {
  const normalized = path?.trim() || '/'
  return normalized.startsWith('/') ? normalized : `/${normalized}`
}

const toWorkspacePath = (path: string) => {
  const basePath = basePathDisplay.value
  if (!path) return '/'
  if (path === basePath) return '/'
  if (path.startsWith(`${basePath}/`)) {
    return normalizeWorkspacePath(path.slice(basePath.length))
  }
  return normalizeWorkspacePath(path)
}

const joinPath = (parent: string, child: string) => {
  const cleanParent = normalizeWorkspacePath(parent)
  const cleanChild = child.replace(/^\/+|\/+$/g, '')
  return cleanParent === '/' ? `/${cleanChild}` : `${cleanParent}/${cleanChild}`
}

const getParentPath = (path: string) => {
  const parts = normalizeWorkspacePath(path).split('/').filter(Boolean)
  parts.pop()
  return parts.length ? `/${parts.join('/')}` : '/'
}

const isValidName = (name: string) => {
  return /^[\u4e00-\u9fa5\w.-]+$/.test(name)
}

onMounted(() => {
  void refreshAll()
})
</script>

<style scoped>
.storage-management {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 20px;
  border: 1px solid var(--theme-border, var(--el-border-color-light));
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(47, 158, 109, 0.1), rgba(49, 99, 178, 0.08)),
    var(--theme-card-background, var(--el-bg-color));
}

.eyebrow {
  margin-bottom: 6px;
  color: #2f9e6d;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0;
  text-transform: uppercase;
}

.page-header h2 {
  margin: 0;
  color: var(--theme-text, var(--el-text-color-primary));
  font-size: 1.45rem;
}

.page-header p {
  max-width: 820px;
  margin: 8px 0 0;
  color: var(--theme-text-secondary, var(--el-text-color-secondary));
  line-height: 1.6;
}

.page-actions,
.header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}

.stats-overview :deep(.el-col) {
  margin-bottom: 16px;
}

.stat-card,
.panel-card {
  border: 1px solid var(--theme-border, var(--el-border-color-light));
  border-radius: 8px;
  box-shadow: var(--theme-shadow-light);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 14px;
}

.stat-icon {
  width: 52px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  border-radius: 8px;
  color: #fff;
}

.stat-icon .el-icon {
  font-size: 28px;
}

.stat-icon.total-storage {
  background: #3163b2;
}

.stat-icon.used-storage {
  background: #d84d4d;
}

.stat-icon.available-storage {
  background: #2f9e6d;
}

.stat-icon.redis-memory {
  background: #d99025;
}

.stat-number {
  color: var(--theme-text, var(--el-text-color-primary));
  font-size: 1.25rem;
  font-weight: 700;
  line-height: 1.1;
}

.stat-label {
  margin-top: 5px;
  color: var(--theme-text-secondary, var(--el-text-color-secondary));
  font-size: 0.9rem;
}

.storage-tabs {
  min-width: 0;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.panel-header.compact {
  align-items: center;
}

.panel-header h3 {
  margin: 0;
  color: var(--theme-text, var(--el-text-color-primary));
  font-size: 1.05rem;
}

.panel-header span {
  display: block;
  margin-top: 4px;
  color: var(--theme-text-secondary, var(--el-text-color-secondary));
  font-size: 0.86rem;
}

.storage-alert {
  margin-bottom: 12px;
}

.capacity-strip {
  margin-bottom: 16px;
  padding: 14px 16px;
  border: 1px solid var(--theme-border, var(--el-border-color-lighter));
  border-radius: 8px;
  background: var(--el-fill-color-extra-light);
}

.capacity-copy {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  color: var(--theme-text-secondary, var(--el-text-color-secondary));
}

.capacity-copy strong {
  color: var(--theme-text, var(--el-text-color-primary));
  font-size: 1.1rem;
}

.browser-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 14px;
}

.path-bar {
  min-width: 280px;
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 9px 12px;
  border: 1px solid var(--theme-border, var(--el-border-color-lighter));
  border-radius: 8px;
  background: color-mix(in srgb, var(--el-color-primary-light-9) 68%, var(--el-bg-color));
  overflow: hidden;
}

.path-label {
  flex: 0 0 auto;
  color: var(--theme-text-secondary, var(--el-text-color-secondary));
  font-size: 0.78rem;
}

.path-breadcrumb {
  min-width: 0;
  display: flex;
  align-items: center;
}

.breadcrumb-button,
.file-name-button {
  border: 0;
  padding: 0;
  appearance: none;
  background: transparent;
  color: var(--el-color-primary);
  cursor: pointer;
  font: inherit;
}

.breadcrumb-button {
  max-width: 220px;
  display: inline-block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 600;
}

.directory-strip {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) repeat(3, minmax(110px, 0.18fr));
  gap: 10px;
  margin-bottom: 14px;
}

.directory-strip div {
  min-width: 0;
  padding: 12px 14px;
  border: 1px solid var(--theme-border, var(--el-border-color-lighter));
  border-radius: 8px;
  background: var(--el-fill-color-extra-light);
}

.directory-strip span {
  display: block;
  color: var(--theme-text-secondary, var(--el-text-color-secondary));
  font-size: 0.78rem;
}

.directory-strip strong {
  display: block;
  margin-top: 5px;
  overflow: hidden;
  color: var(--theme-text, var(--el-text-color-primary));
  font-size: 1rem;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hdfs-file-table {
  border: 1px solid var(--theme-border, var(--el-border-color-lighter));
  border-radius: 8px;
  overflow: hidden;
}

.hdfs-file-table :deep(.el-table__header th) {
  background: color-mix(in srgb, var(--el-fill-color-light) 78%, var(--el-color-primary-light-9));
  color: var(--theme-text-secondary, var(--el-text-color-secondary));
  font-weight: 700;
}

.hdfs-file-table :deep(.el-table__row) {
  transition: background-color 0.16s ease, box-shadow 0.16s ease;
}

.hdfs-file-table :deep(.el-table__row:hover > td) {
  background: color-mix(in srgb, var(--el-color-primary-light-9) 68%, var(--el-bg-color));
}

.hdfs-meta-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(120px, 1fr));
  gap: 10px;
  padding: 12px 18px 14px 58px;
  background: color-mix(in srgb, var(--el-fill-color-extra-light) 82%, var(--el-bg-color));
}

.hdfs-meta-grid div {
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid var(--theme-border, var(--el-border-color-lighter));
  border-radius: 8px;
  background: var(--el-bg-color);
}

.hdfs-meta-grid span {
  display: block;
  color: var(--theme-text-secondary, var(--el-text-color-secondary));
  font-size: 0.76rem;
}

.hdfs-meta-grid strong {
  display: block;
  margin-top: 4px;
  overflow: hidden;
  color: var(--theme-text, var(--el-text-color-primary));
  font-size: 0.92rem;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-name-button {
  max-width: 100%;
  display: inline-flex;
  align-items: center;
  gap: 12px;
  color: var(--theme-text, var(--el-text-color-primary));
  text-align: left;
}

.file-icon {
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: var(--el-fill-color-light);
  color: var(--theme-text-secondary, var(--el-text-color-secondary));
  font-size: 1rem;
}

.file-icon.directory {
  background: color-mix(in srgb, var(--el-color-primary-light-8) 72%, var(--el-bg-color));
  color: var(--el-color-primary);
}

.file-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.file-copy strong,
.file-copy small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-copy strong {
  color: var(--theme-text, var(--el-text-color-primary));
  font-size: 0.95rem;
}

.file-copy small {
  color: var(--theme-text-secondary, var(--el-text-color-secondary));
  font-size: 0.78rem;
}

.file-type-tag {
  min-width: 46px;
  justify-content: center;
}

.row-actions {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  white-space: nowrap;
}

.cache-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.cache-summary div {
  padding: 12px;
  border: 1px solid var(--theme-border, var(--el-border-color-lighter));
  border-radius: 8px;
  background: var(--el-fill-color-extra-light);
}

.cache-summary span {
  display: block;
  color: var(--theme-text-secondary, var(--el-text-color-secondary));
  font-size: 0.82rem;
}

.cache-summary strong {
  display: block;
  margin-top: 4px;
  color: var(--theme-text, var(--el-text-color-primary));
  font-size: 1.05rem;
}

.cache-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 8px;
  margin-bottom: 12px;
}

@media (max-width: 960px) {
  .page-header,
  .panel-header {
    flex-direction: column;
  }

  .page-actions,
  .header-actions {
    justify-content: flex-start;
    width: 100%;
  }

  .cache-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .cache-toolbar {
    grid-template-columns: 1fr;
  }

  .directory-strip {
    grid-template-columns: 1fr 1fr;
  }

  .hdfs-meta-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    padding-left: 18px;
  }
}

@media (max-width: 640px) {
  .path-bar {
    min-width: 0;
    width: 100%;
  }

  .directory-strip {
    grid-template-columns: 1fr;
  }

  .hdfs-meta-grid {
    grid-template-columns: 1fr;
  }
}
</style>
