<template>
  <el-container class="layout-container">
    <el-aside width="250px" class="sidebar">
      <div class="logo card-enter-active">
        <h2>自媒体文案智能生成器</h2>
      </div>
      
      <!-- 管理员导航 -->
      <el-menu
        v-if="userStore.isAdmin"
        :default-active="$route.path"
        router
        background-color="#8B4513"
        text-color="#fff"
        active-text-color="#FFA500"
      >
        <el-menu-item index="/" class="hover-scale">
          <el-icon><DataBoard /></el-icon>
          <span>系统首页</span>
        </el-menu-item>

        <el-sub-menu index="/ai-writing">
          <template #title>
            <el-icon><MagicStick /></el-icon>
            <span>AI 文案生成</span>
          </template>

          <el-menu-item index="/ai-writing/generate">
            <el-icon><EditPen /></el-icon>
            <span>文案生成</span>
          </el-menu-item>

          <el-menu-item index="/ai-writing/polish">
            <el-icon><Brush /></el-icon>
            <span>文章美化</span>
          </el-menu-item>

          <!-- 确认有这个菜单项 -->
          <el-menu-item index="/ai-writing/continue">
            <el-icon><DocumentAdd /></el-icon>
            <span>文章续写</span>
          </el-menu-item>

          <el-menu-item index="/ai-writing/history">
            <el-icon><Clock /></el-icon>
            <span>生成历史</span>
          </el-menu-item>
        </el-sub-menu>



<!--        <el-menu-item index="/merchant">-->
<!--          <el-icon><Document /></el-icon>-->
<!--          <span>商家管理</span>-->
<!--        </el-menu-item>-->

        <el-menu-item index="/articlePlatform">
          <el-icon><Document /></el-icon>
          <span>文章平台管理</span>
        </el-menu-item>

        <el-menu-item index="/articleNews">
          <el-icon><Document /></el-icon>
          <span>网络热点素材</span>
        </el-menu-item>

        <el-menu-item index="/user-management">
          <el-icon><UserFilled /></el-icon>
          <span>用户管理</span>
        </el-menu-item>

        <el-menu-item index="/markdownEdit">
          <el-icon><Document /></el-icon>
          <span>编辑文案</span>
        </el-menu-item>
      </el-menu>

      <!-- 普通用户 / 商家：不包含系统管理类菜单 -->
      <el-menu
        v-else
        :default-active="$route.path"
        router
        background-color="#8B4513"
        text-color="#fff"
        active-text-color="#FFA500"
      >
        <el-sub-menu index="/ai-writing">
          <template #title>
            <el-icon><MagicStick /></el-icon>
            <span>AI 文案生成</span>
          </template>
          <el-menu-item index="/ai-writing/generate">
            <el-icon><EditPen /></el-icon>
            <span>文案生成</span>
          </el-menu-item>
          <el-menu-item index="/ai-writing/polish">
            <el-icon><Brush /></el-icon>
            <span>文章美化</span>
          </el-menu-item>
          <el-menu-item index="/ai-writing/continue">
            <el-icon><DocumentAdd /></el-icon>
            <span>文章续写</span>
          </el-menu-item>
          <el-menu-item index="/ai-writing/history">
            <el-icon><Clock /></el-icon>
            <span>生成历史</span>
          </el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/articlePlatform">
          <el-icon><Document /></el-icon>
          <span>文章平台管理</span>
        </el-menu-item>
        <el-menu-item index="/articleNews">
          <el-icon><Document /></el-icon>
          <span>网络热点素材</span>
        </el-menu-item>
        <el-menu-item index="/markdownEdit">
          <el-icon><Document /></el-icon>
          <span>编辑文案</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ getCurrentPageTitle() }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              {{ userStore.userInfo?.username }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="personal">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import {
  DataBoard,
  ArrowDown,
  MagicStick,
  Brush,
  DocumentAdd,
  EditPen,
  Clock,
  UserFilled,
  Document
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const getCurrentPageTitle = () => {
  const routeMap = {
    '/': '系统首页',
    '/statistics': '数据统计',
    '/ai-writing/generate': '文案生成',
    '/ai-writing/polish': '文章美化',
    '/ai-writing/continue': '文章续写',
    '/ai-writing/history': '生成历史',
    '/user-management': '用户管理',
    '/user-center': '个人中心',
    '/markdownEdit': '编辑文案',
    '/articlePlatform': '文章平台管理',
    '/articleNews': '网络热点素材'
  }
  return routeMap[router.currentRoute.value.path] || '页面'
}

const handleCommand = async (command) => {
  if (command === 'personal') {
    router.push('/user-center')
    return
  }
  if (command === 'logout') {
    await userStore.logoutAction()
    ElMessage.success('退出登录成功')
    router.push('/login')
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  background: var(--background-color);
}

.sidebar {
  background: var(--primary-gradient);
  color: #fff;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
  position: relative;
  overflow: hidden;
}

.sidebar::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0.05) 100%);
  pointer-events: none;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  position: relative;
  z-index: 1;
}

.logo h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  letter-spacing: 0.5px;
}

.header {
  background: var(--card-background);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  color: var(--text-primary);
  box-shadow: var(--shadow-light);
  backdrop-filter: blur(10px);
  position: relative;
  z-index: 10;
}

.header-left {
  flex: 1;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: var(--text-primary);
  font-weight: 500;
  padding: 8px 16px;
  border-radius: var(--border-radius-md);
  transition: all 0.3s ease;
  background: rgba(24, 144, 255, 0.05);
}

.user-info:hover {
  color: var(--primary-color);
  background: rgba(24, 144, 255, 0.1);
  transform: translateY(-1px);
}

.main-content {
  background: var(--background-color);
  padding: 24px;
  overflow-y: auto;
  position: relative;
}

:deep(.el-menu) {
  border-right: none;
  background: transparent !important;
  position: relative;
  z-index: 1;
}

:deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.9) !important;
  margin: 4px 12px;
  border-radius: var(--border-radius-md) !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
  position: relative;
  overflow: hidden;
}

:deep(.el-menu-item::before) {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s ease;
}

:deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.15) !important;
  color: #fff !important;
  transform: translateX(4px);
}

:deep(.el-menu-item:hover::before) {
  left: 100%;
}

:deep(.el-menu-item.is-active) {
  background: rgba(255, 255, 255, 0.2) !important;
  color: #fff !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transform: translateX(4px);
}

:deep(.el-menu-item.is-active::after) {
  content: '';
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  background: #fff;
  border-radius: 2px;
}

:deep(.el-sub-menu__title) {
  color: rgba(255, 255, 255, 0.9) !important;
  margin: 4px 12px;
  border-radius: var(--border-radius-md) !important;
  transition: all 0.3s ease !important;
}

:deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.15) !important;
  color: #fff !important;
  transform: translateX(4px);
}

:deep(.el-sub-menu.is-active .el-sub-menu__title) {
  color: #fff !important;
  background: rgba(255, 255, 255, 0.2) !important;
}

:deep(.el-breadcrumb__item) {
  font-weight: 500;
}

:deep(.el-breadcrumb__inner) {
  color: var(--text-secondary) !important;
}

:deep(.el-breadcrumb__inner.is-link) {
  color: var(--primary-color) !important;
}

:deep(.el-breadcrumb__inner.is-link:hover) {
  color: var(--primary-dark) !important;
}
</style>