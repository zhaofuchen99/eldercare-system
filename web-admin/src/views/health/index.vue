<template>
  <div class="health-page">
    <!-- 选择会员 -->
    <el-card shadow="never" class="filter-card">
      <el-form inline @submit.prevent>
        <el-form-item label="选择会员">
          <el-select
            v-model="selectedUserId"
            filterable
            remote
            clearable
            reserve-keyword
            placeholder="输入姓名 / 手机号搜索会员"
            :remote-method="searchMember"
            :loading="memberLoading"
            style="width: 260px"
            @change="onSelectMember"
          >
            <el-option
              v-for="m in memberOptions"
              :key="m.id"
              :label="`${m.realName || m.phone}（${m.phone}）`"
              :value="m.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :disabled="!selectedUserId" @click="loadAll">查询</el-button>
          <el-button @click="handleClear">清除</el-button>
        </el-form-item>
        <el-form-item v-if="selectedMember">
          <span class="tip">
            当前会员：{{ selectedMember.realName || '-' }}（{{ selectedMember.phone }}），身高
            {{ selectedMember.height ? selectedMember.height + ' cm' : '-' }}，BMI 按录入体重自动计算
          </span>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 健康趋势（近 6 个月） -->
    <el-card v-if="selectedUserId && trendEntries.length" shadow="never" class="trend-card">
      <template #header>健康趋势（近 6 个月）</template>
      <el-table :data="trendEntries" stripe size="small">
        <el-table-column label="指标" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ row.indicatorText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="月份" min-width="80">
          <template #default="{ row }">{{ row.month }}</template>
        </el-table-column>
        <el-table-column label="均值" min-width="90">
          <template #default="{ row }">
            <span :class="cellClass(row.indicator, row.avg)">{{ row.avg ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="最大值" min-width="90">
          <template #default="{ row }">
            <span :class="cellClass(row.indicator, row.max)">{{ row.max ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="最小值" min-width="90">
          <template #default="{ row }">
            <span :class="cellClass(row.indicator, row.min)">{{ row.min ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="参考范围" min-width="160">
          <template #default="{ row }">{{ INDICATOR_RANGES[row.indicator]?.join(' ~ ') || '-' }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 健康记录列表 -->
    <el-card shadow="never" v-if="selectedUserId">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="recordedTime" label="记录时间" width="165">
          <template #default="{ row }">{{ formatDateTime(row.recordedTime) }}</template>
        </el-table-column>
        <el-table-column label="收缩压(mmHg)" width="120">
          <template #default="{ row }">
            <span :class="cellClass('SYSTOLIC', row.systolic)">{{ row.systolic ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="舒张压(mmHg)" width="120">
          <template #default="{ row }">
            <span :class="cellClass('DIASTOLIC', row.diastolic)">{{ row.diastolic ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="空腹血糖" width="100">
          <template #default="{ row }">
            <span :class="cellClass('BLOOD_SUGAR', row.bloodSugar)">{{ row.bloodSugar ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="心率" width="90">
          <template #default="{ row }">
            <span :class="cellClass('HEART_RATE', row.heartRate)">{{ row.heartRate ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="weight" label="体重(kg)" width="100" />
        <el-table-column label="BMI" width="90">
          <template #default="{ row }">
            <span :class="cellClass('BMI', row.bmi)">{{ row.bmi ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="memo" label="备注" show-overflow-tooltip />
      </el-table>
      <el-pagination
        class="pager"
        background
        layout="total, prev, pager, next, sizes"
        :total="total"
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :page-sizes="[10, 20, 50]"
        @current-change="loadRecords"
        @size-change="handleSizeChange"
      />
    </el-card>

    <el-card v-else shadow="never">
      <el-empty description="请先选择会员查看其健康档案" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getMemberPage } from '../../api/member'
import { getHealthRecordPage, getHealthTrend } from '../../api/health'
import { formatDateTime } from '../../utils/format'

const INDICATOR_RANGES = {
  SYSTOLIC: [90, 139],
  DIASTOLIC: [60, 89],
  BLOOD_SUGAR: [3.9, 6.1],
  HEART_RATE: [60, 100],
  BMI: [18.5, 23.9]
}
const INDICATOR_TEXT = {
  SYSTOLIC: '收缩压',
  DIASTOLIC: '舒张压',
  BLOOD_SUGAR: '空腹血糖',
  HEART_RATE: '心率',
  BMI: 'BMI'
}

/** 指标值超出参考范围时红色标识 */
function isAbnormal(indicator, v) {
  const range = INDICATOR_RANGES[indicator]
  if (!range || v === null || v === undefined || v === '') return false
  return v < range[0] || v > range[1]
}
function cellClass(indicator, v) {
  return isAbnormal(indicator, v) ? 'cell-abnormal' : ''
}

const memberLoading = ref(false)
const memberOptions = ref([])
const selectedUserId = ref(null)
const selectedMember = ref(null)

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10 })

const trendEntries = ref([])

async function searchMember(keyword) {
  memberLoading.value = true
  try {
    const data = await getMemberPage({ keyword, page: 1, size: 20 })
    memberOptions.value = data.list || []
  } finally {
    memberLoading.value = false
  }
}

function onSelectMember(id) {
  if (!id) {
    handleClear()
    return
  }
  selectedMember.value = memberOptions.value.find((m) => m.id === id) || null
  if (selectedUserId.value) loadAll()
}

function handleClear() {
  selectedUserId.value = null
  selectedMember.value = null
  list.value = []
  total.value = 0
  trendEntries.value = []
  memberOptions.value = []
}

async function loadAll() {
  query.page = 1
  loadRecords()
  loadTrend()
}

async function loadRecords() {
  if (!selectedUserId.value) return
  loading.value = true
  try {
    const data = await getHealthRecordPage({
      userId: selectedUserId.value,
      page: query.page,
      size: query.size
    })
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function handleSizeChange() {
  query.page = 1
  loadRecords()
}

async function loadTrend() {
  if (!selectedUserId.value) return
  try {
    const d = await getHealthTrend({ userId: selectedUserId.value })
    const map = (d && d.data) || {}
    trendEntries.value = Object.keys(map)
      .flatMap((indicator) =>
        (map[indicator] || []).map((p) => ({
          indicator,
          indicatorText: INDICATOR_TEXT[indicator] || indicator,
          ...p
        }))
      )
  } catch {
    trendEntries.value = []
  }
}

onMounted(() => {
  // 默认加载部分会员供选择
  searchMember('')
})
</script>

<style scoped>
.filter-card {
  margin-bottom: 14px;
}
.trend-card {
  margin-bottom: 14px;
}
.tip {
  color: #909399;
  font-size: 12px;
}
/* 超出参考范围的指标值 */
.cell-abnormal {
  color: var(--el-color-danger);
  font-weight: 600;
}
.pager {
  margin-top: 14px;
  justify-content: flex-end;
}
</style>
