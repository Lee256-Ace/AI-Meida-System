<template>
  <!-- 移除 app-container，改用一个不带 padding 的满屏容器 -->
  <div class="editor-fullscreen-container">
    <!-- 1. 标签栏：紧贴边缘 -->
    <div class="tabs-header-bar">
      <div
          v-for="(tab, index) in tabs"
          :key="tab.id"
          :class="['tab-node', { active: activeTabId === tab.id }]"
          @click="activeTabId = tab.id"
          @dblclick="editTabTitle(tab)"
      >
        <input
            v-if="tab.isEditing"
            v-model="tab.title"
            class="tab-input-box"
            v-focus
            @blur="doneEdit(tab)"
            @keyup.enter="doneEdit(tab)"
            @click.stop
        />
        <span v-else class="tab-label-text">{{ tab.title   || '新文档' }}</span>
        <span class="tab-close-btn" @click.stop="removeTab(index)">×</span>
      </div>
      <button class="add-tab-btn" @click="addTab">+</button>
    </div>

    <!-- 2. 编辑器主体：通过 Flex 1 占据所有剩余空间 -->
    <div class="editor-content-area">
      <Editor
          :value="activeTabContent"
          :plugins="plugins"
          :locale="zhHans"
          mode="split"
          :placeholder="placeholder"
          class="bytemd-instance-override"
          @change="handleChange"
      />
    </div>
    <!-- 3. 左下角功能操作栏 -->
    <div class="editor-footer">
      <div class="action-group top-row">
        <el-button
            type="success"
            size="small"
            plain
            icon="MagicStick"
            @click="goPolish"
        >
          美化此篇
        </el-button>
        <el-button
            type="warning"
            size="small"
            plain
            icon="EditPen"
            @click="goContinue"
        >
          续写此篇
        </el-button>
        <el-button
            type="info"
            size="small"
            plain
            icon="VideoPlay"
            @click="goGenerate"
        >
          文案生成
        </el-button>
      </div>
      <div class="action-group bottom-row">
        <el-button type="primary" link icon="DocumentCopy" @click="copyAll">复制文案</el-button>
        <el-button type="primary" link icon="Download" @click="downloadAll">下载文案</el-button>
        <el-button type="primary" link icon="EditPen" :loading="saveLoading" @click="saveCopy">保存文案</el-button>
        <el-button type="primary" link @click="openPublishDialog">
          <el-icon class="el-icon--left"><Promotion /></el-icon>
          发布
        </el-button>
      </div>
    </div>

    <!-- 发布向导：复制正文 + 跳转各平台创作页（无官方代发 API） -->
    <el-dialog
        v-model="publishDialogVisible"
        title="发布到社交平台"
        width="520px"
        destroy-on-close
        class="publish-guide-dialog"
    >
      <p class="publish-guide-tip">
        小红书、抖音、视频号等平台<strong>不向第三方开放「一键代发」接口</strong>，需使用浏览器登录官方创作后台。
        下面会<strong>复制当前标签正文</strong>并<strong>打开发布页</strong>，您粘贴后即可发布。
      </p>
      <el-form label-width="88px" class="publish-guide-form">
        <el-form-item label="目标平台">
          <el-select v-model="publishPlatformKey" placeholder="请选择" style="width: 100%">
            <el-option
                v-for="p in publishPlatforms"
                :key="p.key"
                :label="p.name"
                :value="p.key"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="标题/主题">
          <el-input v-model="publishTitle" placeholder="与正文一起登记到「历史发布」时使用" clearable />
        </el-form-item>
        <el-form-item label="复制格式">
          <el-checkbox v-model="publishCopyAsPlain">复制为纯文本（去掉常见 Markdown 符号，便于粘贴到部分编辑器）</el-checkbox>
        </el-form-item>
        <el-form-item v-if="selectedPublishPlatform" label="说明">
          <span class="publish-platform-hint">{{ selectedPublishPlatform.hint }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishDialogVisible = false">关闭</el-button>
        <el-button :loading="publishRegisterLoading" @click="registerPublishFromEditor">登记到历史发布</el-button>
        <el-button type="primary" :loading="publishCopyOpenLoading" @click="copyAndOpenPublishPage">
          复制正文并打开发布页
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {ref, computed, onMounted, onBeforeUnmount} from 'vue'
import { useRouter } from 'vue-router'
import { Editor } from '@bytemd/vue-next'
import gfm from '@bytemd/plugin-gfm'
import highlight from '@bytemd/plugin-highlight'
import math from '@bytemd/plugin-math'
import mermaid from '@bytemd/plugin-mermaid'
import zhHans from 'bytemd/locales/zh_Hans.json'
import { MagicStick, EditPen, VideoPlay, DocumentCopy, Download, Promotion } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useMarkdownCacheStore } from '@/stores/markDownCache'
import { saveMarkdownCopy } from '@/api/markdownCopy'
import { addPublishHistory } from '@/api/publishHistory'
import 'bytemd/dist/index.css'
import 'katex/dist/katex.css'
import 'highlight.js/styles/github.css'

