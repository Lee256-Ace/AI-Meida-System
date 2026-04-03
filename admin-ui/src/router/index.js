import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import Layout from '@/layout/Layout.vue'
// 生成历史页静态导入，避免动态 import 在部分环境下 Failed to fetch（History 已不依赖 markdown，不会拖垮首屏）
import History from '@/views/History.vue'

const routes = [
  {
    path: '/dashboard',
    redirect: '/'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },

  {
    path: '/',
    component: () => import('@/layout/Layout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { requiresAdmin: true }
      },
      {
        path: 'dashboard',
        name: 'DashboardAlt',
        component: () => import('@/views/Dashboard.vue'),
        meta: { requiresAdmin: true }
      },
      // AI 文案生成模块（扁平为根的直接子路由，保证 Layout 的 router-view 能渲染）
      {
        path: 'ai-writing/generate',
        name: 'AiGenerate',
        component: () => import('@/views/Writing.vue'),
        meta: { title: '文案生成', icon: 'EditPen' }
      },
      {
        path: 'ai-writing/polish',
        name: 'AiPolish',
        component: () => import('@/views/Polish.vue'),
        meta: { title: '文章美化', icon: 'Brush' }
      },
      {
        path: 'ai-writing/continue',
        name: 'AiContinue',
        component: () => import('@/views/Continue.vue'),
        meta: { title: '文章续写', icon: 'DocumentAdd' }
      },
      {
        path: 'ai-writing/history',
        name: 'AiHistory',
        component: History,
        meta: { title: '生成历史', icon: 'Clock' }
      },
      {
        path: 'articlePlatform',
        name: 'ArticlePlatformManagement',
        component: () => import('@/views/ArticlePlatformManagement.vue'),
        meta: { title: '文章平台管理' }
      },
      {
        path: 'articleNews',
        name: 'ArticleNewsManagement',
        component: () => import('@/views/ArticleNewsManagement.vue'),
        meta: { title: '网络热点素材' }
      },
      {
          path: 'markdownEdit',
          name: 'MarkdownEditor',
          component: () => import('@/views/MarkdownEditor.vue'),
          meta: { title: '生成历史', icon: 'Clock' }
      },
      {
        path: 'user-management',
        name: 'UserManagement',
        component: () => import('@/views/UserManagement.vue'),
        meta: { requiresAdmin: true, title: '用户管理' }
      },
      {
        path: 'user-center',
        name: 'UserCenter',
        component: () => import('@/views/PersonalCenter.vue'),
        meta: { title: '个人中心' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Navigation guard
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  // 检查token有效性
  const isTokenValid = userStore.checkTokenValidity()
  
  // 如果访问登录页面且已经登录，重定向到首页
  if (to.path === '/login' && isTokenValid) {
    next('/')
    return
  }
  
  // 检查是否需要登录认证
  if (to.meta.requiresAuth !== false && !isTokenValid) {
    next('/login')
    return
  }

  // 非管理员不应落在仅管理员可看的首页（仪表盘），改去 AI 文案生成
  if (isTokenValid && (to.path === '/' || to.path === '/dashboard')) {
    const ut = userStore.userInfo?.userType
    if (ut && ut !== 'admin') {
      next({ path: '/ai-writing/generate', replace: true })
      return
    }
  }
  
  // 检查管理员权限（已登录但非管理员：送回用户可用首页，避免误进管理页）
  if (to.meta.requiresAdmin && userStore.userInfo?.userType !== 'admin') {
    next({ path: '/ai-writing/generate', replace: true })
    return
  }

  next()
})

export default router