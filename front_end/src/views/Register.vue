<template>
  <div class="register-page">
    <router-link to="/welcome" class="back-home">
      <el-icon><ArrowLeft /></el-icon>
      返回首页
    </router-link>

    <div class="register-container">
      <section class="register-visual" aria-label="注册说明">
        <div class="visual-content">
          <div class="visual-badge">
            <el-icon><Cloudy /></el-icon>
            Account Setup
          </div>
          <h1>创建系统账号</h1>
          <p>普通用户可使用数据采集、统计分析、GIS 可视化和趋势预测等功能。</p>

          <div class="features-preview">
            <div class="feature-item">
              <el-icon><DataLine /></el-icon>
              <span>采集任务管理</span>
            </div>
            <div class="feature-item">
              <el-icon><DataAnalysis /></el-icon>
              <span>统计分析图表</span>
            </div>
            <div class="feature-item">
              <el-icon><Location /></el-icon>
              <span>GIS 空间展示</span>
            </div>
            <div class="feature-item">
              <el-icon><TrendCharts /></el-icon>
              <span>历史趋势预测</span>
            </div>
          </div>
        </div>
      </section>

      <section class="register-form-section">
        <div class="form-container">
          <div class="form-header">
            <h2>账户注册</h2>
            <p>公开注册仅创建普通用户，管理员账户由管理员分配</p>
          </div>

          <el-form
            ref="registerFormRef"
            class="register-form"
            :model="registerForm"
            :rules="rules"
            @submit.prevent="handleRegister"
          >
            <el-row :gutter="16">
              <el-col :xs="24" :sm="12">
                <el-form-item prop="username">
                  <el-input
                    v-model="registerForm.username"
                    placeholder="请输入用户名"
                    prefix-icon="User"
                    size="large"
                  />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item prop="email">
                  <el-input
                    v-model="registerForm.email"
                    type="email"
                    placeholder="请输入邮箱地址"
                    prefix-icon="Message"
                    size="large"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item prop="emailCode">
              <div class="email-code-row">
                <el-input
                  v-model="registerForm.emailCode"
                  placeholder="请输入邮箱验证码"
                  prefix-icon="Key"
                  size="large"
                  maxlength="6"
                  class="email-code-input"
                />
                <el-button
                  type="primary"
                  size="large"
                  :disabled="!canSendCode || sendingCode"
                  :loading="sendingCode"
                  class="send-code-btn"
                  @click="handleSendCode"
                >
                  {{ codeCountdown > 0 ? codeCountdown + 's 后重发' : '发送验证码' }}
                </el-button>
              </div>
            </el-form-item>

            <el-row :gutter="16">
              <el-col :xs="24" :sm="12">
                <el-form-item prop="password">
                  <el-input
                    v-model="registerForm.password"
                    type="password"
                    placeholder="请输入密码"
                    prefix-icon="Lock"
                    size="large"
                    show-password
                    @input="checkPasswordStrength"
                  />
                  <div class="password-strength" v-if="registerForm.password">
                    <el-progress
                      :percentage="passwordStrengthPercentage"
                      :color="passwordStrengthColor"
                      :show-text="false"
                      :stroke-width="4"
                    />
                    <span class="strength-text">{{ passwordStrengthText }}</span>
                  </div>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item prop="confirmPassword">
                  <el-input
                    v-model="registerForm.confirmPassword"
                    type="password"
                    placeholder="请再次输入密码"
                    prefix-icon="Lock"
                    size="large"
                    show-password
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="16">
              <el-col :xs="24" :sm="12">
                <el-form-item prop="phone">
                  <el-input
                    v-model="registerForm.phone"
                    type="tel"
                    placeholder="请输入手机号码（可选）"
                    prefix-icon="Phone"
                    size="large"
                  />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item prop="organization">
                  <el-input
                    v-model="registerForm.organization"
                    placeholder="请输入所属机构（可选）"
                    prefix-icon="OfficeBuilding"
                    size="large"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item prop="agreeToTerms">
              <el-checkbox v-model="registerForm.agreeToTerms">
                我已确认注册信息真实有效，并同意系统保存必要的账号资料
              </el-checkbox>
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :loading="isLoading"
                :disabled="!canSubmit"
                class="register-btn"
                @click="handleRegister"
              >
                {{ isLoading ? '注册中...' : '创建账户' }}
              </el-button>
            </el-form-item>
          </el-form>

          <div class="form-footer">
            <span>已有账户？</span>
            <router-link to="/login" class="login-link">返回登录</router-link>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  Cloudy,
  DataAnalysis,
  DataLine,
  Location,
  TrendCharts,
} from '@element-plus/icons-vue'
import http from '@/api/http'
import { getAuthErrorMessage, normalizeAuthErrorMessage } from '@/utils/authError'

const router = useRouter()

const isLoading = ref(false)
const registerFormRef = ref<FormInstance>()

const registerForm = ref({
  username: '',
  email: '',
  emailCode: '',
  password: '',
  confirmPassword: '',
  phone: '',
  organization: '',
  agreeToTerms: false,
})

const sendingCode = ref(false)
const codeCountdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const canSendCode = computed(() => {
  const email = registerForm.value.email
  return email && /^[\w.+-]+@[\w.-]+\.[a-zA-Z]{2,}$/.test(email) && codeCountdown.value === 0
})

