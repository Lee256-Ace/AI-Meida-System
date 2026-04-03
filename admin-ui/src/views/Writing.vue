<template>
  <div class="app-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>输入你的创作需求</span>
        </div>
      </template>

      <el-input
          v-model="genForm.prompt"
          type="textarea"
          :rows="6"
          placeholder="例如：写一篇关于人工智能未来发展的深度文章，要求观点新颖、论证充分..."
      />

      <div class="options-row">
        <div class="option-item">
          <label>生成平台：</label>
          <el-checkbox-group v-model="genForm.platforms">
            <el-checkbox value="douyin">抖音</el-checkbox>
            <el-checkbox value="xiaohongshu">小红书</el-checkbox>
            <el-checkbox value="video_account">视频号</el-checkbox>
            <el-checkbox value="toutiao">今日头条</el-checkbox>
          </el-checkbox-group>
        </div>
      </div>

      <div class="options-row">
        <div class="option-item">
          <label>目标读者（可选）：</label>
          <el-select v-model="genForm.targetAudience" clearable placeholder="选填" style="width: 120px">
            <el-option label="大众" value="大众" />
            <el-option label="专业人士" value="专业人士" />
            <el-option label="学生" value="学生" />
            <el-option label="企业" value="企业" />
          </el-select>
        </div>
      </div>

      <el-button
          type="primary"
          size="large"
          :loading="genLoading"
          @click="generate"
      >
        <el-icon><MagicStick /></el-icon>
        {{ genLoading ? '生成中...' : '开始生成' }}
      </el-button>
    </el-card>

    <!-- 结果展示 -->
    <el-card v-if="genResult.show" class="box-card result-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span>任务ID: <el-tag>{{ genResult.taskId }}</el-tag></span>
          </div>
          <el-tag :type="statusType">{{ statusText }}</el-tag>
        </div>
      </template>

      <!-- 思考过程 -->
      <div v-if="genResult.reasoning" class="section">
        <h4>💭 思考过程</h4>
        <el-input
            v-model="genResult.reasoning"
            type="textarea"
            :rows="4"
            readonly
            class="reasoning-box"
        />
      </div>

      <!-- 生成结果：有分平台数据时按平台展示，每篇单独提供美化和续写 -->
      <div class="section">
        <h4>生成结果</h4>
        <template v-if="displayBlocks.length > 0">
          <div
              v-for="block in displayBlocks"
              :key="block.key"
              class="platform-block"
          >
            <div class="platform-label">【{{ block.label }}】</div>
            <div
                class="result-content markdown-body"
                v-html="block.text === TOUTIAO_PLACEHOLDER_LEGACY ? escapeHtml(TOUTIAO_PLACEHOLDER_DISPLAY) : renderMarkdown(block.text)"
            />
            <div v-if="block.text !== TOUTIAO_PLACEHOLDER_LEGACY" class="platform-actions">
              <el-button type="success" size="small" @click="useForPolish(block)">
                <el-icon><Brush /></el-icon> 美化此篇
              </el-button>
              <el-button type="warning" size="small" @click="useForContinue(block)">
                <el-icon><DocumentAdd /></el-icon> 续写此篇
              </el-button>
              <el-button type="primary" size="small" @click="startEditFromGenerate(block)">
                <el-icon><DocumentAdd /></el-icon> 开始编辑
              </el-button>
            </div>
          </div>
        </template>
        <template v-else>
          <div class="result-content markdown-body" v-html="renderMarkdown(genResult.content)" />
          <div class="platform-actions single-actions">
            <el-button type="success" size="small" @click="useForPolish(null)">
              <el-icon><Brush /></el-icon> 美化此文
            </el-button>
            <el-button type="warning" size="small" @click="useForContinue(null)">
              <el-icon><DocumentAdd /></el-icon> 续写此文
            </el-button>
          </div>
        </template>
      </div>

      <!-- 整体操作：复制、下载 -->
      <div class="action-bar">
        <el-button @click="copyText(fullContentForCopy)">
          <el-icon><DocumentCopy /></el-icon> 复制全部
        </el-button>
        <el-button @click="downloadText(fullContentForCopy, 'generated')">
          <el-icon><Download /></el-icon> 下载全部
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
defineOptions({ name: 'AiGenerate' })
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MagicStick, DocumentCopy, Download, Brush, DocumentAdd } from '@element-plus/icons-vue'
import { generateStream, getGenerationTask } from '@/api/aiWriting'
import { renderMarkdown } from '@/utils/markdown'
import { useAiWritingCacheStore } from '@/stores/aiWritingCache'
import { useMarkdownCacheStore } from '@/stores/markDownCache'