const cacheStore = useMarkdownCacheStore()
const router = useRouter()
const vFocus = { mounted: (el) => el.focus() }
const props = defineProps({ placeholder: { type: String, default: '开始创作...' } })

const tabs = ref([
  { id: 1, title: '新建文档 1', content: '# 营销文案\n\n在此编写...\n\n\n\n\n\n\n\n\n\n', isEditing: false, savedCopyId: null }
])
const activeTabId = ref(1)
const saveLoading = ref(false)
const plugins = [gfm(), highlight(), math(), mermaid()]

const activeTabContent = computed(() => {
  return tabs.value.find(t => t.id === activeTabId.value)?.content || ''
})

const editTabTitle = (tab) => { tab.isEditing = true }
const doneEdit = (tab) => { tab.isEditing = false; if (!tab.title.trim()) tab.title = '未命名' }

const addTab = () => {
  const newId = Date.now()
  tabs.value.push({ id: newId, title: `新建文档 ${tabs.value.length + 1}`, content: '# 营销文案\n\n在此编写...\n\n\n\n\n\n\n\n\n\n', isEditing: false, savedCopyId: null })
  activeTabId.value = newId
}

const removeTab = (index) => {
  if (tabs.value.length <= 1) return
  const id = tabs.value[index].id
  tabs.value.splice(index, 1)
  if (activeTabId.value === id) activeTabId.value = tabs.value[0].id
}

const handleChange = (v) => {
  const tab = tabs.value.find(t => t.id === activeTabId.value)
  if (tab) tab.content = v
}

const getActiveTab = () => tabs.value.find(t => t.id === activeTabId.value)

/** 各平台官方创作入口（链接可能随平台改版调整，以登录后实际页面为准） */
const publishPlatforms = [
  {
    key: 'xiaohongshu',
    name: '小红书',
    url: 'https://creator.xiaohongshu.com/publish/publish',
    hint: '需已开通小红书「专业号/创作者」并在电脑浏览器登录创作服务平台，在发布页粘贴正文与配图。'
  },
  {
    key: 'douyin',
    name: '抖音',
    url: 'https://creator.douyin.com/creator-micro/content/publish',
    hint: '建议在已登录状态下使用；短视频多在抖音 App 内发布，网页端以实际入口为准。'
  },
  {
    key: 'channels',
    name: '微信视频号',
    url: 'https://channels.weixin.qq.com/platform/post/create',
    hint: '需微信扫码登录视频号助手；部分账号仅支持手机端发表，以平台提示为准。'
  },
  {
    key: 'toutiao',
    name: '今日头条',
    url: 'https://mp.toutiao.com/profile_v4/graphic/publish',
    hint: '需登录头条号后台；图文发布入口以当前后台菜单为准。'
  },
  {
    key: 'weibo',
    name: '微博',
    url: 'https://weibo.com/',
    hint: '在首页顶部发布框粘贴；长文可考虑「头条文章」等入口。'
  },
  {
    key: 'bilibili',
    name: '哔哩哔哩',
    url: 'https://member.bilibili.com/platform/upload/text/init',
    hint: '专栏投稿入口若变更，请从创作中心进入「投稿」选择图文/专栏。'
  }
]

