import request from '@/utils/request'

// 分页查询商家表
export function getMerchantList(params) {
  return request({
    url: '/merchant/page',
    method: 'get',
    params
  })
}

// 根据ID查询商家表
export function getMerchantById(id) {
  return request({
    url: `/merchant/${id}`,
    method: 'get'
  })
}

// 新增商家表
export function addMerchant(data) {
  return request({
    url: '/merchant',
    method: 'post',
    data
  })
}

// 更新商家表
export function updateMerchant(data) {
  return request({
    url: '/merchant',
    method: 'put',
    data
  })
}

// 删除商家表
export function deleteMerchant(id) {
  return request({
    url: `/merchant/${id}`,
    method: 'delete'
  })
}

// 查询所有商家表列表
export function getMerchantAll() {
  return request({
    url: '/merchant/list',
    method: 'get'
  })
}
