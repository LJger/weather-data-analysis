<template>
  <div class="profile-page">
    <!-- 顶部用户概览 -->
    <div class="profile-banner">
      <div class="banner-bg"></div>
      <div class="banner-content">
        <div class="banner-avatar-wrapper">
          <el-avatar :size="96" :src="userInfo.avatar" class="banner-avatar">
            <el-icon :size="40"><User /></el-icon>
          </el-avatar>
          <el-upload
            v-if="editMode"
            class="avatar-uploader-overlay"
            action="#"
            :show-file-list="false"
            :before-upload="beforeAvatarUpload"
          >
            <div class="avatar-edit-mask">
              <el-icon :size="20"><Camera /></el-icon>
            </div>
          </el-upload>
        </div>
        <div class="banner-info">
          <h2 class="banner-username">{{ userInfo.username || '未设置' }}</h2>
          <div class="banner-meta">
            <el-tag :type="userInfo.role === 1 ? 'danger' : 'info'" effect="dark" round size="small">
              {{ userInfo.role === 1 ? '管理员' : '普通用户' }}
            </el-tag>
            <span class="banner-date" v-if="userInfo.createdAt">
              <el-icon><Calendar /></el-icon>
              注册于 {{ formatDate(userInfo.createdAt) }}
            </span>
          </div>
          <p class="banner-bio">{{ userInfo.bio || '这个人很懒，什么都没写~' }}</p>
        </div>
        <div class="banner-actions">
          <el-button
            :type="editMode ? 'default' : 'primary'"
            @click="editMode = !editMode"
            round
          >
            <el-icon><Edit v-if="!editMode" /><Close v-else /></el-icon>
            {{ editMode ? '取消编辑' : '编辑资料' }}
          </el-button>
        </div>
      </div>
    </div>

    <!-- 信息表单区域 -->
    <div class="profile-body" v-loading="loading">
      <el-row :gutter="20">
        <!-- 左侧：基本信息 -->
        <el-col :xs="24" :lg="14">
          <el-card class="info-card" shadow="never">
            <template #header>
              <div class="card-title">
                <el-icon class="card-title-icon"><UserFilled /></el-icon>
                基本信息
              </div>
            </template>
            <el-form
              :model="userInfo"
              :rules="rules"
              ref="profileFormRef"
              label-position="top"
              class="profile-form"
            >
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="用户名" prop="username">
                    <el-input
                      v-model="userInfo.username"
                      :disabled="!editMode"
                      prefix-icon="User"
                      placeholder="请输入用户名"
                    />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="邮箱" prop="email">
                    <el-input
                      v-model="userInfo.email"
                      :disabled="!editMode"
                      prefix-icon="Message"
                      placeholder="请输入邮箱"
                    />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="手机号" prop="phone">
                    <el-input
                      v-model="userInfo.phone"
                      :disabled="!editMode"
                      prefix-icon="Phone"
                      placeholder="请输入手机号"
                    />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="年龄" prop="age">
                    <el-input-number
                      v-model="userInfo.age"
                      :min="1"
                      :max="120"
                      :disabled="!editMode"
                      style="width: 100%"
                      placeholder="请输入年龄"
                    />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="性别" prop="gender">
                    <el-radio-group v-model="userInfo.gender" :disabled="!editMode">
                      <el-radio-button value="male">男</el-radio-button>
                      <el-radio-button value="female">女</el-radio-button>
                      <el-radio-button value="other">其他</el-radio-button>
                    </el-radio-group>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="地区" prop="region">
                    <el-cascader
                      v-model="userInfo.region"
                      :options="regionOptions"
                      :disabled="!editMode"
                      clearable
                      style="width: 100%"
                      placeholder="请选择地区"
                    />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="所属机构" prop="organization">
                <el-input
                  v-model="userInfo.organization"
                  :disabled="!editMode"
                  prefix-icon="OfficeBuilding"
                  placeholder="请输入所属机构"
                />
              </el-form-item>
              <el-form-item label="个人简介" prop="bio">
                <el-input
                  v-model="userInfo.bio"
                  type="textarea"
                  :rows="3"
                  :disabled="!editMode"
                  placeholder="介绍一下自己吧..."
                  :maxlength="200"
                  show-word-limit
                />
              </el-form-item>
              <el-form-item v-if="editMode" class="form-actions">
                <el-button type="primary" @click="saveProfile" :loading="saving" round>
                  <el-icon><Check /></el-icon>保存修改
                </el-button>
                <el-button @click="resetForm" round>重置</el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>

        <!-- 右侧：账户安全 -->
        <el-col :xs="24" :lg="10">
          <el-card class="security-card" shadow="never">
            <template #header>
              <div class="card-title">
                <el-icon class="card-title-icon"><Lock /></el-icon>
                账户安全
              </div>
            </template>
            <div class="security-list">
              <div class="security-item" @click="showPasswordDialog = true">
                <div class="security-item-icon password-icon">
                  <el-icon :size="20"><Key /></el-icon>
                </div>
                <div class="security-item-content">
                  <h4>修改密码</h4>
                  <p>定期更新密码以确保账户安全</p>
                </div>
                <el-icon class="security-item-arrow"><ArrowRight /></el-icon>
              </div>
              <div class="security-item" @click="showLoginHistory">
                <div class="security-item-icon history-icon">
                  <el-icon :size="20"><Clock /></el-icon>
                </div>
                <div class="security-item-content">
                  <h4>登录记录</h4>
                  <p>查看最近的登录活动</p>
                </div>
                <el-icon class="security-item-arrow"><ArrowRight /></el-icon>
              </div>
              <div class="security-item">
                <div class="security-item-icon status-icon">
                  <el-icon :size="20"><CircleCheck /></el-icon>
                </div>
                <div class="security-item-content">
                  <h4>账户状态</h4>
                  <p>
                    <el-tag type="success" size="small" effect="plain" round>正常</el-tag>
                  </p>
                </div>
              </div>
              <el-divider />
              <div class="security-item danger-item" @click="showDeleteDialog = true">
                <div class="security-item-icon danger-icon">
                  <el-icon :size="20"><Delete /></el-icon>
                </div>
                <div class="security-item-content">
                  <h4>注销账户</h4>
                  <p>永久删除账户及所有关联数据</p>
                </div>
                <el-icon class="security-item-arrow"><ArrowRight /></el-icon>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 修改密码对话框 -->
    <el-dialog v-model="showPasswordDialog" title="修改密码" width="420px" :close-on-click-modal="false">
      <el-form
        :model="passwordForm"
        :rules="passwordRules"
        ref="passwordFormRef"
        label-position="top"
      >
        <el-form-item label="当前密码" prop="currentPassword">
          <el-input v-model="passwordForm.currentPassword" type="password" show-password prefix-icon="Lock" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password prefix-icon="Key" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password prefix-icon="Key" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPasswordDialog = false" round>取消</el-button>
        <el-button type="primary" @click="changePassword" :loading="changingPassword" round>
          确认修改
        </el-button>
      </template>
    </el-dialog>

    <!-- 登录记录对话框 -->
    <el-dialog v-model="showLoginHistoryDialog" title="登录记录" width="640px">
      <el-table :data="loginHistory" style="width: 100%" stripe>
        <el-table-column prop="time" label="登录时间" width="180" />
        <el-table-column prop="ip" label="IP地址" width="130" />
        <el-table-column prop="location" label="登录地点" />
        <el-table-column prop="device" label="设备信息" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === '成功' ? 'success' : 'danger'" size="small" round>
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 注销账户确认对话框 -->
    <el-dialog v-model="showDeleteDialog" title="注销账户" width="420px">
      <div class="delete-warning">
        <div class="warning-icon-wrapper">
          <el-icon class="warning-icon"><Warning /></el-icon>
        </div>
        <h4>此操作不可撤销</h4>
        <p>注销账户将永久删除您的所有数据，包括：</p>
        <ul>
          <li>个人信息与偏好设置</li>
          <li>采集任务与历史记录</li>
          <li>分析报告与预测结果</li>
        </ul>
      </div>
      <template #footer>
        <el-button @click="showDeleteDialog = false" round>取消</el-button>
        <el-button type="danger" @click="deleteAccount" :loading="deletingAccount" round>
          确认注销
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  User,
  Camera,
  Calendar,
  Edit,
  Close,
  Check,
  UserFilled,
  Lock,
  Key,
  Clock,
  CircleCheck,
  Delete,
  ArrowRight,
  Warning,
} from '@element-plus/icons-vue'
import http from '@/api/http'
import { useRouter } from 'vue-router'
import { regionOptions } from '@/data/regionData'

