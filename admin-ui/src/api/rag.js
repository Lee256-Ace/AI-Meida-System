/**
 * RAG 私有知识库 API（代理前缀 /rag → FastAPI :8001）
 */

const RAG_BASE = '/rag'

export async function uploadKnowledgeFile(file) {
  const form = new FormData()
  form.append('file', file)
  const res = await fetch(`${RAG_BASE}/upload`, {
    method: 'POST',
    body: form
  })
  const data = await res.json().catch(() => ({}))
  if (!res.ok) {
    throw new Error(data.detail || data.message || '上传失败')
  }
  return data
}

export async function listKnowledgeDocs() {
  const res = await fetch(`${RAG_BASE}/docs/list`)
  const data = await res.json().catch(() => ({}))
  if (!res.ok) throw new Error(data.detail || '获取文档列表失败')
  return data.list || []
}

export async function checkRagHealth() {
  const res = await fetch(`${RAG_BASE}/health`)
  if (!res.ok) throw new Error('RAG 服务不可用')
  return res.json()
}

/**
 * 流式问答：回调 onToken / onContexts / onDone / onError
 * @returns {() => void} abort 函数
 */
export function chatStream(question, { onToken, onContexts, onDone, onError } = {}) {
  const controller = new AbortController()

  ;(async () => {
    try {
      const res = await fetch(`${RAG_BASE}/chat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ question, stream: true, top_k: 4 }),
        signal: controller.signal
      })
      if (!res.ok) {
        const err = await res.json().catch(() => ({}))
        throw new Error(err.detail || `请求失败 (${res.status})`)
      }
      const reader = res.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''
      let eventName = 'message'
      let finished = false
      const finish = () => {
        if (finished) return
        finished = true
        onDone && onDone()
      }

      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const parts = buffer.split('\n')
        buffer = parts.pop() || ''

        for (const rawLine of parts) {
          const line = rawLine.replace(/\r$/, '')
          if (!line) {
            eventName = 'message'
            continue
          }
          if (line.startsWith('event:')) {
            eventName = line.slice(6).trim()
            continue
          }
          if (line.startsWith('data:')) {
            const dataStr = line.slice(5).trim()
            if (eventName === 'token') {
              try {
                onToken && onToken(JSON.parse(dataStr))
              } catch {
                onToken && onToken(dataStr)
              }
            } else if (eventName === 'contexts') {
              try {
                onContexts && onContexts(JSON.parse(dataStr))
              } catch (_) {}
            } else if (eventName === 'error') {
              try {
                onError && onError(JSON.parse(dataStr))
              } catch {
                onError && onError(dataStr)
              }
              finished = true
            } else if (eventName === 'done' || dataStr === '[DONE]') {
              finish()
            }
          }
        }
      }
      finish()
    } catch (e) {
      if (e.name === 'AbortError') return
      onError && onError(e.message || String(e))
    }
  })()

  return () => controller.abort()
}
