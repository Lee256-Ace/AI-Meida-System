<template>
  <div class="app-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>文章美化</span>
        </div>
      </template>

      <el-input
          v-model="polishForm.content"
          type="textarea"
          :rows="8"
          placeholder="粘贴你的文章 here，AI 将帮你优化表达、修正语法、提升文采..."
      />

      <div class="options-row">
        <div class="option-item">
          <label>所属平台：</label>
          <el-select v-model="polishForm.platform" clearable placeholder="可选，用于分类" style="width: 140px">
            <el-option label="抖音" value="douyin" />
            <el-option label="小红书" value="xiaohongshu" />
            <el-option label="视频号" value="video_account" />
            <el-option label="今日头条" value="toutiao" />
          </el-select>
        </div>
        <div class="option-item">
          <label>美化类型：</label>
          <el-select v-model="polishForm.type" style="width: 150px">
            <el-option label="简化表达" value="simplify" />
            <el-option label="润色提升" value="enhance" />
            <el-option label="语法修正" value="grammar" />
            <el-option label="改写重构" value="rewrite" />
          </el-select>
        </div>
        <div class="option-item">
          <label>目标读者：</label>
          <el-select v-model="polishForm.targetAudience" style="width: 140px" placeholder="默认大众">
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
          :loading="polishLoading"
          @click="polish"
      >
        <el-icon><Brush /></el-icon>
        {{ polishLoading ? '处理中...' : '开始美化' }}
      </el-button>
    </el-card>

    <!-- 对比结果 -->
    <el-card v-if="polishResult.show" class="box-card result-card">
      <template #header>
        <div class="card-header">
          <span>任务ID: <el-tag>{{ polishResult.taskId }}</el-tag></span>
          <el-tag v-if="polishResult.platformName" type="info" size="small">{{ polishResult.platformName }}</el-tag>
          <el-tag :type="statusType">{{ statusText }}</el-tag>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col :xs="24" :sm="12">
          <h4>原文</h4>
          <div class="compare-box original">{{ polishForm.content }}</div>
        </el-col>
        <el-col :xs="24" :sm="12">
          <h4>✨ 美化后</h4>
          <div class="compare-box result markdown-body" v-html="renderMarkdown(polishResult.content)" />
        </el-col>
      </el-row>

      <div class="action-bar">
        <el-button @click="copyText(polishResult.content)">
          <el-icon><DocumentCopy /></el-icon> 复制结果
        </el-button>
        <el-button @click="downloadText(polishResult.content, 'polished')">
          <el-icon><Download /></el-icon> 下载
        </el-button>
        <el-button type="primary" @click="startEditFromPolish">
          <el-icon><DocumentAdd /></el-icon> 开始编辑
        </el-button>
        <el-button type="warning" @click="useForContinue">
          <el-icon><DocumentAdd /></el-icon> 续写此文
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
defineOptions({ name: 'AiPolish' })
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Brush, DocumentCopy, Download, DocumentAdd } from '@element-plus/icons-vue'
import { polishStream, getPolishTask } from '@/api/aiWriting'
import { useAiWritingCacheStore } from '@/stores/aiWritingCache'
import { useMarkdownCacheStore } from '@/stores/markDownCache'
import { renderMarkdown } from '@/utils/markdown'

const PLATFORM_NAMES = { douyin: '抖音', xiaohongshu: '小红书', video_account: '视频号', toutiao: '今日头条' }

const route = useRoute()
const router = useRouter()
const cacheStore = useAiWritingCacheStore()
const markdownCache = useMarkdownCacheStore()

const polishForm = reactive({
  content: '',
  type: 'enhance',
  targetAudience: '大众', // 官方：大众/专业人士/学生/企业，默认大众
  platform: '' // douyin/xiaohongshu/video_account/toutiao，可选
})

const polishLoading = ref(false)
const polishResult = reactive({
  show: false,
  taskId: '-',
  status: 'processing',
  content: '',
  platformName: '' // 所属平台展示名
})

const statusType = computed(() => ({
  processing: 'warning',
  completed: 'success',
  error: 'danger'
}[polishResult.status] || 'info'))

const statusText = computed(() => ({
  processing: '处理中...',
  completed: '✓ 已完成',
  error: '✗ 失败'
}[polishResult.status] || ''))

// 从路由参数获取内容与平台（文案生成页按篇跳转时带 platform）；否则从缓存恢复
onMounted(() => {
  if (route.query.content) {
    polishForm.content = decodeURIComponent(route.query.content)
    if (route.query.platform) polishForm.platform = route.query.platform
  } else {
    const c = cacheStore.polish
    if (c.form.content || c.result.content || c.result.show) {
      polishForm.content = c.form.content
      polishForm.type = c.form.type
      polishForm.targetAudience = c.form.targetAudience
      polishForm.platform = c.form.platform
      polishResult.show = c.result.show
      polishResult.taskId = c.result.taskId
      polishResult.status = c.result.status
      polishResult.content = c.result.content
      polishResult.platformName = c.result.platformName
    }
  }
})

