<template>
  <div class="app-container">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="8">
        <el-card class="stat-card">
          <div class="stat-value">{{ stats.total }}</div>
          <div class="stat-label">总记录数</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card class="stat-card">
          <div class="stat-value" style="color: #67c23a;">{{ stats.today }}</div>
          <div class="stat-label">今日新增</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card class="stat-card">
          <div class="stat-value" style="color: #e6a23c;">{{ formatNumber(stats.words) }}</div>
          <div class="stat-label">总字数</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选工具栏 -->
    <el-card class="filter-card">
      <el-row :gutter="15" align="middle">
        <el-col :xs="24" :sm="6">
          <el-select v-model="filters.type" placeholder="类型" @change="loadHistory">
            <el-option label="全部类型" value="all" />
            <el-option label="文案生成" value="generate" />
            <el-option label="文章美化" value="polish" />
            <el-option label="文章续写" value="continue" />
            <el-option label="保存文案" value="markdown" />
          </el-select>
        </el-col>
        <el-col :xs="24" :sm="6">
          <el-select v-model="filters.status" placeholder="状态" @change="loadHistory">
            <el-option label="全部状态" value="all" />
            <el-option label="已完成" value="completed" />
            <el-option label="处理中" value="processing" />
            <el-option label="失败" value="failed" />
          </el-select>
        </el-col>
        <el-col :xs="24" :sm="5">
          <el-select v-model="filters.platform" placeholder="平台" clearable @change="loadHistory">
            <el-option label="全部平台" value="" />
            <el-option label="抖音" value="douyin" />
            <el-option label="小红书" value="xiaohongshu" />
            <el-option label="视频号" value="video_account" />
            <el-option label="今日头条" value="toutiao" />
          </el-select>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-input
              v-model="filters.keyword"
              placeholder="搜索标题或内容..."
              @keyup.enter="loadHistory"
          >
            <template #append>
              <el-button @click="loadHistory">
                <el-icon><Search /></el-icon>
              </el-button>
            </template>
          </el-input>
        </el-col>
        <el-col :xs="24" :sm="4">
          <el-button @click="resetFilters">重置</el-button>
        </el-col>
      </el-row>

      <!-- 批量操作 -->
      <div class="batch-bar">
        <el-checkbox v-model="selectAll" @change="handleSelectAll">全选</el-checkbox>
        <span class="select-count">已选 {{ selectedCount }} 项</span>
        <el-button type="danger" :disabled="selectedCount === 0" @click="batchDelete">
          批量删除
        </el-button>
      </div>
    </el-card>

    <!-- 历史列表 -->
    <el-card v-loading="loading">
      <div v-if="historyList.length === 0" class="empty-state">
        <el-empty description="暂无历史记录" />
      </div>

      <div v-else class="history-list">
        <div
            v-for="item in historyList"
            :key="`${item.type}:${item.id}`"
            class="history-item"
        >
          <el-checkbox
              :model-value="selected.has(`${item.type}:${item.id}`)"
              @change="toggleSelect(`${item.type}:${item.id}`)"
          />

          <div class="item-content">
            <div class="item-header">
              <div>
                <el-tag :type="typeColor(item.type)" size="small">{{ typeName(item.type) }}</el-tag>
                <el-tag v-if="item.platformName" type="info" size="small" style="margin-left: 6px;">{{ item.platformName }}</el-tag>
                <el-tag :type="statusColor(item.status)" size="small" style="margin-left: 6px;">
                  {{ item.statusName }}
                </el-tag>
              </div>
              <span class="time">{{ item.timeAgo }}</span>
            </div>

            <h4 class="title" @click="viewDetail(item)">{{ item.title }}</h4>
            <p class="preview">{{ item.preview || '暂无预览' }}</p>

            <div class="item-footer">
              <span>🆔 {{ item.id }} | 📝 {{ item.wordCount || 0 }} 字 | {{ formatDate(item.createdAt) }}</span>
              <div class="actions">
                <el-button type="primary" link @click="viewDetail(item)">查看</el-button>
                <el-button type="success" link @click="reuse(item)">复用</el-button>
                <el-button type="danger" link @click="deleteItem(item)">删除</el-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadHistory"
          @current-change="loadHistory"
          style="margin-top: 20px; justify-content: flex-end;"
      />
    </el-card>

    <!-- 详情弹窗：分块展示时每块可「编辑」并传入 Markdown 编辑器新标签页 -->
    <el-dialog v-model="detailVisible" title="记录详情" width="800px" class="record-detail-dialog">
      <div
          ref="detailWrapRef"
          class="detail-dialog-wrap"
          @mousemove="onDetailMouseMove"
          @mouseleave="onDetailMouseLeave"
      >
        <template v-if="detailSections.length">
          <div class="detail-by-platform">
            <p v-if="detailPrompt" class="detail-prompt">
              <strong>创作需求：</strong>{{ detailPrompt }}
            </p>
            <div
                v-for="(sec, idx) in detailSections"
                :key="`${sec.key}-${idx}`"
                class="detail-platform-block"
            >
              <div class="detail-platform-label detail-platform-label-row">
                <span>【{{ sec.label }}】</span>
                <el-button link size="small" class="detail-edit-markdown-btn" @click="editSectionInMarkdown(sec)">
                  编辑
                </el-button>
              </div>
              <pre class="detail-platform-content">{{ displaySectionText(sec.text) }}</pre>
            </div>
          </div>
        </template>
        <div v-else v-html="detailContent" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { useMarkdownCacheStore } from '@/stores/markDownCache'
