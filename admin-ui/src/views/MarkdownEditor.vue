<template>
  <div class="chrome-style-container">
    <!-- 1. 极简标签栏 -->
    <div class="tabs-bar">
      <div
          v-for="(tab, index) in tabs"
          :key="tab.id"
          :class="['tab-item', { active: activeTabId === tab.id }]"
          @click="activeTabId = tab.id"
          @dblclick="editTabTitle(tab)"
      >
        <input
            v-if="tab.isEditing"
            v-model="tab.title"
            class="tab-input"
            v-focus
            @blur="doneEdit(tab)"
            @keyup.enter="doneEdit(tab)"
            @click.stop
        />
        <span v-else class="tab-text">{{ tab.title || '未命名' }}</span>
        <span class="tab-close-icon" @click.stop="removeTab(index)">×</span>
      </div>
      <button class="add-btn" @click="addTab" title="新建文档">+</button>
    </div>

    <!-- 2. 编辑器主体 -->
    <div class="editor-wrapper">
      <Editor
          :value="activeTabContent"
          :plugins="plugins"
          :locale="zhHans"
          mode="split"
          :placeholder="placeholder"
          class="custom-bytemd"
          @change="handleChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Editor } from '@bytemd/vue-next'
import gfm from '@bytemd/plugin-gfm'
import highlight from '@bytemd/plugin-highlight'
import math from '@bytemd/plugin-math'
import mermaid from '@bytemd/plugin-mermaid'
import zhHans from 'bytemd/locales/zh_Hans.json'

import 'bytemd/dist/index.css'
import 'katex/dist/katex.css'
import 'highlight.js/styles/github.css'

const vFocus = { mounted: (el) => el.focus() }

const props = defineProps({
  placeholder: { type: String, default: '开始你的创作...' }
})

const tabs = ref([
  { id: 1, title: '新建文档 1', content: '# 营销文案\n\n在这里输入内容...\n\n\n\n\n\n\n\n\n\n\n\n', isEditing: false }
])
const activeTabId = ref(1)

const plugins = [gfm(), highlight(), math(), mermaid()]

const activeTabContent = computed(() => {
  return tabs.value.find(t => t.id === activeTabId.value)?.content || ''
})

const editTabTitle = (tab) => { tab.isEditing = true }
const doneEdit = (tab) => {
  tab.isEditing = false
  if (!tab.title.trim()) tab.title = '未命名'
}

const addTab = () => {
  const newId = Date.now()
  tabs.value.push({ id: newId, title: `新建文档 ${tabs.value.length + 1}`, content: '# 营销文案\n\n在这里输入内容...\n\n\n\n\n\n\n\n\n\n\n\n', isEditing: false })
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
</script>

<style scoped>
/* 容器撑满屏幕 */
.chrome-style-container {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px); /* 减去顶部面包屑和间距的高度 */
  background: #fff;
  border: 1px solid #e1e4e8;
  border-radius: 6px;
  overflow: hidden;
  margin: 10px;
}

/* 极简标签栏 */
.tabs-bar {
  display: flex;
  align-items: center;
  padding: 8px 12px 0;
  background: #fff; /* 保持与整体一致 */
  gap: 4px;
}

.tab-item {
  position: relative;
  display: flex;
  align-items: center;
  height: 32px;
  padding: 0 12px;
  min-width: 80px;
  max-width: 180px;
  cursor: pointer;
  font-size: 13px;
  color: #6a737d;
  border-radius: 6px 6px 0 0;
  transition: all 0.2s;
  border: 1px solid transparent;
  border-bottom: none;
}

.tab-item:hover {
  background: #f6f8fa;
}

.tab-item.active {
  background: #fff;
  color: #24292e;
  border-color: #e1e4e8;
  font-weight: 500;
  box-shadow: 0 -2px 8px rgba(0,0,0,0.02);
}

/* 激活状态下的底部遮挡线，实现一体化 */
.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 2px;
  background: #fff;
  z-index: 2;
}

.tab-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-right: 6px;
}

.tab-input {
  border: none;
  background: transparent;
  font-size: 13px;
  width: 100%;
  outline: none;
  color: #0366d6;
}

.tab-close-icon {
  font-size: 14px;
  color: #d1d5da;
  border-radius: 4px;
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tab-close-icon:hover {
  background: #e1e4e8;
  color: #586069;
}

.add-btn {
  border: none;
  background: transparent;
  font-size: 18px;
  color: #586069;
  cursor: pointer;
  padding: 0 8px;
  line-height: 1;
}

.add-btn:hover { color: #0366d6; }

/* 编辑器区域撑满剩余空间 */
.editor-wrapper {
  flex: 1;
  border-top: 1px solid #e1e4e8;
  z-index: 1;
}

.custom-bytemd :deep(.bytemd) {
  height: 100%;
  border: none;
}

/* 隐藏 GitHub 源码图标 (引用自 cite: 1) */
.custom-bytemd :deep(.bytemd-toolbar-icon.bytemd-tippy-right[bytemd-tippy-path="5"]),
.custom-bytemd :deep(.bytemd-toolbar-icon:has(svg path[d*="29.344 30.477"])) {
  display: none !important;
}
</style>