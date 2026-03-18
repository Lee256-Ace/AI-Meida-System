<template>
  <div class="app-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>输入文章开头</span>
        </div>
      </template>

      <el-input
          v-model="continueForm.content"
          type="textarea"
          :rows="8"
          placeholder="输入文章的开头部分，AI 将为你续写后续内容..."
      />

      <div class="options-row">
        <div class="option-item">
          <label>所属平台：</label>
          <el-select v-model="continueForm.platform" clearable placeholder="可选，用于分类" style="width: 140px">
            <el-option label="抖音" value="douyin" />
            <el-option label="小红书" value="xiaohongshu" />
            <el-option label="视频号" value="video_account" />
            <el-option label="今日头条" value="toutiao" />
          </el-select>
        </div>
        <div class="option-item">
          <label>续写长度：</label>
          <el-select v-model="continueForm.extendLength" placeholder="选填" clearable style="width: 120px">
            <el-option label="短篇" value="短篇" />
            <el-option label="中等" value="中等" />
            <el-option label="长篇" value="长篇" />
          </el-select>
        </div>
        <div class="option-item">
          <label>续写风格：</label>
          <el-select v-model="continueForm.extendStyle" placeholder="选填" clearable style="width: 140px">
            <el-option label="保持原风格" value="保持原风格" />
            <el-option label="更正式" value="更正式" />
            <el-option label="更生动" value="更生动" />
          </el-select>
        </div>
        <div class="option-item" style="flex: 1;">
          <label>续写提示（可选）：</label>
          <el-input v-model="continueForm.hint" type="text" placeholder="如：转向悬疑情节、加入对话描写..." style="width: 100%;" />
        </div>
      </div>

      <el-button
          type="primary"
          size="large"
          :loading="continueLoading"
          @click="startContinue"
      >
        <el-icon><DocumentAdd /></el-icon>
        {{ continueLoading ? '处理中...' : '开始续写' }}
      </el-button>
    </el-card>

    <!-- 续写结果 -->
    <el-card v-if="continueResult.show" class="box-card result-card">
      <template #header>
        <div class="card-header">
          <span>任务ID: <el-tag>{{ continueResult.taskId }}</el-tag></span>
          <el-tag v-if="continueResult.platformName" type="info" size="small">{{ continueResult.platformName }}</el-tag>
          <el-tag :type="statusType">{{ statusText }}</el-tag>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col :xs="24" :sm="12">
          <h4>原文开头</h4>
          <div class="compare-box original">{{ continueForm.content }}</div>
        </el-col>
        <el-col :xs="24" :sm="12">
          <h4>续写部分</h4>
          <div class="compare-box result markdown-body" v-html="renderMarkdown(continueResult.content)" />
        </el-col>
      </el-row>

      <div class="action-bar">
        <el-button @click="copyText(continueResult.content)">
          <el-icon><DocumentCopy /></el-icon> 复制结果
        </el-button>
        <el-button @click="downloadText(continueResult.content, 'continuation')">
          <el-icon><Download /></el-icon> 下载
        </el-button>
<!--        TODO-->
        <el-button @click=" ">
          <el-icon>< /></el-icon> 开始编辑
        </el-button>
        <el-button type="warning" @click="continueAgain">
          <el-icon><DocumentAdd /></el-icon> 继续续写
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
defineOptions({ name: 'AiContinue' })
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { DocumentCopy, Download, DocumentAdd } from '@element-plus/icons-vue'
import { continueStream, getContinuationTask } from '@/api/aiWriting'
import { useAiWritingCacheStore } from '@/stores/aiWritingCache'
import { renderMarkdown } from '@/utils/markdown'

const PLATFORM_NAMES = { douyin: '抖音', xiaohongshu: '小红书', video_account: '视频号', toutiao: '今日头条' }

const route = useRoute()
const cacheStore = useAiWritingCacheStore()

const continueForm = reactive({
  content: '',
  hint: '',
  platform: '',
  extendLength: '中等',
  extendStyle: '保持原风格'
})

const continueLoading = ref(false)
const continueResult = reactive({
  show: false,
  taskId: '-',
  status: 'processing',
  content: '',
  platformName: ''
})

const statusType = computed(() => ({
  processing: 'warning',
  completed: 'success',
  error: 'danger'
}[continueResult.status] || 'info'))

const statusText = computed(() => ({
  processing: '处理中...',
  completed: '✓ 已完成',
  error: '✗ 失败'
}[continueResult.status] || ''))

// 从路由参数获取内容与平台（文案生成页按篇跳转时带 platform）；否则从缓存恢复
onMounted(() => {
  if (route.query.content) {
    continueForm.content = decodeURIComponent(route.query.content)
    if (route.query.platform) continueForm.platform = route.query.platform
  } else {
    const c = cacheStore.continue
    if (c.form.content || c.result.content || c.result.show) {
      continueForm.content = c.form.content
      continueForm.hint = c.form.hint
      continueForm.platform = c.form.platform
      continueForm.extendLength = c.form.extendLength
      continueForm.extendStyle = c.form.extendStyle
      continueResult.show = c.result.show
      continueResult.taskId = c.result.taskId
      continueResult.status = c.result.status
      continueResult.content = c.result.content
      continueResult.platformName = c.result.platformName
    }
  }
})

onBeforeUnmount(() => {
  cacheStore.saveContinue(continueForm, continueResult)
})

const startContinue = async () => {
  const content = continueForm.content.trim()
  if (!content) {
    ElMessage.warning('请输入文章开头')
    return
  }

  continueLoading.value = true
  continueResult.show = true
  continueResult.content = ''
  continueResult.platformName = ''
  continueResult.status = 'processing'
  continueResult.taskId = '-'

  try {
    const response = await continueStream({
      content,
      hint: continueForm.hint || undefined,
      platform: continueForm.platform || undefined,
      extendLength: continueForm.extendLength || undefined,
      extendStyle: continueForm.extendStyle || undefined
    })

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let lastEvent = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      const text = decoder.decode(value)
      const lines = text.split('\n')

      lines.forEach(line => {
        if (line.startsWith('event:')) {
          lastEvent = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
          const data = line.slice(5).trim()
          if (lastEvent === 'taskId') {
            continueResult.taskId = '#' + data
          } else if (lastEvent === 'complete' && (data === '[DONE]' || data === 'done')) {
            continueResult.status = 'completed'
            continueLoading.value = false
            const idStr = continueResult.taskId.replace(/^#/, '')
            const id = parseInt(idStr, 10)
            if (!isNaN(id)) {
              getContinuationTask(id).then(res => {
                if (res) {
                  if (res.platform) continueResult.platformName = PLATFORM_NAMES[res.platform] || res.platform
                  const text = res.continuation ?? ''
                  if (text) continueResult.content = text
                }
              }).catch(() => {})
            }
            ElMessage.success('续写完成')
          } else if (lastEvent === 'content') {
            continueResult.content += data
          } else if (lastEvent === 'error') {
            continueResult.status = 'error'
            continueLoading.value = false
            ElMessage.error(data || '续写失败')
          }
        }
      })
    }
  } catch (error) {
    continueResult.status = 'error'
    continueLoading.value = false
    ElMessage.error('续写失败')
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

const continueAgain = () => {
  continueForm.content = continueForm.content + '\n\n' + continueResult.content
  continueResult.show = false
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