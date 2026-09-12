<template>
  <div class="message-page">
    <el-card shadow="never" class="filter-card">
      <el-form inline @submit.prevent>
        <el-form-item label="接收用户ID">
          <el-input v-model="query.userId" placeholder="精确用户ID" clearable style="width: 130px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="消息类型">
          <el-select v-model="query.type" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="(t, v) in enumMaps.messageType" :key="v" :label="t" :value="v" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button v-perm="'admin:message:push'" type="success" plain @click="openSinglePush">推送单条</el-button>
          <el-button v-perm="'admin:message:push'" type="warning" plain @click="openBatchPush">批量推送</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="userId" label="接收用户ID" width="100" align="center" />
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="content" label="内容" min-width="240" show-overflow-tooltip />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ enumText('messageType', row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="已读" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isRead === 1 ? 'info' : 'success'" size="small">
              {{ row.isRead === 1 ? '已读' : '未读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发送时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="110">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-perm="'admin:message:manage'" link type="danger" @click="remove(row)">删除</el-button>
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

    <!-- 消息详情 -->
    <el-dialog v-model="detailVisible" title="消息详情" width="520px" destroy-on-close>
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="ID">{{ detail?.id }}</el-descriptions-item>
        <el-descriptions-item label="接收用户ID">{{ detail?.userId }}</el-descriptions-item>
        <el-descriptions-item label="标题">{{ detail?.title }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ enumText('messageType', detail?.type) }}</el-descriptions-item>
        <el-descriptions-item label="已读">{{ detail?.isRead === 1 ? '已读' : '未读' }}</el-descriptions-item>
        <el-descriptions-item label="内容">{{ detail?.content || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发送时间">{{ formatDateTime(detail?.createTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 推送单条 -->
    <el-dialog v-model="singleVisible" title="推送单条消息" width="520px" destroy-on-close>
      <el-form :model="singleForm" label-width="90px">
        <el-form-item label="接收用户" required>
          <el-select
            v-model="singleForm.userId"
            filterable
            remote
            clearable
            reserve-keyword
            placeholder="输入姓名 / 手机号搜索会员"
            :remote-method="searchMember"
            :loading="memberLoading"
            style="width: 100%"
          >
            <el-option
              v-for="m in memberOptions"
              :key="m.id"
              :label="`${m.realName || m.phone}（${m.phone}）`"
              :value="m.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="singleForm.title" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="singleForm.type" style="width: 100%">
            <el-option v-for="(t, v) in enumMaps.messageType" :key="v" :label="t" :value="v" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="singleForm.content" type="textarea" :rows="4" maxlength="10000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="singleVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitSingle">发送</el-button>
      </template>
    </el-dialog>

    <!-- 批量推送 -->
    <el-dialog v-model="batchVisible" title="批量推送消息" width="520px" destroy-on-close>
      <el-form :model="batchForm" label-width="90px">
        <el-form-item label="接收用户" required>
          <el-select
            v-model="batchForm.userIds"
            multiple
            filterable
            remote
            collapse-tags
            reserve-keyword
            placeholder="输入姓名 / 手机号搜索并选择会员"
            :remote-method="searchMember"
            :loading="memberLoading"
            style="width: 100%"
          >
            <el-option
              v-for="m in memberOptions"
              :key="m.id"
              :label="`${m.realName || m.phone}（${m.phone}）`"
              :value="m.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="batchForm.title" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="batchForm.type" style="width: 100%">
            <el-option v-for="(t, v) in enumMaps.messageType" :key="v" :label="t" :value="v" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="batchForm.content" type="textarea" :rows="4" maxlength="10000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitBatch">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMessagePage, getMessageDetail, pushMessage, pushBatchMessage, deleteMessage } from '../../api/message'
import { getMemberPage } from '../../api/member'
import { formatDateTime, enumMaps, enumText } from '../../utils/format'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ userId: '', type: '', page: 1, size: 10 })

const detailVisible = ref(false)
const detail = ref(null)

const memberLoading = ref(false)
const memberOptions = ref([])

const singleVisible = ref(false)
const singleForm = reactive({ userId: null, title: '', type: 'SYSTEM', content: '' })
const batchVisible = ref(false)
const batchForm = reactive({ userIds: [], title: '', type: 'SYSTEM', content: '' })
const submitting = ref(false)

async function load() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.userId) params.userId = query.userId
    if (query.type) params.type = query.type
    const data = await getMessagePage(params)
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
  query.userId = ''
  query.type = ''
  query.page = 1
  load()
}

function handleSizeChange() {
  query.page = 1
  load()
}

async function openDetail(row) {
  detail.value = row
  detailVisible.value = true
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除消息「${row.title}」吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteMessage(row.id)
    ElMessage.success('已删除')
    load()
  } catch {
    /* 拦截器已提示 */
  }
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

function openSinglePush() {
  Object.assign(singleForm, { userId: null, title: '', type: 'SYSTEM', content: '' })
  singleVisible.value = true
}

async function submitSingle() {
  if (!singleForm.userId) return ElMessage.warning('请选择接收用户')
  if (!singleForm.title.trim()) return ElMessage.warning('请输入标题')
  submitting.value = true
  try {
    await pushMessage(singleForm)
    ElMessage.success('消息已推送')
    singleVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

function openBatchPush() {
  Object.assign(batchForm, { userIds: [], title: '', type: 'SYSTEM', content: '' })
  batchVisible.value = true
}

async function submitBatch() {
  if (!batchForm.userIds.length) return ElMessage.warning('请选择接收用户')
  if (!batchForm.title.trim()) return ElMessage.warning('请输入标题')
  submitting.value = true
  try {
    const count = await pushBatchMessage(batchForm)
    ElMessage.success(`已推送 ${count} 条`)
    batchVisible.value = false
    load()
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
</style>