const router = useRouter()

// 响应式数据
const editMode = ref(false)
const saving = ref(false)
const showPasswordDialog = ref(false)
const showLoginHistoryDialog = ref(false)
const showDeleteDialog = ref(false)
const changingPassword = ref(false)
const deletingAccount = ref(false)
const loading = ref(false)

const profileFormRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()

// 用户信息（从后端加载）
const userInfo = reactive({
  username: '',
  email: '',
  phone: '',
  gender: 'unknown' as string,
  age: undefined as number | undefined,
  region: [] as string[],
  organization: '',
  bio: '',
  avatar: '',
  role: 0 as number,
  createdAt: '',
})

// gender 在后端存为 Short(0/1/2)，前端显示为字符串
const genderMap: Record<number, string> = { 0: 'unknown', 1: 'male', 2: 'female' }
const genderReverseMap: Record<string, number> = { unknown: 0, male: 1, female: 2, other: 0 }

// 日期格式化
const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
}

// 从后端加载用户资料
const loadProfile = async () => {
  const userId = localStorage.getItem('userId')
  if (!userId) return

  loading.value = true
  try {
    const { data } = await http.get('/api/auth/profile', { params: { userId } })
    userInfo.username = data.username || ''
    userInfo.email = data.email || ''
    userInfo.phone = data.phone || ''
    userInfo.gender = genderMap[data.gender ?? 0] || 'unknown'
    userInfo.age = data.age ?? undefined
    userInfo.organization = data.organization || ''
    userInfo.bio = data.bio || ''
    userInfo.role = data.role ?? 0
    userInfo.createdAt = data.createdAt || ''
    if (data.avatarUrl) {
      userInfo.avatar = (http.defaults.baseURL || 'http://localhost:8080') + data.avatarUrl
    }
    if (data.region) {
      userInfo.region = data.region.split('/').filter(Boolean)
    } else {
      userInfo.region = []
    }
  } catch (error) {
    console.error('加载用户信息失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadProfile()
})

// 密码表单
const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

// 地区选项（从外部数据文件导入）

// 登录记录
const loginHistory = ref([
  { time: '2024-01-15 14:30:25', ip: '192.168.1.100', location: '北京市朝阳区', device: 'Chrome 浏览器', status: '成功' },
  { time: '2024-01-14 09:15:42', ip: '192.168.1.100', location: '北京市朝阳区', device: 'Chrome 浏览器', status: '成功' },
  { time: '2024-01-13 16:45:18', ip: '192.168.1.100', location: '北京市朝阳区', device: 'Chrome 浏览器', status: '成功' },
])

// 表单验证规则
const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }],
}

