<template>
  <div class="appointment-page">
    <el-tabs v-model="activeTab">
      <!-- ============ 套餐管理 ============ -->
      <el-tab-pane label="套餐管理" name="package">
        <el-card shadow="never">
          <div class="toolbar">
            <div class="toolbar-title">体检套餐</div>
            <el-button v-perm="'admin:appointment:manage'" type="primary" @click="openPackageEdit()">新建套餐</el-button>
          </div>
          <el-table :data="packageList" v-loading="packageLoading" stripe>
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
            <el-table-column prop="price" label="积分价" width="90" align="center" />
            <el-table-column prop="suitablePeople" label="适合人群" min-width="140" show-overflow-tooltip />
            <el-table-column label="包含项目" min-width="220">
              <template #default="{ row }">
                <el-tag v-for="(it, i) in row.items || []" :key="i" size="small" class="item-tag">{{ it }}</el-tag>
                <span v-if="!row.items || !row.items.length">-</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'">
                  {{ enumText('slotStatus', row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="160">
              <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" fixed="right" width="200">
              <template #default="{ row }">
                <el-button v-perm="'admin:appointment:manage'" link type="primary" @click="openPackageEdit(row)">编辑</el-button>
                <el-button v-perm="'admin:appointment:manage'" link type="primary" @click="openSlotGen(row)">生成时段</el-button>
                <el-button v-perm="'admin:appointment:manage'" link type="danger" @click="removePackage(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            class="pager"
            background
            layout="total, prev, pager, next, sizes"
            :total="packageTotal"
            v-model:current-page="packageQuery.page"
            v-model:page-size="packageQuery.size"
            :page-sizes="[10, 20, 50]"
            @current-change="loadPackages"
            @size-change="packageSizeChange"
          />
        </el-card>
      </el-tab-pane>

      <!-- ============ 时段管理 ============ -->
      <el-tab-pane label="时段管理" name="slot">
        <el-card shadow="never" class="filter-card">
          <el-form inline @submit.prevent>
            <el-form-item label="套餐">
              <el-select v-model="slotQuery.packageId" placeholder="全部" clearable filterable style="width: 180px">
                <el-option v-for="p in packageList" :key="p.id" :label="p.name" :value="p.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="日期">
              <el-date-picker v-model="slotQuery.date" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadSlots">查询</el-button>
              <el-button @click="resetSlotQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
        <el-card shadow="never">
          <el-table :data="slotList" v-loading="slotLoading" stripe>
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="packageName" label="套餐" min-width="150" show-overflow-tooltip />
            <el-table-column prop="appointDate" label="预约日期" width="110" />
            <el-table-column prop="timeRange" label="时段" width="110" />
            <el-table-column prop="maxCount" label="名额" width="70" align="center" />
            <el-table-column prop="currentCount" label="已约" width="70" align="center" />
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="{ AVAILABLE: 'success', FULL: 'warning', CLOSED: 'info' }[row.status] || 'info'">
                  {{ enumText('slotStatus', row.status) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!slotList.length && !slotLoading" description="暂无时段，可到套餐管理里生成" />
        </el-card>
      </el-tab-pane>

      <!-- ============ 预约管理 ============ -->
      <el-tab-pane label="预约管理" name="appointment">
        <el-card shadow="never" class="filter-card">
          <el-form inline @submit.prevent>
            <el-form-item label="状态">
              <el-select v-model="apptQuery.status" placeholder="全部" clearable style="width: 130px">
                <el-option v-for="(t, v) in enumMaps.appointmentStatus" :key="v" :label="t" :value="v" />
              </el-select>
            </el-form-item>
            <el-form-item label="预约日期">
              <el-date-picker v-model="apptQuery.appointDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearchAppt">查询</el-button>
              <el-button @click="resetApptQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
        <el-card shadow="never">
          <el-table :data="apptList" v-loading="apptLoading" stripe>
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="userName" label="预约人" width="100" />
            <el-table-column prop="phone" label="手机号" width="120" />
            <el-table-column prop="packageName" label="套餐" min-width="140" show-overflow-tooltip />
            <el-table-column prop="appointDate" label="日期" width="105" />
            <el-table-column prop="timeRange" label="时段" width="105" />
            <el-table-column prop="price" label="积分" width="70" align="center" />
            <el-table-column label="状态" width="85" align="center">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)">{{ enumText('appointmentStatus', row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="originalFilename" label="报告" min-width="130" show-overflow-tooltip>
              <template #default="{ row }">
                <el-button
                  v-if="row.reportDownloadUrl"
                  link
                  type="primary"
                  @click="downloadReport(row)"
                >
                  {{ row.originalFilename || '下载报告' }}
                </el-button>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" fixed="right" width="170">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === 'PENDING'"
                  v-perm="'admin:appointment:manage'"
                  link
                  type="primary"
                  @click="setStatus(row, 'CONFIRMED')"
                >
                  确认
                </el-button>
                <el-button
                  v-if="row.status === 'PENDING' || row.status === 'CONFIRMED'"
                  v-perm="'admin:appointment:manage'"
                  link
                  type="danger"
                  @click="setStatus(row, 'CANCELED')"
                >
                  取消
                </el-button>
                <el-button
                  v-if="row.status !== 'CANCELED'"
                  v-perm="'admin:appointment:report'"
                  link
                  type="primary"
                  @click="openUpload(row)"
                >
                  上传报告
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            class="pager"
            background
            layout="total, prev, pager, next, sizes"
            :total="apptTotal"
            v-model:current-page="apptQuery.page"
            v-model:page-size="apptQuery.size"
            :page-sizes="[10, 20, 50]"
            @current-change="loadAppointments"
            @size-change="apptSizeChange"
          />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 新建 / 编辑套餐 -->
    <el-dialog v-model="packageEditVisible" :title="packageForm.id ? '编辑套餐' : '新建套餐'" width="600px" destroy-on-close>
      <el-form :model="packageForm" label-width="90px" ref="packageFormRef" :rules="packageRules">
        <el-form-item label="名称" prop="name">
          <el-input v-model="packageForm.name" maxlength="200" />
        </el-form-item>
        <el-form-item label="封面URL">
          <el-input v-model="packageForm.coverUrl" maxlength="500" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="packageForm.description" type="textarea" :rows="3" maxlength="2000" />
        </el-form-item>
        <el-form-item label="积分价">
          <el-input-number v-model="packageForm.price" :min="0" />
        </el-form-item>
        <el-form-item label="适合人群">
          <el-input v-model="packageForm.suitablePeople" maxlength="200" />
        </el-form-item>
        <el-form-item label="包含项目">
          <el-select
            v-model="packageForm.items"
            multiple
            filterable
            allow-create
            default-first-option
            no-data-text="输入后回车添加"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="packageForm.status">
            <el-radio value="ENABLED">启用</el-radio>
            <el-radio value="DISABLED">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="packageEditVisible = false">取消</el-button>
        <el-button type="primary" :loading="packageSubmitting" @click="submitPackage">保存</el-button>
      </template>
    </el-dialog>

    <!-- 批量生成时段 -->
    <el-dialog v-model="slotGenVisible" title="批量生成时段" width="560px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="套餐">
          <span>{{ slotGenForm.packageName }}</span>
        </el-form-item>
        <el-form-item label="日期" required>
          <el-date-picker
            v-model="slotGenForm.dates"
            type="dates"
            value-format="YYYY-MM-DD"
            placeholder="选择一个或多个日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="时间段" required>
          <div class="time-wrap">
            <div v-for="(t, i) in slotGenForm.timeRanges" :key="i" class="time-row">
              <el-input v-model="slotGenForm.timeRanges[i]" placeholder="如 09:00-10:00" style="width: 200px" />
              <el-button link type="danger" @click="slotGenForm.timeRanges.splice(i, 1)">删除</el-button>
            </div>
            <el-button type="primary" plain size="small" @click="slotGenForm.timeRanges.push('')">添加时间段</el-button>
          </div>
        </el-form-item>
        <el-form-item label="名额上限">
          <el-input-number v-model="slotGenForm.maxCount" :min="1" :max="1000" />
        </el-form-item>
        <el-alert type="info" :closable="false" title="按 日期×时间段 笛卡尔生成时段；已存在的「日期+时间段」自动跳过" />
      </el-form>
      <template #footer>
        <el-button @click="slotGenVisible = false">取消</el-button>
        <el-button type="primary" :loading="slotGenSubmitting" @click="submitSlotGen">生成</el-button>
      </template>
    </el-dialog>

    <!-- 上传报告 -->
    <el-dialog v-model="uploadVisible" title="上传体检报告" width="460px" destroy-on-close>
      <el-alert
        type="info"
        :closable="false"
        :title="`预约人：${uploadRow?.userName}（${uploadRow?.packageName}）`"
        class="upload-alert"
      />
      <el-upload
        drag
        :auto-upload="false"
        :limit="1"
        accept="application/pdf"
        :on-change="onFileChange"
        :on-exceed="() => ElMessage.warning('仅上传一个 PDF 文件')"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽 PDF 到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">仅支持 PDF 文件，不超过 20MB；上传成功后预约自动置为「已完成」</div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploadSubmitting" :disabled="!uploadFile" @click="submitUpload">
          上传
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import {
  createPackage,
  updatePackage,
  deletePackage,
  getPackagePage,
  createSlotsBatch,
  getSlots,
  getAppointmentPage,
  updateAppointmentStatus,
  uploadReport
} from '../../api/appointment'
import { formatDateTime, enumMaps, enumText } from '../../utils/format'

const activeTab = ref('package')

// ---- 套餐 ----
const packageLoading = ref(false)
const packageList = ref([])
const packageTotal = ref(0)
const packageQuery = reactive({ page: 1, size: 10 })

const packageEditVisible = ref(false)
const packageSubmitting = ref(false)
const packageFormRef = ref(null)
const emptyPackage = () => ({
  id: null,
  name: '',
  coverUrl: '',
  description: '',
  price: 0,
  suitablePeople: '',
  items: [],
  status: 'ENABLED'
})
const packageForm = reactive(emptyPackage())
const packageRules = { name: [{ required: true, message: '请输入套餐名称', trigger: 'blur' }] }

// ---- 时段 ----
const slotLoading = ref(false)
const slotList = ref([])
const slotQuery = reactive({ packageId: '', date: '' })
const slotGenVisible = ref(false)
const slotGenSubmitting = ref(false)
const slotGenForm = reactive({ packageId: null, packageName: '', dates: [], timeRanges: [''], maxCount: 10 })

// ---- 预约 ----
const apptLoading = ref(false)
const apptList = ref([])
const apptTotal = ref(0)
const apptQuery = reactive({ status: '', appointDate: '', page: 1, size: 10 })

const uploadVisible = ref(false)
const uploadRow = ref(null)
const uploadFile = ref(null)
const uploadSubmitting = ref(false)

// ---- 套餐接口 ----
async function loadPackages() {
  packageLoading.value = true
  try {
    const data = await getPackagePage(packageQuery)
    packageList.value = data.list || []
    packageTotal.value = data.total || 0
  } finally {
    packageLoading.value = false
  }
}

function packageSizeChange() {
  packageQuery.page = 1
  loadPackages()
}

function openPackageEdit(row) {
  Object.assign(packageForm, emptyPackage())
  if (row) {
    Object.assign(packageForm, {
      ...row,
      items: (row.items || []).map((i) => String(i))
    })
  }
  packageEditVisible.value = true
}

async function submitPackage() {
  await packageFormRef.value.validate().catch(() => Promise.reject())
  packageSubmitting.value = true
  try {
    const payload = {
      name: packageForm.name,
      coverUrl: packageForm.coverUrl,
      description: packageForm.description,
      price: packageForm.price,
      suitablePeople: packageForm.suitablePeople,
      items: packageForm.items,
      status: packageForm.status
    }
    if (packageForm.id) {
      await updatePackage(packageForm.id, payload)
      ElMessage.success('套餐已更新')
    } else {
      await createPackage(payload)
      ElMessage.success('套餐已创建')
    }
    packageEditVisible.value = false
    loadPackages()
  } finally {
    packageSubmitting.value = false
  }
}

async function removePackage(row) {
  try {
    await ElMessageBox.confirm(`确定删除套餐「${row.name}」吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deletePackage(row.id)
    ElMessage.success('已删除')
    loadPackages()
  } catch {
    /* 拦截器已提示 */
  }
}

// ---- 时段接口 ----
function openSlotGen(row) {
  Object.assign(slotGenForm, { packageId: row.id, packageName: row.name, dates: [], timeRanges: [''], maxCount: 10 })
  slotGenVisible.value = true
}

async function submitSlotGen() {
  if (!slotGenForm.dates.length) return ElMessage.warning('请选择日期')
  const ranges = slotGenForm.timeRanges.filter((t) => t.trim())
  if (!ranges.length) return ElMessage.warning('请至少填写一个时间段')
  slotGenSubmitting.value = true
  try {
    await createSlotsBatch({
      packageId: slotGenForm.packageId,
      dates: slotGenForm.dates,
      timeRanges: ranges,
      maxCount: slotGenForm.maxCount
    })
    ElMessage.success('时段已生成')
    slotGenVisible.value = false
    slotQuery.packageId = slotGenForm.packageId
    slotQuery.date = ''
    loadSlots()
  } finally {
    slotGenSubmitting.value = false
  }
}

async function loadSlots() {
  slotLoading.value = true
  try {
    const params = {}
    if (slotQuery.packageId) params.packageId = slotQuery.packageId
    if (slotQuery.date) params.date = slotQuery.date
    slotList.value = await getSlots(params)
  } finally {
    slotLoading.value = false
  }
}

function resetSlotQuery() {
  slotQuery.packageId = ''
  slotQuery.date = ''
  loadSlots()
}

// ---- 预约接口 ----
async function loadAppointments() {
  apptLoading.value = true
  try {
    const params = { page: apptQuery.page, size: apptQuery.size }
    if (apptQuery.status) params.status = apptQuery.status
    if (apptQuery.appointDate) params.appointDate = apptQuery.appointDate
    const data = await getAppointmentPage(params)
    apptList.value = data.list || []
    apptTotal.value = data.total || 0
  } finally {
    apptLoading.value = false
  }
}

function handleSearchAppt() {
  apptQuery.page = 1
  loadAppointments()
}

function resetApptQuery() {
  apptQuery.status = ''
  apptQuery.appointDate = ''
  apptQuery.page = 1
  loadAppointments()
}

function apptSizeChange() {
  apptQuery.page = 1
  loadAppointments()
}

function statusTagType(status) {
  return { PENDING: 'warning', CONFIRMED: 'success', CANCELED: 'info', COMPLETED: 'primary' }[status]
}

async function setStatus(row, status) {
  const label = status === 'CONFIRMED' ? '确认' : '取消'
  try {
    await ElMessageBox.confirm(`确定要${label}预约 #${row.id} 吗？${status === 'CANCELED' ? '取消将退还积分并释放名额。' : ''}`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await updateAppointmentStatus(row.id, status)
    ElMessage.success(`${label}成功`)
    loadAppointments()
  } catch {
    /* 拦截器已提示 */
  }
}

function downloadReport(row) {
  window.open(row.reportDownloadUrl, '_blank')
}

function openUpload(row) {
  uploadRow.value = row
  uploadFile.value = null
  uploadVisible.value = true
}

function onFileChange(file) {
  if (file.raw && file.raw.type !== 'application/pdf') {
    ElMessage.warning('仅支持 PDF 文件')
    uploadFile.value = null
    return
  }
  uploadFile.value = file.raw
}

async function submitUpload() {
  uploadSubmitting.value = true
  try {
    await uploadReport(uploadRow.value.id, uploadFile.value)
    ElMessage.success('报告上传成功，预约已完成')
    uploadVisible.value = false
    loadAppointments()
  } finally {
    uploadSubmitting.value = false
  }
}

onMounted(() => {
  loadPackages()
  loadAppointments()
})
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
.filter-card {
  margin-bottom: 14px;
}
.item-tag {
  margin: 2px 4px 2px 0;
}
.time-wrap {
  width: 100%;
}
.time-row {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
.upload-alert {
  margin-bottom: 14px;
}
</style>
