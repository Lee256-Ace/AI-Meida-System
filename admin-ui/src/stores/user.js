import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, getUserInfo, logout } from '@/api/auth'

/** 仅当前标签页有效，关闭标签或新开站点后需重新登录（不再跨浏览器会话持久化） */
const authStorage = sessionStorage
const TOKEN_KEY = 'token'
const USER_INFO_KEY = 'userInfo'

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref(authStorage.getItem(TOKEN_KEY) || '')
  const userInfo = ref(JSON.parse(authStorage.getItem(USER_INFO_KEY) || '{}'))

  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value.userType === 'admin')

  // 方法
  const setToken = (newToken) => {
    token.value = newToken
    authStorage.setItem(TOKEN_KEY, newToken)
  }

  const setUserInfo = (info) => {
    userInfo.value = info
    authStorage.setItem(USER_INFO_KEY, JSON.stringify(info))
  }

  const loginAction = async (loginData) => {
      try {
          const result = await login(loginData) // result 就是 { token, userInfo }
          if (result && result.token) {
              setToken(result.token)          // 使用后端返回的真实 token
              setUserInfo(result.userInfo)
              return { success: true }
          } else {
              // 如果返回的数据中没有 token，说明后端格式可能仍有问题
              return { success: false, message: result?.message || '登录失败：返回数据格式错误' }
          }
      } catch (error) {
          // 这里捕获的是请求失败或业务错误（如密码错误，拦截器会 reject）
          console.error('登录异常:', error)
          return { success: false, message: error.message }
      }
  }

  const getUserInfoAction = async () => {
    try {
      const response = await getUserInfo(userInfo.value.userType, userInfo.value.id)
      if (response.code === 200) {
        setUserInfo(response.data)
        return { success: true }
      } else {
        return { success: false, message: response.message }
      }
    } catch (error) {
      return { success: false, message: error.message }
    }
  }

  const logoutAction = async () => {
    try {
      await logout()
    } catch (error) {
      console.error('登出失败:', error)
    } finally {
      // 清除本地存储
      clearUserData()
    }
  }

  // 清除用户数据
  const clearUserData = () => {
    token.value = ''
    userInfo.value = {}
    authStorage.removeItem(TOKEN_KEY)
    authStorage.removeItem(USER_INFO_KEY)
  }

  // 检查 token 与本地用户信息是否自洽（权限以 JWT 为准，不因缺少 id 就整段清空）
  const checkTokenValidity = () => {
    if (!token.value) return false
    const info = userInfo.value
    if (!info || typeof info !== 'object') {
      clearUserData()
      return false
    }
    if (Object.keys(info).length === 0) {
      clearUserData()
      return false
    }
    const idOk = info.id != null && info.id !== ''
    const typeOk = info.userType != null && info.userType !== ''
    if (!idOk && !typeOk) {
      clearUserData()
      return false
    }
    return true
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    isAdmin,
    setUserInfo,
    loginAction,
    getUserInfoAction,
    logoutAction,
    clearUserData,
    checkTokenValidity
  }
}) 