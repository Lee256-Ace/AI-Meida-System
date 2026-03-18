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
<!--        TODO 增加保存入MySQL的前后端功能-->
        <el-button type="primary" link icon="EditPen" @click=" ">保存文案</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
// ... (script 逻辑保持不变，确保 vFocus 指令存在)
import { ref, computed } from 'vue'
import { Editor } from '@bytemd/vue-next'
import gfm from '@bytemd/plugin-gfm'
import highlight from '@bytemd/plugin-highlight'
import math from '@bytemd/plugin-math'
import mermaid from '@bytemd/plugin-mermaid'
import zhHans from 'bytemd/locales/zh_Hans.json'
import { MagicStick, EditPen, VideoPlay, DocumentCopy, Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

import 'bytemd/dist/index.css'
import 'katex/dist/katex.css'
import 'highlight.js/styles/github.css'

const vFocus = { mounted: (el) => el.focus() }
const props = defineProps({ placeholder: { type: String, default: '开始创作...' } })

const tabs = ref([
  { id: 1, title: '新建文档 1', content: '# 营销文案\n\n在此编写...\n\n\n\n\n\n\n\n\n\n', isEditing: false }
])
const activeTabId = ref(1)
const plugins = [gfm(), highlight(), math(), mermaid()]

const activeTabContent = computed(() => {
  return tabs.value.find(t => t.id === activeTabId.value)?.content || ''
})

const editTabTitle = (tab) => { tab.isEditing = true }
const doneEdit = (tab) => { tab.isEditing = false; if (!tab.title.trim()) tab.title = '未命名' }

const addTab = () => {
  const newId = Date.now()
  tabs.value.push({ id: newId, title: `新建文档 ${tabs.value.length + 1}`, content: '# 营销文案\n\n在此编写...\n\n\n\n\n\n\n\n\n\n', isEditing: false })
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
   示例

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

</script>

<style scoped>
/* 1. 顶层容器：强制减去外部 Padding 的影响 */
.editor-fullscreen-container {
  display: flex;
  flex-direction: column;
  /* 关键：视口高度减去外部导航栏的高度（根据你的实际情况调整） */
  height: calc(100vh - 140px);
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
  /* 抵消父级 .app-container 可能存在的 padding 影响 */
  margin: -10px -10px 0 -10px;
}

/* 2. 标签栏：适配你实例中的 Element 风格色彩 */
.tabs-header-bar {
  display: flex;
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

/* 2. 编辑器主体撑满中间 */
.editor-content-area {
  flex: 1;
  display: flex;
  min-height: 0;
}
.bytemd-instance-override :deep(.bytemd) {
  height: 100% !important;
  width: 100%;
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