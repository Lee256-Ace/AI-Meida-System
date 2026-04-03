import request from '@/utils/request'

export function getPublishHistoryPage(params) {
  return request({
    url: '/publishHistory/page',
    method: 'get',
    params
  })
}

export function getPublishHistoryById(id) {
  return request({
    url: `/publishHistory/${id}`,
    method: 'get'
  })
}

export function addPublishHistory(data) {
  return request({
    url: '/publishHistory',
    method: 'post',
    data
  })
}

export function updatePublishHistory(data) {
  return request({
    url: '/publishHistory',
    method: 'put',
    data
  })
}

export function deletePublishHistory(id) {
  return request({
    url: `/publishHistory/${id}`,
    method: 'delete'
  })
}
