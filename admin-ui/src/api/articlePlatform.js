import request from '@/utils/request'

// 分页查询文章发布的平台
export function getArticlePlatformList(params) {
  return request({
    url: '/articlePlatform/page',
    method: 'get',
    params
  })
}

// 根据ID查询文章发布的平台
export function getArticlePlatformById(id) {
  return request({
    url: `/articlePlatform/${id}`,
    method: 'get'
  })
}

// 新增文章发布的平台
export function addArticlePlatform(data) {
  return request({
    url: '/articlePlatform',
    method: 'post',
    data
  })
}

// 更新文章发布的平台
export function updateArticlePlatform(data) {
  return request({
    url: '/articlePlatform',
    method: 'put',
    data
  })
}

// 删除文章发布的平台
export function deleteArticlePlatform(id) {
  return request({
    url: `/articlePlatform/${id}`,
    method: 'delete'
  })
}

// 查询所有文章发布的平台列表
export function getArticlePlatformAll() {
  return request({
    url: '/articlePlatform/list',
    method: 'get'
  })
}
