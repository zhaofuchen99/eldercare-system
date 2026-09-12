<template>
  <div class="home-page">
    <!-- 顶部问候：简单、清楚，不做大 Banner -->
    <div class="home-head">
      <div class="head-text">
        <div class="greet-line">{{ greeting }}，{{ displayName }}</div>
        <div class="date-line">{{ todayText }} · {{ levelText }}会员</div>
      </div>
      <div class="head-msg" @click="goMessages">
        <van-icon name="bell" />
        <van-badge v-if="unread > 0" :content="unread > 99 ? '99+' : unread" class="msg-badge" />
      </div>
    </div>

    <div class="page-content">
      <!-- 今日健康：最近一次记录的直观展示 -->
      <div class="card section" @click="go('/health')">
        <div class="sec-head">
          <span class="sec-title">今日健康</span>
          <span class="sec-more">健康档案 ›</span>
        </div>
        <template v-if="metrics.length">
          <div class="metric-row" v-for="m in metrics" :key="m.label">
            <span class="metric-label">{{ m.label }}</span>
            <span class="metric-value">{{ m.value }}<em class="metric-unit">{{ m.unit }}</em></span>
            <span class="metric-status" :class="m.level">{{ m.status }}</span>
          </div>
          <div class="record-time">记录于 {{ formatDate(lastRecord.recordedTime) }}</div>
          <div v-if="tipText" class="health-tip">{{ tipText }}</div>
        </template>
        <div v-else class="metric-empty">
          <span>还没有健康记录，先录入一条吧</span>
          <van-button size="small" type="primary" round>去录入</van-button>
        </div>
      </div>

      <!-- 健康咨询入口：可靠的咨询渠道，不做科技化装饰 -->
      <div class="card section ai-entry">
        <div class="ai-icon"><van-icon name="chat-o" /></div>
        <div class="ai-text">
          <div class="ai-title">健康咨询</div>
          <div class="ai-desc">有健康问题，可以随时问我</div>
        </div>
        <van-button type="primary" round size="small" class="ai-btn" @click="go('/chat')">开始咨询</van-button>
      </div>

      <!-- 常用功能：大图标 + 大文字，一眼看懂 -->
      <div class="grid-title">常用功能</div>
      <div class="func-grid">
        <div v-for="f in funcs" :key="f.text" class="func-item" @click="go(f.path)">
          <div class="func-icon" :style="{ background: f.bg, color: f.color }">
            <van-icon :name="f.icon" />
          </div>
          <div class="func-text">{{ f.text }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getProfile } from '../../api/auth'
import { getUnreadCount } from '../../api/message'
import { getHealthHistory } from '../../api/health'
import { useAuthStore } from '../../store/auth'
import { formatDate, enumText, healthMetrics, healthTip } from '../../utils/format'

const router = useRouter()
const auth = useAuthStore()

const unread = ref(0)
const lastRecord = ref(null)

const displayName = computed(() => auth.realName || auth.phone)
const levelText = computed(() => enumText('memberLevel', auth.memberLevel))
const metrics = computed(() => healthMetrics(lastRecord.value))
const tipText = computed(() => healthTip(lastRecord.value))

// 按时段问候
const greeting = computed(() => {
  const h = new Date().getHours()
  if (h >= 5 && h < 11) return '上午好'
  if (h >= 11 && h < 13) return '中午好'
  if (h >= 13 && h < 18) return '下午好'
  if (h >= 18 && h < 23) return '晚上好'
  return '您好'
})

const todayText = computed(() => {
  const d = new Date()
  const week = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()]
  return `${d.getMonth() + 1}月${d.getDate()}日 周${week}`
})

// 常用功能：柔和底色区分，图标大、文字大
const funcs = [
  { icon: 'chat-o', text: '健康咨询', path: '/chat', bg: 'var(--brand-bg)', color: 'var(--brand-deep)' },
  { icon: 'bar-chart-o', text: '健康档案', path: '/health', bg: '#eaf1f7', color: '#3d6f96' },
  { icon: 'shop-o', text: '体检预约', path: '/appointment', bg: 'var(--warm-bg)', color: 'var(--warm-deep)' },
  { icon: 'todo-list-o', text: '健康评测', path: '/assessment', bg: '#eaf1f7', color: '#3d6f96' },
  { icon: 'flag-o', text: '社区活动', path: '/activity', bg: 'var(--warm-bg)', color: 'var(--warm-deep)' },
  { icon: 'volume-o', text: '消息中心', path: '/message', bg: 'var(--brand-bg)', color: 'var(--brand-deep)' }
]

function go(path) {
  router.push(path)
}

function goMessages() {
  router.push('/message')
}

onMounted(async () => {
  try {
    // 刷新最新资料与积分
    const profile = await getProfile()
    auth.setUserInfo(profile)
  } catch {
    /* 忽略 */
  }
  try {
    const d = await getUnreadCount()
    unread.value = d.unread || 0
  } catch {
    /* 忽略 */
  }
  try {
    const data = await getHealthHistory({ page: 1, size: 1 })
    lastRecord.value = data.list?.[0] || null
  } catch {
    /* 忽略 */
  }
})
</script>

<style scoped>
.home-page {
  background: var(--page-bg);
  min-height: 100vh;
}
/* 顶部问候 */
.home-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 16px 6px;
}
.greet-line {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-main);
  line-height: 1.3;
}
.date-line {
  margin-top: 8px;
  font-size: 15px;
  color: var(--text-sub);
}
/* 消息入口：圆形大热区 + 未读角标 */
.head-msg {
  position: relative;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: var(--card-bg);
  box-shadow: var(--card-shadow);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: var(--text-sub);
  flex-shrink: 0;
  cursor: pointer;
}
.head-msg:active {
  background: var(--brand-bg);
}
.msg-badge {
  position: absolute;
  top: 1px;
  right: -2px;
}
/* 今日健康 / 咨询入口 区块 */
.home-page .section {
  margin-bottom: 14px;
}
.metric-empty {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 10px 0 4px;
  font-size: 15px;
  color: var(--text-sub);
}
/* 健康咨询入口 */
.ai-entry {
  display: flex;
  align-items: center;
  gap: 12px;
}
.ai-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: var(--brand-bg);
  color: var(--brand-deep);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}
.ai-text {
  flex: 1;
  min-width: 0;
}
.ai-title {
  font-size: 17px;
  font-weight: 600;
}
.ai-desc {
  margin-top: 4px;
  font-size: 14px;
  color: var(--text-sub);
}
.ai-btn {
  height: 40px;
  min-width: 96px;
  font-size: 15px;
  font-weight: 600;
  flex-shrink: 0;
}
/* 常用功能宫格 */
.grid-title {
  font-size: 18px;
  font-weight: 700;
  margin: 18px 2px 12px;
}
.func-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
.func-item {
  background: var(--card-bg);
  border-radius: var(--radius-card);
  box-shadow: var(--card-shadow);
  padding: 18px 6px 14px;
  text-align: center;
  cursor: pointer;
}
.func-item:active {
  background: #f5f3ee;
}
.func-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
}
.func-text {
  margin-top: 10px;
  font-size: 16px;
  color: var(--text-main);
}
</style>