const publishDialogVisible = ref(false)
const publishPlatformKey = ref('xiaohongshu')
const publishTitle = ref('')
const publishCopyAsPlain = ref(false)
const publishRegisterLoading = ref(false)
const publishCopyOpenLoading = ref(false)

const selectedPublishPlatform = computed(() =>
  publishPlatforms.find((p) => p.key === publishPlatformKey.value) || null
)

/** 粗略将 Markdown 转为纯文本，便于粘贴到只支持纯文本的框 */
const markdownToPlainText = (md) => {
  if (md == null) return ''
  let s = String(md)
  s = s.replace(/\r\n/g, '\n')
  s = s.replace(/^#{1,6}\s+/gm, '')
  s = s.replace(/\*\*([^*]+)\*\*/g, '$1')
  s = s.replace(/\*([^*]+)\*/g, '$1')
  s = s.replace(/~~([^~]+)~~/g, '$1')
  s = s.replace(/```[\s\S]*?```/g, (block) => block.replace(/```\w*\n?/g, '').replace(/```/g, ''))
  s = s.replace(/`([^`]+)`/g, '$1')
  s = s.replace(/^\s{0,3}[-*+]\s+/gm, '')
  s = s.replace(/^\s*\d+\.\s+/gm, '')
  s = s.replace(/\[([^\]]+)]\([^)]+\)/g, '$1')
  s = s.replace(/\n{3,}/g, '\n\n')
  return s.trim()
}

const getPublishBodyText = () => {
  const tab = getActiveTab()
  const raw = tab?.content ?? ''
  return publishCopyAsPlain.value ? markdownToPlainText(raw) : raw
}

const openPublishDialog = () => {
  const tab = getActiveTab()
  if (!tab || !String(tab.content || '').trim()) {
    ElMessage.warning('当前标签正文为空，无法发布')
    return
  }
  publishTitle.value = (tab.title || '').trim() || '未命名'
  publishPlatformKey.value = 'xiaohongshu'
  publishCopyAsPlain.value = false
  publishDialogVisible.value = true
}

/** 构建写入 publish_history 的请求体（正文为当前标签完整 Markdown） */
const buildPublishHistoryPayload = (remark) => {
  const p = selectedPublishPlatform.value
  const tab = getActiveTab()
  if (!p || !tab) return null
  const content = tab.content ?? ''
  if (!content.trim()) return null
  return {
    platformName: p.name,
    title: (publishTitle.value || tab.title || '未命名').trim(),
    content,
    publishUrl: '',
    status: 'recorded',
    remark: remark || '自 Markdown 编辑器发布向导登记'
  }
}

const copyAndOpenPublishPage = async () => {
  const p = selectedPublishPlatform.value
  if (!p) {
    ElMessage.warning('请选择平台')
    return
  }
  const text = getPublishBodyText()
  if (!text.trim()) {
    ElMessage.warning('没有可复制的内容')
    return
  }
  const historyPayload = buildPublishHistoryPayload('自编辑器：复制正文并打开发布页')
  publishCopyOpenLoading.value = true
  try {
    await navigator.clipboard.writeText(text)
    window.open(p.url, '_blank', 'noopener,noreferrer')
    let savedOk = false
    if (historyPayload) {
      try {
        await addPublishHistory(historyPayload)
        savedOk = true
      } catch {
        /* 接口错误由拦截器提示；复制与打开仍视为成功 */
      }
    }
    if (savedOk) {
      ElMessage.success(
        `已复制正文、已打开发布页（${p.name}），并已保存至「历史发布」。登录后粘贴即可发布。`
      )
    } else {
      ElMessage.success(
        `已复制正文，已打开发布页：${p.name}。请在浏览器中登录后粘贴发布。（发布记录未写入时可从侧栏「历史发布」手动登记）`
      )
    }
    publishDialogVisible.value = false
  } catch (e) {
    console.error(e)
    ElMessage.error('复制失败，请检查浏览器权限或手动全选复制')
  } finally {
    publishCopyOpenLoading.value = false
  }
}

const registerPublishFromEditor = async () => {
  const payload = buildPublishHistoryPayload('自 Markdown 编辑器发布向导登记')
  if (!payload) {
    ElMessage.warning('当前标签正文为空')
    return
  }
  publishRegisterLoading.value = true
  try {
    await addPublishHistory(payload)
    ElMessage.success('已写入「历史发布」，可在侧栏进入补充链接与状态')
    publishDialogVisible.value = false
  } catch {
    /* 错误由拦截器提示 */
  } finally {
    publishRegisterLoading.value = false
  }
}

/** 跳转到文章美化，并把当前标签页内容带过去 */
const goPolish = () => {
  const tab = getActiveTab()
  const content = tab?.content?.trim()
  if (!content) {
    ElMessage.warning('当前篇章为空，无法跳转到文章美化')
    return
  }
  router.push({
    path: '/ai-writing/polish',
    query: { content: encodeURIComponent(content) }
  })
}

/** 跳转到文章续写，并把当前标签页内容带过去 */
const goContinue = () => {
  const tab = getActiveTab()
  const content = tab?.content?.trim()
  if (!content) {
    ElMessage.warning('当前篇章为空，无法跳转到文章续写')
    return
  }
  router.push({
    path: '/ai-writing/continue',
    query: { content: encodeURIComponent(content) }
  })
}

/** 跳转到文案生成，并把当前标签页内容作为 prompt 带过去 */
const goGenerate = () => {
  const tab = getActiveTab()
  const prompt = tab?.content?.trim()
  if (!prompt) {
    ElMessage.warning('当前篇章为空，无法跳转到文案生成')
    return
  }
  router.push({
    path: '/ai-writing/generate',
    query: { prompt: encodeURIComponent(prompt) }
  })
}

/** 把当前标签页内容当作 prompt，让 AI 直接覆盖当前标签页正文 */
const generateThis = async () => {
  if (generateLoading.value || polishLoading.value || continueLoading.value) return

  const tabId = activeTabId.value
  const tab = tabs.value.find(t => t.id === tabId)
  const prompt = tab?.content?.trim()
  if (!prompt) {
    ElMessage.warning('当前篇章为空，无法开始生成')
    return
  }

  // 清空并开始写入流式结果
  if (genEventSource) {
    try { genEventSource.close() } catch (_) {}
  }

  generateLoading.value = true
  tab.content = ''

  let taskId = null
  genEventSource = generateStream({
    prompt,
    platforms: ['douyin', 'xiaohongshu', 'video_account', 'toutiao'],
    targetAudience: undefined
  })

  genEventSource.addEventListener('taskId', (e) => {
    taskId = parseInt(e.data, 10)
  })

  genEventSource.addEventListener('content', (e) => {
    const t = tabs.value.find(x => x.id === tabId)
    if (t) t.content += e.data
  })

  genEventSource.addEventListener('complete', async () => {
    generateLoading.value = false
    try {
      if (typeof taskId === 'number' && !Number.isNaN(taskId)) {
        const res = await getGenerationTask(taskId)
        const t = tabs.value.find(x => x.id === tabId)
        if (t && res && res.content) t.content = res.content
      }
    } catch (_) {
      // ignore：不影响已流式写入的内容
    } finally {
      try { genEventSource.close() } catch (_) {}
      genEventSource = null
    }
    ElMessage.success('生成完成')
  })

  genEventSource.addEventListener('error', () => {
    generateLoading.value = false
    try { genEventSource.close() } catch (_) {}
    genEventSource = null
    ElMessage.error('生成失败')
  })
}

/** 美化：把当前标签页正文当作原文，流式覆盖为美化结果 */
const polishThis = async () => {
  if (polishLoading.value || generateLoading.value || continueLoading.value) return

  const tabId = activeTabId.value
  const tab = tabs.value.find(t => t.id === tabId)
  const originalContent = tab?.content?.trim()
  if (!originalContent) {
    ElMessage.warning('当前篇章为空，无法美化')
    return
  }

  polishLoading.value = true
  tab.content = ''

  let taskId = null
  let finished = false

  try {
    const response = await polishStream({
      content: originalContent,
      type: 'enhance',
      targetAudience: '大众',
      platform: undefined
    })

    const reader = response.body?.getReader()
    if (!reader) throw new Error('No response.body')

    const decoder = new TextDecoder()
    let lastEvent = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      const text = decoder.decode(value)
      const lines = text.split('\n')
      lines.forEach(line => {
        if (finished) return

        if (line.startsWith('event:')) {
          lastEvent = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
          const data = line.slice(5).trim()

          if (lastEvent === 'taskId') {
            taskId = parseInt(data, 10)
          } else if (lastEvent === 'content') {
            const t = tabs.value.find(x => x.id === tabId)
            if (t) t.content += data
          } else if (lastEvent === 'complete' && (data === '[DONE]' || data === 'done')) {
            finished = true
            polishLoading.value = false

            // 以任务详情结果为准（保证拿到最终完整文本）
            ;(async () => {
              try {
                if (typeof taskId === 'number' && !Number.isNaN(taskId)) {
                  const res = await getPolishTask(taskId)
                  const t = tabs.value.find(x => x.id === tabId)
                  if (t) t.content = (res?.resultContent ?? res?.result_content ?? '') || ''
                }
              } catch (_) {
                // ignore
              } finally {
                ElMessage.success('美化完成')
              }
            })()
          } else if (lastEvent === 'error') {
            finished = true
            polishLoading.value = false
            ElMessage.error(data || '美化失败')
          }
        }
      })

      if (finished) break
    }
  } catch (e) {
    polishLoading.value = false
    ElMessage.error('美化失败')
  }
}

/** 续写：保留原文，并把 AI 续写追加到原文末尾 */
const continueThis = async () => {
  if (continueLoading.value || generateLoading.value || polishLoading.value) return

  const tabId = activeTabId.value
  const tab = tabs.value.find(t => t.id === tabId)
  const baseContent = tab?.content ?? ''
  const originalContent = baseContent.trim()
  if (!originalContent) {
    ElMessage.warning('当前篇章为空，无法续写')
    return
  }

  continueLoading.value = true
  // 保留原文；流式 content 直接追加
  tab.content = baseContent

  let taskId = null
  let finished = false

  try {
    const response = await continueStream({
      content: originalContent,
      hint: undefined,
      platform: undefined,
      extendLength: '中等',
      extendStyle: '保持原风格'
    })

    const reader = response.body?.getReader()
    if (!reader) throw new Error('No response.body')

    const decoder = new TextDecoder()
    let lastEvent = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      const text = decoder.decode(value)
      const lines = text.split('\n')
      lines.forEach(line => {
        if (finished) return

        if (line.startsWith('event:')) {
          lastEvent = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
          const data = line.slice(5).trim()

          if (lastEvent === 'taskId') {
            taskId = parseInt(data, 10)
          } else if (lastEvent === 'content') {
            const t = tabs.value.find(x => x.id === tabId)
            if (t) t.content += data
          } else if (lastEvent === 'complete' && (data === '[DONE]' || data === 'done')) {
            finished = true
            continueLoading.value = false

            ;(async () => {
              try {
                if (typeof taskId === 'number' && !Number.isNaN(taskId)) {
                  const res = await getContinuationTask(taskId)
                  const t = tabs.value.find(x => x.id === tabId)
                  const continuation = (res?.continuation ?? '') || ''
                  if (t) {
                    const sep = baseContent && continuation
                      ? (continuation.startsWith('\n') ? '' : (baseContent.endsWith('\n') ? '\n' : '\n\n'))
                      : ''
                    t.content = baseContent + sep + continuation
                  }
                }
              } catch (_) {
                // ignore
              } finally {
                ElMessage.success('续写完成')
              }
            })()
          } else if (lastEvent === 'error') {
            finished = true
            continueLoading.value = false
            ElMessage.error(data || '续写失败')
          }
        }
      })

      if (finished) break
    }
  } catch (e) {
    continueLoading.value = false
    ElMessage.error('续写失败')
  }
}

const downloadAll = () => {
  // 1. 获取当前激活的标签对象
  const currentTab = tabs.value.find(t => t.id === activeTabId.value)

  if (!currentTab || !currentTab.content) {
    // 如果没有内容，可以弹窗提醒 (假设你使用了 Element Plus 的 ElMessage)
    // ElMessage.warning('当前篇章没有内容可以下载')
    return
  }

  // 2. 创建 Blob 对象 (类文件对象)
  // 指定类型为 text/markdown 或 text/plain
  const blob = new Blob([currentTab.content], { type: 'text/markdown' })

  // 3. 创建下载链接
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')

  // 4. 设置文件名 (使用标签页的标题)
  link.href = url
  link.download = `${currentTab.title || '未命名文档'}.md`

  // 5. 触发点击并清理
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url) // 释放内存
}
/**
 * 复制当前编辑区的文案
 */
/** 将当前标签 Markdown 保存到 MySQL（ai_markdown_copy），同一标签再次保存为更新 */
const saveCopy = async () => {
  const currentTab = tabs.value.find(t => t.id === activeTabId.value)
  if (!currentTab || !currentTab.content.trim()) {
    ElMessage.warning({ message: '当前篇章内容为空，无法保存', grouping: true })
    return
  }
  saveLoading.value = true
  try {
    const payload = {
      title: currentTab.title || '未命名',
      content: currentTab.content
    }
    if (currentTab.savedCopyId != null) {
      payload.id = currentTab.savedCopyId
    }
    const res = await saveMarkdownCopy(payload)
    if (res && res.id != null) {
      currentTab.savedCopyId = res.id
      ElMessage.success({
        message: `已保存到数据库（#${res.id}）`,
        duration: 2000,
        showClose: true
      })
    }
  } catch {
    // 错误提示由 request 拦截器处理
  } finally {
    saveLoading.value = false
  }
}

