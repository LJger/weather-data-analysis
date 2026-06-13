<template>
  <div class="user-management">
    <!-- 用户统计概览 -->
    <div class="stats-overview">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon total-users">
                <el-icon><UserFilled /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ userStats.totalUsers }}</div>
                <div class="stat-label">总用户数</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon active-users">
                <el-icon><CircleCheckFilled /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ userStats.activeUsers }}</div>
                <div class="stat-label">活跃用户</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon new-users">
                <el-icon><TrendCharts /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ userStats.newUsers }}</div>
                <div class="stat-label">本月新增</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon banned-users">
                <el-icon><CircleCloseFilled /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ userStats.bannedUsers }}</div>
                <div class="stat-label">禁用用户</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 用户画像可视化 -->
    <el-row :gutter="20" class="charts-section">
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <h3>用户性别分布</h3>
          </template>
          <div ref="genderChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <h3>用户地区分布</h3>
          </template>
          <div ref="regionChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="charts-section">
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <h3>用户年龄分布</h3>
          </template>
          <div ref="ageChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <h3>用户注册趋势</h3>
          </template>
          <div ref="trendChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 用户管理表格 -->
    <el-card class="table-card">
      <template #header>
        <div class="table-header">
          <h3>用户列表</h3>
          <div class="header-actions">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索用户名、邮箱或手机号"
              style="width: 300px; margin-right: 10px"
              clearable
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button @click="handleSearch">搜索</el-button>
            <el-button @click="refreshUsers">刷新</el-button>
            <el-button :disabled="selectedUsers.length === 0" @click="showBatchDialog = true">
              批量操作
            </el-button>
            <el-button type="primary" @click="showAddUserDialog = true">
              <el-icon><Plus /></el-icon>
              添加用户
            </el-button>
          </div>
        </div>
      </template>

      <el-table
        :data="filteredUsers"
        style="width: 100%"
        v-loading="loading"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="userId" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="200">
          <template #default="scope">{{ scope.row.email || '-' }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="130">
          <template #default="scope">{{ scope.row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column label="性别" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.gender === 1 ? 'primary' : scope.row.gender === 2 ? 'success' : 'info'" size="small">
              {{ getGenderLabel(scope.row.gender) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" width="80">
          <template #default="scope">{{ scope.row.age || '-' }}</template>
        </el-table-column>
        <el-table-column prop="region" label="地区" min-width="160">
          <template #default="scope">{{ scope.row.region || '-' }}</template>
        </el-table-column>
        <el-table-column prop="organization" label="机构" min-width="180">
          <template #default="scope">{{ scope.row.organization || '-' }}</template>
        </el-table-column>
        <el-table-column label="角色" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.role === 1 ? 'danger' : 'info'" size="small">
              {{ getRoleLabel(scope.row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="160">
          <template #default="scope">{{ formatTime(scope.row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="editUser(scope.row)">编辑</el-button>
            <el-button
              size="small"
              :type="scope.row.role === 1 ? 'warning' : 'success'"
              @click="toggleUserRole(scope.row)"
            >
              {{ scope.row.role === 1 ? '取消管理' : '设为管理' }}
            </el-button>
            <el-button size="small" type="danger" @click="deleteUser(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="totalUsers"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 添加/编辑用户对话框 -->
    <el-dialog
      v-model="showAddUserDialog"
      :title="editingUser ? '编辑用户' : '添加用户'"
      width="600px"
    >
      <el-form :model="userForm" :rules="userRules" ref="userFormRef" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="userForm.username" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="userForm.email" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="userForm.phone" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="密码" prop="password">
              <el-input v-model="userForm.password" type="password" show-password />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-select v-model="userForm.gender" style="width: 100%">
                <el-option label="未知" value="未知" />
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年龄" prop="age">
              <el-input-number v-model="userForm.age" :min="1" :max="120" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="地区" prop="region">
          <el-input v-model="userForm.region" />
        </el-form-item>

        <el-form-item label="机构" prop="organization">
          <el-input v-model="userForm.organization" />
        </el-form-item>

        <el-form-item label="用户类型" prop="userType">
          <el-radio-group v-model="userForm.userType">
            <el-radio label="user">普通用户</el-radio>
            <el-radio label="admin">管理员</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showAddUserDialog = false">取消</el-button>
        <el-button type="primary" @click="saveUser" :loading="saving">
          {{ editingUser ? '更新' : '添加' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 批量操作 -->
    <el-dialog v-model="showBatchDialog" title="批量操作" width="400px">
      <p>已选择 {{ selectedUsers.length }} 个用户</p>
      <el-form label-width="100px">
        <el-form-item label="操作类型">
          <el-select v-model="batchAction" style="width: 100%">
            <el-option label="批量设为普通用户" value="set-user" />
            <el-option label="批量设为管理员" value="set-admin" />
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
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { UserFilled, CircleCheckFilled, TrendCharts, CircleCloseFilled } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import http from '@/api/http'

// 定义接口类型
interface User {
  userId: number
  username: string
  email: string
  phone: string
  gender: number | null
  age: number | null
  region: string
  organization: string
  role: number
  bio: string
  avatarUrl: string
  createdAt: string
  updatedAt: string
}

// 响应式数据
const loading = ref(false)
const saving = ref(false)
const batchProcessing = ref(false)
const showAddUserDialog = ref(false)
const showBatchDialog = ref(false)
const editingUser = ref<User | null>(null)
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const totalUsers = ref(0)
const selectedUsers = ref<User[]>([])
const batchAction = ref('')

const userFormRef = ref<FormInstance>()

// 图表引用
const genderChartRef = ref()
const regionChartRef = ref()
const ageChartRef = ref()
const trendChartRef = ref()

// 图表实例（用于清理）
let genderChart: echarts.ECharts | null = null
let regionChart: echarts.ECharts | null = null
let ageChart: echarts.ECharts | null = null
let trendChart: echarts.ECharts | null = null

const geoJsonCache = new Map<string, any>()
const CHINA_MAP_KEY = 'china'
const CHINA_MAP_URL = 'https://geo.datav.aliyun.com/areas_v3/bound/100000_full.json'

const ensureChinaMap = async () => {
  if (echarts.getMap(CHINA_MAP_KEY)) return
  const cached = geoJsonCache.get(CHINA_MAP_KEY)
  if (cached) {
    echarts.registerMap(CHINA_MAP_KEY, cached)
    return
  }
  const response = await fetch(CHINA_MAP_URL)
  if (!response.ok) {
    throw new Error(`中国地图数据加载失败（${response.status}）`)
  }
  const geoJson = await response.json()
  geoJsonCache.set(CHINA_MAP_KEY, geoJson)
  echarts.registerMap(CHINA_MAP_KEY, geoJson)
}

const getOrCreateChart = (chartRef: any): echarts.ECharts | null => {
  if (!chartRef?.value) return null
  return echarts.getInstanceByDom(chartRef.value) || echarts.init(chartRef.value)
}

const normalizeProvinceName = (region?: string): string | null => {
  if (!region) return null
  const raw = region.split('/')[0]?.trim().replace(/\s+/g, '')
  if (!raw) return null

  // 已经是标准行政区名称，直接返回
  if (/(省|市|自治区|特别行政区)$/.test(raw)) {
    return raw
  }

  // 简称映射为地图标准名称
  const provinceAliasMap: Record<string, string> = {
    北京: '北京市',
    天津: '天津市',
    上海: '上海市',
    重庆: '重庆市',
    内蒙古: '内蒙古自治区',
    广西: '广西壮族自治区',
    西藏: '西藏自治区',
    宁夏: '宁夏回族自治区',
    新疆: '新疆维吾尔自治区',
    香港: '香港特别行政区',
    澳门: '澳门特别行政区',
    黑龙江: '黑龙江省',
    吉林: '吉林省',
    辽宁: '辽宁省',
    河北: '河北省',
    河南: '河南省',
    山东: '山东省',
    山西: '山西省',
    陕西: '陕西省',
    甘肃: '甘肃省',
    青海: '青海省',
    四川: '四川省',
    贵州: '贵州省',
    云南: '云南省',
    湖北: '湖北省',
    湖南: '湖南省',
    江苏: '江苏省',
    浙江: '浙江省',
    安徽: '安徽省',
    福建: '福建省',
    江西: '江西省',
    广东: '广东省',
    海南: '海南省',
    台湾: '台湾省',
  }

  return provinceAliasMap[raw] || raw
}

const buildRecentMonths = (count = 6): string[] => {
  const months: string[] = []
  const now = new Date()
  for (let i = count - 1; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    months.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`)
  }
  return months
}

// 用户统计数据
const userStats = reactive({
  totalUsers: 0,
  activeUsers: 0,
  newUsers: 0,
  bannedUsers: 0,
})

// 用户表单
const userForm = reactive({
  username: '',
  email: '',
  phone: '',
  password: '',
  gender: '未知',
  age: 0,
  region: '',
  organization: '',
  userType: 'user',
})

// 用户列表（从后端加载）
const users = ref<User[]>([])

// 表单验证规则
const userRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }],
}

// 性别显示
const getGenderLabel = (gender: number | null) => {
  if (gender === 1) return '男'
  if (gender === 2) return '女'
  return '未知'
}

// 角色显示
const getRoleLabel = (role: number) => {
  return role === 1 ? '管理员' : '普通用户'
}

// 格式化时间
const formatTime = (dateStr: string) => {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return d.toLocaleDateString('zh-CN') + ' ' + d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

// 计算属性
const filteredUsers = computed(() => {
  return users.value
})

const mapGenderTextToCode = (genderText: string): number => {
  if (genderText === '男') return 1
  if (genderText === '女') return 2
  return 0
}

// 初始化图表（使用真实数据）
const initCharts = (allUsers: User[]) => {
  nextTick(async () => {
    // 性别分布饼图
    const maleCount = allUsers.filter(u => u.gender === 1).length
    const femaleCount = allUsers.filter(u => u.gender === 2).length
    const unknownCount = allUsers.filter(u => u.gender !== 1 && u.gender !== 2).length

    const genderInstance = getOrCreateChart(genderChartRef)
    if (genderInstance) {
      genderChart = genderInstance
      genderChart.setOption({
      color: ['#5B8FF9', '#5AD8A6', '#9CA3AF'],
      tooltip: { trigger: 'item', formatter: '{a} <br/>{b}: {c} ({d}%)' },
      legend: { orient: 'vertical', left: 'left' },
      series: [{
        name: '性别分布', type: 'pie', radius: '50%',
        data: [
          { value: maleCount, name: '男' },
          { value: femaleCount, name: '女' },
          { value: unknownCount, name: '未知' },
        ],
        emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.5)' } },
      }],
      })
    }

    // 地区分布地图（中国省级）
    const provinceMap: Record<string, number> = {}
    allUsers.forEach(u => {
      const province = normalizeProvinceName(u.region)
      if (!province) return
      provinceMap[province] = (provinceMap[province] || 0) + 1
    })

    const regionInstance = getOrCreateChart(regionChartRef)
    if (regionInstance) {
      regionChart = regionInstance
      const regionData = Object.entries(provinceMap).map(([name, value]) => ({ name, value }))
      const maxRegionValue = Math.max(...regionData.map(item => item.value), 1)
      try {
        await ensureChinaMap()
        regionChart.setOption({
          tooltip: {
            trigger: 'item',
            formatter: ({ name, value }: { name: string; value?: number }) => `${name}<br/>用户数：${value || 0}`,
          },
          visualMap: {
            min: 0,
            max: maxRegionValue,
            left: 16,
            bottom: 16,
            text: ['高', '低'],
            calculable: true,
            inRange: {
              color: ['#e6f4ff', '#91caff', '#409eff', '#1677ff'],
            },
            textStyle: {
              color: '#606266',
            },
          },
          series: [
            {
              name: '用户地区分布',
              type: 'map',
              map: CHINA_MAP_KEY,
              roam: true,
              zoom: 1.05,
              label: {
                show: true,
                fontSize: 10,
                color: '#606266',
              },
              emphasis: {
                label: {
                  color: '#303133',
                },
                itemStyle: {
                  areaColor: '#95de64',
                },
              },
              itemStyle: {
                borderColor: '#ffffff',
                borderWidth: 1,
                areaColor: '#f5f7fa',
              },
              data: regionData,
            },
          ],
        })
      } catch (error) {
        console.error('加载中国地图失败:', error)
        regionChart.setOption({
          tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
          xAxis: { type: 'category', data: regionData.map(item => item.name), axisLabel: { rotate: 30 } },
          yAxis: { type: 'value' },
          series: [
            {
              name: '用户数量',
              type: 'bar',
              data: regionData.map(item => item.value),
            },
          ],
        })
      }
    }

    // 年龄分布直方图
    const ageBuckets = { '18-25': 0, '26-30': 0, '31-35': 0, '36-40': 0, '41-45': 0, '46-50': 0, '51+': 0 }
    allUsers.forEach(u => {
      const age = u.age
      if (!age) return
      if (age <= 25) ageBuckets['18-25']++
      else if (age <= 30) ageBuckets['26-30']++
      else if (age <= 35) ageBuckets['31-35']++
      else if (age <= 40) ageBuckets['36-40']++
      else if (age <= 45) ageBuckets['41-45']++
      else if (age <= 50) ageBuckets['46-50']++
      else ageBuckets['51+']++
    })

    const ageInstance = getOrCreateChart(ageChartRef)
    if (ageInstance) {
      ageChart = ageInstance
      ageChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      xAxis: { type: 'category', data: Object.keys(ageBuckets) },
      yAxis: { type: 'value' },
      series: [{
        name: '用户数量', type: 'bar', data: Object.values(ageBuckets),
        itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#764ba2' },
          { offset: 1, color: '#c084fc' }
        ])}
      }],
      })
    }

    // 注册趋势折线图（按月统计）
    const monthMap: Record<string, number> = {}
    allUsers.forEach(u => {
      if (!u.createdAt) return
      const d = new Date(u.createdAt)
      const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
      monthMap[key] = (monthMap[key] || 0) + 1
    })
    const recentMonths = buildRecentMonths(6)
    recentMonths.forEach(month => {
      if (!monthMap[month]) monthMap[month] = 0
    })

    const trendInstance = getOrCreateChart(trendChartRef)
    if (trendInstance) {
      trendChart = trendInstance
      trendChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: recentMonths },
      yAxis: { type: 'value' },
      series: [{
        name: '新增用户', type: 'line', data: recentMonths.map(k => monthMap[k] || 0),
        smooth: true,
        itemStyle: { color: '#67c23a' },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(103, 194, 58, 0.3)' },
          { offset: 1, color: 'rgba(103, 194, 58, 0.05)' }
        ])}
      }],
      })
    }

    // 响应式调整
    window.removeEventListener('resize', handleResize)
    window.addEventListener('resize', handleResize)
  })
}

const handleResize = () => {
  genderChart?.resize()
  regionChart?.resize()
  ageChart?.resize()
  trendChart?.resize()
}

// 方法
const handleSelectionChange = (selection: User[]) => {
  selectedUsers.value = selection
}

const handleSizeChange = (val: number) => {
  pageSize.value = val
  loadUsers()
}

const handleCurrentChange = (val: number) => {
  currentPage.value = val
  loadUsers()
}

const editUser = (user: User) => {
  editingUser.value = user
  userForm.username = user.username
  userForm.email = user.email || ''
  userForm.phone = user.phone || ''
  userForm.gender = getGenderLabel(user.gender)
  userForm.age = user.age || 0
  userForm.region = user.region || ''
  userForm.organization = user.organization || ''
  userForm.userType = user.role === 1 ? 'admin' : 'user'
  userForm.password = ''
  showAddUserDialog.value = true
}

const toggleUserRole = async (user: User) => {
  const newRole = user.role === 1 ? 0 : 1
  const action = newRole === 1 ? '设为管理员' : '设为普通用户'
  try {
    await ElMessageBox.confirm(`确定要将用户 ${user.username} ${action}吗？`, '确认操作')
    await http.patch(`/api/admin/users/${user.userId}/role`, { role: newRole })
    ElMessage.success(`${action}成功`)
    loadUsers()
  } catch {
    // 用户取消操作
  }
}

const deleteUser = async (user: User) => {
  try {
    await ElMessageBox.confirm(`确定要删除用户 ${user.username} 吗？此操作不可撤销！`, '确认删除', {
      type: 'error',
    })
    await http.delete(`/api/admin/users/${user.userId}`)
    ElMessage.success('用户删除成功')
    loadUsers()
  } catch {
    // 用户取消操作
  }
}

const saveUser = async () => {
  if (!userFormRef.value) return

  await userFormRef.value.validate(async (valid) => {
    if (valid) {
      saving.value = true
      try {
        if (editingUser.value) {
          await http.put(`/api/admin/users/${editingUser.value.userId}`, {
            username: userForm.username,
            email: userForm.email || null,
            phone: userForm.phone || null,
            gender: mapGenderTextToCode(userForm.gender),
            age: userForm.age || null,
            region: userForm.region || null,
            organization: userForm.organization || null,
            role: userForm.userType === 'admin' ? 1 : 0,
          })
          // 如果填了新密码，重置密码
          if (userForm.password) {
            await http.patch(`/api/admin/users/${editingUser.value.userId}/reset-password`, { newPassword: userForm.password })
          }
          ElMessage.success('用户更新成功')
        } else {
          if (!userForm.password || userForm.password.length < 6) {
            ElMessage.error('新增用户时，密码长度不能少于6位')
            return
          }
          await http.post('/api/admin/users', {
            username: userForm.username,
            email: userForm.email || null,
            phone: userForm.phone || null,
            password: userForm.password,
            gender: mapGenderTextToCode(userForm.gender),
            age: userForm.age || null,
            region: userForm.region || null,
            organization: userForm.organization || null,
            role: userForm.userType === 'admin' ? 1 : 0,
          })
          ElMessage.success('用户添加成功')
        }

        showAddUserDialog.value = false
        resetForm()
        loadUsers()
      } catch (error: any) {
        ElMessage.error(error?.response?.data?.message || '操作失败，请重试')
      } finally {
        saving.value = false
      }
    }
  })
}

const resetForm = () => {
  editingUser.value = null
  Object.assign(userForm, {
    username: '',
    email: '',
    phone: '',
    password: '',
    gender: '未知',
    age: 0,
    region: '',
    organization: '',
    userType: 'user',
  })
}

const executeBatchAction = async () => {
  if (selectedUsers.value.length === 0) return
  if (!batchAction.value) {
    ElMessage.warning('请选择批量操作类型')
    return
  }

  batchProcessing.value = true
  try {
    switch (batchAction.value) {
      case 'set-user':
        for (const u of selectedUsers.value) {
          if (u.role !== 0) {
            await http.patch(`/api/admin/users/${u.userId}/role`, { role: 0 })
          }
        }
        ElMessage.success('批量设为普通用户完成')
        break
      case 'set-admin':
        for (const u of selectedUsers.value) {
          if (u.role !== 1) {
            await http.patch(`/api/admin/users/${u.userId}/role`, { role: 1 })
          }
        }
        ElMessage.success('批量设为管理员完成')
        break
      case 'delete':
        for (const u of selectedUsers.value) await http.delete(`/api/admin/users/${u.userId}`)
        ElMessage.success('批量删除完成')
        break
    }

    showBatchDialog.value = false
    selectedUsers.value = []
    batchAction.value = ''
    loadUsers()
    loadStatistics()
    loadAllUsersForCharts()
  } catch (error) {
    ElMessage.error('批量操作失败，请重试')
  } finally {
    batchProcessing.value = false
  }
}

// 加载用户列表
const loadUsers = async () => {
  try {
    loading.value = true
    const { data } = await http.get('/api/admin/users', {
      params: {
        page: currentPage.value - 1,
        size: pageSize.value,
        keyword: searchKeyword.value || undefined,
      },
    })
    users.value = data?.records || []
    totalUsers.value = data?.total || 0
  } catch (error: any) {
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

// 加载统计数据
const loadStatistics = async () => {
  try {
    const { data } = await http.get('/api/admin/users/statistics')
    userStats.totalUsers = data.totalUsers || 0
    userStats.activeUsers = data.normalCount || 0
    userStats.newUsers = data.newUsers || 0
    userStats.bannedUsers = data.bannedUsers || 0
  } catch {
    // ignore
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadUsers()
}

const refreshUsers = () => {
  loadUsers()
  loadStatistics()
  loadAllUsersForCharts()
}

// 加载全部用户用于图表展示
const loadAllUsersForCharts = async () => {
  try {
    const { data } = await http.get('/api/admin/users', { params: { page: 0, size: 10000 } })
    const allUsers = data?.records || []
    initCharts(allUsers)
  } catch {
    // 图表加载失败不影响主功能
  }
}

// 生命周期
onMounted(() => {
  loadUsers()
  loadStatistics()
  loadAllUsersForCharts()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  genderChart?.dispose()
  regionChart?.dispose()
  ageChart?.dispose()
  trendChart?.dispose()
})
</script>

<style scoped>
.user-management {
  padding: 0;
}

.stats-overview {
  margin-bottom: 20px;
}

.stat-card {
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 15px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  color: #fff;
}

.stat-icon .el-icon {
  font-size: 34px;
}

.stat-icon.total-users {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon.active-users {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
}

.stat-icon.new-users {
  background: linear-gradient(135deg, #e6a23c 0%, #f0c78a 100%);
}

.stat-icon.banned-users {
  background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
}

.stat-info {
  flex: 1;
}

.stat-number {
  font-size: 2rem;
  font-weight: 600;
  color: var(--theme-text, var(--el-text-color-primary));
  line-height: 1;
}

.stat-label {
  color: var(--theme-text-secondary, var(--el-text-color-secondary));
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

.table-card {
  margin-bottom: 20px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table-header h3 {
  margin: 0;
  color: var(--theme-text, var(--el-text-color-primary));
}

.header-actions {
  display: flex;
  align-items: center;
}

.pagination-container {
  margin-top: 20px;
  text-align: right;
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
}
</style>
