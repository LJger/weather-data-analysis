<template>
  <div class="admin-layout">
    <!-- 导航栏 -->
    <nav class="sidebar">
      <div class="nav-header">
        <h2>管理员中心</h2>
        <div class="admin-badge">管理端</div>
      </div>
      <ul class="nav-menu">
        <li>
          <router-link
            to="/admin/user-management"
            class="nav-link"
            :class="{ active: $route.path === '/admin/user-management' }"
          >
            <el-icon class="nav-icon"><UserFilled /></el-icon>
            用户管理
          </router-link>
        </li>
        <li>
          <router-link
            to="/admin/storage-management"
            class="nav-link"
            :class="{ active: $route.path === '/admin/storage-management' }"
          >
            <el-icon class="nav-icon"><FolderOpened /></el-icon>
            存储管理
          </router-link>
        </li>
      </ul>
    </nav>

    <!-- 主内容区 -->
    <main class="main-content">
      <div class="content-header">
        <h1>{{ getPageTitle() }}</h1>
        <div class="header-actions">
          <div class="user-info">
            <span class="user-name">管理员</span>
            <span class="user-role">系统管理</span>
          </div>
          <el-button type="danger" plain @click="logout">退出登录</el-button>
        </div>
      </div>

      <div class="content-body">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { UserFilled, FolderOpened } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

// 根据路由获取页面标题
const getPageTitle = () => {
  const titles: Record<string, string> = {
    '/admin/user-management': '用户管理',
    '/admin/storage-management': '存储管理',
  }
  return titles[route.path] || '管理员中心'
}

// 退出登录
const logout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userId')
  localStorage.removeItem('username')
  localStorage.removeItem('role')
  router.push('/login')
}
</script>

<style scoped>
.admin-layout {
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
  text-align: center;
  border-bottom: 1px solid var(--theme-border);
}

.nav-header h2 {
  margin: 0 0 8px 0;
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--theme-text);
}

.admin-badge {
  display: inline-block;
  background: var(--el-fill-color-light);
  color: var(--theme-text-secondary);
  padding: 4px 10px;
  border-radius: var(--el-border-radius-round);
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.5px;
  border: 1px solid var(--theme-border);
}

.nav-menu {
  list-style: none;
  padding: 10px 0;
  margin: 0;
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
  color: var(--theme-text, var(--el-text-color-primary));
  padding: 16px var(--theme-content-padding, 24px);
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  border-bottom: 1px solid var(--theme-border);
  z-index: 100;
  flex-shrink: 0;
}

.content-header h1 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 700;
  line-height: 1.3;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.user-name {
  font-weight: 600;
  font-size: 0.9rem;
  color: var(--theme-text);
}

.user-role {
  font-size: 0.8rem;
  color: var(--theme-text-secondary, var(--el-text-color-secondary));
}

.content-body {
  padding: var(--theme-content-padding, 20px);
  background-color: var(--el-bg-color-page);
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
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
    gap: 15px;
    text-align: center;
  }
}
</style>
