<template>
  <div class="login-container">
    <div class="login-box card-enter-active">
      <div class="login-header">
        <h2>自媒体文案智能生成器</h2>
        <p>管理后台登录</p>
      </div>
      
      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        @keyup.enter="handleLogin"
      >
        <!-- <el-form-item prop="userType">
          <el-radio-group v-model="loginForm.userType" class="user-type-group">
            <el-radio value="admin">管理员</el-radio>
          </el-radio-group>
        </el-form-item> -->
        
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            size="large"
            prefix-icon="User"
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-button btn-ripple hover-scale"
            :loading="loading"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="login-tips">
        <p>管理员：admin / admin123</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loginFormRef = ref()
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: '',
  userType: 'admin'
})

const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  userType: [
    { required: true, message: '请选择用户类型', trigger: 'change' }
  ]
}

const handleLogin = async () => {
  try {
    await loginFormRef.value.validate()
    loading.value = true
    
    const result = await userStore.loginAction(loginForm)
    
    if (result.success) {
      ElMessage.success('登录成功')
      if (loginForm.userType === 'user') {
        window.location.href = 'http://localhost:5174/home'
      } else {
        router.push('/')
      }
    } else {
      ElMessage.error(result.message || '登录失败')
    }
  } catch (error) {
    console.error('登录失败:', error)
    ElMessage.error('登录失败，请检查输入信息')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  background: var(--primary-gradient);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.login-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><pattern id="grain" width="100" height="100" patternUnits="userSpaceOnUse"><circle cx="50" cy="50" r="1" fill="%23ffffff" opacity="0.1"/></pattern></defs><rect width="100" height="100" fill="url(%23grain)"/></svg>') repeat;
  animation: float 20s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-10px); }
}

.login-box {
  width: 420px;
  background: var(--card-background);
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow-heavy);
  padding: 48px;
  position: relative;
  z-index: 1;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  animation: slideUp 0.6s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-header {
  text-align: center;
  margin-bottom: 36px;
}

.login-header h2 {
  color: var(--text-primary);
  margin-bottom: 12px;
  font-size: 28px;
  font-weight: 600;
  background: var(--primary-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.login-header p {
  color: var(--text-secondary);
  font-size: 16px;
  font-weight: 400;
}

.login-form {
  margin-bottom: 24px;
}

.user-type-group {
  width: 100%;
  display: flex;
  justify-content: space-around;
  background: var(--background-color);
  padding: 12px;
  border-radius: var(--border-radius-md);
  margin-bottom: 8px;
}

.login-button {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
  background: var(--primary-gradient) !important;
  border: none !important;
  border-radius: var(--border-radius-md) !important;
  box-shadow: var(--shadow-medium);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.login-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  transition: left 0.5s ease;
}

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-heavy);
}

.login-button:hover::before {
  left: 100%;
}

.login-tips {
  text-align: center;
  color: var(--text-disabled);
  font-size: 13px;
  line-height: 1.6;
  background: rgba(24, 144, 255, 0.05);
  padding: 16px;
  border-radius: var(--border-radius-md);
  border: 1px solid rgba(24, 144, 255, 0.1);
}

.login-tips p {
  margin: 3px 0;
}

.register-link {
  text-align: center;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid var(--border-color);
}

.register-link p {
  color: var(--text-secondary);
  font-size: 14px;
  margin: 0;
}

.register-link .link {
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 500;
  transition: all 0.3s ease;
  padding: 4px 8px;
  border-radius: var(--border-radius-sm);
}

.register-link .link:hover {
  color: var(--primary-dark);
  background: rgba(24, 144, 255, 0.1);
  text-decoration: none;
}

:deep(.el-form-item__label) {
  color: var(--text-primary) !important;
  font-weight: 500;
}

:deep(.el-input__wrapper) {
  border-radius: var(--border-radius-md) !important;
  box-shadow: var(--shadow-light) !important;
  transition: all 0.3s ease !important;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: var(--shadow-medium) !important;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2) !important;
}

:deep(.el-radio) {
  margin-right: 24px;
  font-weight: 500;
}

:deep(.el-radio__input.is-checked .el-radio__inner) {
  background-color: var(--primary-color) !important;
  border-color: var(--primary-color) !important;
}

:deep(.el-radio__input.is-checked + .el-radio__label) {
  color: var(--primary-color) !important;
}
</style>