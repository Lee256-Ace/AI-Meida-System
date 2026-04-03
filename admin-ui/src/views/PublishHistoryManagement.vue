<template>
  <div class="publish-history-management">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>历史发布</span>
          <el-button type="primary" @click="openAddDialog">登记发布</el-button>
        </div>
      </template>

      <el-form :model="searchForm" :inline="true" class="search-form">
        <el-form-item label="平台名称:">
          <el-input v-model="searchForm.platformName" placeholder="模糊查询" clearable />
        </el-form-item>
        <el-form-item label="标题:">
          <el-input v-model="searchForm.title" placeholder="模糊查询" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="platformName" label="平台" width="120" show-overflow-tooltip />
        <el-table-column prop="title" label="标题" min-width="140" show-overflow-tooltip />
        <el-table-column label="正文摘要" min-width="200">
          <template #default="scope">
            <span class="preview-text">{{ previewContent(scope.row.content) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="publishUrl" label="发布链接" min-width="160" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="statusTagType(scope.row.status)" size="small">
              {{ statusLabel(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="登记时间" width="170">
          <template #default="scope">
            {{ formatDateTime(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" link @click="openEditDialog(scope.row)">编辑</el-button>
            <el-button type="danger" size="small" link @click="deleteRecord(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.currentPage"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 20px; justify-content: flex-end"
        @size-change="getList"
        @current-change="getList"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="平台名称" prop="platformName">
          <el-input v-model="form.platformName" placeholder="如：抖音、小红书、今日头条" />
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="发布主题或文章标题" />
        </el-form-item>
        <el-form-item label="发布链接" prop="publishUrl">
          <el-input v-model="form.publishUrl" placeholder="选填，已发布后的 URL" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择" style="width: 100%">
            <el-option label="已记录" value="recorded" />
            <el-option label="发布成功" value="success" />
            <el-option label="发布失败" value="failed" />
          </el-select>
        </el-form-item>
        <el-form-item label="文案正文" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="12" placeholder="完整发布文案" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getPublishHistoryPage,
  addPublishHistory,
  updatePublishHistory,
  deletePublishHistory
} from '@/api/publishHistory'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref()

const searchForm = reactive({
  platformName: '',
  title: ''
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

const form = reactive({
  id: null,
  platformName: '',
  title: '',
  content: '',
  publishUrl: '',
  status: 'recorded',
  remark: ''
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }]
}

const statusLabel = (s) => {
  if (s === 'success') return '成功'
  if (s === 'failed') return '失败'
  return '已记录'
}

const statusTagType = (s) => {
  if (s === 'success') return 'success'
  if (s === 'failed') return 'danger'
  return 'info'
}

const previewContent = (text) => {
  if (!text) return '—'
  const t = String(text).replace(/\s+/g, ' ').trim()
  return t.length > 80 ? t.slice(0, 80) + '…' : t
}

const formatDateTime = (v) => {
  if (!v) return ''
  try {
    return new Date(v).toLocaleString('zh-CN')
  } catch {
    return String(v)
  }
}

const getList = async () => {
  loading.value = true
  try {
    const res = await getPublishHistoryPage({
      page: pagination.currentPage,
      size: pagination.pageSize,
      platformName: searchForm.platformName || undefined,
      title: searchForm.title || undefined
    })
    tableData.value = res.list || []
    pagination.total = res.total || 0
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const search = () => {
  pagination.currentPage = 1
  getList()
}

const resetSearch = () => {
  searchForm.platformName = ''
  searchForm.title = ''
  search()
}

const resetForm = () => {
  form.id = null
  form.platformName = ''
  form.title = ''
  form.content = ''
  form.publishUrl = ''
  form.status = 'recorded'
  form.remark = ''
  formRef.value?.clearValidate?.()
}

const openAddDialog = () => {
  dialogTitle.value = '登记发布记录'
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  dialogTitle.value = '编辑发布记录'
  isEdit.value = true
  resetForm()
  Object.assign(form, {
    id: row.id,
    platformName: row.platformName || '',
    title: row.title || '',
    content: row.content || '',
    publishUrl: row.publishUrl || '',
    status: row.status || 'recorded',
    remark: row.remark || ''
  })
  dialogVisible.value = true
}

const submitForm = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    if (isEdit.value) {
      await updatePublishHistory({ ...form })
      ElMessage.success('更新成功')
    } else {
      await addPublishHistory({ ...form })
      ElMessage.success('登记成功')
    }
    dialogVisible.value = false
    getList()
  } catch (e) {
    if (e !== false) ElMessage.error('保存失败')
  }
}

const deleteRecord = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该条发布记录？', '提示', { type: 'warning' })
    await deletePublishHistory(row.id)
    ElMessage.success('已删除')
    getList()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.publish-history-management {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.search-form {
  margin-bottom: 20px;
}
.preview-text {
  color: #606266;
  font-size: 13px;
}
</style>