onBeforeUnmount(() => {
  cacheStore.savePolish(polishForm, polishResult)
})

const polish = async () => {
  const content = polishForm.content.trim()
  if (!content) {
    ElMessage.warning('请输入需要美化的文章')
    return
  }

  polishLoading.value = true
  polishResult.show = true
  polishResult.content = ''
  polishResult.platformName = ''
  polishResult.status = 'processing'
  polishResult.taskId = '-'

  try {
    const response = await polishStream({
      content,
      type: polishForm.type,
      targetAudience: polishForm.targetAudience || '大众',
      platform: polishForm.platform || undefined
    })

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let lastEvent = ''

    const processRawEvent = (rawEvent) => {
      if (!rawEvent || !String(rawEvent).trim()) return
      const lines = String(rawEvent).replace(/\r\n/g, '\n').split('\n')
      let eventName = lastEvent
      const dataLines = []

      for (const line of lines) {
        if (line.startsWith('event:')) {
          eventName = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
          dataLines.push(line.slice(5))
        }
      }

      lastEvent = eventName || lastEvent
      const data = dataLines.join('\n').trim()

      if (lastEvent === 'taskId') {
        polishResult.taskId = '#' + data
      } else if (lastEvent === 'complete' && (data === '[DONE]' || data === 'done')) {
        polishResult.status = 'completed'
        polishLoading.value = false
        const idStr = polishResult.taskId.replace(/^#/, '')
        const id = parseInt(idStr, 10)
        if (!isNaN(id)) {
          getPolishTask(id).then(res => {
            if (res) {
              if (res.platform) polishResult.platformName = PLATFORM_NAMES[res.platform] || res.platform
              const text = res.resultContent ?? res.result_content ?? ''
              if (text) polishResult.content = text
            }
          }).catch(() => {})
        }
        ElMessage.success('美化完成')
      } else if (lastEvent === 'content') {
        polishResult.content += data
      } else if (lastEvent === 'error') {
        polishResult.status = 'error'
        polishLoading.value = false
        ElMessage.error(data || '美化失败')
      }
    }

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      buffer = buffer.replace(/\r\n/g, '\n')

      let sepIndex
      while ((sepIndex = buffer.indexOf('\n\n')) !== -1) {
        const rawEvent = buffer.slice(0, sepIndex)
        buffer = buffer.slice(sepIndex + 2)
        processRawEvent(rawEvent)
      }
    }

    // 流结束时可能没有以空行结尾，补一次 flush，避免丢最后一帧
    if (buffer && buffer.trim()) {
      processRawEvent(buffer)
    }
  } catch (error) {
    polishResult.status = 'error'
    polishLoading.value = false
    ElMessage.error('处理失败')
  }
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

const useForContinue = () => {
  router.push({
    path: '/ai-writing/continue',
    query: { content: encodeURIComponent(polishResult.content) }
  })
}

const formatTabTime = () => {
  const d = new Date()
  const MM = String(d.getMonth() + 1).padStart(2, '0')
  const DD = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${MM}/${DD} ${hh}:${mm}`
}

/** 把美化后的新文案送到 MarkdownEditor 的新标签页，并跳转过去 */
const startEditFromPolish = () => {
  const content = (polishResult.content ?? '').trim()
  if (!content) {
    ElMessage.warning('当前没有可编辑的美化结果')
    return
  }
  const platform = polishResult.platformName || '默认'
  const taskId = polishResult.taskId && polishResult.taskId !== '-' ? polishResult.taskId : ''
  const title = `美化-${platform}${taskId ? `-${taskId}` : ''}-${formatTabTime()}`
  markdownCache.openNewTab({ title, content })
  router.push({ path: '/markdownEdit' })
}
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

.compare-box {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px;
  min-height: 200px;
  line-height: 1.8;
  white-space: pre-wrap;
  margin-top: 10px;
}

.compare-box.result {
  background: #f0f9ff;
  border-left: 4px solid #409eff;
}

.compare-box.result.markdown-body {
  white-space: normal;
}

.compare-box.result.markdown-body :deep(p) { margin: 0.5em 0; }
.compare-box.result.markdown-body :deep(ul),
.compare-box.result.markdown-body :deep(ol) { margin: 0.5em 0; padding-left: 1.5em; }
.compare-box.result.markdown-body :deep(strong) { font-weight: 600; }

.action-bar {
  display: flex;
  gap: 10px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}
</style>