const copyAll = async () => {
  // 1. 获取当前正在编辑的标签页对象
  const currentTab = tabs.value.find(t => t.id === activeTabId.value)

  // 2. 检查是否有内容
  if (!currentTab || !currentTab.content.trim()) {
    ElMessage.warning({
      message: '当前篇章内容为空，无法复制',
      grouping: true // 防止重复点击弹出多个消息
    })
    return
  }

  try {
    // 3. 执行写入剪贴板
    await navigator.clipboard.writeText(currentTab.content)

    // 4. 成功反馈
    ElMessage.success({
      message: `已成功复制《${currentTab.title}》全文`,
      duration: 2000, // 2秒后自动消失
      showClose: true
    })
  } catch (err) {
    // 5. 失败反馈（例如浏览器安全限制）
    console.error('复制出错:', err)
    ElMessage.error('复制失败，请尝试手动选中复制')
  }
}

// 1. 页面挂载时：从 Store 恢复数据
onMounted(() => {
  if (cacheStore.tabs.length > 0) {
    tabs.value = cacheStore.tabs.map((t) => ({
      ...t,
      savedCopyId: t.savedCopyId != null ? t.savedCopyId : null
    }))
    activeTabId.value = cacheStore.activeTabId
  }
})

// 2. 页面卸载（切换路由）前：将当前状态存入 Store
onBeforeUnmount(() => {
  // 就像实例代码中那样，离开前“锁死”当前进度
  cacheStore.saveEditorState(tabs.value, activeTabId.value)
})

