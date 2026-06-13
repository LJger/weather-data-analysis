<template>
  <div class="login-page">
    <router-link to="/welcome" class="back-home">
      <el-icon><ArrowLeft /></el-icon>
      返回首页
    </router-link>

    <div class="login-container">
      <section class="login-visual" aria-label="气象系统登录说明">
        <div class="visual-content">
          <div class="visual-badge">
            <el-icon><Cloudy /></el-icon>
            Weather Data Workspace
          </div>
          <h1>气象数据智能分析系统</h1>
          <p>登录后可管理采集任务、查看统计图表、使用 GIS 地图和趋势预测功能。</p>

          <div class="weather-animation" aria-hidden="true">
            <div class="cloud-shape cloud-1"></div>
            <div class="cloud-shape cloud-2"></div>
            <div class="rain-drops">
              <span v-for="i in 18" :key="i" class="drop"></span>
            </div>
          </div>
        </div>
      </section>

      <section class="login-form-section">
        <div class="form-container">
          <div class="form-header">
            <h2>账号登录</h2>
            <p>输入用户名和密码，系统将自动识别账号角色</p>
          </div>

          <el-form
            ref="loginFormRef"
            class="login-form"
            :model="loginForm"
            :rules="rules"
            @submit.prevent="handleLogin"
          >
            <el-form-item prop="username">
              <el-input
                v-model="loginForm.username"
                placeholder="请输入用户名"
                prefix-icon="User"
                size="large"
              />
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                prefix-icon="Lock"
                size="large"
                show-password
              />
            </el-form-item>

            <el-form-item>
              <div class="form-options">
                <el-checkbox v-model="rememberMe">记住账号</el-checkbox>
                <span class="form-note">密码由后端账号体系校验</span>
              </div>
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :loading="isLoading"
                class="login-btn"
                @click="handleLogin"
              >
                {{ isLoading ? '登录中...' : '登录' }}
              </el-button>
            </el-form-item>
          </el-form>

          <div class="form-footer">
            <span>还没有账户？</span>
            <router-link to="/register" class="register-link">注册普通用户</router-link>
          </div>

        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import http from '@/api/http'
import { getAuthErrorMessage, normalizeAuthErrorMessage } from '@/utils/authError'

const router = useRouter()

const rememberMe = ref(false)
const isLoading = ref(false)
const loginFormRef = ref<FormInstance>()

const loginForm = ref({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' },
  ],
}

const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      isLoading.value = true

      try {
        const { data } = await http.post('/api/auth/login', {
          username: loginForm.value.username,
          password: loginForm.value.password,
        })

        if (data?.success) {
          if (data.token) {
            localStorage.setItem('token', data.token)
            localStorage.setItem('userId', String(data.userId))
            localStorage.setItem('username', data.username || '')
            localStorage.setItem('role', String(data.role || 0))
          }

          ElMessage.success(data.message || '登录成功')

          if (data.role === 1) {
            router.push('/admin')
          } else {
            router.push('/dashboard')
          }
        } else {
          ElMessage.error(normalizeAuthErrorMessage(data?.message, '用户名或密码错误'))
        }
      } catch (error: any) {
        console.error('登录失败:', error)
        const errorMsg = getAuthErrorMessage(error, '登录失败，请稍后重试')
        ElMessage.error(errorMsg)
      } finally {
        isLoading.value = false
      }
    }
  })
}

</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background:
    linear-gradient(180deg, rgba(232, 241, 250, 0.92), rgba(245, 248, 252, 0.98)),
    #f4f7fb;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
  position: relative;
}

.login-container {
  width: min(980px, 100%);
  min-height: 600px;
  display: grid;
  grid-template-columns: 0.92fr 1fr;
  overflow: hidden;
  background: #fff;
  border: 1px solid #d9e2ec;
  border-radius: 8px;
  box-shadow: 0 18px 45px rgba(25, 55, 90, 0.12);
}

.login-visual {
  position: relative;
  overflow: hidden;
  padding: 48px;
  color: #f8fbff;
  background: #1f5f99;
}

.login-visual::after {
  content: '';
  position: absolute;
  inset: auto -60px -90px auto;
  width: 260px;
  height: 260px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}

.visual-content {
  position: relative;
  z-index: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.visual-badge {
  width: fit-content;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  color: #dbeafe;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 6px;
  font-size: 13px;
  font-weight: 700;
}

.login-visual h1 {
  margin: 42px 0 16px;
  font-size: 32px;
  line-height: 1.25;
}

.login-visual p {
  margin: 0;
  color: #dbeafe;
  line-height: 1.8;
}

.weather-animation {
  position: relative;
  height: 220px;
  margin-top: auto;
}

.cloud-shape {
  position: absolute;
  width: 140px;
  height: 46px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
}

.cloud-shape::before,
.cloud-shape::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  background: inherit;
}

.cloud-shape::before {
  width: 58px;
  height: 58px;
  left: 24px;
  top: -28px;
}

.cloud-shape::after {
  width: 74px;
  height: 74px;
  right: 18px;
  top: -38px;
}

.cloud-1 {
  top: 76px;
  left: 18px;
  opacity: 0.72;
  animation: cloudDrift 7s ease-in-out infinite;
}

.cloud-2 {
  top: 34px;
  right: 16px;
  width: 118px;
  opacity: 0.46;
  animation: cloudDrift 9s ease-in-out infinite reverse;
}

.rain-drops {
  position: absolute;
  inset: 84px 24px 0;
}

.drop {
  position: absolute;
  width: 2px;
  height: 14px;
  border-radius: 2px;
  background: rgba(219, 234, 254, 0.72);
  animation: rainFall 2.8s linear infinite;
}

.drop:nth-child(3n + 1) {
  left: 18%;
}

.drop:nth-child(3n + 2) {
  left: 48%;
  animation-delay: 0.7s;
}

.drop:nth-child(3n) {
  left: 78%;
  animation-delay: 1.4s;
}

.drop:nth-child(odd) {
  animation-duration: 3.3s;
}

@keyframes cloudDrift {
  0%,
  100% {
    transform: translateX(0);
  }
  50% {
    transform: translateX(14px);
  }
}

@keyframes rainFall {
  0% {
    transform: translateY(-16px);
    opacity: 0;
  }
  18% {
    opacity: 0.8;
  }
  100% {
    transform: translateY(132px);
    opacity: 0;
  }
}

.login-form-section {
  padding: 52px;
  display: flex;
  align-items: center;
  background: #fff;
}

.form-container {
  width: 100%;
}

.form-header {
  margin-bottom: 26px;
}

.form-header h2 {
  margin: 0 0 8px;
  color: #102a43;
  font-size: 28px;
}

.form-header p,
.form-note,
.form-footer {
  color: #627d98;
}

.form-options {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.form-note {
  font-size: 13px;
}

.login-btn {
  width: 100%;
  font-weight: 700;
}

.form-footer {
  display: flex;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
}

.register-link {
  color: #1d4f91;
  text-decoration: none;
  font-weight: 700;
}

.back-home {
  position: absolute;
  top: 24px;
  left: 24px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #334e68;
  text-decoration: none;
  font-weight: 700;
}

.back-home:hover {
  color: #1d4f91;
}

@media (max-width: 820px) {
  .login-container {
    grid-template-columns: 1fr;
    max-width: 460px;
  }

  .login-visual {
    display: none;
  }

  .login-form-section {
    padding: 38px 28px;
  }
}

@media (max-width: 480px) {
  .login-page {
    padding: 72px 16px 24px;
  }

  .form-options {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
