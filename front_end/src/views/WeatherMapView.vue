<template>
  <div class="weather-map-container">
    <!-- 顶部工具栏 -->
    <div class="map-toolbar">
      <div class="toolbar-left">
        <h2>气象GIS分析</h2>
      </div>
      
      <div class="toolbar-center">
        <!-- 展示模式切换 -->
        <el-radio-group v-model="displayMode" @change="handleModeChange" class="mode-switch">
          <el-radio-button label="station">站点展示</el-radio-button>
          <el-radio-button label="element">气象要素</el-radio-button>
        </el-radio-group>

        <!-- 站点展示模式的控件 -->
        <template v-if="displayMode === 'station'">
          <!-- 省份筛选 -->
          <el-select 
            v-model="selectedProvince" 
            placeholder="选择省份" 
            clearable
            @change="handleProvinceChange"
            style="width: 150px; margin-left: 15px"
          >
            <el-option label="全部省份" value="all"></el-option>
            <el-option 
              v-for="province in provinces" 
              :key="province" 
              :label="province" 
              :value="province"
            />
          </el-select>

          <!-- 站点选择 -->
          <el-select 
            v-model="selectedStationIds" 
            placeholder="选择站点（可选）" 
            clearable
            multiple
            collapse-tags
            collapse-tags-tooltip
            filterable
            @change="handleStationSelectionChange"
            style="width: 200px; margin-left: 10px"
          >
            <el-option 
              v-for="station in filteredStations" 
              :key="station.stationId" 
              :label="`${station.stationName} (${station.stationId})`" 
              :value="station.stationId"
            />
          </el-select>
        </template>

        <!-- 气象要素展示模式的控件 -->
        <template v-else-if="displayMode === 'element'">
          <!-- 气象要素选择（单选） -->
          <el-select 
            v-model="selectedElement" 
            placeholder="选择气象要素" 
            @change="handleElementChange"
            style="width: 150px; margin-left: 15px"
          >
            <el-option label="温度" value="temperature">
              <span>温度</span>
            </el-option>
            <el-option label="相对湿度" value="humidity">
              <span>相对湿度</span>
            </el-option>
            <el-option label="降水量" value="precipitation">
              <span>降水量</span>
            </el-option>
          </el-select>

          <!-- 可视化类型选择 -->
          <el-select 
            v-model="visualizationType" 
            placeholder="可视化类型"
            @change="handleVisualizationChange"
            style="width: 130px; margin-left: 10px"
          >
            <el-option label="热力图" value="heatmap"></el-option>
            <el-option label="等值线" value="contour"></el-option>
            <el-option label="网格" value="grid"></el-option>
          </el-select>

          <!-- 时间选择 -->
          <el-date-picker
            v-model="selectedDate"
            type="datetime"
            placeholder="选择观测时间"
            :disabled="false"
            value-format="YYYY-MM-DD HH:mm:ss"
            @change="handleDateChange"
            style="width: 200px; margin-left: 10px"
          />
        </template>
      </div>

      <div class="toolbar-right">
        <el-button-group>
          <el-button :icon="ZoomIn" @click="zoomIn">放大</el-button>
          <el-button :icon="ZoomOut" @click="zoomOut">缩小</el-button>
          <el-button :icon="RefreshRight" @click="resetView">复位</el-button>
        </el-button-group>
      </div>
    </div>

    <!-- 信息面板 -->
    <div class="info-panel">
      <el-card shadow="hover">
        <template #header>
          <div class="card-header">
            <span>{{ displayMode === 'station' ? '站点信息' : '要素信息' }}</span>
          </div>
        </template>
        <div class="info-content">
          <!-- 站点展示模式信息 -->
          <template v-if="displayMode === 'station'">
            <div class="info-item">
              <label>显示模式：</label>
              <span class="value">站点分布</span>
            </div>
            <div class="info-item">
              <label>站点数量：</label>
              <span class="value">
                {{ selectedStationIds.length > 0 ? selectedStationIds.length : filteredStations.length }}
              </span>
            </div>
            <div class="info-item" v-if="selectedProvince && selectedProvince !== 'all'">
              <label>筛选省份：</label>
              <span class="value">{{ selectedProvince }}</span>
            </div>
          </template>

          <!-- 气象要素展示模式信息 -->
          <template v-else-if="displayMode === 'element'">
            <div class="info-item">
              <label>显示模式：</label>
              <span class="value">要素可视化</span>
            </div>
            <div class="info-item">
              <label>当前要素：</label>
              <span class="value">{{ selectedElement ? elementLabelMap[selectedElement] : '未选择' }}</span>
            </div>
            <div class="info-item">
              <label>可视化类型：</label>
              <span class="value">{{ visualizationTypeLabel }}</span>
            </div>
            <div class="info-item" v-if="selectedDate">
              <label>观测时间：</label>
              <span class="value">{{ selectedDate }}</span>
            </div>
            <div class="info-item tip" v-if="!selectedElement">
              <el-icon><InfoFilled /></el-icon>
              <span>请选择气象要素进行可视化</span>
            </div>
          </template>
        </div>
      </el-card>
    </div>

    <!-- 地图容器 -->
    <div id="weatherMap" class="map-view"></div>

    <!-- 热力图图例 -->
    <div v-if="displayMode === 'element' && selectedElement && visualizationType === 'heatmap'" class="heatmap-legend">
      <el-card shadow="hover">
        <template #header>
          <div class="legend-header">
            <span>{{ elementLabelMap[selectedElement] }}</span>
          </div>
        </template>
        <div class="legend-content">
          <!-- 渐变条 -->
          <div class="gradient-bar" :style="{ background: getLegendGradient() }"></div>
          
          <!-- 数值标签 -->
          <div class="legend-labels">
            <span class="legend-label">{{ currentMinValue?.toFixed(1) }}{{ getElementUnit() }}</span>
            <span class="legend-label">{{ currentMaxValue?.toFixed(1) }}{{ getElementUnit() }}</span>
          </div>
          
          <!-- 颜色说明 -->
          <div class="color-description">
            <div class="color-item" v-for="item in getLegendItems()" :key="item.label">
              <div class="color-box" :style="{ backgroundColor: item.color }"></div>
              <span class="color-label">{{ item.label }}</span>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 站点信息窗口 -->
    <el-dialog
      v-model="showInfoDialog"
      title="气象站点信息"
      width="400px"
    >
      <div v-if="selectedStation" class="station-info">
        <p><strong>站点ID：</strong>{{ selectedStation.stationId }}</p>
        <p><strong>站点名称：</strong>{{ selectedStation.stationName }}</p>
        <p><strong>所属省份：</strong>{{ selectedStation.province }}</p>
        <p><strong>经度：</strong>{{ selectedStation.longitude?.toFixed(2) }}°</p>
        <p><strong>纬度：</strong>{{ selectedStation.latitude?.toFixed(2) }}°</p>
        <p v-if="selectedStation.observationAltitude">
          <strong>观测场海拔：</strong>{{ selectedStation.observationAltitude }} 米
        </p>
        <p v-if="selectedStation.pressureSensorAltitude">
          <strong>气压传感器海拔：</strong>{{ selectedStation.pressureSensorAltitude }} 米
        </p>
      </div>
    </el-dialog>

    <!-- 气象要素信息窗口 -->
    <el-dialog
      v-model="showElementInfoDialog"
      width="640px"
      :show-close="true"
      class="weather-info-dialog"
    >
      <!-- 自定义标题 -->
      <template #header>
        <div class="dialog-header">
          <div class="header-icon"><el-icon><Location /></el-icon></div>
          <div class="header-content">
            <div class="header-title">{{ clickedLocationName }}</div>
            <div class="header-subtitle">气象要素信息</div>
          </div>
        </div>
      </template>

      <!-- 加载状态 -->
      <div v-if="elementInfoLoading" class="loading-container">
        <el-icon class="is-loading" :size="40"><Loading /></el-icon>
        <span>正在查询气象数据...</span>
      </div>

      <!-- 数据展示 -->
      <div v-else-if="nearestStationData" class="element-info">
        <div class="query-summary">
          <div class="summary-main">
            <span class="summary-kicker">最近站点</span>
            <strong>{{ nearestStationData.stationName }}</strong>
            <span class="summary-meta">ID {{ nearestStationData.stationId }}</span>
          </div>
          <div class="summary-distance">
            <el-icon><Location /></el-icon>
            <span>{{ nearestStationData.distance?.toFixed(2) }}</span>
            <small>km</small>
          </div>
        </div>

        <!-- 气象数据卡片 -->
        <section class="info-card weather-card" v-if="nearestStationData.elementData">
          <div class="section-heading">
            <div>
              <span class="section-kicker">Observed values</span>
              <h3>气象数据</h3>
            </div>
            <el-icon><DataLine /></el-icon>
          </div>
          <div class="weather-data">
            <!-- 温度 -->
            <article class="weather-item temperature-metric" v-if="nearestStationData.elementData.temperature !== null">
              <div class="weather-icon temp-icon">T</div>
              <div class="weather-content">
                <div class="weather-label">温度</div>
                <div class="weather-value temp-value">
                  {{ nearestStationData.elementData.temperature?.toFixed(1) }}
                  <span class="unit">°C</span>
                </div>
              </div>
            </article>

            <!-- 湿度 -->
            <article class="weather-item humidity-metric" v-if="nearestStationData.elementData.humidity !== null">
              <div class="weather-icon humidity-icon">%</div>
              <div class="weather-content">
                <div class="weather-label">相对湿度</div>
                <div class="weather-value humidity-value">
                  {{ nearestStationData.elementData.humidity?.toFixed(1) }}
                  <span class="unit">%</span>
                </div>
              </div>
            </article>

            <!-- 降水量 -->
            <article class="weather-item rain-metric" v-if="nearestStationData.elementData.precipitation !== null">
              <div class="weather-icon rain-icon">R</div>
              <div class="weather-content">
                <div class="weather-label">3h降水量</div>
                <div class="weather-value rain-value">
                  {{ nearestStationData.elementData.precipitation?.toFixed(1) }}
                  <span class="unit">mm</span>
                </div>
              </div>
            </article>
          </div>

          <!-- 观测时间 -->
          <div class="observation-time" v-if="nearestStationData.elementData.observationTime">
            <el-icon><Clock /></el-icon>
            <span>观测时间：{{ nearestStationData.elementData.observationTime }}</span>
          </div>
        </section>

        <!-- 位置信息卡片 -->
        <section class="info-card location-card">
          <div class="section-heading">
            <div>
              <span class="section-kicker">Geographic context</span>
              <h3>位置信息</h3>
            </div>
            <el-icon><Location /></el-icon>
          </div>
          <div class="location-body">
            <div class="location-node click-node">
              <span class="node-dot"></span>
              <div class="location-copy">
                <span class="location-label">点击位置</span>
                <span class="location-value">
                  {{ clickedLng?.toFixed(4) }}°E, {{ clickedLat?.toFixed(4) }}°N
                </span>
              </div>
            </div>
            <div class="location-path"></div>
            <div class="location-node station-node">
              <span class="node-dot"></span>
              <div class="location-copy">
                <span class="location-label">站点位置</span>
                <span class="location-value">
                  {{ nearestStationData.longitude?.toFixed(4) }}°E, {{ nearestStationData.latitude?.toFixed(4) }}°N
                </span>
              </div>
            </div>
          </div>
        </section>
      </div>

      <!-- 无数据状态 -->
      <div v-else class="no-data">
        <el-icon :size="48"><WarningFilled /></el-icon>
        <p>暂无数据</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import {
  ZoomIn,
  ZoomOut,
  RefreshRight,
  InfoFilled,
  Loading,
  Location,
  Clock,
  WarningFilled,
  DataLine,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import http from '@/api/http'

// 声明BMapGL全局类型
declare const BMapGL: any
declare const mapvgl: any

// 气象站点接口定义
interface WeatherStation {
  stationId: string
  stationName: string
  latitude: number
  longitude: number
  province: string
  pressureSensorAltitude?: string
  observationAltitude?: string
}

// 地图实例
let map: any = null

// 展示模式：station(站点展示) 或 element(气象要素展示)
const displayMode = ref<'station' | 'element'>('station')

// 省份列表
const provinces = ref<string[]>([])
const selectedProvince = ref<string>('all')

// 站点数据
const allStations = ref<WeatherStation[]>([])
const markers = ref<any[]>([])

// 站点选择（站点展示模式使用）
const selectedStationIds = ref<string[]>([])

// 气象要素选择（气象要素展示模式使用）
const selectedElement = ref<string>('')
const visualizationType = ref<string>('heatmap')
const selectedDate = ref<string | null>(null)

// 图例数据
const currentMinValue = ref<number | null>(null)
const currentMaxValue = ref<number | null>(null)

// 气象要素信息窗口
const showElementInfoDialog = ref(false)
const elementInfoLoading = ref(false)
const clickedLng = ref<number | null>(null)
const clickedLat = ref<number | null>(null)
const clickedLocationName = ref<string>('选中位置')
const nearestStationData = ref<any>(null)

// 气象数据相关
const availableObservationTimes = ref<string[]>([])
const latestObservationTime = ref<string>('')
const currentHeatmapLayer = ref<any>(null)
const mapvglView = ref<any>(null)  // MapVGL视图实例

// 气象要素标签映射
const elementLabelMap: Record<string, string> = {
  temperature: '温度',
  humidity: '相对湿度',
  precipitation: '降水量'
}

// 可视化类型标签映射
const visualizationTypeMap: Record<string, string> = {
  heatmap: '热力图',
  contour: '等值线',
  grid: '网格'
}

// 计算属性：可视化类型标签
const visualizationTypeLabel = computed(() => {
  return visualizationTypeMap[visualizationType.value] || '未选择'
})

// 信息窗口
const showInfoDialog = ref(false)
const selectedStation = ref<WeatherStation | null>(null)

// 筛选后的站点列表
const filteredStations = computed(() => {
  if (selectedProvince.value === 'all' || !selectedProvince.value) {
    return allStations.value
  }
  return allStations.value.filter(s => s.province === selectedProvince.value)
})

// 初始化地图
const initMap = () => {
  try {
    // 创建地图实例
    map = new BMapGL.Map('weatherMap')
    
    // 设置中心点（中国中心位置）
    const point = new BMapGL.Point(105.0, 36.0)
    map.centerAndZoom(point, 5)
    
    // 启用滚轮缩放
    map.enableScrollWheelZoom(true)
    
    // 添加缩放控件
    const zoomCtrl = new BMapGL.ZoomControl({
      anchor: BMapGL.ANCHOR_TOP_LEFT,
      offset: new BMapGL.Size(10, 10)
    })
    map.addControl(zoomCtrl)
    
    // 添加比例尺控件
    const scaleCtrl = new BMapGL.ScaleControl({
      anchor: BMapGL.ANCHOR_BOTTOM_LEFT
    })
    map.addControl(scaleCtrl)
    
    // 等待地图完全加载后再初始化MapVGL
    map.addEventListener('tilesloaded', function onTilesLoaded() {
      console.log('地图瓦片加载完成，准备初始化MapVGL')
      // 移除监听器，只执行一次
      map.removeEventListener('tilesloaded', onTilesLoaded)
      
      // 延迟初始化MapVGL，确保WebGL上下文准备好
      setTimeout(() => {
        if (typeof mapvgl !== 'undefined' && !mapvglView.value) {
          try {
            mapvglView.value = new mapvgl.View({
              map: map
            })
            console.log('MapVGL视图预初始化成功')
          } catch (err) {
            console.warn('MapVGL视图预初始化失败:', err)
          }
        }
      }, 300)
    })
    
    // 添加地图点击事件监听 - 用于气象要素信息查询
    map.addEventListener('click', handleMapClick)
    
    console.log('地图初始化成功')
  } catch (error) {
    console.error('地图初始化失败:', error)
    ElMessage.error('地图初始化失败，请检查网络连接')
  }
}

// 加载省份列表
const loadProvinces = async () => {
  try {
    const response = await http.get('/api/weather/stations/provinces')
    provinces.value = response.data || []
    console.log('省份列表加载成功:', provinces.value.length)
  } catch (error) {
    console.error('加载省份列表失败:', error)
    ElMessage.error('加载省份列表失败')
  }
}

// 加载所有气象站点
const loadAllStations = async () => {
  try {
    const response = await http.get('/api/weather/stations/all')
    allStations.value = response.data || []
    console.log('站点数据加载成功:', allStations.value.length)
    
    // 加载完成后在地图上显示
    displayStationsOnMap()
  } catch (error) {
    console.error('加载站点数据失败:', error)
    ElMessage.error('加载站点数据失败')
  }
}

// 在地图上显示站点标记
const displayStationsOnMap = () => {
  if (!map) {
    console.error('地图未初始化')
    return
  }
  
  // 清除现有标记
  clearMarkers()
  
  // 确定要显示的站点列表
  let stationsToDisplay = filteredStations.value
  
  // 如果有选中特定站点，只显示选中的站点
  if (selectedStationIds.value.length > 0) {
    stationsToDisplay = filteredStations.value.filter(station => 
      selectedStationIds.value.includes(station.stationId)
    )
  }
  
  // 为每个站点添加标记
  stationsToDisplay.forEach((station) => {
    if (!station.latitude || !station.longitude) {
      console.warn('站点坐标无效:', station)
      return
    }
    
    const point = new BMapGL.Point(station.longitude, station.latitude)
    
    // 判断是否为选中的站点，使用不同颜色的标记
    const isSelected = selectedStationIds.value.includes(station.stationId)
    const marker = new BMapGL.Marker(point, {
      icon: isSelected ? createCustomIcon('#409EFF') : undefined // 选中的站点使用蓝色图标
    })
    
    // 添加点击事件
    marker.addEventListener('click', () => {
      showStationInfo(station)
    })
    
    // 添加悬停提示
    const label = new BMapGL.Label(station.stationName, {
      offset: new BMapGL.Size(20, -10)
    })
    label.setStyle({
      color: '#333',
      fontSize: '12px',
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      border: '1px solid #ccc',
      padding: '2px 5px',
      borderRadius: '3px',
      display: 'none'
    })
    marker.setLabel(label)
    
    // 鼠标悬停显示标签
    marker.addEventListener('mouseover', () => {
      label.setStyle({ display: 'block' })
    })
    marker.addEventListener('mouseout', () => {
      label.setStyle({ display: 'none' })
    })
    
    map.addOverlay(marker)
    markers.value.push(marker)
  })
  
  console.log('已添加', markers.value.length, '个站点标记')
}

// 清除所有标记
const clearMarkers = () => {
  markers.value.forEach(marker => {
    map.removeOverlay(marker)
  })
  markers.value = []
}

// 创建自定义颜色图标（可选功能）
const createCustomIcon = (color: string) => {
  // 这里可以创建自定义颜色的图标，暂时返回null使用默认图标
  // 百度地图GL版本的自定义图标需要更复杂的实现
  return null
}

// 显示站点信息
const showStationInfo = (station: WeatherStation) => {
  selectedStation.value = station
  showInfoDialog.value = true
}

// 地图缩放
const zoomIn = () => {
  map?.zoomIn()
}

const zoomOut = () => {
  map?.zoomOut()
}

const resetView = () => {
  const point = new BMapGL.Point(105.0, 36.0)
  map?.centerAndZoom(point, 5)
}

// 省份筛选变化处理
const handleProvinceChange = () => {
  console.log('省份筛选变化:', selectedProvince.value)
  // 清空站点选择
  selectedStationIds.value = []
  displayStationsOnMap()
}

// 站点选择变化处理
const handleStationSelectionChange = () => {
  console.log('选中的站点:', selectedStationIds.value)
  displayStationsOnMap()
  
  // 如果选中了站点，自动调整地图视野以显示所有选中的站点
  if (selectedStationIds.value.length > 0) {
    const selectedStations = allStations.value.filter(s => 
      selectedStationIds.value.includes(s.stationId)
    )
    if (selectedStations.length > 0) {
      fitMapToStations(selectedStations)
    }
  }
}

// 展示模式切换处理
const handleModeChange = () => {
  console.log('展示模式切换:', displayMode.value)
  
  if (displayMode.value === 'station') {
    // 切换到站点展示模式
    clearElementVisualization()
    displayStationsOnMap()
  } else if (displayMode.value === 'element') {
    // 切换到气象要素展示模式
    clearStationMarkers()
    displayElementVisualization()
  }
}

// 气象要素选择变化处理
const handleElementChange = () => {
  console.log('选中的气象要素:', selectedElement.value)
  displayElementVisualization()
}

// 可视化类型变化处理
const handleVisualizationChange = () => {
  console.log('可视化类型变化:', visualizationType.value)
  displayElementVisualization()
}

// 时间选择变化处理
const handleDateChange = () => {
  console.log('观测时间变化:', selectedDate.value)
  displayElementVisualization()
}

// 调整地图视野以显示所有指定的站点
const fitMapToStations = (stations: WeatherStation[]) => {
  if (!map || stations.length === 0) return
  
  // 如果只有一个站点，直接定位到该站点
  if (stations.length === 1) {
    const station = stations[0]
    if (station && station.longitude && station.latitude) {
      const point = new BMapGL.Point(station.longitude, station.latitude)
      map.centerAndZoom(point, 12)
    }
    return
  }
  
  // 多个站点时，计算边界并自动调整视野
  const points = stations
    .filter(s => s && s.longitude && s.latitude)
    .map(s => new BMapGL.Point(s.longitude, s.latitude))
  
  if (points.length > 0) {
    const view = map.getViewport(points)
    map.centerAndZoom(view.center, view.zoom)
  }
}

// 清除站点标记
const clearStationMarkers = () => {
  markers.value.forEach(marker => {
    map.removeOverlay(marker)
  })
  markers.value = []
}

// 清除气象要素可视化图层
const clearElementVisualization = () => {
  // 清除MapVGL热力图图层
  if (currentHeatmapLayer.value && mapvglView.value) {
    try {
      mapvglView.value.removeLayer(currentHeatmapLayer.value)
      currentHeatmapLayer.value = null
      console.log('清除MapVGL热力图图层')
    } catch (error) {
      console.error('清除图层失败:', error)
    }
  }
}

// 处理地图点击事件
const handleMapClick = async (e: any) => {
  // 仅在气象要素模式下处理点击事件
  if (displayMode.value !== 'element') {
    return
  }
  
  const point = e.latlng
  clickedLng.value = point.lng
  clickedLat.value = point.lat
  
  console.log(`地图点击位置: ${point.lng.toFixed(4)}, ${point.lat.toFixed(4)}`)
  
  // 获取点击位置的地理信息（使用逆地理编码）
  const geoCoder = new BMapGL.Geocoder()
  geoCoder.getLocation(point, (result: any) => {
    if (result) {
      clickedLocationName.value = result.address || '未知位置'
    }
  })
  
  // 查询最近的气象站点数据
  await queryNearestStationData(point.lng, point.lat)
}

// 查询最近气象站点的数据
const queryNearestStationData = async (lng: number, lat: number) => {
  elementInfoLoading.value = true
  showElementInfoDialog.value = true
  nearestStationData.value = null
  
  try {
    // 1. 找到最近的气象站点
    let nearestStation: any = null
    let minDistance = Infinity
    
    for (const station of allStations.value) {
      const distance = calculateDistance(
        lat, lng,
        station.latitude, station.longitude
      )
      
      if (distance < minDistance) {
        minDistance = distance
        nearestStation = station
      }
    }
    
    if (!nearestStation) {
      ElMessage.warning('未找到附近的气象站点')
      elementInfoLoading.value = false
      return
    }
    
    console.log(`找到最近站点: ${nearestStation.stationName}, 距离: ${minDistance.toFixed(2)}km`)
    
    // 2. 查询该站点的气象数据
    const params: any = {}
    
    // 如果选择了时间，使用选择的时间
    if (selectedDate.value) {
      const isoTime = selectedDate.value.replace(' ', 'T')
      params.observationTime = isoTime
    }
    
    // 查询该站点的所有气象要素数据
    const response = await http.get(`/api/weather/station/${nearestStation.stationId}/data`, {
      params: params
    })
    
    // 3. 组合数据
    nearestStationData.value = {
      ...nearestStation,
      distance: minDistance,
      elementData: response.data || {}
    }
    
    console.log('气象要素数据:', nearestStationData.value)
    
  } catch (error) {
    console.error('查询气象数据失败:', error)
    ElMessage.error('查询气象数据失败')
  } finally {
    elementInfoLoading.value = false
  }
}

// 计算两点之间的距离（单位：公里）
const calculateDistance = (lat1: number, lon1: number, lat2: number, lon2: number): number => {
  const R = 6371 // 地球半径（公里）
  const dLat = (lat2 - lat1) * Math.PI / 180
  const dLon = (lon2 - lon1) * Math.PI / 180
  const a = 
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
    Math.sin(dLon / 2) * Math.sin(dLon / 2)
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return R * c
}

// 加载气象要素数据
const loadElementData = async () => {
  if (!selectedElement.value) {
    ElMessage.warning('请先选择气象要素')
    return null
  }
  
  try {
    const params: any = {
      element: selectedElement.value
    }
    
    // 如果选择了时间，添加到参数中（转换为ISO 8601格式）
    if (selectedDate.value) {
      // 将 "YYYY-MM-DD HH:mm:ss" 转换为 "YYYY-MM-DDTHH:mm:ss"
      const isoTime = selectedDate.value.replace(' ', 'T')
      params.observationTime = isoTime
    }
    
    const response = await http.get('/api/weather/element-data', { params })
    console.log('气象要素数据加载成功:', response.data)
    return response.data
  } catch (error) {
    console.error('加载气象要素数据失败:', error)
    ElMessage.error('加载气象要素数据失败')
    return null
  }
}

// 绘制热力图 (使用MapVGL)
const drawHeatmap = async () => {
  if (!map) {
    console.error('地图未初始化')
    return
  }
  
  // 检查MapVGL库是否加载
  if (typeof mapvgl === 'undefined') {
    console.error('MapVGL库未加载')
    ElMessage.error('可视化库未加载，请刷新页面重试')
    return
  }
  
  // 加载数据
  const elementData = await loadElementData()
  if (!elementData || !elementData.dataPoints || elementData.dataPoints.length === 0) {
    ElMessage.warning('暂无数据')
    return
  }
  
  // 准备MapVGL热力图数据格式
  const heatmapData = elementData.dataPoints
    .filter((point: any) => point.latitude && point.longitude && point.value !== null)
    .map((point: any) => ({
      geometry: {
        type: 'Point',
        coordinates: [parseFloat(point.longitude), parseFloat(point.latitude)]
      },
      properties: {
        count: parseFloat(point.value),
        // 添加强度字段，用于热力图渲染
        intensity: Math.abs(parseFloat(point.value))
      }
    }))
  
  if (heatmapData.length === 0) {
    ElMessage.warning('没有有效的数据点')
    return
  }
  
  const minValue = parseFloat(elementData.minValue)
  const maxValue = parseFloat(elementData.maxValue)
  
  // 更新图例数据
  currentMinValue.value = minValue
  currentMaxValue.value = maxValue
  
  console.log(`绘制热力图: ${heatmapData.length}个数据点, 范围: ${minValue} ~ ${maxValue}`)
  console.log('热力图数据示例（前3条）:', JSON.stringify(heatmapData.slice(0, 3), null, 2))
  
  try {
    // 检查mapvgl库是否可用
    if (typeof mapvgl === 'undefined') {
      console.error('MapVGL库未加载')
      ElMessage.error('MapVGL库未加载，请刷新页面')
      return
    }
    
    // 创建或获取MapVGL视图
    if (!mapvglView.value) {
      console.log('MapVGL视图未预初始化，立即创建...')
      
      mapvglView.value = new mapvgl.View({
        map: map
      })
      console.log('MapVGL视图创建成功')
      
      // 等待视图就绪后再绘制
      await new Promise(resolve => setTimeout(resolve, 300))
    } else {
      console.log('使用已预初始化的MapVGL视图')
    }
    
    // 获取渐变色配置
    const gradient = getGradientConfig(selectedElement.value)
    console.log('渐变色配置:', gradient)
    
    // 创建热力图图层
    console.log('创建HeatmapLayer，参数:', {
      size: 100000,  // 使用较大的半径（单位：米）
      max: maxValue,
      min: minValue,
      unit: 'm',
      height: 0,
      gradient: gradient
    })
    const heatmapLayer = new mapvgl.HeatmapLayer({
      size: 100000,       // 热力点半径（米）- 使用较大值以便看到效果
      max: maxValue,      // 最大值
      min: minValue,      // 最小值
      unit: 'm',          // 单位（米）
      height: 0,          // 高度（2D热力图）
      gradient: gradient, // 渐变色配置
      opacity: 0.9        // 透明度
    })
    console.log('HeatmapLayer创建成功')
    
    // 设置数据
    console.log('设置热力图数据，数据点数量:', heatmapData.length)
    heatmapLayer.setData(heatmapData)
    console.log('数据设置成功')
    
    // 添加到视图
    console.log('添加图层到视图...')
    mapvglView.value.addLayer(heatmapLayer)
    console.log('图层添加成功')
    console.log('当前MapVGL视图的图层数量:', mapvglView.value.getAllLayers?.()?.length || '未知')
    
    // 保存图层引用
    currentHeatmapLayer.value = heatmapLayer
    
    ElMessage.success(`${elementLabelMap[selectedElement.value]}热力图加载成功，共 ${heatmapData.length} 个数据点`)
    
  } catch (error) {
    console.error('热力图绘制失败:', error)
    console.error('错误堆栈:', error instanceof Error ? error.stack : '无堆栈信息')
    ElMessage.error('热力图绘制失败: ' + error)
  }
}

// 获取MapVGL渐变色配置
const getGradientConfig = (element: string): Record<number, string> => {
  const defaultGradient: Record<number, string> = {
    0.0: 'blue',
    0.25: 'cyan',
    0.5: 'lime',
    0.75: 'yellow',
    1.0: 'red'
  }
  
  const gradients: Record<string, Record<number, string>> = {
    temperature: {
      0.0: 'blue',
      0.25: 'cyan',
      0.5: 'lime',
      0.75: 'yellow',
      1.0: 'red'
    },
    humidity: {
      0.0: 'rgba(255, 255, 255, 0)',
      0.25: 'lightblue',
      0.5: 'blue',
      0.75: 'darkblue',
      1.0: 'navy'
    },
    precipitation: {
      0.0: 'rgba(255, 255, 255, 0)',
      0.25: 'lightgreen',
      0.5: 'green',
      0.75: 'blue',
      1.0: 'darkblue'
    }
  }
  
  return gradients[element] || defaultGradient
}

// 获取图例渐变CSS
const getLegendGradient = (): string => {
  const gradient = getGradientConfig(selectedElement.value)
  const stops = Object.entries(gradient)
    .sort(([a], [b]) => parseFloat(a) - parseFloat(b))
    .map(([position, color]) => `${color} ${parseFloat(position) * 100}%`)
    .join(', ')
  
  return `linear-gradient(to right, ${stops})`
}

// 获取要素单位
const getElementUnit = (): string => {
  const units: Record<string, string> = {
    temperature: '°C',
    humidity: '%',
    precipitation: 'mm'
  }
  return units[selectedElement.value] || ''
}

// 获取图例项目
const getLegendItems = () => {
  const items: Record<string, Array<{color: string, label: string}>> = {
    temperature: [
      { color: 'blue', label: '极低温' },
      { color: 'cyan', label: '低温' },
      { color: 'lime', label: '适中' },
      { color: 'yellow', label: '温暖' },
      { color: 'red', label: '高温' }
    ],
    humidity: [
      { color: 'rgba(255, 255, 255, 0.3)', label: '干燥' },
      { color: 'lightblue', label: '偏干' },
      { color: 'blue', label: '适中' },
      { color: 'darkblue', label: '潮湿' },
      { color: 'navy', label: '高湿' }
    ],
    precipitation: [
      { color: 'rgba(255, 255, 255, 0.3)', label: '无降水' },
      { color: 'lightgreen', label: '小雨' },
      { color: 'green', label: '中雨' },
      { color: 'blue', label: '大雨' },
      { color: 'darkblue', label: '暴雨' }
    ]
  }
  return items[selectedElement.value] || []
}

// 显示气象要素可视化
const displayElementVisualization = async () => {
  if (!selectedElement.value) {
    ElMessage.warning('请先选择气象要素')
    return
  }
  
  // 清除之前的可视化
  clearElementVisualization()
  
  console.log('显示气象要素可视化:', {
    element: selectedElement.value,
    type: visualizationType.value,
    date: selectedDate.value
  })
  
  // 根据可视化类型绘制
  if (visualizationType.value === 'heatmap') {
    await drawHeatmap()
  } else if (visualizationType.value === 'contour') {
    ElMessage.info('等值线功能开发中')
    // TODO: 实现等值线绘制
  } else if (visualizationType.value === 'grid') {
    ElMessage.info('网格功能开发中')
    // TODO: 实现网格绘制
  }
}

// 组件挂载
onMounted(() => {
  // 延迟初始化地图，确保DOM已渲染
  setTimeout(() => {
    initMap()
    // 加载数据
    loadProvinces()
    loadAllStations()
  }, 100)
})

// 组件卸载
onBeforeUnmount(() => {
  clearMarkers()
  if (map) {
    map.destroy()
    map = null
  }
})
</script>

<style scoped>
.weather-map-container {
  width: 100%;
  height: 100%;
  position: relative;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.map-toolbar {
  height: 60px;
  background: #fff;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  z-index: 100;
  flex-shrink: 0;
}

.toolbar-left h2 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.toolbar-center {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
}

.toolbar-right {
  display: flex;
  gap: 10px;
}

.map-view {
  flex: 1;
  width: 100%;
  background: #f5f5f5;
  overflow: hidden;
  min-height: 0;
}

#weatherMap {
  width: 100%;
  height: 100%;
}

.station-info p {
  margin: 10px 0;
  font-size: 14px;
  color: #333;
}

.station-info strong {
  color: #666;
  min-width: 120px;
  display: inline-block;
}

/* 信息面板样式 */
.info-panel {
  position: absolute;
  top: 80px;
  left: 20px;
  z-index: 1000;
  max-width: 320px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  font-size: 16px;
}

.info-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-item {
  display: flex;
  align-items: center;
  font-size: 14px;
}

.info-item label {
  color: #666;
  font-weight: 500;
  min-width: 100px;
}

.info-item .value {
  color: #333;
  font-weight: 600;
}

.info-item.tip {
  color: #909399;
  font-size: 13px;
  margin-top: 8px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
  gap: 6px;
}

.info-item.tip .el-icon {
  font-size: 16px;
}

/* 模式切换样式 */
.mode-switch {
  border-radius: 4px;
}

.mode-switch :deep(.el-radio-button__inner) {
  padding: 8px 20px;
  font-weight: 500;
}

/* 热力图图例样式 */
.heatmap-legend {
  position: absolute;
  bottom: 40px;
  right: 20px;
  z-index: 1000;
  min-width: 280px;
}

.legend-header {
  font-weight: 600;
  font-size: 15px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.legend-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.gradient-bar {
  width: 100%;
  height: 20px;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.legend-labels {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #666;
  font-weight: 600;
  margin-top: -8px;
}

.legend-label {
  padding: 0 4px;
}

.color-description {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid #ebeef5;
}

.color-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.color-box {
  width: 24px;
  height: 16px;
  border-radius: 3px;
  border: 1px solid #ddd;
  flex-shrink: 0;
}

.color-label {
  color: #666;
  flex: 1;
}

/* 气象要素信息窗口样式 */
.weather-info-dialog :deep(.el-dialog) {
  border-radius: 14px;
  overflow: hidden;
  background: #f6f8fb;
  box-shadow: 0 22px 60px rgba(28, 47, 73, 0.28);
}

.weather-info-dialog :deep(.el-dialog__header) {
  padding: 0;
  margin: 0;
}

.weather-info-dialog :deep(.el-dialog__body) {
  padding: 18px 20px 20px;
  background:
    radial-gradient(circle at 12% 0%, rgba(41, 123, 202, 0.08), transparent 28%),
    linear-gradient(180deg, #f8fbff 0%, #f3f6fa 100%);
}

.dialog-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 22px 26px;
  background:
    linear-gradient(135deg, rgba(14, 83, 140, 0.96), rgba(35, 111, 176, 0.94)),
    #1f5f99;
  color: #f7fbff;
  position: relative;
}

.dialog-header::after {
  content: '';
  position: absolute;
  left: 26px;
  right: 26px;
  bottom: 0;
  height: 1px;
  background: rgba(255, 255, 255, 0.28);
}

.header-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.22);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.18);
  font-size: 24px;
}

