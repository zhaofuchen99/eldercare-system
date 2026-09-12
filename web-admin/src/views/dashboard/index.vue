<template>
  <div class="dashboard-page">
    <!-- 页头：问候 + 数据更新时间 -->
    <div class="page-head">
      <div class="page-head-left">
        <div class="page-head-title">{{ greeting }}，{{ userName }}</div>
        <div class="page-head-sub">{{ todayText }}，欢迎使用养老社区运营工作台</div>
      </div>
      <div class="page-head-right">
        <span v-if="updatedAt" class="data-time">数据更新于 {{ updatedAt }}</span>
        <el-tooltip content="刷新数据" placement="top">
          <el-button :icon="Refresh" circle size="small" :loading="loading" @click="loadData" />
        </el-tooltip>
      </div>
    </div>

    <!-- 核心指标（真实接口数据） -->
    <el-row :gutter="16" class="stat-row">
      <el-col v-for="card in statCards" :key="card.title" :xs="12" :md="6" class="stat-col">
        <StatisticCard v-bind="card" />
      </el-col>
    </el-row>

    <!-- 健康数据分析 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :md="16" class="row-col">
        <el-card shadow="never" class="fill-card">
          <template #header>
            <ChartHeader title="血压异常趋势" desc="近 7 天异常记录（人次）" mock />
          </template>
          <HealthChart :option="lineOption" height="272px" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8" class="row-col">
        <el-card shadow="never" class="fill-card">
          <template #header>
            <ChartHeader title="老人健康状态分布" desc="基于最近一次健康巡检" mock />
          </template>
          <div class="dist-list">
            <div v-for="d in healthDistribution" :key="d.name" class="dist-item">
              <div class="dist-line">
                <span class="dist-name">{{ d.name }}</span>
                <span class="dist-count">{{ d.value }} 人</span>
              </div>
              <div class="dist-bar">
                <i :style="{ width: distPct(d.value) + '%', background: distColor(d.name) }"></i>
              </div>
            </div>
          </div>
          <div class="completion">
            <div class="dist-line">
              <span class="dist-name">本月体检完成率</span>
              <span class="dist-count">{{ checkupCompletion }}%</span>
            </div>
            <el-progress :percentage="checkupCompletion" :show-text="false" :stroke-width="8" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 待办事项 + 最近动态 -->
    <el-row :gutter="16" class="bottom-row">
      <el-col :xs="24" :md="10" class="row-col">
        <el-card shadow="never" class="fill-card">
          <template #header>
            <ChartHeader title="待办事项" desc="需要您尽快处理" />
          </template>
          <TodoList :items="todoItems" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="14" class="row-col">
        <el-card shadow="never" class="fill-card">
          <template #header>
            <ChartHeader title="最近动态" desc="社区实时动态" mock />
          </template>
          <RecentActivity :items="recentActivities" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, h } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getDashboard } from '../../api/dashboard'
import { useAuthStore } from '../../store/auth'
import StatisticCard from '../../components/dashboard/StatisticCard.vue'
import HealthChart from '../../components/dashboard/HealthChart.vue'
import TodoList from '../../components/dashboard/TodoList.vue'
import RecentActivity from '../../components/dashboard/RecentActivity.vue'
import {
  healthDistribution,
  bpTrend,
  checkupCompletion,
  healthAlertCount,
  unreadMessageCount,
  recentActivities
} from './mock'

const auth = useAuthStore()
const data = ref({})
const loading = ref(false)
const updatedAt = ref('')

async function loadData() {
  loading.value = true
  try {
    data.value = await getDashboard()
    const d = new Date()
    const pad = (n) => String(n).padStart(2, '0')
    updatedAt.value = `${pad(d.getHours())}:${pad(d.getMinutes())}`
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

// ===== 页头：问候与日期 =====
const userName = computed(() => auth.userInfo?.realName || auth.userInfo?.phone || '管理员')
const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '凌晨好'
  if (h < 9) return '早上好'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})
const WEEKS = ['日', '一', '二', '三', '四', '五', '六']
const todayText = computed(() => {
  const d = new Date()
  return `${d.getFullYear()} 年 ${d.getMonth() + 1} 月 ${d.getDate()} 日 星期${WEEKS[d.getDay()]}`
})

// ===== 统计卡片（真实接口数据，无虚构环比）=====
const statCards = computed(() => [
  {
    title: '会员总数',
    value: data.value.totalMembers ?? 0,
    icon: 'User',
    description: '社区注册会员总数',
    to: '/member'
  },
  {
    title: '今日新增会员',
    value: data.value.todayNewMembers ?? 0,
    icon: 'UserFilled',
    description: '今日完成注册',
    to: '/member'
  },
  {
    title: '今日预约',
    value: data.value.todayAppointments ?? 0,
    icon: 'Calendar',
    description: '今日体检预约单量',
    to: '/appointment'
  },
  {
    title: '待处理预约',
    value: data.value.pendingAppointments ?? 0,
    icon: 'Bell',
    description: '待确认的预约申请',
    to: '/appointment'
  }
])

