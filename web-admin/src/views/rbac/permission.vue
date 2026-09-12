<template>
  <div class="permission-page">
    <el-card shadow="never" class="filter-card">
      <el-form inline @submit.prevent>
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="权限编码 / 名称"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button v-perm="'admin:permission:manage'" type="primary" plain @click="openEdit()">新增权限</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="permissionCode" label="权限编码" min-width="200" />
        <el-table-column prop="permissionName" label="权限名称" min-width="140" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="更新时间" width="165">
          <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="{ row }">
            <el-button v-perm="'admin:permission:manage'" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-perm="'admin:permission:manage'" link type="primary" @click="openResources(row)">挂资源</el-button>
            <el-button v-perm="'admin:permission:manage'" link type="danger" @click="remove(row)">删除</el-button>
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

    <!-- 新增 / 编辑权限 -->
    <el-dialog v-model="editVisible" :title="form.id ? '编辑权限' : '新增权限'" width="500px" destroy-on-close>
      <el-form :model="form" label-width="90px" ref="formRef" :rules="formRules">
        <el-form-item label="权限编码" prop="permissionCode">
          <el-input v-model="form.permissionCode" maxlength="100" placeholder="如 admin:member:manage" />
        </el-form-item>
        <el-form-item label="权限名称" prop="permissionName">
          <el-input v-model="form.permissionName" maxlength="100" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 挂资源 -->
    <el-dialog v-model="resVisible" :title="`挂资源 · ${current?.permissionName}`" width="680px" destroy-on-close>
      <el-tabs v-model="resTab" @tab-change="loadResourceType">
        <el-tab-pane label="接口资源" name="API" />
        <el-tab-pane label="菜单资源" name="MENU" />
        <el-tab-pane label="按钮资源" name="BUTTON" />
      </el-tabs>
      <el-checkbox-group v-model="selectedResIds" class="res-group">
        <el-checkbox v-for="r in resList" :key="r.id" :value="r.id">
          {{ r.resourceName }}（{{ r.resourceCode }}）
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="resVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitResources">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getPermissionPage,
  createPermission,
  updatePermission,
  deletePermission,
  getPermissionResources,
  assignPermissionResources
} from '../../api/rbac'
import { getResourcePage } from '../../api/rbac'
import { formatDateTime } from '../../utils/format'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ keyword: '', page: 1, size: 10 })

const editVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const emptyForm = () => ({ id: null, permissionCode: '', permissionName: '', description: '' })
const form = reactive(emptyForm())
const formRules = {
  permissionCode: [{ required: true, message: '请输入权限编码', trigger: 'blur' }],
  permissionName: [{ required: true, message: '请输入权限名称', trigger: 'blur' }]
}

const current = ref(null)
const resVisible = ref(false)
const resTab = ref('API')
const resList = ref([])
const selectedResIds = ref([])

async function load() {
  loading.value = true
  try {
    const data = await getPermissionPage(query)
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
  if (row) Object.assign(form, { ...row })
  editVisible.value = true
}

async function submitEdit() {
  await formRef.value.validate().catch(() => Promise.reject())
  submitting.value = true
  try {
    const payload = {
      permissionCode: form.permissionCode,
      permissionName: form.permissionName,
      description: form.description
    }
    if (form.id) {
      await updatePermission(form.id, payload)
      ElMessage.success('权限已更新')
    } else {
      await createPermission(payload)
      ElMessage.success('权限已创建')
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
      `删除后已授权的角色将立即失去该权限，确定删除「${row.permissionName}」吗？`,
      '删除确认',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await deletePermission(row.id)
    ElMessage.success('已删除')
    load()
  } catch {
    /* 拦截器已提示 */
  }
}

// 按类型拉全量资源（57 个，一次约 20 条以内）
async function loadResourceType() {
  const data = await getResourcePage({ type: resTab.value, page: 1, size: 1000 })
  resList.value = data.list || []
}

async function openResources(row) {
  current.value = row
  resTab.value = 'API'
  try {
    const granted = await getPermissionResources(row.id)
    selectedResIds.value = granted.map((r) => r.id)
    resVisible.value = true
    await loadResourceType()
  } catch {
    /* 拦截器已提示 */
  }
}

async function submitResources() {
  submitting.value = true
  try {
    await assignPermissionResources(current.value.id, selectedResIds.value)
    ElMessage.success('资源已保存，已刷新缓存')
    resVisible.value = false
  } finally {
    submitting.value = false
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
.res-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 420px;
  overflow: auto;
}
</style>