</script>

<style scoped>
/* 整个编辑组件的包裹容器 */
.editor-fullscreen-container {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 160px); /* 根据你的页面顶部高度微调 */
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden; /* 严禁外溢 */
  margin: 0;
}

/* 中间编辑区域 */
.editor-content-area {
  flex: 1;
  min-height: 0; /* 解决 Flex 容器被内容撑开的经典 Bug */
  max-height: 100%;
  overflow-y: auto;
  display: flex;
  border-bottom: 1px solid #ebeef5;
}

/* 深度穿透 ByteMD */
.bytemd-instance-override :deep(.bytemd) {
  height: 100% !important;
  width: 100% !important;
}

/* 底部操作栏（固定在最下方） */
.editor-footer {
  height: auto;
  padding: 12px 16px;
  background: #fff;
  flex-shrink: 0; /* 确保操作栏不会因为编辑器太大而被挤扁 */
  z-index: 10;
}

/* 标签栏 */
.tabs-header-bar {
  flex-shrink: 0; /* 确保标签栏高度固定 */
  background: #f5f7fa;
  /*display: flex;
  align-items: center;
  background: #f5f7fa; /* 匹配 .reasoning-box 的背景色 */
  padding: 8px 12px 0;
  border-bottom: 1px solid #ebeef5;
}

