import { marked } from 'marked'

marked.setOptions({ gfm: true, breaks: true })

/**
 * 将 Markdown 字符串转为 HTML，用于 v-html 展示
 * @param {string} raw
 * @returns {string}
 */
export function renderMarkdown(raw) {
  if (raw == null || typeof raw !== 'string') return ''
  const trimmed = raw.trim()
  if (!trimmed) return ''
  return marked.parse(trimmed)
}
