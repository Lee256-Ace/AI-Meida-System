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
        <span v-else class="tab-label-text">{{ tab.title || '新文档' }}</span>
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
        <el-button type="success" size="small" plain icon="MagicStick">美化此篇</el-button>
        <el-button type="warning" size="small" plain icon="EditPen">续写此篇</el-button>
        <el-button type="info" size="small" plain icon="VideoPlay">开始生成</el-button>
      </div>
      <div class="action-group bottom-row">
        <el-button type="primary" link icon="DocumentCopy" @click="copyAll">复制文案</el-button>
        <el-button type="primary" link icon="Download" @click="downloadAll">下载文案</el-button>
        <el-button type="primary" link icon="EditPen" :loading="saveLoading" @click="saveCopy">保存文案</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
// ... (script 逻辑保持不变，确保 vFocus 指令存在)
import {ref, computed, onMounted, onBeforeUnmount} from 'vue'
import { Editor } from '@bytemd/vue-next'
import gfm from '@bytemd/plugin-gfm'
import highlight from '@bytemd/plugin-highlight'
import math from '@bytemd/plugin-math'
import mermaid from '@bytemd/plugin-mermaid'
import zhHans from 'bytemd/locales/zh_Hans.json'
import { MagicStick, EditPen, VideoPlay, DocumentCopy, Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useMarkdownCacheStore } from '@/stores/markDownCache'
import { saveMarkdownCopy } from '@/api/markdownCopy'

import 'bytemd/dist/index.css'
import 'katex/dist/katex.css'
import 'highlight.js/styles/github.css'

const cacheStore = useMarkdownCacheStore()
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

/* TODO// 完善将编辑区的文案投送到文案生成、文章续写、文章美化的功能
 * 示例
 *
 */

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
</style>