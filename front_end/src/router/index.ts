import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/components/Layout.vue'
import AdminLayout from '@/components/AdminLayout.vue'
import Welcome from '@/views/Welcome.vue'
import Login from '@/views/Login.vue'
import Register from '@/views/Register.vue'
import Profile from '@/views/Profile.vue'
import DataCollection from '@/views/DataCollection.vue'
import StatisticalAnalysis from '@/views/StatisticalAnalysis.vue'
import DataPrediction from '@/views/DataPrediction.vue'
import PersonalizedService from '@/views/PersonalizedService.vue'
import WeatherMapView from '@/views/WeatherMapView.vue'
import UserManagement from '@/views/admin/UserManagement.vue'
import StorageManagement from '@/views/admin/StorageManagement.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // 欢迎页面（首页）
    {
      path: '/welcome',
      name: 'welcome',
      component: Welcome,
      meta: { public: true },
    },
    // 认证页面
    {
      path: '/login',
      name: 'login',
      component: Login,
      meta: { public: true },
    },
    {
      path: '/register',
      name: 'register',
      component: Register,
      meta: { public: true },
    },
    // 默认重定向到欢迎页面
    {
      path: '/',
      redirect: '/welcome',
    },
    // 用户功能页面
    {
      path: '/dashboard',
      component: Layout,
      redirect: '/dashboard/profile',
      meta: { requiresAuth: true, requiresUser: true },
      children: [
        {
          path: 'profile',
          name: 'profile',
          component: Profile,
        },
        {
          path: 'data-collection',
          name: 'data-collection',
          component: DataCollection,
        },
        {
          path: 'data-analysis/statistical',
          name: 'statistical-analysis',
          component: StatisticalAnalysis,
        },
        {
          path: 'data-analysis/gis-map',
          name: 'gis-map-analysis',
          component: WeatherMapView,
        },
        {
          path: 'data-prediction',
          name: 'data-prediction',
          component: DataPrediction,
        },
        {
          path: 'personalized-service',
          name: 'personalized-service',
          component: PersonalizedService,
        },
      ],
    },
    // 管理员功能页面
    {
      path: '/admin',
      component: AdminLayout,
      redirect: '/admin/user-management',
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [
        {
          path: 'user-management',
          name: 'admin-user-management',
          component: UserManagement,
        },
        {
          path: 'storage-management',
          name: 'admin-storage-management',
          component: StorageManagement,
        },
      ],
    },
  ],
})

const getHomePathByRole = (role: string | null) => (role === '1' ? '/admin' : '/dashboard')

// 路由守卫
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('role')

  // 公开页面直接放行
  if (to.meta.public) {
    // 如果已登录访问登录/注册页，重定向到 dashboard
    if (token && (to.name === 'login' || to.name === 'register')) {
      next(getHomePathByRole(role))
      return
    }
    next()
    return
  }

  // 需要认证的页面
  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!token) {
      next({ name: 'login', query: { redirect: to.fullPath } })
      return
    }

    // 需要管理员权限
    if (to.matched.some(record => record.meta.requiresAdmin)) {
      if (role !== '1') {
        next('/dashboard')
        return
      }
    }

    if (to.matched.some(record => record.meta.requiresUser)) {
      if (role === '1') {
        next('/admin')
        return
      }
    }
  }

  next()
})

export default router
