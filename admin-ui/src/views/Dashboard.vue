<template>
  <div class="dashboard page-enter-active">
    <!-- 欢迎信息 -->
    <div class="welcome-section card-enter-active">
      <h2>您好！管理员，欢迎使用系统管理后台！</h2>
      <p class="welcome-desc">这是一个基础的管理系统框架，您可以在此基础上扩展更多功能。</p>
    </div>
    
    <!-- 系统信息卡片 -->
    <el-row :gutter="20" class="info-row">
      <el-col :span="8">
        <el-card class="info-card hover-lift card-enter-active">
          <div class="info-content">
            <div class="info-icon system">
              <el-icon><Monitor /></el-icon>
            </div>
            <div class="info-text">
              <div class="info-title">系统状态</div>
              <div class="info-value">运行正常</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card class="info-card">
          <div class="info-content">
            <div class="info-icon version">
              <el-icon><Setting /></el-icon>
            </div>
            <div class="info-text">
              <div class="info-title">系统版本</div>
              <div class="info-value">v1.0.0</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card class="info-card">
          <div class="info-content">
            <div class="info-icon time">
              <el-icon><Clock /></el-icon>
            </div>
            <div class="info-text">
              <div class="info-title">当前时间</div>
              <div class="info-value">{{ currentTime }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { Monitor, Setting, Clock } from '@element-plus/icons-vue'

// 当前时间
const currentTime = ref('')

// 更新时间
const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN')
}

let timeInterval = null

onMounted(() => {
  updateTime()
  timeInterval = setInterval(updateTime, 1000)
})

onUnmounted(() => {
  if (timeInterval) {
    clearInterval(timeInterval)
  }
})
</script>

<style scoped>
.dashboard {
  padding: 24px;
  animation: fadeIn 0.6s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.welcome-section {
  margin-bottom: 36px;
  text-align: center;
  background: var(--card-background);
  padding: 32px;
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow-light);
  border: 1px solid rgba(24, 144, 255, 0.1);
  position: relative;
  overflow: hidden;
}

.welcome-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: var(--primary-gradient);
}

.welcome-section h2 {
  color: var(--text-primary);
  font-size: 28px;
  font-weight: 600;
  margin: 0 0 12px 0;
  background: var(--primary-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.welcome-desc {
  color: var(--text-secondary);
  font-size: 16px;
  margin: 0;
  line-height: 1.6;
}

.info-row {
  margin-bottom: 36px;
}

.info-card {
  height: 140px;
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow-light);
  transition: all 0.3s ease;
  border: 1px solid rgba(0, 0, 0, 0.06);
  overflow: hidden;
  position: relative;
}

.info-card:hover {
  box-shadow: var(--shadow-medium);
  transform: translateY(-4px);
}

.info-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: var(--primary-gradient);
}

.info-content {
  display: flex;
  align-items: center;
  height: 100%;
  padding: 24px;
}

.info-icon {
  width: 64px;
  height: 64px;
  border-radius: var(--border-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20px;
  font-size: 28px;
  color: #fff;
  position: relative;
  overflow: hidden;
}

.info-icon::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(45deg, rgba(255, 255, 255, 0.2), transparent);
}

.info-icon.system {
  background: linear-gradient(135deg, var(--success-color) 0%, #73d13d 100%);
}

.info-icon.version {
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-light) 100%);
}

.info-icon.time {
  background: linear-gradient(135deg, var(--secondary-color) 0%, #b37feb 100%);
}

.info-text {
  flex: 1;
}

.info-title {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 8px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.info-value {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.2;
}

.feature-card {
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow-light);
  border: 1px solid rgba(0, 0, 0, 0.06);
  overflow: hidden;
  transition: all 0.3s ease;
}

.feature-card:hover {
  box-shadow: var(--shadow-medium);
}

.feature-content {
  padding: 24px;
}

.feature-item {
  padding: 20px;
  background: rgba(24, 144, 255, 0.02);
  border-radius: var(--border-radius-md);
  border: 1px solid rgba(24, 144, 255, 0.1);
  transition: all 0.3s ease;
}

.feature-item:hover {
  background: rgba(24, 144, 255, 0.05);
  transform: translateY(-2px);
}

.feature-item h4 {
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 16px 0;
  display: flex;
  align-items: center;
}

.feature-item h4::before {
  content: '';
  width: 4px;
  height: 20px;
  background: var(--primary-gradient);
  border-radius: 2px;
  margin-right: 12px;
}

.feature-item ul {
  margin: 0;
  padding: 0;
  list-style: none;
}

.feature-item li {
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 8px;
  padding-left: 20px;
  position: relative;
}

.feature-item li::before {
  content: '✓';
  position: absolute;
  left: 0;
  color: var(--success-color);
  font-weight: bold;
}

.feature-item li:last-child {
  margin-bottom: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: rgba(24, 144, 255, 0.02);
  border-bottom: 1px solid rgba(24, 144, 255, 0.1);
}

.card-header span {
  font-weight: 600;
  color: var(--text-primary);
  font-size: 18px;
}

:deep(.el-card__header) {
  background: rgba(24, 144, 255, 0.02);
  border-bottom: 1px solid rgba(24, 144, 255, 0.1);
}

:deep(.el-card__body) {
  padding: 0;
}
</style>