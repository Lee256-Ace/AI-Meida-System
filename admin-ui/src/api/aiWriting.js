// admin-ui/src/api/aiWriting.js
import request from '@/utils/request'

// 文案生成 - SSE 流式；platforms 为选中的平台数组，必须传以限制只生成/只返回这些平台
export function generateStream(params) {
    const platforms = (params.platforms && params.platforms.length)
        ? params.platforms
        : ['douyin', 'xiaohongshu', 'video_account', 'toutiao']
    const query = new URLSearchParams({
        prompt: params.prompt,
        platforms: platforms.join(',')
    })
    if (params.targetAudience) query.set('targetAudience', params.targetAudience)
    const url = `/api/generate/stream?${query.toString()}`
    console.log('SSE URL:', url)
    return new EventSource(url)
}

// 文章美化 - 流式
export function polishStream(data) {
    return fetch(`/api/polish/stream`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    })
}

// 文章续写 - 流式
export function continueStream(data) {
    return fetch(`/api/continue/stream`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    })
}

// 文案生成任务详情（含分平台文案 douyinCopy / xiaohongshuCopy 等）
export function getGenerationTask(id) {
  return request({
    url: `/generate/task/${id}`,
    method: 'get'
  })
}

// 文章美化任务详情（含 platform）
export function getPolishTask(id) {
  return request({
    url: `/polish/task/${id}`,
    method: 'get'
  })
}

// 文章续写任务详情（含 platform）
export function getContinuationTask(id) {
  return request({
    url: `/continue/task/${id}`,
    method: 'get'
  })
}

// 历史记录（使用 axios request）
export function getHistoryList(params) {
    return request({
        url: '/history/list',
        method: 'get',
        params
    })
}

export function getHistoryDetail(type, id) {
    return request({
        url: '/history/detail',
        method: 'get',
        params: { type, id }
    })
}

export function getHistoryStatistics() {
    return request({
        url: '/history/statistics',
        method: 'get'
    })
}

export function deleteHistory(type, id) {
    return request({
        url: `/history/${type}/${id}`,
        method: 'delete'
    })
}

export function batchDeleteHistory(ids) {
    return request({
        url: '/history/batch-delete',
        method: 'post',
        data: { ids }
    })
}