// ===== 待办事项（预约数为真实数据，其余待接后端）=====
const todoItems = computed(() => [
  {
    title: '待审核预约',
    description: '会员提交的体检预约等待确认',
    icon: 'Calendar',
    color: '#2c5aa0',
    colorBg: '#eef3fa',
    count: data.value.pendingAppointments ?? 0,
    route: '/appointment'
  },
  {
    title: '健康异常提醒',
    description: '巡检发现指标异常，需人工复核',
    icon: 'Warning',
    color: '#e6a23c',
    colorBg: '#fbf3e4',
    count: healthAlertCount,
    route: '/health',
    mock: true
  },
  {
    title: '未处理消息',
    description: '会员留言与系统通知待回复',
    icon: 'ChatDotRound',
    color: '#8a919c',
    colorBg: '#f2f3f5',
    count: unreadMessageCount,
    route: '/message',
    mock: true
  }
])

// ===== 健康分布条形图（示例数据，替代饼图）=====
const DIST_COLORS = {
  健康良好: 'var(--ec-green)',
  血压偏高: 'var(--ec-warning)',
  血糖偏高: 'var(--ec-warning)',
  心率异常: 'var(--ec-danger)',
  重点关注: 'var(--ec-danger)'
}
const distMax = computed(() => Math.max(...healthDistribution.map((d) => d.value), 1))
const distPct = (v) => Math.round((v / distMax.value) * 100)
const distColor = (name) => DIST_COLORS[name] || 'var(--ec-primary)'

// ===== 卡片标题（标题 + 描述 + 示例数据标识）=====
const ChartHeader = {
  props: { title: String, desc: String, mock: Boolean },
  setup(props) {
    return () =>
      h('div', { class: 'chart-header' }, [
        h('div', { class: 'chart-header-left' }, [
          h('span', { class: 'chart-title' }, props.title),
          h('span', { class: 'chart-desc' }, props.desc)
        ]),
        props.mock
          ? h(
              'span',
              { class: 'chart-mock' },
              '示例数据'
            )
          : null
      ])
  }
}

// ===== 折线图配置（克制配色）=====
const lineOption = computed(() => ({
  color: ['#2c5aa0', '#9db4d3'],
  tooltip: { trigger: 'axis' },
  legend: {
    bottom: 0,
    icon: 'circle',
    itemWidth: 8,
    itemHeight: 8,
    textStyle: { color: '#8a919c', fontSize: 12 }
  },
  grid: { left: 8, right: 12, top: 24, bottom: 36, containLabel: true },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: bpTrend.days,
    axisLine: { lineStyle: { color: '#e5e8ee' } },
    axisTick: { show: false },
    axisLabel: { color: '#8a919c', fontSize: 12 }
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: '#f0f2f5' } },
    axisLabel: { color: '#8a919c', fontSize: 12 }
  },
  series: [
    {
      name: '收缩压异常',
      type: 'line',
      smooth: false,
      symbolSize: 5,
      data: bpTrend.systolic
    },
    {
      name: '舒张压异常',
      type: 'line',
      smooth: false,
      symbolSize: 5,
      data: bpTrend.diastolic
    }
  ]
}))
</script>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ===== 页头 ===== */
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 2px 2px 0;
}
.page-head-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--ec-text-primary);
}
.page-head-sub {
  margin-top: 4px;
  font-size: 13px;
  color: var(--ec-text-secondary);
}
.page-head-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.data-time {
  font-size: 12px;
  color: var(--ec-text-secondary);
  font-variant-numeric: tabular-nums;
}

/* ===== 统计卡片行 ===== */
.stat-row .stat-col {
  margin-bottom: 0;
}

/* ===== 图表区 ===== */
.row-col {
  display: flex;
}
.fill-card {
  width: 100%;
}
.chart-row :deep(.el-card__header),
.bottom-row :deep(.el-card__header) {
  padding: 14px 20px 12px;
  border-bottom: 1px solid var(--ec-border-light);
}
.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.chart-header-left {
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.chart-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--ec-text-primary);
}
.chart-desc {
  font-size: 12px;
  color: var(--ec-text-secondary);
}
.chart-mock {
  flex: none;
  font-size: 11px;
  color: var(--ec-text-placeholder);
  border: 1px solid var(--ec-border);
  border-radius: var(--ec-radius-sm);
  padding: 1px 6px;
}

/* ===== 健康分布条形列表 ===== */
.dist-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 6px 0 2px;
}
.dist-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.dist-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
}
.dist-name {
  color: var(--ec-text-regular);
}
.dist-count {
  color: var(--ec-text-primary);
  font-variant-numeric: tabular-nums;
}
.dist-bar {
  height: 6px;
  border-radius: 3px;
  background: var(--ec-border-light);
  overflow: hidden;
}
.dist-bar i {
  display: block;
  height: 100%;
  border-radius: 3px;
}
.completion {
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px dashed var(--ec-border);
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>