const handleSendCode = async () => {
  if (!canSendCode.value) return
  sendingCode.value = true
  try {
    const { data } = await http.post('/api/auth/send-email-code', {
      email: registerForm.value.email,
    })
    if (data.success) {
      ElMessage.success(data.message || '验证码已发送')
      codeCountdown.value = 60
      countdownTimer = setInterval(() => {
        codeCountdown.value--
        if (codeCountdown.value <= 0) {
          if (countdownTimer) clearInterval(countdownTimer)
          countdownTimer = null
        }
      }, 1000)
    } else {
      ElMessage.error(normalizeAuthErrorMessage(data.message, '发送失败'))
    }
  } catch (error: any) {
    ElMessage.error(getAuthErrorMessage(error, '发送失败，请稍后重试'))
  } finally {
    sendingCode.value = false
  }
}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 30, message: '用户名长度在2到30个字符', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' },
  ],
  emailCode: [
    { required: true, message: '请输入邮箱验证码', trigger: 'blur' },
    { min: 6, max: 6, message: '验证码为6位数字', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== registerForm.value.password) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }],
  organization: [],
  agreeToTerms: [
    {
      validator: (_rule, value, callback) => {
        if (!value) {
          callback(new Error('请确认注册信息说明'))
        } else {
          callback()
        }
      },
      trigger: 'change',
    },
  ],
}

const passwordsMatch = computed(() => {
  return registerForm.value.password === registerForm.value.confirmPassword
})

const passwordStrengthPercentage = computed(() => {
  const password = registerForm.value.password
  if (!password) return 0

  let score = 0
  if (password.length >= 8) score++
  if (/[a-z]/.test(password)) score++
  if (/[A-Z]/.test(password)) score++
  if (/[0-9]/.test(password)) score++
  if (/[^A-Za-z0-9]/.test(password)) score++

  return score * 20
})

const passwordStrengthColor = computed(() => {
  const percentage = passwordStrengthPercentage.value
  if (percentage <= 40) return '#d64545'
  if (percentage <= 60) return '#b7791f'
  if (percentage <= 80) return '#1d4f91'
  return '#2f855a'
})

const passwordStrengthText = computed(() => {
  const percentage = passwordStrengthPercentage.value
  if (percentage <= 20) return '很弱'
  if (percentage <= 40) return '弱'
  if (percentage <= 60) return '中等'
  if (percentage <= 80) return '强'
  return '很强'
})

const canSubmit = computed(() => {
  const form = registerForm.value
  return (
    form.username &&
    form.email &&
    form.emailCode &&
    form.password &&
    form.confirmPassword &&
    passwordsMatch.value &&
    form.agreeToTerms
  )
})

const checkPasswordStrength = () => {
  // 密码强度由计算属性自动更新
}

const handleRegister = async () => {
  if (!registerFormRef.value) return

  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      isLoading.value = true

      try {
        const payload = {
          username: registerForm.value.username,
          password: registerForm.value.password,
          email: registerForm.value.email,
          emailCode: registerForm.value.emailCode,
          phone: registerForm.value.phone || null,
          gender: 0,
          region: null,
          organization: registerForm.value.organization || null,
          bio: null,
        }

        const { data } = await http.post('/api/auth/register', payload)

        if (data?.success) {
          localStorage.setItem('token', data.token)
          localStorage.setItem('userId', String(data.userId))
          localStorage.setItem('username', data.username)
          localStorage.setItem('role', String(data.role))

          ElMessage.success(data.message || '注册成功，请登录')
          router.push('/login')
        } else {
          ElMessage.error(normalizeAuthErrorMessage(data?.message, '注册失败，请检查信息'))
        }
      } catch (error: any) {
        console.error('注册失败:', error)
        const errorMsg = getAuthErrorMessage(error, '注册失败，请稍后重试')
        ElMessage.error(errorMsg)
      } finally {
        isLoading.value = false
      }
    }
  })
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  background: #f4f7fb;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
  position: relative;
}

.register-container {
  width: min(1120px, 100%);
  min-height: 690px;
  display: grid;
  grid-template-columns: 0.84fr 1.16fr;
  overflow: hidden;
  background: #fff;
  border: 1px solid #d9e2ec;
  border-radius: 8px;
  box-shadow: 0 18px 45px rgba(25, 55, 90, 0.1);
}

.register-visual {
  padding: 48px;
  color: #f8fbff;
  background: #1f5f99;
}

.visual-content {
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

.register-visual h1 {
  margin: 42px 0 16px;
  font-size: 32px;
}

.register-visual p {
  margin: 0;
  color: #dbeafe;
  line-height: 1.8;
}

.features-preview {
  margin-top: auto;
  display: grid;
  gap: 12px;
}

.feature-item {
  min-height: 52px;
  padding: 0 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #eaf4ff;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 8px;
}

.feature-item .el-icon {
  font-size: 20px;
}

.register-form-section {
  padding: 44px;
  display: flex;
  align-items: center;
  overflow-y: auto;
  background: #fff;
}

.form-container {
  width: 100%;
}

.form-header {
  margin-bottom: 24px;
}

.form-header h2 {
  margin: 0 0 8px;
  color: #102a43;
  font-size: 28px;
}

.form-header p,
.form-footer,
.strength-text {
  color: #627d98;
}

.email-code-row {
  width: 100%;
  display: flex;
  gap: 12px;
}

.email-code-input {
  flex: 1;
}

.send-code-btn {
  min-width: 130px;
}

.password-strength {
  width: 100%;
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.password-strength .el-progress {
  flex: 1;
}

.strength-text {
  font-size: 13px;
}

.register-btn {
  width: 100%;
  font-weight: 700;
}

.form-footer {
  display: flex;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
}

.login-link {
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

@media (max-width: 980px) {
  .register-container {
    grid-template-columns: 1fr;
    max-width: 640px;
  }

  .register-visual {
    display: none;
  }
}

@media (max-width: 560px) {
  .register-page {
    padding: 72px 16px 24px;
  }

  .register-form-section {
    padding: 30px 22px;
  }

  .email-code-row {
    flex-direction: column;
  }

  .send-code-btn {
    width: 100%;
  }
}
</style>
