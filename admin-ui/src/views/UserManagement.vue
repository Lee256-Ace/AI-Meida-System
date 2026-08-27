<template>
  <div class="app-container user-management">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button type="primary" size="small" :loading="loading" @click="loadAccounts">刷新</el-button>
        </div>
      </template>

      <el-form :model="searchForm" :inline="true" class="search-form">
        <el-form-item label="用户名:">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="类型:">
          <el-select v-model="searchForm.accountType" placeholder="全部" clearable style="width: 140px">
            <el-option label="全部" value="" />
            <el-option label="管理员" value="admin" />
            <el-option label="普通用户" value="user" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态:">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="正常" value="1" />
            <el-option label="禁用" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table
        ref="userTableRef"
        v-loading="loading"
        :data="displayedTableData"
        style="width: 100%"
        @sort-change="onSortChange"
      >
        <el-table-column prop="accountType" label="类型" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.accountType === 'admin' ? 'info' : 'default'">
              {{ scope.row.accountType === 'admin' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="username"
          label="用户名"
          min-width="160"
          sortable="custom"
        />
        <el-table-column prop="passwordMasked" label="密码(脱敏)" min-width="180">
          <template #default="scope">
            <el-tooltip v-if="scope.row.passwordMasked" :content="scope.row.passwordMasked" placement="top">
              <span class="nowrap">{{ scope.row.passwordMasked }}</span>
            </el-tooltip>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
              {{ scope.row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="createdAt"
          label="创建时间"
          min-width="180"
          sortable="custom"
        >
          <template #default="scope">
            <span>{{ formatDateTime(scope.row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="scope">
            <template v-if="scope.row.recordSource === 'app_user'">
              <el-button
                v-if="scope.row.status === 1"
                link
                type="warning"
                size="small"
                :loading="rowActionId === scope.row.id && actionKind === 'ban'"
                @click="onBan(scope.row)"
              >
                封禁
              </el-button>
              <el-button
                v-else
                link
                type="success"
                size="small"
                :loading="rowActionId === scope.row.id && actionKind === 'unban'"
                @click="onUnban(scope.row)"
              >
                解禁
              </el-button>
              <el-button
                link
                type="danger"
                size="small"
                :loading="rowActionId === scope.row.id && actionKind === 'del'"
                @click="onDelete(scope.row)"
              >
                删除
              </el-button>
            </template>
            <span v-else class="op-muted">—</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAllAccounts, setAppUserStatus, deleteAppUser } from '@/api/userManagement'

const userTableRef = ref(null)
const loading = ref(false)
const tableData = ref([])

/** 搜索条件（与文章平台管理一致：表单 + 搜索/重置） */
const searchForm = reactive({
  username: '',
  accountType: '',
  status: ''
})
/** 已应用的筛选（点击「搜索」后生效） */
const appliedSearch = reactive({
  username: '',
  accountType: '',
  status: ''
})

/** 用户点击列头排序：null 表示用默认顺序（与后端一致：非管理员按创建时间倒序） */
const sortState = ref({ prop: null, order: null })
const rowActionId = ref(null)
const actionKind = ref('')

const isAdminRow = (row) => row?.accountType === 'admin'

/** 非管理员区域默认排序：创建时间新的在前（与后端 list 一致） */
const defaultSortRest = (rest) => {
  return [...rest].sort((a, b) => sortByCreatedAt(b, a))
}

/** 按已应用条件筛选（管理员置顶逻辑在下游仍保留） */
const filteredList = computed(() => {
  let list = tableData.value
  if (!list.length) return []

  const kw = appliedSearch.username.trim().toLowerCase()
  if (kw) {
    list = list.filter((r) => (r.username || '').toLowerCase().includes(kw))
  }
  if (appliedSearch.accountType) {
    list = list.filter((r) => r.accountType === appliedSearch.accountType)
  }
  if (appliedSearch.status !== '' && appliedSearch.status != null) {
    const s = Number(appliedSearch.status)
    list = list.filter((r) => r.status === s)
  }
  return list
})

/**
 * 展示用数据：所有「管理员」行永远置顶，其下再按当前排序规则排其它账号。
 * 这样点击用户名/创建时间升降序时，不会因为整表重排把管理员挤下去。
 */
const displayedTableData = computed(() => {
  const list = filteredList.value
  if (!list.length) return []
  const admins = list.filter(isAdminRow)
  const rest = list.filter((r) => !isAdminRow(r))
  const { prop, order } = sortState.value
  if (!prop || !order) {
    return [...admins, ...defaultSortRest(rest)]
  }
  const sorted = [...rest]
  const factor = order === 'ascending' ? 1 : -1
  if (prop === 'username') {
    sorted.sort((a, b) => factor * sortByUsername(a, b))
  } else if (prop === 'createdAt') {
    sorted.sort((a, b) => factor * sortByCreatedAt(a, b))
  } else {
    defaultSortRest(sorted)
  }
  return [...admins, ...sorted]
})

const onSortChange = ({ prop, order }) => {
  sortState.value = {
    prop: prop || null,
    order: order || null
  }
  attachSortCaretTips()
}

/** 为表头排序小三角（递增/递减）设置浏览器原生 title，悬停即出简单提示 */
const attachSortCaretTips = () => {
  nextTick(() => {
    const inst = userTableRef.value
    const root = inst && (inst.$el ?? inst)
    if (!root || typeof root.querySelector !== 'function') return
    const header = root.querySelector('.el-table__header-wrapper')
    if (!header) return
    header.querySelectorAll('.sort-caret.ascending').forEach((el) => {
      el.setAttribute('title', '升序')
    })
    header.querySelectorAll('.sort-caret.descending').forEach((el) => {
      el.setAttribute('title', '降序')
    })
  })
}

const formatDateTime = (v) => {
  if (!v) return ''
  try {
    const d = new Date(v)
    return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
  } catch (_) {
    return String(v)
  }
}

/** 用户名：升序为 A→Z / 拼音习惯（中文环境） */
const sortByUsername = (a, b) => {
  const ua = (a.username || '').toLowerCase()
  const ub = (b.username || '').toLowerCase()
  return ua.localeCompare(ub, 'zh-Hans-CN')
}

/** 创建时间：升序为从早到晚；无时间的行排在最后 */
const sortByCreatedAt = (a, b) => {
  const rawA = a.createdAt
  const rawB = b.createdAt
  const ta = rawA ? new Date(rawA).getTime() : null
  const tb = rawB ? new Date(rawB).getTime() : null
  const na = ta == null || Number.isNaN(ta)
  const nb = tb == null || Number.isNaN(tb)
  if (na && nb) return 0
  if (na) return 1
  if (nb) return -1
  return ta - tb
}

const loadAccounts = async () => {
  loading.value = true
  try {
    const res = await getAllAccounts()
    // request 拦截器返回 res.data
    tableData.value = Array.isArray(res) ? res : (res?.list || [])
    sortState.value = { prop: null, order: null }
  } catch (e) {
    const msg = e?.message && String(e.message).trim() ? e.message : '加载用户列表失败'
    ElMessage.error(msg)
  } finally {
    loading.value = false
    nextTick(() => {
      userTableRef.value?.clearSort?.()
      attachSortCaretTips()
    })
  }
}

onMounted(() => {
  loadAccounts()
  attachSortCaretTips()
})

watch([() => tableData.value.length, () => filteredList.value.length, () => displayedTableData.value.length, loading], () => {
  attachSortCaretTips()
})

const search = () => {
  appliedSearch.username = (searchForm.username || '').trim()
  appliedSearch.accountType = searchForm.accountType || ''
  appliedSearch.status = searchForm.status === undefined || searchForm.status === null ? '' : String(searchForm.status)
  sortState.value = { prop: null, order: null }
  nextTick(() => {
    userTableRef.value?.clearSort?.()
    attachSortCaretTips()
  })
}

const resetSearch = () => {
  searchForm.username = ''
  searchForm.accountType = ''
  searchForm.status = ''
  search()
}

const runRowAction = async (row, kind, fn) => {
  rowActionId.value = row.id
  actionKind.value = kind
  try {
    await fn()
    ElMessage.success('操作成功')
    await loadAccounts()
  } catch {
    /* 错误已由拦截器提示 */
  } finally {
    rowActionId.value = null
    actionKind.value = ''
  }
}

const onBan = (row) => {
  runRowAction(row, 'ban', () => setAppUserStatus(row.id, 0))
}

const onUnban = (row) => {
  runRowAction(row, 'unban', () => setAppUserStatus(row.id, 1))
}

const onDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定永久删除普通用户「${row.username}」？删除后无法恢复。`,
      '删除用户',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  await runRowAction(row, 'del', () => deleteAppUser(row.id))
}
</script>

<style scoped>
.app-container {
  padding: 24px;
}

.user-management .search-form {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.nowrap {
  display: inline-block;
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.op-muted {
  color: #c0c4cc;
  font-size: 13px;
}
</style>

