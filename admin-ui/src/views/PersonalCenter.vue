<template>
  <div class="app-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>个人中心</span>
        </div>
      </template>

      <div class="profile-row">
        <el-avatar :size="64" :src="userInfo.avatar" v-if="userInfo.avatar" />
        <el-avatar :size="64" v-else> {{ (userInfo.username || 'U').slice(0, 1) }} </el-avatar>

        <div class="profile-meta">
          <div class="meta-line">
            <span class="meta-label">用户名：</span>
            <span class="meta-value">{{ userInfo.username || '-' }}</span>
          </div>
          <div class="meta-line">
            <span class="meta-label">角色：</span>
            <span class="meta-value">{{ roleLabel }}</span>
          </div>
          <div class="meta-line">
            <span class="meta-label">用户ID：</span>
            <span class="meta-value">{{ userInfo.id || '-' }}</span>
          </div>
        </div>
      </div>

      <div class="actions-row">
        <el-button type="primary" @click="refreshUserInfo">刷新用户信息</el-button>
        <el-button type="danger" @click="logout">退出登录</el-button>
      </div>
    </el-card>

    <el-card class="register-card">
      <template #header>
        <div class="card-header">
          <span>注册新用户</span>
          <span class="card-sub">所有登录用户均可注册新的普通账号（写入用户表）；未登录用户请在登录页使用「注册普通用户」。</span>
        </div>
      </template>

      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        label-width="100px"
        class="register-form"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="3-20 位，全系统唯一" maxlength="20" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="6-20 位"
            maxlength="20"
            show-password
            clearable
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="password2">
          <el-input
            v-model="registerForm.password2"
            type="password"
            placeholder="再次输入密码"
            maxlength="20"
            show-password
            clearable
          />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="registerForm.realName" placeholder="选填" maxlength="50" clearable />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="registerForm.phone" placeholder="选填" maxlength="11" clearable />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="registerForm.email" placeholder="选填" maxlength="100" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="registerLoading" @click="submitRegister">提交注册</el-button>
          <el-button @click="resetRegisterForm">清空</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { registerNormalUser } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()

const userInfo = computed(() => userStore.userInfo || {})

const roleLabel = computed(() => {
  const t = userStore.userInfo?.userType
  if (t === 'admin') return '管理员'
  if (t === 'user') return '普通用户'
  return t || '-'
})

const refreshUserInfo = async () => {
  try {
    if (!userStore.userInfo?.userType || !userStore.userInfo?.id) {
      ElMessage.warning('当前用户信息不完整，无法刷新')
      return
    }
    await userStore.getUserInfoAction()
    ElMessage.success('用户信息已更新')
  } catch (e) {
    ElMessage.error('刷新失败')
  }
}

const logout = async () => {
  await userStore.logoutAction()
  ElMessage.success('退出登录成功')
  router.push('/login')
}

const registerFormRef = ref()
const registerLoading = ref(false)
const registerForm = reactive({
  username: '',
  password: '',
  password2: '',
  realName: '',
  phone: '',
  email: ''
})

const validatePass2 = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
  } else if (value !== registerForm.password) {
    callback(new Error('两次密码不一致'))
  } else {
    callback()
  }
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度必须在 3-20 个字符之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度必须在 6-20 个字符之间', trigger: 'blur' }
  ],
  password2: [{ required: true, validator: validatePass2, trigger: 'blur' }]
}

const resetRegisterForm = () => {
  registerForm.username = ''
  registerForm.password = ''
  registerForm.password2 = ''
  registerForm.realName = ''
  registerForm.phone = ''
  registerForm.email = ''
  registerFormRef.value?.resetFields()
}

const submitRegister = async () => {
  try {
    await registerFormRef.value.validate()
  } catch {
    return
  }
  registerLoading.value = true
  try {
    await registerNormalUser({
      username: registerForm.username.trim(),
      password: registerForm.password,
      realName: registerForm.realName?.trim() || undefined,
      phone: registerForm.phone?.trim() || undefined,
      email: registerForm.email?.trim() || undefined
    })
    ElMessage.success('新用户注册成功')
    resetRegisterForm()
  } catch {
    /* 错误提示由 axios 拦截器处理 */
  } finally {
    registerLoading.value = false
  }
}
</script>

<style scoped>
.app-container {
  padding: 24px;
}

.card-header {
  font-weight: 600;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.card-sub {
  font-weight: 400;
  font-size: 13px;
  color: #909399;
}

.register-card {
  margin-top: 20px;
}

.register-form {
  max-width: 520px;
}

.profile-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 0 16px;
}

.profile-meta {
  flex: 1;
}

.meta-line {
  margin: 8px 0;
  display: flex;
  gap: 8px;
  align-items: baseline;
}

.meta-label {
  color: #909399;
  min-width: 60px;
}

.meta-value {
  color: #303133;
  word-break: break-all;
}

.actions-row {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}
</style>
