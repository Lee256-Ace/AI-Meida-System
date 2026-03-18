<template>
  <div class="articleNews-management">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>新浪热点素材</span>
          <el-button type="primary" @click="openAddDialog">抓取网络素材</el-button>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :model="searchForm" :inline="true" class="search-form">
        <el-form-item label="备注:">
          <el-input v-model="searchForm.remark" placeholder="请输入备注" />
        </el-form-item>
        <el-form-item label="新闻标题:">
          <el-input v-model="searchForm.title" placeholder="请输入新闻标题" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="200"/>
        <el-table-column prop="source" label="新闻来源" />
        <el-table-column prop="title" label="新闻标题" width="200"/>
        <!--        <el-table-column prop="content" label="新闻内容" />-->
        <el-table-column prop="url" label="新闻链接" width="200"/>
<!--        <el-table-column prop="pic" label="新闻图片" width="200"/>-->
<!--        <el-table-column prop="hot" label="热度指数" width="100"/>-->
        <el-table-column prop="type" label="新闻类型" width="200"/>
<!--        <el-table-column label="新闻日期"width="200">-->
<!--          <template #default="scope">-->
<!--            <span>{{ formatDateTime(scope.row.newsDate) }}</span>-->
<!--          </template>-->
<!--        </el-table-column>-->
        <el-table-column prop="media" label="媒体" />
<!--        <el-table-column prop="picCount" label="图片数量" />-->
<!--        <el-table-column prop="picList" label="图片列表" />-->
<!--        <el-table-column prop="wordNum" label="选择多少字数以内的文章发布" />-->
        <el-table-column prop="remark" label="备注" />
        <el-table-column label="操作" width="200">
          <template #default="scope">
<!--            <el-button type="primary" size="small" @click="openEditDialog(scope.row)">编辑</el-button>-->
            <el-button type="danger" size="small" @click="deleteRecord(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.currentPage"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        style="margin-top: 20px; text-align: right"
      />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <div>实时抓取网络热点素材！！！请点击确定开始抓取，抓取素材后需要等几分钟等数据同步完，再点击查询即可看到数据</div>
<!--        <el-form-item label="" prop="createBy">-->
<!--          <el-input v-model="form.createBy" placeholder="请输入创建人" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="创建日期:" prop="createTime">-->
<!--          <el-date-picker v-model="form.createTime" type="datetime" placeholder="请选择创建日期" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="更新人:" prop="updateBy">-->
<!--          <el-input v-model="form.updateBy" placeholder="请输入更新人" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="更新日期:" prop="updateTime">-->
<!--          <el-date-picker v-model="form.updateTime" type="datetime" placeholder="请选择更新日期" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="所属部门:" prop="sysOrgCode">-->
<!--          <el-input v-model="form.sysOrgCode" placeholder="请输入所属部门" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="备注:" prop="remark">-->
<!--          <el-input v-model="form.remark" placeholder="请输入备注" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="逻辑删除，0正常1已删除:" prop="delFlag">-->
<!--          <el-input-number v-model="form.delFlag" placeholder="请输入逻辑删除，0正常1已删除" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="新闻来源:" prop="source">-->
<!--          <el-input v-model="form.source" placeholder="请输入新闻来源" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="新闻标题:" prop="title">-->
<!--          <el-input v-model="form.title" placeholder="请输入新闻标题" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="新闻内容:" prop="content">-->
<!--          <el-input v-model="form.content" placeholder="请输入新闻内容" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="新闻链接:" prop="url">-->
<!--          <el-input v-model="form.url" placeholder="请输入新闻链接" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="新闻图片:" prop="pic">-->
<!--          <el-input v-model="form.pic" placeholder="请输入新闻图片" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="热度指数:" prop="hot">-->
<!--          <el-input-number v-model="form.hot" placeholder="请输入热度指数" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="新闻类型:" prop="type">-->
<!--          <el-input v-model="form.type" placeholder="请输入新闻类型" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="新闻日期:" prop="newsDate">-->
<!--          <el-date-picker v-model="form.newsDate" type="datetime" placeholder="请选择新闻日期" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="媒体:" prop="media">-->
<!--          <el-input v-model="form.media" placeholder="请输入媒体" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="图片数量:" prop="picCount">-->
<!--          <el-input-number v-model="form.picCount" placeholder="请输入图片数量" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="图片列表:" prop="picList">-->
<!--          <el-input v-model="form.picList" placeholder="请输入图片列表" />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="选择多少字数以内的文章发布:" prop="wordNum">-->
<!--          <el-input-number v-model="form.wordNum" placeholder="请输入选择多少字数以内的文章发布" />-->
<!--        </el-form-item>-->
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, ZoomIn } from '@element-plus/icons-vue'
import { getArticleNewsList, addArticleNews, updateArticleNews, deleteArticleNews } from '@/api/articleNews'

