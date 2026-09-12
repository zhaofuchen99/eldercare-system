<template>
  <div class="health-page">
    <!-- 健康概览：最近一次指标的直观解读 -->
    <div class="card section overview">
      <div class="sec-head">
        <span class="sec-title">健康概览</span>
        <span v-if="latest" class="sec-more">{{ formatDateTime(latest.recordedTime) }}</span>
      </div>
      <template v-if="metrics.length">
        <div class="metric-row" v-for="m in metrics" :key="m.label">
          <span class="metric-label">{{ m.label }}</span>
          <span class="metric-value">{{ m.value }}<em class="metric-unit">{{ m.unit }}</em></span>
          <span class="metric-status" :class="m.level">{{ m.status }}</span>
        </div>
        <div v-if="tipText" class="health-tip">{{ tipText }}</div>
      </template>
      <div v-else-if="!loading" class="overview-empty">
        还没有健康记录，在下方填写数据后，这里会展示您最新的健康状况
      </div>
    </div>

    <!-- 录入健康数据 -->
    <van-form @submit="onSubmit">
      <van-cell-group inset title="录入健康数据">
        <van-field
          v-model="form.systolic"
          type="number"
          label="收缩压"
          placeholder="30-250 mmHg"
          :rules="[{ validator: optionalNum, message: '收缩压需为 30-250 的数值', range: [30, 250] }]"
        />
        <van-field
          v-model="form.diastolic"
          type="number"
          label="舒张压"
          placeholder="30-150 mmHg"
          :rules="[{ validator: optionalNum, message: '舒张压需为 30-150 的数值', range: [30, 150] }]"
        />
        <van-field
          v-model="form.bloodSugar"
          type="number"
          label="空腹血糖"
          placeholder="1.0-30.0 mmol/L"
          :rules="[{ validator: optionalNum, message: '空腹血糖需为 1-30 的数值', range: [1, 30] }]"
        />
        <van-field
          v-model="form.heartRate"
          type="number"
          label="心率"
          placeholder="20-250 次/分"
          :rules="[{ validator: optionalNum, message: '心率需为 20-250 的数值', range: [20, 250] }]"
        />
        <van-field
          v-model="form.weight"
          type="number"
          label="体重"
          placeholder="10.0-300.0 kg"
          :rules="[{ validator: optionalNum, message: '体重需为 10-300 的数值', range: [10, 300] }]"
        />
        <van-field
          v-model="form.memo"
          label="备注"
          placeholder="选填，如：晨起测量"
          maxlength="500"
        />
      </van-cell-group>
      <div class="record-actions">
        <van-button round block type="primary" native-type="submit" :loading="submitting">保存记录</van-button>
      </div>
    </van-form>

    <!-- 健康服务：体检与评测入口 -->
    <van-cell-group inset title="健康服务">
      <van-cell icon="todo-list-o" title="健康评测" label="回答问卷，生成健康评分与建议" is-link @click="go('/assessment')" />
      <van-cell icon="shop-o" title="体检预约" label="选择体检套餐，预约合适时段" is-link @click="go('/appointment')" />
      <van-cell icon="calendar-o" title="我的体检记录" label="查看预约情况，下载体检报告" is-link @click="go('/appointment/mine')" />
    </van-cell-group>

    <!-- 历史记录 -->
    <van-cell-group inset title="历史记录">
      <van-cell
        v-for="r in list"
        :key="r.id"
        :title="`${r.systolic ?? '-'}/${r.diastolic ?? '-'} mmHg · ${r.bloodSugar ?? '-'} 血糖`"
        :label="`心率 ${r.heartRate ?? '-'} · 体重 ${r.weight ?? '-'}kg · BMI ${r.bmi ?? '-'} · ${r.memo || ''}`"
      >
        <template #value>
          <span class="history-time">{{ formatDateTime(r.recordedTime) }}</span>
        </template>
      </van-cell>
      <van-empty v-if="!list.length && !loading" description="暂无健康记录" image-size="80" />
      <div v-if="total > list.length" class="load-more" @click="loadMore">加载更多</div>
    </van-cell-group>

    <!-- 近 6 月趋势 -->
    <van-cell-group inset title="近 6 月趋势" v-if="trendEntries.length">
      <van-collapse v-model="activeNames">
        <van-collapse-item v-for="t in trendEntries" :key="t.indicator" :title="t.indicatorText" :name="t.indicator">
          <div class="trend-row" v-for="p in t.points" :key="p.month">
            <span>{{ p.month }}</span>
            <span>均值 {{ p.avg }}</span>
            <span>最高 {{ p.max }}</span>
            <span>最低 {{ p.min }}</span>
          </div>
        </van-collapse-item>
      </van-collapse>
    </van-cell-group>

    <div class="empty-pad" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast, showFailToast } from 'vant'