const route = useRoute()
const router = useRouter()
const cacheStore = useAiWritingCacheStore()
const markdownCache = useMarkdownCacheStore()

const PLATFORM_OPTIONS = ['douyin', 'xiaohongshu', 'video_account', 'toutiao']

const genForm = reactive({
  prompt: '',
  platforms: ['douyin', 'xiaohongshu', 'video_account', 'toutiao'],
  targetAudience: ''
})

const genLoading = ref(false)
const genResult = reactive({
  show: false,
  taskId: '-',
  status: 'processing',
  reasoning: '',
  content: '',
  /** 任务详情（含 douyinCopy、xiaohongshuCopy 等分平台文案） */
  task: null
})

const statusType = computed(() => ({
  processing: 'warning',
  completed: 'success',
  error: 'danger'
}[genResult.status] || 'info'))

const statusText = computed(() => ({
  processing: '生成中...',
  completed: '✓ 已完成',
  error: '✗ 失败'
}[genResult.status] || ''))

/** 今日头条缺失时的展示文案：后端已改为返回简短提示；兼容旧数据中的长占位符 */
const TOUTIAO_PLACEHOLDER_LEGACY = '[本次生成未返回今日头条文案。若工作流已含今日头条节点，请检查 Coze 返回的 final_result 结构或联系管理员。]'
const TOUTIAO_PLACEHOLDER_DISPLAY = '今日头条文案本次未生成，请重试或联系管理员。'

/** 从任务对象取某平台文案，兼容后端返回 camelCase 或 snake_case */
function getPlatformCopyFromTask(task, key) {
  if (!task) return null
  const map = {
    douyinCopy: task.douyinCopy ?? task.douyin_copy,
    xiaohongshuCopy: task.xiaohongshuCopy ?? task.xiaohongshu_copy,
    videoAccountCopy: task.videoAccountCopy ?? task.video_account_copy,
    toutiaoCopy: task.toutiaoCopy ?? task.toutiao_copy
  }
  return map[key] ?? null
}

/** 供 v-html 安全显示占位文案等纯文本 */
function escapeHtml(str) {
  if (str == null) return ''
  const s = String(str)
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
}

/** 从整段 content 按【平台名】解析出各平台块（无 task 分平台数据时兜底，确保今日头条等都能展示） */
function parseContentByPlatform(content) {
  if (!content || typeof content !== 'string') return []
  const markers = ['【抖音】', '【小红书】', '【视频号】', '【今日头条】']
  const keyToLabel = { douyin: '抖音', xiaohongshu: '小红书', video_account: '视频号', toutiao: '今日头条' }
  const list = []
  for (const marker of markers) {
    const key = { '【抖音】': 'douyin', '【小红书】': 'xiaohongshu', '【视频号】': 'video_account', '【今日头条】': 'toutiao' }[marker]
    const start = content.indexOf(marker)
    if (start === -1) continue
    const textStart = start + marker.length
    let end = content.length
    for (const m of markers) {
      if (m === marker) continue
      const next = content.indexOf(m, textStart)
      if (next !== -1 && next < end) end = next
    }
    const text = content.slice(textStart, end).trim()
    if (text) list.push({ key, label: keyToLabel[key], text })
  }
  return list
}

