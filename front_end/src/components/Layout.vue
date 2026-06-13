<template>
  <div class="layout">
    <!-- 导航栏 -->
    <nav class="sidebar">
      <div class="nav-header">
        <div class="logo-section">
          <div class="logo-icon">
            <el-icon :size="28"><Cloudy /></el-icon>
          </div>
          <h2>气象数据平台</h2>
        </div>
      </div>
      <ul class="nav-menu">
        <li>
          <router-link
            to="/dashboard/profile"
            class="nav-link"
            :class="{ active: $route.path === '/dashboard/profile' }"
          >
            <el-icon class="nav-icon"><User /></el-icon>
            个人信息
          </router-link>
        </li>
        <li>
          <router-link
            to="/dashboard/data-collection"
            class="nav-link"
            :class="{ active: $route.path === '/dashboard/data-collection' }"
          >
            <el-icon class="nav-icon"><DataLine /></el-icon>
            数据采集
          </router-link>
        </li>
        <li>
          <router-link
            to="/dashboard/data-analysis/statistical"
            class="nav-link"
            :class="{ active: $route.path === '/dashboard/data-analysis/statistical' }"
          >
            <el-icon class="nav-icon"><DataAnalysis /></el-icon>
            统计分析
          </router-link>
        </li>
        <li>
          <router-link
            to="/dashboard/data-analysis/gis-map"
            class="nav-link"
            :class="{ active: $route.path === '/dashboard/data-analysis/gis-map' }"
          >
            <el-icon class="nav-icon"><Location /></el-icon>
            GIS分析
          </router-link>
        </li>
        <li>
          <router-link
            to="/dashboard/data-prediction"
            class="nav-link"
            :class="{ active: $route.path === '/dashboard/data-prediction' }"
          >
            <el-icon class="nav-icon"><TrendCharts /></el-icon>
            数据预测
          </router-link>
        </li>
        <li>
          <router-link
            to="/dashboard/personalized-service"
            class="nav-link"
            :class="{ active: $route.path === '/dashboard/personalized-service' }"
          >
            <el-icon class="nav-icon"><Setting /></el-icon>
            系统设置
          </router-link>
        </li>
      </ul>
    </nav>

    <!-- 主内容区 -->
    <main class="main-content">
      <div class="content-header">
        <div class="header-left">
          <h1>{{ getPageTitle() }}</h1>
          <span class="header-breadcrumb">{{ headerSubtitle }}</span>
        </div>
        <div class="header-actions">
          <!-- 设置按钮 -->
          <el-tooltip content="系统设置" placement="bottom">
            <el-button class="header-icon-btn" circle @click="showSettingsDrawer = true">
              <el-icon :size="18"><Setting /></el-icon>
            </el-button>
          </el-tooltip>
          <!-- 用户信息 -->
          <el-dropdown trigger="click" @command="handleUserCommand">
            <div class="user-info-btn">
              <el-avatar :size="36" :src="userAvatar" class="header-avatar">
                <el-icon><User /></el-icon>
              </el-avatar>
              <div class="user-info-text">
                <span class="user-name">{{ displayName }}</span>
                <el-tag :type="roleTagType" size="small" effect="plain" round>{{ roleName }}</el-tag>
              </div>
              <el-icon class="user-arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人信息
                </el-dropdown-item>
                <el-dropdown-item command="settings">
                  <el-icon><Setting /></el-icon>系统设置
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <div class="content-body">
        <router-view />
      </div>
    </main>

    <!-- 右侧设置抽屉 -->
    <el-drawer
      v-model="showSettingsDrawer"
      title="系统设置"
      direction="rtl"
      size="320px"
      :show-close="true"
    >
      <div class="settings-drawer-content">
        <!-- 主题设置 -->
        <div class="settings-section">
          <h4 class="settings-section-title">
            <el-icon><Brush /></el-icon>
            界面设置
          </h4>
          <p class="settings-section-desc">保留少量界面偏好，避免影响业务页面阅读</p>
          <el-form label-width="86px" size="small" class="settings-form">
            <el-form-item label="主色">
              <el-color-picker v-model="themeStore.currentTheme.primaryColor" @change="handleThemeFieldChange" />
            </el-form-item>
            <el-form-item label="背景">
              <el-radio-group v-model="themeStore.currentTheme.backgroundMode" @change="handleThemeFieldChange">
                <el-radio-button value="light">浅色</el-radio-button>
                <el-radio-button value="dark">深色</el-radio-button>
                <el-radio-button value="auto">跟随系统</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="字号">
              <el-slider
                v-model="themeStore.currentTheme.fontSize"
                :min="12"
                :max="18"
                :step="1"
                @change="handleThemeFieldChange"
              />
            </el-form-item>
            <el-form-item>
              <el-button plain @click="themeStore.resetTheme()">恢复默认</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  User,
  DataLine,
  TrendCharts,
  DataAnalysis,
  ArrowDown,
  Location,
  Setting,
  SwitchButton,
  Brush,
  Cloudy,
} from '@element-plus/icons-vue'
import { useThemeStore } from '@/stores/theme'
import http from '@/api/http'