import {
  getHistoryList,
  getHistoryStatistics,
  getHistoryDetail,
  deleteHistory,
  batchDeleteHistory
} from '@/api/aiWriting'

const router = useRouter()
const markdownCache = useMarkdownCacheStore()

const loading = ref(false)
const historyList = ref([])
const selected = ref(new Set())
const selectAll = ref(false)

const stats = reactive({
  total: 0,
  today: 0,
  words: 0
})

const filters = reactive({
  type: 'all',
  status: 'all',
  platform: '',
  keyword: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const detailVisible = ref(false)
const detailContent = ref('')
/** 当前详情对应的列表项（标题等） */
const detailListItem = ref(null)
/** 详情原始数据，用于构建可编辑分块 */
const detailData = ref(null)
const detailType = ref(null)
const detailWrapRef = ref(null)
const SCROLLBAR_ZONE_WIDTH = 14
const SCROLLABLE_SELECTOR = '.detail-by-platform, .detail-platform-content, .detail-pre'

const selectedCount = computed(() => selected.value.size)

const detailPrompt = computed(() => {
  if (detailType.value !== 'generate' || !detailData.value) return ''
  const p = detailData.value.prompt
  return p != null ? String(p) : ''
})

/** 与 renderDetail 分块逻辑一致，用于结构化展示与「编辑」入口 */
const detailSections = computed(() => {
  const type = detailType.value
  const data = detailData.value
  if (!type || !data) return []
  return buildDetailSections(type, data)
})

const typeName = (type) => ({
  generate: '文案生成',
  polish: '文章美化',
  continue: '文章续写',
  markdown: '保存文案'
}[type])

const typeColor = (type) => ({
  generate: 'primary',
  polish: 'success',
  continue: 'warning',
  markdown: 'info'
}[type])

const statusColor = (status) => ({
  completed: 'success',
  processing: 'warning',
  failed: 'danger',
  saved: 'info'
}[status])

const formatNumber = (num) => {
  if (num >= 10000) return (num / 10000).toFixed(1) + '万'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k'
  return num
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const loadHistory = async () => {
  loading.value = true
  try {
    const res = await getHistoryList({
      page: pagination.page,
      size: pagination.size,
      type: filters.type,
      status: filters.status,
      platform: filters.platform || undefined,
      keyword: filters.keyword
    })
    historyList.value = res.list || []
    pagination.total = res.total || 0
    selected.value.clear()
    selectAll.value = false
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const updateStats = async () => {
  try {
    const res = await getHistoryStatistics()
    stats.total = res.totalCount || 0
    stats.today = res.todayCount || 0
    stats.words = res.totalWords || 0
  } catch (e) {
    console.error(e)
  }
}

const handleSelectAll = (val) => {
  if (val) {
    historyList.value.forEach(item => {
      selected.value.add(`${item.type}:${item.id}`)
    })
  } else {
    selected.value.clear()
  }
}

const toggleSelect = (id) => {
  if (selected.value.has(id)) {
    selected.value.delete(id)
  } else {
    selected.value.add(id)
  }
  selectAll.value = selected.value.size === historyList.value.length
}

const viewDetail = async (item) => {
  try {
    const res = await getHistoryDetail(item.type, item.id)
    detailListItem.value = item
    detailData.value = res
    detailType.value = item.type
    detailContent.value = renderDetail(item.type, res)
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('加载详情失败')
  }
}

/** 详情里展示的文案（今日头条占位符与正文展示一致） */
const displaySectionText = (text) => {
  if (text == null) return ''
  const s = String(text)
  return s === TOUTIAO_PLACEHOLDER_LEGACY ? TOUTIAO_PLACEHOLDER_DISPLAY : s
}

/**
 * 将详情拆成可单独编辑的「篇」：生成按平台多块，美化/续写/保存文案各一块。
 * @returns {{ key: string, label: string, text: string }[]}
 */
const buildDetailSections = (type, data) => {
  if (!data) return []

  if (type === 'generate') {
    const sections = []
    const hasPlatformCopies = getPlatformCopy(data, 'douyin') != null || getPlatformCopy(data, 'xiaohongshu') != null || getPlatformCopy(data, 'video_account') != null || getPlatformCopy(data, 'toutiao') != null
    if (hasPlatformCopies) {
      PLATFORM_LABELS.forEach(({ key, label }) => {
        const copy = getPlatformCopy(data, key)
        if (copy && String(copy).trim()) sections.push({ key, label, text: copy })
      })
    }
    if (sections.length === 0 && data.content) {
      parseContentByPlatform(data.content).forEach(({ key, label, text }) => sections.push({ key, label, text }))
    }
    if (sections.length === 0 && data.content) {
      sections.push({ key: 'all', label: '全部', text: data.content })
    }
    return sections
  }

  if (type === 'markdown' && data.content != null && String(data.content).trim()) {
    const label = data.title ? String(data.title) : '保存文案'
    return [{ key: 'markdown', label, text: data.content }]
  }

  if (type === 'polish') {
    const text = data.resultContent ?? data.result_content
    if (text != null && String(text).trim()) {
      return [{ key: 'polish', label: '美化结果', text }]
    }
  }

  if (type === 'continue' && data.continuation != null && String(data.continuation).trim()) {
    return [{ key: 'continue', label: '续写内容', text: data.continuation }]
  }

  return []
}

/** 将当前分块全文写入 Markdown 编辑器新标签页并跳转 */
const editSectionInMarkdown = (sec) => {
  if (!sec?.text || !String(sec.text).trim()) {
    ElMessage.warning('该段文案为空，无法编辑')
    return
  }
  const base = detailListItem.value?.title || '历史记录'
  const tabTitle = `${base}-${sec.label}`.replace(/\s+/g, ' ').trim().slice(0, 80) || '未命名'
  markdownCache.openNewTab({ title: tabTitle, content: String(sec.text) })
  detailVisible.value = false
  router.push({ path: '/markdownEdit' })
  ElMessage.success('已在新标签页打开，可继续修改')
}

/** 每个滚动区域独立判定：仅当鼠标进入该元素右侧窄条时显示；重叠时只激活最右侧包含鼠标的那一个（靠近总滑动条只显整体） */
function onDetailMouseMove(e) {
  const wrap = detailWrapRef.value
  if (!wrap) return
  const scrollables = Array.from(wrap.querySelectorAll(SCROLLABLE_SELECTOR))
  const x = e.clientX
  const y = e.clientY
  // 按右边界从右到左排序，优先匹配最右侧的（整体滚动条）
  const withRect = scrollables.map((el) => ({ el, rect: el.getBoundingClientRect() }))
  withRect.sort((a, b) => b.rect.right - a.rect.right)
  let activeEl = null
  for (const { el, rect } of withRect) {
    const inStrip = x >= rect.right - SCROLLBAR_ZONE_WIDTH && x <= rect.right
    const inVertical = y >= rect.top && y <= rect.bottom
    if (inStrip && inVertical) {
      activeEl = el
      break
    }
  }
  scrollables.forEach((el) => {
    el.classList.toggle('scrollbar-zone-active', el === activeEl)
  })
}

function onDetailMouseLeave() {
  const wrap = detailWrapRef.value
  if (!wrap) return
  wrap.querySelectorAll(SCROLLABLE_SELECTOR).forEach((el) => {
    el.classList.remove('scrollbar-zone-active')
  })
}

const PLATFORM_LABELS = [
  { key: 'douyin', label: '抖音' },
  { key: 'xiaohongshu', label: '小红书' },
  { key: 'video_account', label: '视频号' },
  { key: 'toutiao', label: '今日头条' }
]

/** 今日头条缺失时：后端已改为返回简短提示；兼容旧数据中的长占位符 */
const TOUTIAO_PLACEHOLDER_LEGACY = '[本次生成未返回今日头条文案。若工作流已含今日头条节点，请检查 Coze 返回的 final_result 结构或联系管理员。]'
const TOUTIAO_PLACEHOLDER_DISPLAY = '今日头条文案本次未生成，请重试或联系管理员。'

/** 从整段 content 中按【平台名】解析出各平台文案（无分平台字段时兜底） */
function parseContentByPlatform(content) {
  if (!content || typeof content !== 'string') return []
  const list = []
  const markers = ['【抖音】', '【小红书】', '【视频号】', '【今日头条】']
  const keyByMarker = { '【抖音】': 'douyin', '【小红书】': 'xiaohongshu', '【视频号】': 'video_account', '【今日头条】': 'toutiao' }
  const labelByKey = { douyin: '抖音', xiaohongshu: '小红书', video_account: '视频号', toutiao: '今日头条' }
  for (let i = 0; i < markers.length; i++) {
    const marker = markers[i]
    const key = marker === '【今日头条】' ? 'toutiao' : (keyByMarker[marker] || marker)
    const label = labelByKey[key] || marker
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
    if (text) list.push({ key, label, text })
  }
  return list
}

/** 从详情数据中取某平台文案，兼容后端返回 camelCase 或 snake_case */
function getPlatformCopy(data, key) {
  if (!data) return null
  const map = {
    douyin: data.douyinCopy ?? data.douyin_copy,
    xiaohongshu: data.xiaohongshuCopy ?? data.xiaohongshu_copy,
    video_account: data.videoAccountCopy ?? data.video_account_copy,
    toutiao: data.toutiaoCopy ?? data.toutiao_copy
  }
  return map[key] ?? null
}

const renderDetail = (type, data) => {
  if (!data) return '<p>无详情</p>'

  if (type === 'generate') {
    // 文案生成：按平台分块展示（优先用 douyinCopy 等，否则从 content 解析）
    const sections = []
    const hasPlatformCopies = getPlatformCopy(data, 'douyin') != null || getPlatformCopy(data, 'xiaohongshu') != null || getPlatformCopy(data, 'video_account') != null || getPlatformCopy(data, 'toutiao') != null
    if (hasPlatformCopies) {
      PLATFORM_LABELS.forEach(({ key, label }) => {
        const copy = getPlatformCopy(data, key)
        if (copy && String(copy).trim()) sections.push({ key, label, text: copy })
      })
    }
    if (sections.length === 0 && data.content) {
      const parsed = parseContentByPlatform(data.content)
      parsed.forEach(({ key, label, text }) => sections.push({ key, label, text }))
    }
    if (sections.length === 0 && data.content) {
      sections.push({ key: 'all', label: '全部', text: data.content })
    }

    let html = '<div class="detail-by-platform">'
    if (data.prompt) html += `<p class="detail-prompt"><strong>创作需求：</strong>${escapeHtml(data.prompt)}</p>`
    sections.forEach(({ label, text }) => {
      const displayText = (text === TOUTIAO_PLACEHOLDER_LEGACY) ? TOUTIAO_PLACEHOLDER_DISPLAY : text
      html += `<div class="detail-platform-block"><div class="detail-platform-label">【${escapeHtml(label)}】</div><pre class="detail-platform-content">${escapeHtml(displayText)}</pre></div>`
    })
    html += '</div>'
    return html
  }

  if (type === 'markdown') {
    let html = ''
    if (data.title) html += `<p><strong>标题：</strong>${escapeHtml(data.title)}</p>`
    if (data.updatedAt || data.createdAt) {
      const t = data.updatedAt || data.createdAt
      html += `<p><strong>更新时间：</strong>${escapeHtml(String(t))}</p>`
    }
    if (data.content) html += `<p><strong>文案内容：</strong></p><pre class="detail-pre">${escapeHtml(data.content)}</pre>`
    if (!html) html = '<p>无详情</p>'
    return html
  }

  // 美化 / 续写：保留原有整体展示，仅增加平台与主要内容
  const platformName = data.platform ? { douyin: '抖音', xiaohongshu: '小红书', video_account: '视频号', toutiao: '今日头条' }[data.platform] || data.platform : null
  let html = ''
  if (platformName) html += `<p><strong>所属平台：</strong>${escapeHtml(platformName)}</p>`
  if (type === 'polish' && data.resultContent) html += `<p><strong>美化结果：</strong></p><pre class="detail-pre">${escapeHtml(data.resultContent)}</pre>`
  if (type === 'continue' && data.continuation) html += `<p><strong>续写内容：</strong></p><pre class="detail-pre">${escapeHtml(data.continuation)}</pre>`
  if (!html && data) html += `<pre class="detail-pre">${escapeHtml(JSON.stringify(data, null, 2))}</pre>`
  return html || '<p>无详情</p>'
}

function escapeHtml(str) {
  if (str == null) return ''
  const s = String(str)
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

const reuse = (item) => {
  const routes = {
    generate: '/ai-writing/generate',
    polish: '/ai-writing/polish',
    continue: '/ai-writing/continue',
    markdown: '/markdownEdit'
  }
  const path = routes[item.type]
  if (!path) return
  router.push({
    path,
    query: { content: encodeURIComponent(item.title) }
  })
}

const deleteItem = async (item) => {
  try {
    await ElMessageBox.confirm('确定删除这条记录？', '提示', { type: 'warning' })
    await deleteHistory(item.type, item.id)
    ElMessage.success('删除成功')
    loadHistory()
    updateStats()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

const batchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selected.value.size} 条记录？`, '提示', { type: 'warning' })
    await batchDeleteHistory(Array.from(selected.value))
    ElMessage.success('批量删除成功')
    loadHistory()
    updateStats()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

const resetFilters = () => {
  filters.type = 'all'
  filters.status = 'all'
  filters.platform = ''
  filters.keyword = ''
  pagination.page = 1
  loadHistory()
}

onMounted(() => {
  loadHistory()
  updateStats()
})
</script>

<style scoped>
.app-container {
  padding: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  padding: 20px;
}

.stat-value {
  font-size: 36px;
  font-weight: 700;
  color: #409eff;
  margin-bottom: 8px;
}

.stat-label {
  color: #909399;
}

.filter-card {
  margin-bottom: 20px;
}

.batch-bar {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  gap: 15px;
}

.select-count {
  color: #909399;
  flex: 1;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.history-item {
  display: flex;
  gap: 15px;
  padding: 20px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  transition: all 0.3s;
}

.history-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.1);
}

.item-content {
  flex: 1;
}

.item-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}

.time {
  color: #909399;
  font-size: 13px;
}

.title {
  margin: 0 0 10px;
  cursor: pointer;
  color: #303133;
}

.title:hover {
  color: #409eff;
}

.preview {
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  margin: 0 0 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #909399;
}

.actions {
  display: flex;
  gap: 10px;
}

.empty-state {
  padding: 60px 0;
}

/* 详情弹窗内按平台分块展示（v-html 内容） */
:deep(.detail-by-platform) {
  max-height: 70vh;
  overflow-y: auto;
  scrollbar-gutter: stable;
  padding-right: 20px; /* 与整体滚动条拉开距离，避免与小窗口滚动条贴在一起 */
}
:deep(.detail-prompt) {
  margin-bottom: 16px;
  color: #606266;
}
:deep(.detail-platform-block) {
  margin-bottom: 20px;
  margin-right: 12px; /* 各平台块与整体右侧滚动条留出间距 */
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
}
:deep(.detail-platform-label) {
  padding: 8px 12px;
  background: #ecf5ff;
  color: #409eff;
  font-weight: 600;
  font-size: 15px;
}
:deep(.detail-platform-label-row) {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
/* 详情内「编辑」：醒目紫色，与平台标题栏形成对比 */
:deep(.detail-edit-markdown-btn) {
  flex-shrink: 0;
  font-weight: 600;
  font-size: 14px;
  color: #7c3aed !important;
}
:deep(.detail-edit-markdown-btn:hover),
:deep(.detail-edit-markdown-btn:focus) {
  color: #6d28d9 !important;
}
:deep(.detail-platform-content),
:deep(.detail-pre) {
  margin: 0;
  padding: 16px;
  background: #fafafa;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
  line-height: 1.6;
  max-height: 300px;
  overflow-y: auto;
  scrollbar-gutter: stable;
}

/* 详情弹窗：仅保留「触发式」逻辑（滑块默认透明，进入右侧窄条时显示），其余沿用全局滚动条格式 */
.detail-dialog-wrap {
  outline: none;
}
@supports not selector(::-webkit-scrollbar) {
  :deep(.detail-by-platform),
  :deep(.detail-platform-content),
  :deep(.detail-pre) {
    scrollbar-width: thin;
    scrollbar-color: transparent transparent;
  }
  :deep(.detail-by-platform.scrollbar-zone-active),
  :deep(.detail-platform-content.scrollbar-zone-active),
  :deep(.detail-pre.scrollbar-zone-active) {
    scrollbar-color: #c0c4cc transparent;
  }
}
:deep(.detail-by-platform)::-webkit-scrollbar-thumb,
:deep(.detail-platform-content)::-webkit-scrollbar-thumb,
:deep(.detail-pre)::-webkit-scrollbar-thumb {
  background: transparent;
  transition: background 0.2s ease;
}
:deep(.detail-by-platform.scrollbar-zone-active)::-webkit-scrollbar-thumb,
:deep(.detail-platform-content.scrollbar-zone-active)::-webkit-scrollbar-thumb,
:deep(.detail-pre.scrollbar-zone-active)::-webkit-scrollbar-thumb {
  background: #c0c4cc;
}
:deep(.detail-by-platform.scrollbar-zone-active)::-webkit-scrollbar-thumb:hover,
:deep(.detail-platform-content.scrollbar-zone-active)::-webkit-scrollbar-thumb:hover,
:deep(.detail-pre.scrollbar-zone-active)::-webkit-scrollbar-thumb:hover {
  background: #909399;
}
</style>