.tab-node {
  display: flex;
  align-items: center;
  height: 36px;
  padding: 0 16px;
  background: #f0f2f5;
  border: 1px solid #ebeef5;
  border-bottom: none;
  border-radius: 4px 4px 0 0;
  margin-right: 4px;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
  transition: all 0.2s;
}

.tab-node.active {
  background: #fff;
  color: #409eff; /* 匹配你 CSS 中的主色 */
  font-weight: 600;
  height: 37px; /* 盖住底边线 */
  margin-bottom: -1px;
  z-index: 10;
}

.tab-label-text {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tab-close-btn {
  margin-left: 10px;
  font-size: 16px;
  color: #c0c4cc;
}
.tab-close-btn:hover { color: #f56c6c; }

.add-tab-btn {
  border: none;
  background: none;
  font-size: 20px;
  color: #909399;
  cursor: pointer;
  padding: 0 10px;
}

/* 3. 编辑器主体：强制纵向拉伸 */
.editor-content-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0; /* 防止内容撑破 flex 容器 */
}

/* 强制覆盖 ByteMD 内部的所有高度限制 */
.bytemd-instance-override :deep(.bytemd) {
  height: 100% !important;
  border: none;
}

.bytemd-instance-override :deep(.bytemd-main) {
  height: calc(100% - 40px) !important; /* 减去工具栏高度 */
}

.bytemd-instance-override :deep(.bytemd-editor),
.bytemd-instance-override :deep(.bytemd-preview) {
  height: 100% !important;
}

/* 隐藏特定的工具栏图标 */
.bytemd-instance-override :deep(.bytemd-toolbar-icon.bytemd-tippy-right[bytemd-tippy-path="5"]),
.bytemd-instance-override :deep(.bytemd-toolbar-icon:has(svg path[d*="29.344 30.477"])) {
  display: none !important;
}

/* TODO /

 */
   /* 1. 顶层容器高度计算 */
 .editor-fullscreen-container {
   display: flex;
   flex-direction: column;
   /* 这里的高度要根据你的顶栏高度微调，确保不出现页面滚动条 */
   height: calc(100vh - 120px);
   background: #fff;
   border: 1px solid #ebeef5;
   border-radius: 4px;
   overflow: hidden;
   margin: -10px -10px 0 -10px;
 }

.bytemd-instance-override :deep(.bytemd) {
  height: 100% !important;
  width: 100%;
}

  /* 全屏时置于最顶层，确保工具栏和「退出全屏」按钮可见、可点击 */
.editor-fullscreen-container :deep(.bytemd-fullscreen.bytemd) {
  z-index: 9999;
}

/* 3. 底部操作栏样式 */
.editor-footer {
  padding: 12px 16px;
  background: #fff;
  border-top: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.action-group {
  display: flex;
  gap: 12px;
  align-items: center;
}

.bottom-row {
  margin-top: 4px;
  padding-top: 8px;
  border-top: 1px dashed #f0f2f5;
}

/* 之前提供的标签栏相关 CSS 保持不变... */
.tabs-header-bar {
  display: flex;
  align-items: center;
  background: #f5f7fa;
  padding: 8px 12px 0;
  border-bottom: 1px solid #ebeef5;
}

.publish-guide-tip {
  margin: 0 0 16px;
  font-size: 13px;
  line-height: 1.65;
  color: #606266;
}
.publish-guide-form {
  margin-top: 8px;
}
.publish-platform-hint {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}
</style>