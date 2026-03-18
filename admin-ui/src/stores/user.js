import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, getUserInfo, logout } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || '{}'))

  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value.userType === 'admin')
  const isMerchant = computed(() => userInfo.value.userType === 'merchant')

  // 方法
  const setToken = (newToken) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const setUserInfo = (info) => {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
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
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  // 检查token有效性
  const checkTokenValidity = () => {
    if (token.value && (!userInfo.value || !userInfo.value.id)) {
      // 如果有token但没有用户信息，清除token
      clearUserData()
      return false
    }
    return !!token.value
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    isAdmin,
    isMerchant,
    setUserInfo,
    loginAction,
    getUserInfoAction,
    logoutAction,
    clearUserData,
    checkTokenValidity
  }
}) 