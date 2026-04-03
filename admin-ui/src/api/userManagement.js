import request from '@/utils/request'

// 管理员：获取所有注册账号（admins + users），password 只返回脱敏字段
export function getAllAccounts() {
  return request({
    url: '/admin/users/list',
    method: 'get'
  })
}

/** 封禁/解禁 users 表用户：status 0 禁用，1 正常 */
export function setAppUserStatus(id, status) {
  return request({
    url: `/admin/users/app-user/${id}/status`,
    method: 'put',
    params: { status }
  })
}

/** 删除 users 表用户 */
export function deleteAppUser(id) {
  return request({
    url: `/admin/users/app-user/${id}`,
    method: 'delete'
  })
}