import { recordHealth, getHealthHistory, getHealthTrend } from '../../api/health'
import { formatDateTime, healthMetrics, healthTip } from '../../utils/format'

const router = useRouter()

const INDICATOR_TEXT = {
  SYSTOLIC: '收缩压', DIASTOLIC: '舒张压', BLOOD_SUGAR: '空腹血糖', HEART_RATE: '心率', BMI: 'BMI'
}

const submitting = ref(false)
const form = reactive({ systolic: '', diastolic: '', bloodSugar: '', heartRate: '', weight: '', memo: '' })

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10 })

const activeNames = ref([])
const trendEntries = ref([])

// 概览：取最近一条记录的展示解读
const latest = computed(() => list.value[0] || null)
const metrics = computed(() => healthMetrics(latest.value))
const tipText = computed(() => healthTip(latest.value))

function go(path) {
  router.push(path)
}

// 可选数字校验：为空则通过，非空需在范围内
function optionalNum(val, rule) {
  if (val === '' || val === null || val === undefined) return true
  const n = Number(val)
  const [min, max] = rule && rule.range
  if (min === undefined || max === undefined) return !Number.isNaN(n)
  return !Number.isNaN(n) && n >= min && n <= max
}

async function onSubmit() {
  // 至少填一项
  const fields = [form.systolic, form.diastolic, form.bloodSugar, form.heartRate, form.weight]
  if (!fields.some((f) => f !== '' && f !== null && f !== undefined)) {
    return showFailToast('请至少填写一项健康指标')
  }
  submitting.value = true
  try {
    await recordHealth(form)
    showSuccessToast('保存成功')
    Object.assign(form, { systolic: '', diastolic: '', bloodSugar: '', heartRate: '', weight: '', memo: '' })
    query.page = 1
    loadHistory()
  } finally {
    submitting.value = false
  }
}

async function loadHistory() {
  loading.value = true
  try {
    const data = await getHealthHistory({ page: query.page, size: query.size })
    if (query.page === 1) {
      list.value = data.list || []
    } else {
      list.value = list.value.concat(data.list || [])
    }
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function loadMore() {
  query.page += 1
  loadHistory()
}

async function loadTrend() {
  try {
    const d = await getHealthTrend()
    const map = (d && d.data) || {}
    trendEntries.value = Object.keys(map).map((indicator) => ({
      indicator,
      indicatorText: INDICATOR_TEXT[indicator] || indicator,
      points: map[indicator] || []
    }))
  } catch {
    trendEntries.value = []
  }
}

onMounted(() => {
  loadHistory()
  loadTrend()
})
</script>

<style scoped>
.health-page {
  background: var(--page-bg);
  min-height: 100vh;
  padding-bottom: 20px;
}
/* 健康概览卡片 */
.overview {
  margin: 14px;
}
.overview-empty {
  padding: 8px 0 4px;
  font-size: 15px;
  color: var(--text-sub);
  line-height: 1.7;
}
.health-page :deep(.van-cell-group--inset) {
  box-shadow: var(--card-shadow);
}
.record-actions {
  margin: 6px 20px 16px;
}
.record-actions :deep(.van-button) {
  height: 50px;
  font-size: 17px;
  font-weight: 600;
}
.history-time {
  color: var(--text-dim);
  font-size: 14px;
}
/* 趋势数据行：分列清晰 */
.trend-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 11px 16px;
  font-size: 15px;
  color: var(--text-sub);
  background: var(--brand-bg);
  border-radius: 8px;
  margin: 6px 12px;
}
.trend-row span:first-child {
  font-weight: 600;
  color: var(--text-main);
}
</style>