const route = useRoute()
const router = useRouter()
const themeStore = useThemeStore()
const showSettingsDrawer = ref(false)

// 用户信息
const displayName = ref('用户')
const userRole = ref(0)
const userAvatar = ref('')

// 加载用户头像
const loadUserAvatar = async () => {
  const userId = localStorage.getItem('userId')
  if (!userId) return
  try {
    const { data } = await http.get('/api/auth/profile', { params: { userId } })
    if (data.avatarUrl) {
      userAvatar.value = (http.defaults.baseURL || 'http://localhost:8080') + data.avatarUrl
    }
    if (data.username) {
      displayName.value = data.username
    }
    if (data.role !== undefined) {
      userRole.value = data.role
    }
  } catch (e) {
    console.error('加载用户信息失败:', e)
  }
}

// 监听头像更新事件（由 Profile.vue 触发）
const handleAvatarUpdated = (e: Event) => {
  const detail = (e as CustomEvent).detail
  if (detail?.avatarUrl) {
    userAvatar.value = detail.avatarUrl
  }
}

onMounted(() => {
  displayName.value = localStorage.getItem('username') || '用户'
  userRole.value = Number(localStorage.getItem('role') || 0)
  loadUserAvatar()
  window.addEventListener('avatar-updated', handleAvatarUpdated)
})

onUnmounted(() => {
  window.removeEventListener('avatar-updated', handleAvatarUpdated)
})

const roleName = computed(() => {
  const roleMap: Record<number, string> = { 0: '普通用户', 1: '管理员' }
  return roleMap[userRole.value] || '普通用户'
})

const roleTagType = computed(() => {
  return userRole.value === 1 ? 'danger' : 'info'
})

// 顶部副标题
const headerSubtitle = computed(() => {
  const subtitles: Record<string, string> = {
    '/dashboard/profile': '查看与编辑您的个人资料',
    '/dashboard/data-collection': '管理气象数据采集任务',
    '/dashboard/data-analysis/statistical': '多维度统计与可视化分析',
    '/dashboard/data-analysis/gis-map': '地理信息系统空间分析',
    '/dashboard/data-prediction': '基于模型的预测分析',
    '/dashboard/personalized-service': '界面偏好与默认分析参数',
  }
  return subtitles[route.path] || ''
})

// 主题相关
const handleThemeFieldChange = () => {
  themeStore.applyTheme(themeStore.currentTheme)
}

// 根据路由获取页面标题
const getPageTitle = () => {
  const titles: Record<string, string> = {
    '/dashboard/profile': '个人信息',
    '/dashboard/data-collection': '数据采集',
    '/dashboard/data-analysis/statistical': '统计分析',
    '/dashboard/data-analysis/gis-map': 'GIS分析',
    '/dashboard/data-prediction': '数据预测',
    '/dashboard/personalized-service': '系统设置',
  }
  return titles[route.path] || '用户中心'
}

// 用户下拉菜单命令
const handleUserCommand = (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/dashboard/profile')
      break
    case 'settings':
      showSettingsDrawer.value = true
      break
    case 'logout':
      logout()
      break
  }
}

// 退出登录
const logout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userId')
  localStorage.removeItem('username')
  localStorage.removeItem('role')
  router.push('/welcome')
}
</script>

<style scoped>
.layout {
  display: flex;
  min-height: 100vh;
  background-color: var(--el-bg-color-page);
  transition: background-color var(--el-transition-duration);
}

/* 侧边导航栏 */
.sidebar {
  width: var(--theme-sidebar-width, 240px);
  background: var(--theme-surface);
  color: var(--theme-text);
  padding: 0;
  box-shadow: var(--theme-shadow-light);
  border-right: 1px solid var(--theme-border);
  position: fixed;
  top: 0;
  left: 0;
  height: 100vh;
  z-index: 1000;
  overflow-y: auto;
  transition:
    all var(--el-transition-duration),
    width var(--el-transition-duration-fast) ease;
}

.nav-header {
  padding: 20px 16px;
  border-bottom: 1px solid var(--theme-border);
}

.logo-section {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: var(--el-color-primary-light-9);
  border: 1px solid var(--el-color-primary-light-7);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--theme-primary);
  flex-shrink: 0;
}

