<template>
  <div class="activity-page">
    <el-card shadow="never">
      <div class="toolbar">
        <div class="toolbar-title">社区活动</div>
        <el-button v-perm="'admin:activity:manage'" type="primary" @click="openEdit()">新建活动</el-button>
      </div>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
        <el-table-column prop="location" label="地点" min-width="130" show-overflow-tooltip />
        <el-table-column label="报名时间" min-width="170">
          <template #default="{ row }">
            <div class="time-cell">{{ formatDateTime(row.registrationStartTime) }}</div>
            <div class="time-cell dim">至 {{ formatDateTime(row.registrationEndTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="活动时间" min-width="170">
          <template #default="{ row }">
            <div class="time-cell">{{ formatDateTime(row.activityStartTime) }}</div>
            <div class="time-cell dim">至 {{ formatDateTime(row.activityEndTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="名额" width="100" align="center">
          <template #default="{ row }">
            {{ row.currentParticipants || 0 }}/{{ row.maxParticipants || '不限' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="85" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ enumText('activityStatus', row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="220">
          <template #default="{ row }">
            <el-button v-perm="'admin:activity:manage'" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-perm="'admin:activity:registrations'" link type="primary" @click="openRegistrations(row)">
              报名列表
            </el-button>
            <el-button
              v-if="row.status === 'DRAFT'"
              v-perm="'admin:activity:manage'"
              link
              type="success"
              @click="quickStatus(row, 'REGISTRATING')"
            >
              发布
            </el-button>
            <el-button
              v-if="row.status === 'IN_PROGRESS'"
              v-perm="'admin:activity:manage'"
              link
              type="warning"
              @click="quickStatus(row, 'ENDED')"
            >
              结束
            </el-button>
            <el-button v-perm="'admin:activity:manage'" link type="danger" @click="remove(row)">删除</el-button>
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

    <!-- 新建 / 编辑活动 -->
    <el-dialog v-model="editVisible" :title="form.id ? '编辑活动' : '新建活动'" width="620px" destroy-on-close>
      <el-form :model="form" label-width="110px" ref="formRef" :rules="formRules">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="封面URL">
          <el-input v-model="form.coverUrl" maxlength="500" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="地点">
          <el-input v-model="form.location" maxlength="200" />
        </el-form-item>
        <el-form-item label="活动内容">
          <el-input v-model="form.content" type="textarea" :rows="3" maxlength="10000" show-word-limit />
        </el-form-item>
        <el-form-item label="报名开始">
          <el-date-picker v-model="form.registrationStartTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item label="报名结束">
          <el-date-picker v-model="form.registrationEndTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item label="活动开始">
          <el-date-picker v-model="form.activityStartTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item label="活动结束">
          <el-date-picker v-model="form.activityEndTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item label="人数上限">
          <el-input-number v-model="form.maxParticipants" :min="0" :max="100000" />
          <span class="form-tip">0 表示不限</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 200px">
            <el-option v-for="(t, v) in enumMaps.activityStatus" :key="v" :label="t" :value="v" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 报名列表抽屉 -->
    <el-drawer v-model="regVisible" size="600px" :title="`报名列表 · ${regActivityTitle}`" destroy-on-close>
      <el-table :data="regList" v-loading="regLoading" size="small">
        <el-table-column prop="realName" label="姓名" width="110" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="签到状态" width="95">
          <template #default="{ row }">
            <el-tag :type="row.checkInStatus === 'CHECKED_IN' ? 'success' : 'info'" size="small">
              {{ enumText('checkInStatus', row.checkInStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="签到时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.checkInTime) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="报名时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!regList.length && !regLoading" description="暂无报名" />
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createActivity,
  updateActivity,
  deleteActivity,
  getActivityPage,
  getActivityRegistrations
} from '../../api/activity'
import { formatDateTime, enumMaps, enumText } from '../../utils/format'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10 })

const editVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const emptyForm = () => ({
  id: null,
  title: '',
  coverUrl: '',
  content: '',
  location: '',
  registrationStartTime: '',
  registrationEndTime: '',
  activityStartTime: '',
  activityEndTime: '',
  maxParticipants: 0,
  status: 'DRAFT'
})
const form = reactive(emptyForm())
const formRules = { title: [{ required: true, message: '请输入活动标题', trigger: 'blur' }] }

const regVisible = ref(false)
const regActivityTitle = ref('')
const regList = ref([])
const regLoading = ref(false)

function statusTagType(status) {
  return { DRAFT: 'info', REGISTRATING: 'primary', IN_PROGRESS: 'warning', ENDED: 'info' }[status] || 'info'
}

async function load() {
  loading.value = true
  try {
    const data = await getActivityPage(query)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
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
      title: row.title,
      coverUrl: row.coverUrl || '',
      content: row.content || '',
      location: row.location || '',
      registrationStartTime: row.registrationStartTime || '',
      registrationEndTime: row.registrationEndTime || '',
      activityStartTime: row.activityStartTime || '',
      activityEndTime: row.activityEndTime || '',
      maxParticipants: row.maxParticipants || 0,
      status: row.status
    })
  }
  editVisible.value = true
}

async function submitEdit() {
  await formRef.value.validate().catch(() => Promise.reject())
  submitting.value = true
  try {
    const payload = {
      title: form.title,
      coverUrl: form.coverUrl,
      content: form.content,
      location: form.location,
      registrationStartTime: form.registrationStartTime || null,
      registrationEndTime: form.registrationEndTime || null,
      activityStartTime: form.activityStartTime || null,
      activityEndTime: form.activityEndTime || null,
      maxParticipants: form.maxParticipants || null,
      status: form.status
    }
    if (form.id) {
      await updateActivity(form.id, payload)
      ElMessage.success('活动已更新')
    } else {
      await createActivity(payload)
      ElMessage.success('活动已创建')
    }
    editVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function quickStatus(row, status) {
  const label = status === 'REGISTRATING' ? '发布' : '结束'
  try {
    await ElMessageBox.confirm(`确定要${label}活动「${row.title}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await updateActivity(row.id, { status })
    ElMessage.success(`${label}成功`)
    load()
  } catch {
    /* 拦截器已提示 */
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除活动「${row.title}」吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteActivity(row.id)
    ElMessage.success('已删除')
    load()
  } catch {
    /* 拦截器已提示 */
  }
}

async function openRegistrations(row) {
  regActivityTitle.value = row.title
  regVisible.value = true
  regLoading.value = true
  try {
    regList.value = await getActivityRegistrations(row.id)
  } finally {
    regLoading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}
.toolbar-title {
  font-size: 15px;
  font-weight: 600;
}
.pager {
  margin-top: 14px;
  justify-content: flex-end;
}
.time-cell {
  line-height: 1.5;
}
.time-cell.dim {
  color: #909399;
}
.form-tip {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
</style>
