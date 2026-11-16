 <template>
  <div class="knowledge-page">
    <el-card class="side-card" shadow="never">
      <template #header>
        <div class="side-header">
          <span>知识库文档</span>
          <el-tag size="small" :type="healthOk ? 'success' : 'danger'">
            {{ healthOk ? '服务在线' : '服务离线' }}
          </el-tag>
        </div>
      </template>

      <el-upload
        drag
        :auto-upload="false"
        :show-file-list="false"
        accept=".txt,.md,.markdown,.json,.csv"
        :disabled="uploading"
        @change="onFileChange"
      >
        <div class="upload-tip">
          <el-icon class="upload-icon"><UploadFilled /></el-icon>
          <div>拖拽或点击上传</div>
          <div class="hint">支持 .txt / .md / .json / .csv</div>
        </div>
      </el-upload>

      <el-button
        class="refresh-btn"
        text
        type="primary"
        :loading="docsLoading"
        @click="loadDocs"
      >
        刷新列表
      </el-button>

      <el-scrollbar max-height="360px">
        <div v-if="!docs.length" class="empty-docs">暂无文档，请先上传</div>
        <div v-for="d in docs" :key="d.id" class="doc-item">
          <div class="doc-name" :title="d.filename">{{ d.filename }}</div>
          <div class="doc-meta">{{ d.chunks }} 块 · {{ d.chars }} 字</div>
        </div>
      </el-scrollbar>
    </el-card>

    <el-card class="chat-card" shadow="never">
      <template #header>
        <div class="chat-header">
          <span>私有知识库问答</span>
          <el-button text type="danger" @click="clearChat">清空对话</el-button>
        </div>
      </template>

      <div ref="chatBodyRef" class="chat-body">
        <div v-if="!messages.length" class="welcome">
          <h3>基于已上传资料回答问题</h3>
          <p>先在左侧上传文本资料，再在下方输入问题。回答将流式展示。</p>
        </div>

        <div
          v-for="(m, idx) in messages"
          :key="idx"
          class="msg"
          :class="m.role"
        >
          <div class="bubble">
            <div class="role-label">{{ m.role === 'user' ? '我' : '知识库助手' }}</div>
            <div class="content" v-html="renderContent(m.content)" />
            <div v-if="m.contexts?.length" class="ctx-list">
              <div class="ctx-title">参考资料</div>
              <el-tag
                v-for="(c, i) in m.contexts"
                :key="i"
                size="small"
                class="ctx-tag"
                type="info"
              >
                {{ c.filename }}
              </el-tag>
            </div>
          </div>
        </div>

        <div v-if="streaming" class="msg assistant">
          <div class="bubble">
            <div class="role-label">知识库助手</div>
            <div class="content" v-html="renderContent(streamText || '…')" />
          </div>
        </div>
      </div>

      <div class="composer">
        <el-input
          v-model="input"
          type="textarea"
          :rows="3"
          placeholder="基于知识库提问，例如：这份资料的核心观点是什么？"
          :disabled="streaming"
          @keydown.enter.exact.prevent="send"
        />
        <div class="composer-actions">
          <el-button v-if="streaming" type="warning" @click="stopStream">停止</el-button>
          <el-button type="primary" :loading="streaming" :disabled="!input.trim()" @click="send">
            发送
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
defineOptions({ name: 'Knowledge' })

