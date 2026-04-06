import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, getUserInfo, logout } from '@/api/auth'

/** 仅解析 JWT payload（与后端 JwtUtil 中 claims 一致：userId/username/userType），不校验签名 */
function parseJwtPayload(token) {
  if (!token || typeof token !== 'string') return null
  try {
    const part = token.split('.')[1]
    if (!part) return null
    const b64 = part.replace(/-/g, '+').replace(/_/g, '/')
    const pad = b64.length % 4 ? '='.repeat(4 - (b64.length % 4)) : ''
    return JSON.parse(atob(b64 + pad))
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || '{}'))

  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value.userType === 'admin')

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
              let info = result.userInfo
              if (!info || info.id == null || !info.userType) {
                const p = parseJwtPayload(result.token)
                if (p && p.userId != null) {
                  info = {
                    ...(info || {}),
                    id: Number(p.userId),
                    username: p.username ?? info?.username ?? '',
                    userType: p.userType ?? info?.userType ?? 'user'
                  }
                }
              }
              setUserInfo(info || {})
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

  // 检查 token：本地 userInfo 不完整时用 JWT 补全（避免首登后路由/请求读到空 id）
  const checkTokenValidity = () => {
    if (!token.value) return false

    const info = userInfo.value
    const needHydrate =
      !info ||
      info.id == null ||
      info.userType == null ||
      info.userType === ''

    if (needHydrate) {
      const p = parseJwtPayload(token.value)
      if (!p || p.userId == null) {
        clearUserData()
        return false
      }
      setUserInfo({
        ...(info && typeof info === 'object' ? info : {}),
        id: Number(p.userId),
        username: p.username ?? info?.username ?? '',
        userType: p.userType ?? info?.userType ?? 'user'
      })
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