import request from '@/utils/request'

// 用户登录
export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

// 获取用户信息
export function getUserInfo(userType, userId) {
  return request({
    url: '/auth/userInfo',
    method: 'get',
    params: { userType, userId }
  })
}

// 用户登出
export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

/** 注册普通用户（公开接口，无需登录） */
export function registerNormalUser(data) {
  return request({
    url: '/auth/register/user',
    method: 'post',
    data
  })
}