// 响应式数据
const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref()

// 搜索表单
const searchForm = reactive({
  remark: '',
  title: '',
})

// 表单数据
const form = reactive({
  id: '',
  createBy: '',
  createTime: null,
  updateBy: '',
  updateTime: null,
  sysOrgCode: '',
  remark: '',
  delFlag: 0,
  source: '',
  title: '',
  content: '',
  url: '',
  pic: '',
  hot: 0,
  type: '',
  newsDate: null,
  media: '',
  picCount: 0,
  picList: '',
  wordNum: 0,
})

// 分页数据
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 表单验证规则
const rules = reactive({
  delFlag: [{ required: true, message: '请输入逻辑删除，0正常1已删除', trigger: 'blur' }],
  source: [{ required: true, message: '请输入新闻来源', trigger: 'blur' }],
  title: [{ required: true, message: '请输入新闻标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入新闻内容', trigger: 'blur' }],
  url: [{ required: true, message: '请输入新闻链接', trigger: 'blur' }],
  picCount: [{ required: true, message: '请输入图片数量', trigger: 'blur' }],
})

// 获取数据列表
const getList = async () => {
  loading.value = true
  try {
    const response = await getArticleNewsList({
      page: pagination.currentPage,
      size: pagination.pageSize,
      ...searchForm
    })
    tableData.value = response.list
    pagination.total = response.total

  } catch (error) {
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const search = () => {
  pagination.currentPage = 1
  getList()
}

// 重置搜索
const resetSearch = () => {
  searchForm.remark = ''
  searchForm.title = ''
  search()
}

// 打开新增对话框
const openAddDialog = () => {
  dialogTitle.value = '抓取网络热素材'
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

// 打开编辑对话框
const openEditDialog = (row) => {
  dialogTitle.value = '编辑ArticleNews'
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

// 重置表单
const resetForm = () => {
  form.id = ''
  form.createBy = ''
  form.createTime = null
  form.updateBy = ''
  form.updateTime = null
  form.sysOrgCode = ''
  form.remark = ''
  form.delFlag = 0
  form.source = ''
  form.title = ''
  form.content = ''
  form.url = ''
  form.pic = ''
  form.hot = 0
  form.type = ''
  form.newsDate = null
  form.media = ''
  form.picCount = 0
  form.picList = ''
  form.wordNum = 0
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

// 提交表单
const submitForm = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    
    if (isEdit.value) {
      await updateArticleNews(form)
      ElMessage.success('更新成功')
    } else {
      await addArticleNews(form)
      ElMessage.success('开始抓取素材，请勿重复点击，稍等几分钟即可')
    }
    
    dialogVisible.value = false
    getList()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 删除记录
const deleteRecord = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这条记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await deleteArticleNews(row.id)
    ElMessage.success('删除成功')
    getList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 分页处理
const handleSizeChange = (val) => {
  pagination.pageSize = val
  getList()
}

const handleCurrentChange = (val) => {
  pagination.currentPage = val
  getList()
}

// 时间格式化函数
const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  const date = new Date(dateTime)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

// 组件挂载时获取数据
onMounted(() => {
  getList()
})
</script>

<style scoped>
.articleNews-management {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.field-selector .el-checkbox {
  display: block;
  margin-bottom: 8px;
}

.search-form {
  margin-bottom: 20px;
}

.image-upload-container {
  display: flex;
  align-items: center;
  gap: 20px;
}

.uploaded-image {
  position: relative;
  width: 160px;
  height: 160px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.uploaded-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: pointer;
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  opacity: 0;
  transition: opacity 0.3s;
}

.uploaded-image:hover .image-overlay {
  opacity: 1;
}

.overlay-icon {
  color: white;
  font-size: 40px;
  cursor: pointer;
  padding: 8px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.overlay-icon:hover {
  background-color: rgba(255, 255, 255, 0.2);
}

.image-uploader {
  width: 160px;
  height: 160px;
}

.upload-area {
  width: 160px;
  height: 160px;
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  transition: border-color 0.3s;
  padding: 20px;
  box-sizing: border-box;
}

.upload-area:hover {
  border-color: #409eff;
}

.upload-icon {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #8c939d;
}

.upload-text {
  margin-top: auto;
  font-size: 14px;
  color: #606266;
  text-align: center;
  line-height: 1.4;
}
</style>