/** 平台 key 到路由用 platform 的映射 */
const KEY_TO_PLATFORM = { douyinCopy: 'douyin', xiaohongshuCopy: 'xiaohongshu', videoAccountCopy: 'video_account', toutiaoCopy: 'toutiao' }

/** 分平台展示块：优先用 task 分平台字段（兼容 snake_case），否则从 content 解析【平台】兜底；每块带 platformKey 供美化和续写跳转 */
const displayBlocks = computed(() => {
  const t = genResult.task
  const keys = ['douyinCopy', 'xiaohongshuCopy', 'videoAccountCopy', 'toutiaoCopy']
  const keyToLabel = { douyinCopy: '抖音', xiaohongshuCopy: '小红书', videoAccountCopy: '视频号', toutiaoCopy: '今日头条' }
  if (t) {
    const fromTask = keys
      .map(k => ({ k, text: getPlatformCopyFromTask(t, k) }))
      .filter(({ text }) => text && String(text).trim())
      .map(({ k, text }) => ({ key: k, label: keyToLabel[k], text, platformKey: KEY_TO_PLATFORM[k] }))
    if (fromTask.length > 0) return fromTask
  }
  const content = genResult.content
  if (content && String(content).trim()) {
    const fromContent = parseContentByPlatform(content)
    if (fromContent.length > 0) return fromContent.map(({ key, label, text }) => ({ key, label, text, platformKey: key }))
  }
  return []
})

/** 用于复制/下载的完整文案（优先用分平台拼接，否则用 content） */
const fullContentForCopy = computed(() => {
  if (displayBlocks.value.length > 0) {
    return displayBlocks.value.map(b => `【${b.label}】\n${b.text}`).join('\n\n')
  }
  return genResult.content || ''
})

let evtSource = null

