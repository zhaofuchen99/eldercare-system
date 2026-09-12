<template>
  <div class="member-page">
    <!-- 筛选 -->
    <el-card shadow="never" class="filter-card">
      <el-form inline :model="query" @submit.prevent>
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="姓名 / 手机号"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="(t, v) in enumMaps.userStatus" :key="v" :label="t" :value="v" />
          </el-select>
        </el-form-item>
        <el-form-item label="会员等级">
          <el-select v-model="query.memberLevel" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="(t, v) in enumMaps.memberLevel" :key="v" :label="t" :value="v" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="realName" label="姓名" width="110" />
        <el-table-column label="性别" width="70">
          <template #default="{ row }">{{ genderText(row.gender) }}</template>
        </el-table-column>
        <el-table-column label="出生日期" width="115">
          <template #default="{ row }">{{ formatDate(row.birthDate) }}</template>
        </el-table-column>
        <el-table-column label="会员等级" width="95" align="center">
          <template #default="{ row }">
            <el-tag :type="levelTagType(row.memberLevel)">{{ enumText('memberLevel', row.memberLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="points" label="积分" width="80" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'">
              {{ enumText('userStatus', row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="165">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-dropdown v-perm="'admin:member:manage'" @command="(c) => handleMore(c, row)">
              <el-button link type="primary">
                更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="level">调整等级</el-dropdown-item>
                  <el-dropdown-item command="points">调整积分</el-dropdown-item>
                  <el-dropdown-item command="reset">重置密码</el-dropdown-item>
                  <el-dropdown-item command="toggle" divided>
                    {{ row.status === 'ENABLED' ? '禁用会员' : '启用会员' }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
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

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" size="640px" :title="`会员详情 · ${current?.realName || ''}`" destroy-on-close>
      <template v-if="current">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="ID">{{ current.id }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ current.phone }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ current.realName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ genderText(current.gender) }}</el-descriptions-item>
          <el-descriptions-item label="出生日期">{{ formatDate(current.birthDate) }}</el-descriptions-item>
          <el-descriptions-item label="身高">{{ current.height ? current.height + ' cm' : '-' }}</el-descriptions-item>
          <el-descriptions-item label="紧急联系人">{{ current.emergencyContact || '-' }}</el-descriptions-item>
          <el-descriptions-item label="会员等级">
            {{ enumText('memberLevel', current.memberLevel) }}
          </el-descriptions-item>
          <el-descriptions-item label="积分">{{ current.points ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ enumText('userStatus', current.status) }}</el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">{{ formatDateTime(current.createTime) }}</el-descriptions-item>
        </el-descriptions>

        <el-tabs class="detail-tabs" v-model="activeTab" @tab-change="onTabChange">
          <!-- 健康记录 -->
          <el-tab-pane label="健康记录" name="health">
            <el-table :data="healthList" v-loading="healthLoading" size="small">
              <el-table-column prop="recordedTime" label="记录时间" width="150">
                <template #default="{ row }">{{ formatDateTime(row.recordedTime) }}</template>
              </el-table-column>
              <el-table-column prop="systolic" label="收缩压" width="80" />
              <el-table-column prop="diastolic" label="舒张压" width="80" />
              <el-table-column prop="bloodSugar" label="血糖" width="70" />
              <el-table-column prop="heartRate" label="心率" width="70" />
              <el-table-column prop="weight" label="体重" width="70" />
              <el-table-column prop="bmi" label="BMI" width="70" />
              <el-table-column prop="memo" label="备注" show-overflow-tooltip />
            </el-table>
            <el-pagination
              class="pager"
              small
              background
              layout="total, prev, pager, next"
              :total="healthTotal"
              v-model:current-page="healthQuery.page"
              v-model:page-size="healthQuery.size"
              @current-change="loadHealth"
            />
          </el-tab-pane>

          <!-- 预约记录 -->
          <el-tab-pane label="预约记录" name="appointment">
            <el-table :data="apptList" v-loading="apptLoading" size="small">
              <el-table-column prop="packageName" label="套餐" show-overflow-tooltip />
              <el-table-column prop="appointDate" label="预约日期" width="105" />
              <el-table-column prop="timeRange" label="时段" width="105" />
              <el-table-column label="状态" width="80">
                <template #default="{ row }">
                  <el-tag :type="apptStatusTagType(row.status)" size="small">
                    {{ enumText('appointmentStatus', row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="提交时间" width="150">
                <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
              </el-table-column>
              <el-table-column label="报告" width="80">
                <template #default="{ row }">
                  <el-button
                    v-if="row.reportDownloadUrl"
                    link
                    type="primary"
                    @click="downloadReport(row)"
                  >
                    下载
                  </el-button>
                  <span v-else>-</span>
                </template>
              </el-table-column>
            </el-table>
            <el-pagination
              class="pager"
              small
              background
              layout="total, prev, pager, next"
              :total="apptTotal"
              v-model:current-page="apptQuery.page"
              v-model:page-size="apptQuery.size"
              @current-change="loadAppointments"
            />
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-drawer>

    <!-- 等级调整 -->
    <el-dialog v-model="levelVisible" title="会员等级调整" width="380px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="当前会员">
          <span>{{ current?.realName }}（{{ enumText('memberLevel', current?.memberLevel) }}）</span>
        </el-form-item>
        <el-form-item label="调整至">
          <el-select v-model="levelForm.memberLevel" style="width: 100%">
            <el-option v-for="(t, v) in enumMaps.memberLevel" :key="v" :label="t" :value="v" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="levelVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitLevel">确定</el-button>
      </template>
    </el-dialog>

    <!-- 积分调整 -->
    <el-dialog v-model="pointsVisible" title="积分手动调整" width="380px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="当前积分">
          <span>{{ current?.points ?? 0 }}</span>
        </el-form-item>
        <el-form-item label="调整数量">
          <el-input-number v-model="pointsForm.delta" :step="10" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-alert
          type="info"
          :closable="false"
          title="正数=调增，负数=调减；调减时积分不足将报错"
        />
      </el-form>
      <template #footer>
        <el-button @click="pointsVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitPoints">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码 -->
    <el-dialog v-model="resetVisible" title="重置密码" width="420px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="新密码">
          <el-input
            v-model="resetForm.password"
            type="password"
            show-password
            placeholder="留空则系统生成（10 位随机密码）"
          />
        </el-form-item>
        <el-alert
          v-if="resetResult"
          type="success"
          :closable="false"
          title="重置成功"
          :description="`新密码：${resetResult}`"
        />
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">关闭</el-button>
        <el-button v-if="!resetResult" type="primary" :loading="submitting" @click="submitReset">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import {
  getMemberPage,
  enableMember,
  disableMember,
  updateMemberLevel,
  adjustMemberPoints,
  resetMemberPassword
} from '../../api/member'
import { getHealthRecordPage } from '../../api/health'
import { getAppointmentPage } from '../../api/appointment'
import { formatDateTime, formatDate, enumMaps, enumText } from '../../utils/format'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ keyword: '', status: '', memberLevel: '', page: 1, size: 10 })

// 详情抽屉
const detailVisible = ref(false)
const current = ref(null)
const activeTab = ref('health')
const healthList = ref([])
const healthTotal = ref(0)
const healthLoading = ref(false)
const healthQuery = reactive({ page: 1, size: 10 })
const apptList = ref([])
const apptTotal = ref(0)
const apptLoading = ref(false)
const apptQuery = reactive({ page: 1, size: 10 })

// 弹窗
const levelVisible = ref(false)
const levelForm = reactive({ memberLevel: '' })
const pointsVisible = ref(false)
const pointsForm = reactive({ delta: 0 })
const resetVisible = ref(false)
const resetForm = reactive({ password: '' })
const resetResult = ref('')
const submitting = ref(false)

function genderText(v) {
  if (v === 'M' || v === '1' || v === '男') return '男'
  if (v === 'F' || v === '0' || v === '女') return '女'
  return v || '-'
}

function levelTagType(level) {
  return { NORMAL: 'info', SILVER: 'warning', GOLD: '', PLATINUM: 'primary', DIAMOND: 'danger' }[level] || 'info'
}

function apptStatusTagType(status) {
  return {
    PENDING: 'warning',
    CONFIRMED: 'success',
    CANCELED: 'info',
    COMPLETED: 'primary'
  }[status]
}

async function load() {
  loading.value = true
  try {
    const data = await getMemberPage(query)
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
  query.status = ''
  query.memberLevel = ''
  query.page = 1
  load()
}

function handleSizeChange() {
  query.page = 1
  load()
}

async function loadHealth() {
  healthLoading.value = true
  try {
    const data = await getHealthRecordPage({
      userId: current.value.id,
      page: healthQuery.page,
      size: healthQuery.size
    })
    healthList.value = data.list || []
    healthTotal.value = data.total || 0
  } finally {
    healthLoading.value = false
  }
}

async function loadAppointments() {
  apptLoading.value = true
  try {
    const data = await getAppointmentPage({
      userId: current.value.id,
      page: apptQuery.page,
      size: apptQuery.size
    })
    apptList.value = data.list || []
    apptTotal.value = data.total || 0
  } finally {
    apptLoading.value = false
  }
}

function openDetail(row) {
  current.value = row
  activeTab.value = 'health'
  healthQuery.page = 1
  apptQuery.page = 1
  detailVisible.value = true
  loadHealth()
}

function onTabChange(name) {
  if (name === 'appointment' && apptList.value.length === 0) loadAppointments()
}

function downloadReport(row) {
  window.open(row.reportDownloadUrl, '_blank')
}

function openLevel(row) {
  current.value = row
  levelForm.memberLevel = row.memberLevel
  levelVisible.value = true
}

/** 操作列「更多」下拉命令分发 */
function handleMore(command, row) {
  if (command === 'level') openLevel(row)
  else if (command === 'points') openPoints(row)
  else if (command === 'reset') openReset(row)
  else if (command === 'toggle') toggleStatus(row)
}

async function submitLevel() {
  if (!levelForm.memberLevel) return ElMessage.warning('请选择等级')
  submitting.value = true
  try {
    await updateMemberLevel(current.value.id, levelForm.memberLevel)
    ElMessage.success('等级调整成功')
    levelVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

function openPoints(row) {
  current.value = row
  pointsForm.delta = 0
  pointsVisible.value = true
}

async function submitPoints() {
  if (!pointsForm.delta) return ElMessage.warning('请填写调整数量')
  submitting.value = true
  try {
    const balance = await adjustMemberPoints(current.value.id, pointsForm.delta)
    ElMessage.success(`调整成功，当前余额 ${balance}`)
    pointsVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

function openReset(row) {
  current.value = row
  resetForm.password = ''
  resetResult.value = ''
  resetVisible.value = true
}

async function submitReset() {
  submitting.value = true
  try {
    const pwd = await resetMemberPassword(current.value.id, resetForm.password || null)
    resetResult.value = pwd
    ElMessage.success('密码已重置，该会员已强制下线')
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row) {
  const action = row.status === 'ENABLED' ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确定要${action}会员「${row.realName}」吗？`,
      '提示',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    if (row.status === 'ENABLED') {
      await disableMember(row.id)
      ElMessage.success('已禁用，该会员将无法登录')
    } else {
      await enableMember(row.id)
      ElMessage.success('已启用')
    }
    load()
  } catch {
    /* 错误已由拦截器提示 */
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
.detail-tabs {
  margin-top: 16px;
}
</style>