import { ref, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { uploadKnowledgeFile, listKnowledgeDocs, checkRagHealth, chatStream } from '@/api/rag'
import { renderMarkdown } from '@/utils/markdown'

const healthOk = ref(false)
const docs = ref([])
const docsLoading = ref(false)
const uploading = ref(false)
const messages = ref([])
const input = ref('')
const streaming = ref(false)
const streamText = ref('')
const streamContexts = ref([])
const chatBodyRef = ref(null)
let abortStream = null

const renderContent = (text) => renderMarkdown(text || '')

const scrollToBottom = async () => {
  await nextTick()
  const el = chatBodyRef.value
  if (el) el.scrollTop = el.scrollHeight
}

const loadDocs = async () => {
  docsLoading.value = true
  try {
    docs.value = await listKnowledgeDocs()
  } catch (e) {
    docs.value = []
  } finally {
    docsLoading.value = false
  }
}

const pingHealth = async () => {
  try {
    await checkRagHealth()
    healthOk.value = true
  } catch {
    healthOk.value = false
  }
}

const onFileChange = async (uploadFile) => {
  const raw = uploadFile?.raw
  if (!raw) return
  uploading.value = true
  try {
    const res = await uploadKnowledgeFile(raw)
    ElMessage.success(`已入库：${res.doc?.filename || raw.name}`)
    await loadDocs()
  } catch (e) {
    ElMessage.error(e.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

const clearChat = () => {
  if (streaming.value) stopStream()
  messages.value = []
  streamText.value = ''
}

const stopStream = () => {
  if (abortStream) abortStream()
  abortStream = null
  if (streaming.value) {
    messages.value.push({
      role: 'assistant',
      content: streamText.value || '（已停止）',
      contexts: streamContexts.value
    })
  }
  streaming.value = false
  streamText.value = ''
  streamContexts.value = []
}

const send = async () => {
  const q = input.value.trim()
  if (!q || streaming.value) return
  if (!healthOk.value) {
    await pingHealth()
    if (!healthOk.value) {
      ElMessage.error('RAG 服务未启动，请先运行 rag-service（默认端口 8001）')
      return
    }
  }

  messages.value.push({ role: 'user', content: q })
  input.value = ''
  streaming.value = true
  streamText.value = ''
  streamContexts.value = []
  await scrollToBottom()

  abortStream = chatStream(q, {
    onContexts: (ctx) => {
      streamContexts.value = Array.isArray(ctx) ? ctx : []
    },
    onToken: (t) => {
      streamText.value += t
      scrollToBottom()
    },
    onDone: () => {
      if (!streaming.value) return
      messages.value.push({
        role: 'assistant',
        content: streamText.value || '（无内容返回）',
        contexts: streamContexts.value
      })
      streaming.value = false
      streamText.value = ''
      streamContexts.value = []
      abortStream = null
      scrollToBottom()
    },
    onError: (err) => {
      streaming.value = false
      streamText.value = ''
      abortStream = null
      ElMessage.error(err || '问答失败')
      messages.value.push({
        role: 'assistant',
        content: `出错了：${err}`,
        contexts: []
      })
    }
  })
}

onMounted(async () => {
  await pingHealth()
  await loadDocs()
})

onBeforeUnmount(() => {
  if (abortStream) abortStream()
})
</script>

<style scoped>
.knowledge-page {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 16px;
  height: calc(100vh - 120px);
  min-height: 520px;
}

.side-card,
.chat-card {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.side-card :deep(.el-card__body),
.chat-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 12px 16px 16px;
}

.side-header,
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.upload-tip {
  padding: 12px 8px;
  color: #606266;
  text-align: center;
}

.upload-icon {
  font-size: 36px;
  color: #c0c4cc;
  margin-bottom: 6px;
}

.hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.refresh-btn {
  margin: 8px 0;
  align-self: flex-start;
}

.empty-docs {
  color: #909399;
  font-size: 13px;
  padding: 8px 0;
}

.doc-item {
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.doc-name {
  font-size: 13px;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.doc-meta {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px 4px 16px;
  background: linear-gradient(180deg, #faf8f5 0%, #fff 40%);
  border-radius: 8px;
}

.welcome {
  text-align: center;
  color: #909399;
  padding: 48px 16px;
}

.welcome h3 {
  color: #606266;
  margin-bottom: 8px;
}

.msg {
  display: flex;
  margin-bottom: 14px;
}

.msg.user {
  justify-content: flex-end;
}

.msg.assistant {
  justify-content: flex-start;
}

.bubble {
  max-width: 78%;
  padding: 10px 14px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #ebeef5;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.msg.user .bubble {
  background: #fff7e6;
  border-color: #f5dab1;
}

.role-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.content {
  line-height: 1.7;
  color: #303133;
  word-break: break-word;
}

.content :deep(p) {
  margin: 0.4em 0;
}

.ctx-list {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #e4e7ed;
}

.ctx-title {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}

.ctx-tag {
  margin: 0 6px 4px 0;
}

.composer {
  margin-top: 12px;
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
}

.composer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

@media (max-width: 900px) {
  .knowledge-page {
    grid-template-columns: 1fr;
    height: auto;
  }
}
</style>