const passwordRules: FormRules = {
  currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

// 头像上传处理
const beforeAvatarUpload = async (file: File) => {
  const isImage = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/gif'
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isImage) {
    ElMessage.error('头像图片只能是 JPG/PNG/GIF 格式!')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('头像图片大小不能超过 5MB!')
    return false
  }

  const userId = localStorage.getItem('userId')
  if (!userId) {
    ElMessage.error('用户未登录')
    return false
  }

  // 上传到后端
  const formData = new FormData()
  formData.append('file', file)
  try {
    const { data } = await http.post(`/api/auth/avatar?userId=${userId}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    if (data.success) {
      // 使用后端返回的 URL（拼接 baseURL）
      userInfo.avatar = (http.defaults.baseURL || 'http://localhost:8080') + data.avatarUrl
      // 通知 Layout 顶部头像同步更新
      window.dispatchEvent(new CustomEvent('avatar-updated', { detail: { avatarUrl: userInfo.avatar } }))
      ElMessage.success('头像上传成功')
    } else {
      ElMessage.error(data.message || '头像上传失败')
    }
  } catch (error) {
    console.error('头像上传失败:', error)
    ElMessage.error('头像上传失败，请稍后重试')
  }
  return false
}

// 保存个人信息
const saveProfile = async () => {
  if (!profileFormRef.value) return
  await profileFormRef.value.validate(async (valid) => {
    if (valid) {
      saving.value = true
      try {
        const userId = localStorage.getItem('userId')
        const payload = {
          email: userInfo.email || '',
          phone: userInfo.phone || '',
          gender: genderReverseMap[userInfo.gender] ?? 0,
          age: userInfo.age ?? null,
          region: userInfo.region.length > 0 ? userInfo.region.join('/') : '',
          organization: userInfo.organization || '',
          bio: userInfo.bio || '',
        }
        await http.put('/api/auth/profile', payload, { params: { userId } })
        ElMessage.success('个人信息保存成功')
        editMode.value = false
        localStorage.setItem('username', userInfo.username)
      } catch (error) {
        ElMessage.error('保存失败，请重试')
      } finally {
        saving.value = false
      }
    }
  })
}

const resetForm = () => {
  profileFormRef.value?.resetFields()
}

const changePassword = async () => {
  if (!passwordFormRef.value) return
  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      changingPassword.value = true
      try {
        const { data } = await http.put('/api/auth/password', {
          oldPassword: passwordForm.currentPassword,
          newPassword: passwordForm.newPassword,
        })
        if (data.success) {
          ElMessage.success('密码修改成功')
          showPasswordDialog.value = false
          passwordForm.currentPassword = ''
          passwordForm.newPassword = ''
          passwordForm.confirmPassword = ''
        } else {
          ElMessage.error(data.message || '密码修改失败')
        }
      } catch (error: any) {
        ElMessage.error(error?.response?.data?.message || '密码修改失败，请重试')
      } finally {
        changingPassword.value = false
      }
    }
  })
}

const showLoginHistory = () => {
  showLoginHistoryDialog.value = true
}

const deleteAccount = async () => {
  try {
    await ElMessageBox.confirm(
      '注销账户后，您的所有数据将被永久删除且无法恢复。确定要继续吗？',
      '最终确认',
      {
        confirmButtonText: '确定注销',
        cancelButtonText: '取消',
        type: 'error',
        confirmButtonClass: 'el-button--danger',
      },
    )
    deletingAccount.value = true
    const userId = localStorage.getItem('userId')
    if (!userId) {
      ElMessage.error('未获取到用户信息，请重新登录')
      return
    }
    const res = await http.delete(`/api/auth/account?userId=${userId}`)
    if (res.data.success) {
      ElMessage.success('账户已成功注销')
      // 清除本地存储并跳转到登录页
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      localStorage.removeItem('username')
      localStorage.removeItem('role')
      router.push('/login')
    } else {
      ElMessage.error(res.data.message || '注销失败')
    }
  } catch (error: any) {
    if (error !== 'cancel' && error?.toString() !== 'cancel') {
      ElMessage.error('注销请求失败，请稍后重试')
    }
  } finally {
    deletingAccount.value = false
    showDeleteDialog.value = false
  }
}
</script>

<style scoped>
.profile-page {
  max-width: 1100px;
  margin: 0 auto;
}

/* ====== 顶部 Banner ====== */
.profile-banner {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 20px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  box-shadow: 0 1px 4px rgba(25, 55, 90, 0.06);
}

.banner-bg {
  height: 100px;
  background: var(--el-color-primary-light-9);
  border-bottom: 1px solid var(--el-border-color-light);
}

.banner-content {
  display: flex;
  align-items: flex-end;
  gap: 20px;
  padding: 0 28px 24px;
  margin-top: -48px;
  position: relative;
}

.banner-avatar-wrapper {
  position: relative;
  flex-shrink: 0;
}

.banner-avatar {
  border: 4px solid var(--el-bg-color);
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  box-shadow: 0 2px 8px rgba(25, 55, 90, 0.12);
}

.avatar-uploader-overlay {
  position: absolute;
  inset: 0;
}

.avatar-edit-mask {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.25s;
}

.banner-avatar-wrapper:hover .avatar-edit-mask {
  opacity: 1;
}

.banner-info {
  flex: 1;
  padding-bottom: 2px;
}

.banner-username {
  margin: 0 0 6px 0;
  font-size: 1.4rem;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.banner-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}

.banner-date {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 0.8rem;
  color: var(--el-text-color-secondary);
}

.banner-bio {
  margin: 0;
  font-size: 0.85rem;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
  max-width: 480px;
}

.banner-actions {
  flex-shrink: 0;
  padding-bottom: 4px;
}

/* ====== 信息表单区 ====== */
.profile-body {
  margin-bottom: 24px;
}

.info-card,
.security-card {
  border-radius: 14px;
  border: 1px solid var(--el-border-color-lighter);
  margin-bottom: 20px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 1rem;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.card-title-icon {
  font-size: 18px;
  color: var(--el-color-primary);
}

.profile-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: var(--el-text-color-regular);
}

.form-actions {
  margin-top: 8px;
  margin-bottom: 0;
}

/* ====== 账户安全列表 ====== */
.security-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.security-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s;
}

.security-item:hover {
  background: var(--el-fill-color-light);
}

.security-item-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.password-icon {
  background: rgba(64, 158, 255, 0.1);
  color: var(--el-color-primary);
}

.history-icon {
  background: rgba(103, 194, 58, 0.1);
  color: var(--el-color-success);
}

.status-icon {
  background: rgba(103, 194, 58, 0.1);
  color: var(--el-color-success);
}

.danger-icon {
  background: rgba(245, 108, 108, 0.1);
  color: var(--el-color-danger);
}

.security-item-content {
  flex: 1;
}

.security-item-content h4 {
  margin: 0 0 2px 0;
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.security-item-content p {
  margin: 0;
  font-size: 0.78rem;
  color: var(--el-text-color-secondary);
}

.danger-item .security-item-content h4 {
  color: var(--el-color-danger);
}

.security-item-arrow {
  color: var(--el-text-color-placeholder);
  font-size: 14px;
}

/* ====== 注销警告 ====== */
.delete-warning {
  text-align: center;
  padding: 10px 0;
}

.warning-icon-wrapper {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  border-radius: 50%;
  background: rgba(245, 108, 108, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
}

.warning-icon {
  font-size: 32px;
  color: var(--el-color-danger);
}

.delete-warning h4 {
  color: var(--el-color-danger);
  margin: 0 0 10px 0;
  font-size: 1.05rem;
}

.delete-warning p {
  color: var(--el-text-color-secondary);
  font-size: 0.85rem;
  margin: 0 0 8px 0;
}

.delete-warning ul {
  text-align: left;
  max-width: 280px;
  margin: 0 auto;
  padding-left: 20px;
}

.delete-warning li {
  margin-bottom: 4px;
  color: var(--el-text-color-secondary);
  font-size: 0.85rem;
}

/* 响应式 */
@media (max-width: 768px) {
  .banner-content {
    flex-direction: column;
    align-items: center;
    text-align: center;
    padding: 0 16px 20px;
    margin-top: -40px;
  }

  .banner-meta {
    justify-content: center;
  }

  .banner-bio {
    max-width: 100%;
  }

  .security-item {
    padding: 12px 8px;
  }
}
</style>