.nav-header h2 {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--theme-text);
  white-space: nowrap;
}

.nav-menu {
  list-style: none;
  padding: 10px 0;
  margin: 0;
}

.nav-menu li {
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  position: relative;
}

.nav-menu li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 0;
  background: var(--theme-primary);
  transition: width 0.3s ease;
}

.nav-menu li:hover::before {
  width: 4px;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 15px 20px;
  color: var(--theme-text-secondary);
  text-decoration: none;
  transition: all var(--el-transition-duration-fast);
  font-weight: 500;
  font-size: 1rem;
  border-left: 4px solid transparent;
  position: relative;
  z-index: 1;
}

.nav-link:hover {
  background: var(--el-fill-color-light);
  color: var(--theme-primary);
  border-left-color: var(--theme-primary);
}

.nav-link.active {
  background: var(--el-color-primary-light-9);
  color: var(--theme-primary);
  font-weight: 600;
  border-left-color: var(--theme-primary);
}

.nav-icon {
  font-size: 1.25rem;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.arrow {
  font-size: 0.8rem;
  transition: transform var(--el-transition-duration-fast);
  opacity: 0.7;
  margin-left: auto;
}

/* 主内容区 */
.main-content {
  flex: 1;
  padding: 0;
  margin-left: var(--theme-sidebar-width, 240px);
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--el-bg-color-page);
  transition: margin-left var(--el-transition-duration-fast) ease;
  overflow: hidden;
}

.content-header {
  background: var(--theme-background);
  padding: 16px var(--theme-content-padding, 24px);
  border-bottom: 1px solid var(--theme-border);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  display: flex;
  justify-content: space-between;
  align-items: center;
  z-index: 100;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.content-header h1 {
  margin: 0;
  color: var(--theme-text);
  font-size: 1.5rem;
  font-weight: 700;
  line-height: 1.3;
}

.header-breadcrumb {
  font-size: 0.8rem;
  color: var(--theme-text-secondary, var(--el-text-color-secondary));
  margin-top: 2px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon-btn {
  background: transparent;
  border: 1px solid var(--el-border-color-light);
  color: var(--theme-text-secondary, var(--el-text-color-secondary));
  transition: all 0.25s ease;
}

.header-icon-btn:hover {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

/* 用户信息下拉 */
.user-info-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px;
  border-radius: 24px;
  cursor: pointer;
  transition: all 0.25s ease;
  border: 1px solid transparent;
}

.user-info-btn:hover {
  background: var(--el-fill-color-light);
  border-color: var(--el-border-color-light);
}

.header-avatar {
  background: var(--el-color-primary-light-9);
  color: var(--theme-primary);
  flex-shrink: 0;
}

.user-info-text {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.user-name {
  font-weight: 600;
  font-size: 0.9rem;
  color: var(--theme-text);
  line-height: 1.2;
}

.user-arrow {
  font-size: 12px;
  color: var(--theme-text-secondary, var(--el-text-color-secondary));
  transition: transform 0.2s;
}

.content-body {
  padding: var(--theme-content-padding, 20px);
  background-color: var(--el-bg-color-page);
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
}

/* GIS地图页面特殊处理 */
.content-body:has(.weather-map-container) {
  padding: 0;
  overflow: hidden;
  height: calc(100vh - 80px);
}

/* ====== 右侧设置抽屉 ====== */
.settings-drawer-content {
  padding: 0 4px;
}

.settings-section {
  margin-bottom: 28px;
}

.settings-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 4px 0;
  font-size: 1rem;
  font-weight: 600;
  color: var(--theme-text, var(--el-text-color-primary));
}

.settings-section-desc {
  margin: 0 0 16px 0;
  font-size: 0.8rem;
  color: var(--theme-text-secondary, var(--el-text-color-secondary));
}

.settings-form :deep(.el-slider) {
  width: 180px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sidebar {
    width: 100%;
    position: relative;
    height: auto;
    box-shadow: none;
  }

  .main-content {
    margin-left: 0;
  }

  .content-header {
    flex-direction: column;
    gap: 12px;
    text-align: center;
  }

  .header-actions {
    flex-direction: row;
    gap: 10px;
  }

  .content-body {
    padding: 20px;
  }

  .nav-link {
    padding: 15px 20px;
  }
}

@media (max-width: 480px) {
  .nav-header h2 {
    font-size: 1rem;
  }

  .content-header h1 {
    font-size: 1.3rem;
  }

  .nav-link {
    padding: 12px 15px;
    font-size: 0.9rem;
  }

  .content-body {
    padding: 15px;
  }
}
</style>
