import request from '@/utils/request'

// 分页查询ArticleNews
export function getArticleNewsList(params) {
  return request({
    url: '/articleNews/page',
    method: 'get',
    params
  })
}

// 根据ID查询ArticleNews
export function getArticleNewsById(id) {
  return request({
    url: `/articleNews/${id}`,
    method: 'get'
  })
}

// 新增ArticleNews
export function addArticleNews(data) {
  return request({
    url: '/articleNews',
    method: 'post',
    data
  })
}

// 更新ArticleNews
export function updateArticleNews(data) {
  return request({
    url: '/articleNews',
    method: 'put',
    data
  })
}

// 删除ArticleNews
export function deleteArticleNews(id) {
  return request({
    url: `/articleNews/${id}`,
    method: 'delete'
  })
}

// 查询所有ArticleNews列表
export function getArticleNewsAll() {
  return request({
    url: '/articleNews/list',
    method: 'get'
  })
}