const generate = async () => {
  const prompt = genForm.prompt.trim()
  if (!prompt) {
    ElMessage.warning('请输入创作需求')
    return
  }
  const platforms = (genForm.platforms && genForm.platforms.length)
    ? genForm.platforms
    : PLATFORM_OPTIONS
  if (!platforms.length) {
    ElMessage.warning('请至少选择一个生成平台')
    return
  }

  genLoading.value = true
  genResult.show = true
  genResult.reasoning = ''
  genResult.content = ''
  genResult.task = null
  genResult.status = 'processing'
  genResult.taskId = '-'

  if (evtSource) evtSource.close()

  evtSource = generateStream({
    prompt,
    platforms,
    targetAudience: genForm.targetAudience || undefined
  })

  evtSource.addEventListener('taskId', (e) => {
    genResult.taskId = '#' + e.data
  })

  evtSource.addEventListener('reasoning', (e) => {
    genResult.reasoning += e.data
  })

  evtSource.addEventListener('content', (e) => {
    genResult.content += e.data
  })

  evtSource.addEventListener('complete', async () => {
    genResult.status = 'completed'
    genLoading.value = false
    evtSource.close()
    const idStr = genResult.taskId.replace(/^#/, '')
    const id = parseInt(idStr, 10)
    if (!isNaN(id)) {
      try {
        const res = await getGenerationTask(id)
        genResult.task = res || null
      } catch (_) {
        genResult.task = null
      }
    }
    ElMessage.success('生成完成')
  })

  evtSource.addEventListener('error', () => {
    genResult.status = 'error'
    genLoading.value = false
    evtSource.close()
    ElMessage.error('生成失败')
  })
}

const copyText = (text) => {
  navigator.clipboard.writeText(text).then(() => ElMessage.success('已复制'))
}

const downloadText = (text, prefix) => {
  const blob = new Blob([text], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${prefix}_${Date.now()}.txt`
  a.click()
}

/** 跳转美化：block 为当前平台块时传该篇文案+平台，否则传整段内容（无 platform） */
const useForPolish = (block) => {
  const content = block ? block.text : fullContentForCopy.value
  const query = { content: encodeURIComponent(content) }
  if (block && block.platformKey) query.platform = block.platformKey
  router.push({ path: '/ai-writing/polish', query })
}

/** 跳转续写：同上，按篇传文案+平台 */
const useForContinue = (block) => {
  const content = block ? block.text : fullContentForCopy.value
  const query = { content: encodeURIComponent(content) }
  if (block && block.platformKey) query.platform = block.platformKey
  router.push({ path: '/ai-writing/continue', query })
}

const formatTabTime = () => {
  const d = new Date()
  const MM = String(d.getMonth() + 1).padStart(2, '0')
  const DD = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${MM}/${DD} ${hh}:${mm}`
}

/** 把生成的新文案送到 MarkdownEditor 的新标签页，并跳转过去 */
const startEditFromGenerate = (block) => {
  const text = block ? block.text : fullContentForCopy.value
  const content = (text ?? '').trim()
  if (!content) {
    ElMessage.warning('当前没有可编辑的生成结果')
    return
  }
  const label = block ? block.label : '全部'
  const taskId = genResult.taskId && genResult.taskId !== '-' ? genResult.taskId : ''
  const title = `生成-${label}${taskId ? `-${taskId}` : ''}-${formatTabTime()}`
  markdownCache.openNewTab({ title, content })
  router.push({ path: '/markdownEdit' })
}

onMounted(() => {
  // 支持从 Markdown 编辑器跳转过来：把 prompt 预填到输入框
  if (route.query.prompt) {
    genForm.prompt = decodeURIComponent(route.query.prompt)
    return
  }
  const c =  cacheStore.writing
  if (c.form.prompt || c.result.content || c.result.show) {
    genForm.prompt = c.form.prompt
    genForm.platforms = c.form.platforms.length ? [...c.form.platforms] : ['douyin', 'xiaohongshu', 'video_account', 'toutiao']
    genForm.targetAudience = c.form.targetAudience
    genResult.show = c.result.show
    genResult.taskId = c.result.taskId
    genResult.status = c.result.status
    genResult.reasoning = c.result.reasoning
    genResult.content = c.result.content
    genResult.task = c.result.task
  }
})

onBeforeUnmount(() => {
  cacheStore.saveWriting(genForm, genResult)
  if (evtSource) evtSource.close()
})
</script>

<style scoped>
.app-container {
  padding: 20px;
}

.box-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.options-row {
  display: flex;
  gap: 30px;
  margin: 20px 0;
  align-items: center;
  flex-wrap: wrap;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.option-item label {
  font-weight: 500;
  color: #606266;
}

.section {
  margin-bottom: 20px;
}

.section h4 {
  margin-bottom: 10px;
  color: #303133;
}

.reasoning-box :deep(.el-textarea__inner) {
  background-color: #f5f7fa;
  color: #606266;
}

.platform-block {
  margin-bottom: 20px;
}

.platform-block:last-child {
  margin-bottom: 0;
}

.platform-label {
  font-weight: 600;
  color: #409eff;
  margin-bottom: 8px;
  font-size: 15px;
}

.result-content {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 20px;
  min-height: 80px;
  line-height: 1.8;
  font-size: 16px;
}

.result-content.markdown-body {
  white-space: normal;
}

.result-content.markdown-body :deep(p) {
  margin: 0.5em 0;
}

.result-content.markdown-body :deep(ul),
.result-content.markdown-body :deep(ol) {
  margin: 0.5em 0;
  padding-left: 1.5em;
}

.result-content.markdown-body :deep(strong) {
  font-weight: 600;
}

.platform-actions {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.platform-actions.single-actions {
  margin-bottom: 0;
}

.action-bar {
  display: flex;
  gap: 10px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}
</style>