.header-content {
  flex: 1;
}

.header-title {
  font-size: 19px;
  font-weight: 700;
  line-height: 1.25;
  margin-bottom: 5px;
  letter-spacing: 0;
}

.header-subtitle {
  font-size: 13px;
  opacity: 0.88;
  letter-spacing: 0.04em;
}

/* 加载状态 */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 60px 40px;
  color: #666;
}

.loading-container .el-icon {
  color: #1d4f91;
}

.loading-container span {
  font-size: 14px;
}

/* 信息卡片容器 */
.element-info {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.query-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 16px 18px;
  background: #f9fbfd;
  border: 1px solid #dfe8f2;
  border-radius: 10px;
  box-shadow: 0 8px 22px rgba(37, 75, 111, 0.08);
}

.summary-main {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.summary-kicker,
.section-kicker {
  color: #6f7f8f;
  font-size: 11px;
  font-weight: 700;
  line-height: 1;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.summary-main strong {
  color: #1d2938;
  font-size: 20px;
  line-height: 1.25;
  overflow-wrap: anywhere;
}

.summary-meta {
  color: #6b7787;
  font-size: 13px;
  font-weight: 600;
}

.summary-distance {
  min-width: 116px;
  display: inline-flex;
  align-items: baseline;
  justify-content: center;
  gap: 5px;
  padding: 12px 14px;
  color: #c56a00;
  background: #fff7ea;
  border: 1px solid #f2d6ad;
  border-radius: 10px;
  white-space: nowrap;
}

.summary-distance .el-icon {
  align-self: center;
  color: #f08a00;
  font-size: 16px;
}

.summary-distance span {
  font-size: 20px;
  font-weight: 800;
}

.summary-distance small {
  font-size: 12px;
  font-weight: 700;
}

/* 信息卡片基础样式 */
.info-card {
  background: #fbfcfe;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #dde7f2;
  box-shadow: 0 8px 22px rgba(37, 75, 111, 0.07);
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease;
}

.info-card:hover {
  border-color: #c8d7e8;
  box-shadow: 0 12px 28px rgba(37, 75, 111, 0.1);
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 16px 18px 12px;
}

.section-heading h3 {
  margin: 5px 0 0;
  color: #1f2d3d;
  font-size: 16px;
  font-weight: 750;
  line-height: 1.2;
}

.section-heading > .el-icon {
  width: 34px;
  height: 34px;
  color: #4f6478;
  background: #eef3f8;
  border: 1px solid #dfe7ef;
  border-radius: 9px;
  padding: 7px;
}

.weather-data {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  padding: 0 18px 16px;
}

.weather-item {
  min-height: 94px;
  display: grid;
  grid-template-columns: auto 1fr;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background: #f7fafc;
  border-radius: 10px;
  border: 1px solid #dce5ed;
  transition:
    border-color 0.18s ease,
    transform 0.18s ease,
    background-color 0.18s ease;
}

.weather-item:hover {
  border-color: #b8c8d8;
  background: #ffffff;
  transform: translateY(-1px);
}

.weather-icon {
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 9px;
  color: #f8fbff;
  font-size: 22px;
  font-weight: 750;
  flex-shrink: 0;
}

.temperature-metric .weather-icon {
  background: #d85f69;
}

.humidity-metric .weather-icon {
  background: #2f7ed8;
}

.rain-metric .weather-icon {
  background: #3d9147;
}

.weather-content {
  flex: 1;
  min-width: 0;
}

.weather-label {
  font-size: 12px;
  color: #758493;
  margin-bottom: 7px;
  white-space: nowrap;
}

.weather-value {
  display: flex;
  align-items: baseline;
  gap: 4px;
  font-size: 25px;
  font-weight: 800;
  line-height: 1;
}

.weather-value .unit {
  font-size: 13px;
  font-weight: 700;
  color: #8a96a3;
}

.temp-value {
  color: #d85f69;
}

.humidity-value {
  color: #2f7ed8;
}

.rain-value {
  color: #3d9147;
}

/* 观测时间 */
.observation-time {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 11px 18px;
  background: #f2f5f8;
  border-top: 1px solid #dfe6ee;
  font-size: 13px;
  color: #66717e;
}

.observation-time .el-icon {
  color: #8593a0;
}

/* 位置信息卡片 */
.location-body {
  display: flex;
  align-items: center;
  gap: 0;
  padding: 0 18px 18px;
}

.location-node {
  flex: 1;
  min-width: 0;
  display: grid;
  grid-template-columns: auto 1fr;
  align-items: center;
  gap: 10px;
}

.node-dot {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  background: #7a8794;
  box-shadow: 0 0 0 5px rgba(122, 135, 148, 0.12);
}

.click-node .node-dot {
  background: #2f7ed8;
  box-shadow: 0 0 0 5px rgba(47, 126, 216, 0.13);
}

.station-node .node-dot {
  background: #f08a00;
  box-shadow: 0 0 0 5px rgba(240, 138, 0, 0.13);
}

.location-copy {
  min-width: 0;
  display: grid;
  gap: 6px;
}

.location-label {
  font-size: 12px;
  color: #6d7a87;
  font-weight: 700;
}

.location-value {
  width: 100%;
  display: block;
  color: #202b37;
  font-family: 'Cascadia Mono', 'Consolas', 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.2;
  background: #f1f4f7;
  border: 1px solid #e1e7ee;
  padding: 8px 10px;
  border-radius: 7px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.location-path {
  width: 44px;
  height: 1px;
  margin: 0 12px;
  background: repeating-linear-gradient(
    to right,
    #b7c3cf 0,
    #b7c3cf 5px,
    transparent 5px,
    transparent 9px
  );
}

/* 无数据状态 */
.no-data {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 60px 40px;
  color: #909399;
}

.no-data .el-icon {
  color: #e6a23c;
}

.no-data p {
  margin: 0;
  font-size: 15px;
  font-weight: 500;
}

@media (max-width: 720px) {
  .weather-info-dialog :deep(.el-dialog) {
    width: calc(100vw - 24px) !important;
  }

  .dialog-header,
  .query-summary {
    align-items: flex-start;
  }

  .query-summary,
  .location-body {
    flex-direction: column;
  }

  .summary-distance {
    width: 100%;
    justify-content: flex-start;
  }

  .weather-data {
    grid-template-columns: 1fr;
  }

  .location-path {
    width: 1px;
    height: 24px;
    margin: 8px 0 8px 5px;
    background: repeating-linear-gradient(
      to bottom,
      #b7c3cf 0,
      #b7c3cf 5px,
      transparent 5px,
      transparent 9px
    );
    align-self: flex-start;
  }
}

/* 旧样式（保留兼容） */
.info-section {
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
  border-left: 3px solid #409eff;
}

.info-section h4 {
  margin: 0 0 12px 0;
  font-size: 15px;
  color: #333;
  font-weight: 600;
}

.info-section p {
  margin: 8px 0;
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}

.info-section strong {
  color: #333;
  min-width: 100px;
  display: inline-block;
}

.value-highlight {
  color: #409eff;
  font-weight: 700;
  font-size: 16px;
}
</style>
