import request from '@/utils/request'

/** 保存当前 Markdown 文案（首次无 id 则插入，有 id 则更新） */
export function saveMarkdownCopy(data) {
  return request({
    url: '/markdown-copy/save',
    method: 'post',
    data
  })
}
