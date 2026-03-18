<template>
  <div class="merchant-management">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>商家管理</span>
          <el-button type="primary" @click="openAddDialog">新增</el-button>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :model="searchForm" :inline="true" class="search-form">
        <el-form-item label="用户名:">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="店铺名称:">
          <el-input v-model="searchForm.shopName" placeholder="请输入店铺名称" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="商家ID" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="password" label="密码" />
        <el-table-column prop="shopName" label="店铺名称" />
        <el-table-column label="店铺logo" width="120">
          <template #default="scope">
            <el-image v-if="scope.row.shopLogo" :src="scope.row.shopLogo" style="width: 80px; height: 80px" fit="cover" />
            <span v-else>暂无图片</span>
          </template>
        </el-table-column>
        <el-table-column prop="shopAddress" label="店铺地址" />
        <el-table-column prop="contactName" label="联系人姓名" />
        <el-table-column prop="phone" label="联系电话" />
        <el-table-column prop="email" label="联系邮箱" />
        <el-table-column label="认证状态(0-未认证,1-审核中,2-已认证,3-认证失败)">
          <template #default="scope">
            <el-tag :type="getStatusLevel(scope.row.certificationStatus, '')">
              {{ getStatusText(scope.row.certificationStatus, '0-未认证,1-审核中,2-已认证,3-认证失败') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态(0:禁用 1:正常)" />
        <el-table-column label="创建时间">
          <template #default="scope">
            <span>{{ formatDateTime(scope.row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="更新时间">
          <template #default="scope">
            <span>{{ formatDateTime(scope.row.updatedAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button type="primary" size="small" @click="openEditDialog(scope.row)">编辑</el-button>
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
        <el-form-item label="用户名:" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码:" prop="password">
          <el-input v-model="form.password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="店铺名称:" prop="shopName">
          <el-input v-model="form.shopName" placeholder="请输入店铺名称" />
        </el-form-item>
        <el-form-item label="店铺logo:" prop="shopLogo">
          <div class="image-upload-container">
            <!-- 已上传的图片 -->
            <div v-if="form.shopLogo" class="uploaded-image">
              <img :src="form.shopLogo" class="uploaded-preview" />
              <div class="image-overlay">
                <el-icon class="overlay-icon view-icon"><ZoomIn /></el-icon>
                <el-icon class="overlay-icon delete-icon" @click="form.shopLogo = ''"><Delete /></el-icon>
              </div>
            </div>
            
            <!-- 上传区域 -->
            <el-upload
              class="image-uploader"
              action="/upload"
              :show-file-list="false"
              :on-success="(response) => handleUploadSuccess(response, 'shopLogo')"
              :before-upload="beforeUpload"
            >
              <div class="upload-area">
                <el-icon class="upload-icon"><Plus /></el-icon>
                <div class="upload-text">支持jpg、png格式，建议尺寸300x400，不超过2MB</div>
              </div>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="店铺地址:" prop="shopAddress">
          <el-input v-model="form.shopAddress" placeholder="请输入店铺地址" />
        </el-form-item>
        <el-form-item label="联系人姓名:" prop="contactName">
          <el-input v-model="form.contactName" placeholder="请输入联系人姓名" />
        </el-form-item>
        <el-form-item label="联系电话:" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="联系邮箱:" prop="email">
          <el-input v-model="form.email" placeholder="请输入联系邮箱" />
        </el-form-item>
        <el-form-item label="认证状态(0-未认证,1-审核中,2-已认证,3-认证失败):" prop="certificationStatus">
          <el-select v-model="form.certificationStatus" placeholder="请选择认证状态(0-未认证,1-审核中,2-已认证,3-认证失败)" style="width: 100%">
            <el-option label="未认证" :value="0" />
            <el-option label="审核中" :value="1" />
            <el-option label="已认证" :value="2" />
            <el-option label="认证失败" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态(0:禁用 1:正常):" prop="status">
          <el-input-number v-model="form.status" placeholder="请输入状态(0:禁用 1:正常)" />
        </el-form-item>
        <el-form-item label="创建时间:" prop="createdAt">
          <el-date-picker v-model="form.createdAt" type="datetime" placeholder="请选择创建时间" />
        </el-form-item>
        <el-form-item label="更新时间:" prop="updatedAt">
          <el-date-picker v-model="form.updatedAt" type="datetime" placeholder="请选择更新时间" />
        </el-form-item>
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
import { getMerchantList, addMerchant, updateMerchant, deleteMerchant } from '@/api/merchant'

// 响应式数据
const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref()

// 搜索表单
const searchForm = reactive({
  username: '',
  shopName: '',
})

// 表单数据
const form = reactive({
  id: 0,
  username: '',
  password: '',
  shopName: '',
  shopLogo: '',
  shopAddress: '',
  contactName: '',
  phone: '',
  email: '',
  certificationStatus: 0,
  status: 0,
  createdAt: null,
  updatedAt: null,
})

// 分页数据
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 表单验证规则
const rules = reactive({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
})

// 获取数据列表
const getList = async () => {
  loading.value = true
  try {
    const response = await getMerchantList({
      page: pagination.currentPage,
      size: pagination.pageSize,
      ...searchForm
    })
    if (response.code === 200) {
      tableData.value = response.data.list
      pagination.total = response.data.total
    }
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
  searchForm.username = ''
  searchForm.shopName = ''
  search()
}

// 打开新增对话框
const openAddDialog = () => {
  dialogTitle.value = '新增商家表'
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

// 打开编辑对话框
const openEditDialog = (row) => {
  dialogTitle.value = '编辑商家表'
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

// 重置表单
const resetForm = () => {
  form.id = 0
  form.username = ''
  form.password = ''
  form.shopName = ''
  form.shopLogo = ''
  form.shopAddress = ''
  form.contactName = ''
  form.phone = ''
  form.email = ''
  form.certificationStatus = 0
  form.status = 0
  form.createdAt = null
  form.updatedAt = null
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
      await updateMerchant(form)
      ElMessage.success('更新成功')
    } else {
      await addMerchant(form)
      ElMessage.success('新增成功')
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
    
    await deleteMerchant(row.id)
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

// 图片上传成功回调
const handleUploadSuccess = (response, fieldName) => {
  if (response.code === 200) {
    form[fieldName] = response.data
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error('图片上传失败')
  }
}

// 图片上传前验证
const beforeUpload = (file) => {
  const isJPG = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/gif'
  const isLt2M = file.size / 1024 / 1024 < 2
  
  if (!isJPG) {
    ElMessage.error('上传图片只能是 JPG/PNG/GIF 格式!')
  }
  if (!isLt2M) {
    ElMessage.error('上传图片大小不能超过 2MB!')
  }
  return isJPG && isLt2M
}

// 获取状态文本
const getStatusText = (value, mappingConfig) => {
  if (value === null || value === undefined || !mappingConfig) return value
  const mappings = mappingConfig.split(',')
  for (const mapping of mappings) {
    const [key, text] = mapping.split('-')
    if (key && text && key.trim() === String(value)) {
      return text.trim()
    }
  }
  return value
}

// 获取状态级别
const getStatusLevel = (value, levelConfig) => {
  if (value === null || value === undefined || !levelConfig) return ''
  const levels = levelConfig.split(',')
  for (const level of levels) {
    const [key, type] = level.split('-')
    if (key && type && key.trim() === String(value)) {
      return type.trim()
    }
  }
  return ''
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
.merchant-management {
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