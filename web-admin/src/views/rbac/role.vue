<template>
  <div class="role-page">
    <el-card shadow="never" class="filter-card">
      <el-form inline @submit.prevent>
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="角色编码 / 名称"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button v-perm="'admin:role:manage'" type="primary" plain @click="openEdit()">新增角色</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="roleCode" label="角色编码" min-width="140" />
        <el-table-column prop="roleName" label="角色名称" min-width="120" />
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ enumText('roleStatus', row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="220">
          <template #default="{ row }">
            <el-button v-perm="'admin:role:manage'" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-perm="'admin:role:manage'" link type="primary" @click="openPerms(row)">分配权限</el-button>
            <el-button v-perm="'admin:role:manage'" link type="primary" @click="openUsers(row)">用户</el-button>
            <el-button v-perm="'admin:role:manage'" link type="danger" @click="remove(row)">删除</el-button>
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

    <!-- 新增 / 编辑角色 -->
    <el-dialog v-model="editVisible" :title="form.id ? '编辑角色' : '新增角色'" width="480px" destroy-on-close>
      <el-form :model="form" label-width="90px" ref="formRef" :rules="formRules">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" maxlength="50" placeholder="如 MANAGER" :disabled="isBuiltin" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" maxlength="50" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="200" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分配权限 -->
    <el-dialog v-model="permsVisible" :title="`分配权限 · ${current?.roleName}`" width="620px" destroy-on-close>
      <el-checkbox-group v-model="selectedPermIds" class="perm-group">
        <el-checkbox v-for="p in allPerms" :key="p.id" :value="p.id">
          {{ p.permissionName }}（{{ p.permissionCode }}）
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="permsVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitPerms">保存</el-button>
      </template>
    </el-dialog>

    <!-- 查看 / 分配用户 -->
    <el-dialog v-model="usersVisible" :title="`角色用户 · ${current?.roleName}`" width="560px" destroy-on-close>
      <div class="users-toolbar">
        <el-select
          v-model="userIdsToAssign"
          multiple
          filterable
          remote
          collapse-tags
          reserve-keyword
          placeholder="搜索并选择会员（将替换该角色全部用户）"
          :remote-method="searchMember"
          :loading="memberLoading"
          style="flex: 1"
        >
          <el-option v-for="m in memberOptions" :key="m.id" :label="`${m.realName || m.phone}（${m.phone}）`" :value="m.id" />
        </el-select>
        <el-button type="primary" :loading="submitting" @click="submitUsers">保存分配</el-button>
      </div>
      <el-table :data="userList" v-loading="userLoading" size="small" max-height="360">
        <el-table-column prop="realName" label="姓名" width="110" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="role" label="角色" width="100" />
        <el-table-column prop="createTime" label="加入时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pager"
        small
        background
        layout="total, prev, pager, next"
        :total="userTotal"
        v-model:current-page="userQuery.page"
        v-model:page-size="userQuery.size"
        @current-change="loadUsers"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getRolePage,
  createRole,
  updateRole,
  deleteRole,
  getRolePermissions,
  assignRolePermissions,
  getRoleUsers,
  assignRoleUsers,
  getPermissionPage
} from '../../api/rbac'
import { getMemberPage } from '../../api/member'
import { formatDateTime, enumText } from '../../utils/format'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ keyword: '', page: 1, size: 10 })

const editVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const isBuiltin = ref(false)
const emptyForm = () => ({ id: null, roleCode: '', roleName: '', description: '', status: 1 })
const form = reactive(emptyForm())
const formRules = {
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

const current = ref(null)
const permsVisible = ref(false)
const allPerms = ref([])
const selectedPermIds = ref([])

const usersVisible = ref(false)
const userList = ref([])
const userTotal = ref(0)
const userLoading = ref(false)
const userQuery = reactive({ page: 1, size: 10 })
const userIdsToAssign = ref([])
const memberLoading = ref(false)
const memberOptions = ref([])

async function load() {
  loading.value = true
  try {
    const data = await getRolePage(query)
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
  isBuiltin.value = false
  if (row) {
    Object.assign(form, { ...row })
    isBuiltin.value = row.roleCode === 'ADMIN' || row.roleCode === 'MEMBER'
  }
  editVisible.value = true
}

async function submitEdit() {
  await formRef.value.validate().catch(() => Promise.reject())
  submitting.value = true
  try {
    const payload = {
      roleCode: form.roleCode,
      roleName: form.roleName,
      description: form.description,
      status: form.status
    }
    if (form.id) {
      await updateRole(form.id, payload)
      ElMessage.success('角色已更新')
    } else {
      await createRole(payload)
      ElMessage.success('角色已创建')
    }
    editVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除角色「${row.roleName}」吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteRole(row.id)
    ElMessage.success('已删除')
    load()
  } catch {
    /* 拦截器已提示 */
  }
}

// 全量权限列表（46 条，一次性拉取）
async function ensureAllPerms() {
  if (allPerms.value.length) return
  const data = await getPermissionPage({ page: 1, size: 1000 })
  allPerms.value = data.list || []
}

async function openPerms(row) {
  current.value = row
  try {
    await ensureAllPerms()
    const granted = await getRolePermissions(row.id)
    selectedPermIds.value = granted.map((p) => p.id)
    permsVisible.value = true
  } catch {
    /* 拦截器已提示 */
  }
}

async function submitPerms() {
  submitting.value = true
  try {
    await assignRolePermissions(current.value.id, selectedPermIds.value)
    ElMessage.success('权限已保存，已刷新缓存')
    permsVisible.value = false
  } finally {
    submitting.value = false
  }
}

async function loadUsers() {
  userLoading.value = true
  try {
    const data = await getRoleUsers(current.value.id, { page: userQuery.page, size: userQuery.size })
    userList.value = data.list || []
    userTotal.value = data.total || 0
  } finally {
    userLoading.value = false
  }
}

async function openUsers(row) {
  current.value = row
  userIdsToAssign.value = []
  userQuery.page = 1
  usersVisible.value = true
  loadUsers()
}

async function searchMember(keyword) {
  memberLoading.value = true
  try {
    const data = await getMemberPage({ keyword, page: 1, size: 20 })
    memberOptions.value = data.list || []
  } finally {
    memberLoading.value = false
  }
}

async function submitUsers() {
  if (!userIdsToAssign.value.length) return ElMessage.warning('请选择要分配的用户')
  submitting.value = true
  try {
    await assignRoleUsers(current.value.id, userIdsToAssign.value)
    ElMessage.success('分配成功，将替换该角色全部用户')
    usersVisible.value = false
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
.perm-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 420px;
  overflow: auto;
}
.users-toolbar {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 14px;
}
</style>
