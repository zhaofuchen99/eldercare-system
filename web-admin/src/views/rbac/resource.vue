<template>
  <div class="resource-page">
    <el-card shadow="never" class="filter-card">
      <el-form inline @submit.prevent>
        <el-form-item label="类型">
          <el-select v-model="query.type" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="(t, v) in enumMaps.resourceType" :key="v" :label="t" :value="v" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="资源编码 / 名称"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button v-perm="'admin:resource:manage'" type="primary" plain @click="openEdit()">新增资源</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="resourceCode" label="资源编码" min-width="210" />
        <el-table-column prop="resourceName" label="资源名称" min-width="120" />
        <el-table-column label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ enumText('resourceType', row.resourceType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路径" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.path || '-' }}</template>
        </el-table-column>
        <el-table-column prop="parentId" label="父ID" width="70" align="center" />
        <el-table-column prop="sortOrder" label="排序" width="70" align="center" />
        <el-table-column label="更新时间" width="165">
          <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="120">
          <template #default="{ row }">
            <el-button v-perm="'admin:resource:manage'" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-perm="'admin:resource:manage'" link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pager"
        background
        layout="total, prev, pager, next, sizes"
        :total="total"
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :page-sizes="[10, 20, 50]"
        @current-change="load"
        @size-change="handleSizeChange"
      />
    </el-card>

    <!-- 新增 / 编辑资源 -->
    <el-dialog v-model="editVisible" :title="form.id ? '编辑资源' : '新增资源'" width="520px" destroy-on-close>
      <el-form :model="form" label-width="90px" ref="formRef" :rules="formRules">
        <el-form-item label="资源编码" prop="resourceCode">
          <el-input v-model="form.resourceCode" maxlength="100" placeholder="api: / menu: / btn: 前缀" />
        </el-form-item>
        <el-form-item label="资源名称" prop="resourceName">
          <el-input v-model="form.resourceName" maxlength="100" />
        </el-form-item>
        <el-form-item label="类型" prop="resourceType">
          <el-select v-model="form.resourceType" style="width: 100%">
            <el-option v-for="(t, v) in enumMaps.resourceType" :key="v" :label="t" :value="v" />
          </el-select>
        </el-form-item>
        <el-form-item label="路径">
          <el-input v-model="form.path" maxlength="200" placeholder="API 类为 Ant 路径模式，如 /api/admin/member/**" />
        </el-form-item>
        <el-form-item label="父级ID">
          <el-input-number v-model="form.parentId" :min="0" />
          <span class="form-tip">0 表示顶级</span>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getResourcePage,
  createResource,
  updateResource,
  deleteResource
} from '../../api/rbac'
import { formatDateTime, enumMaps, enumText } from '../../utils/format'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ type: '', keyword: '', page: 1, size: 10 })

const editVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const emptyForm = () => ({
  id: null,
  resourceCode: '',
  resourceName: '',
  resourceType: 'API',
  path: '',
  parentId: 0,
  sortOrder: 0
})
const form = reactive(emptyForm())
const formRules = {
  resourceCode: [{ required: true, message: '请输入资源编码', trigger: 'blur' }],
  resourceName: [{ required: true, message: '请输入资源名称', trigger: 'blur' }],
  resourceType: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

async function load() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.type) params.type = query.type
    if (query.keyword) params.keyword = query.keyword
    const data = await getResourcePage(params)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  load()
}

function handleReset() {
  query.type = ''
  query.keyword = ''
  query.page = 1
  load()
}

function handleSizeChange() {
  query.page = 1
  load()
}

function openEdit(row) {
  Object.assign(form, emptyForm())
  if (row) {
    Object.assign(form, {
      id: row.id,
      resourceCode: row.resourceCode,
      resourceName: row.resourceName,
      resourceType: row.resourceType,
      path: row.path || '',
      parentId: row.parentId || 0,
      sortOrder: row.sortOrder || 0
    })
  }
  editVisible.value = true
}

async function submitEdit() {
  await formRef.value.validate().catch(() => Promise.reject())
  submitting.value = true
  try {
    const payload = {
      resourceCode: form.resourceCode,
      resourceName: form.resourceName,
      resourceType: form.resourceType,
      path: form.path,
      parentId: form.parentId,
      sortOrder: form.sortOrder
    }
    if (form.id) {
      await updateResource(form.id, payload)
      ElMessage.success('资源已更新')
    } else {
      await createResource(payload)
      ElMessage.success('资源已创建')
    }
    editVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `删除资源「${row.resourceName}」后，相关权限将失去该资源，确定删除吗？`,
      '删除确认',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await deleteResource(row.id)
    ElMessage.success('已删除')
    load()
  } catch {
    /* 拦截器已提示 */
  }
}

onMounted(load)
</script>

<style scoped>
.filter-card {
  margin-bottom: 14px;
}
.pager {
  margin-top: 14px;
  justify-content: flex-end;
}
.form-tip {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
</style>
