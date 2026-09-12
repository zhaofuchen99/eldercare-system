<template>
  <div class="appt-mine-page">
    <van-nav-bar title="我的预约" left-arrow @click-left="$router.back()" fixed placeholder safe-area-inset-top />

    <div class="page-content">
      <van-tabs v-model="tab">
        <van-tab title="全部" name="all" />
        <van-tab title="待确认" name="PENDING" />
        <van-tab title="已完成" name="COMPLETED" />
      </van-tabs>

      <div class="appt-list">
        <van-empty v-if="!filtered.length && !loading" description="暂无预约" image-size="80" />
        <div v-for="a in filtered" :key="a.id" class="appt-card">
          <div class="appt-main">
            <div class="appt-name">{{ a.packageName }}</div>
            <div class="appt-meta">
              <van-icon name="calendar-o" class="meta-icon" /> {{ a.appointDate }} {{ a.timeRange }}
            </div>
            <div class="appt-meta">积分消费：{{ a.price }}</div>
          </div>
          <div class="appt-right">
            <van-tag :type="statusType(a.status)">{{ enumText('appointmentStatus', a.status) }}</van-tag>
            <van-button
              v-if="a.status === 'PENDING' || a.status === 'CONFIRMED'"
              size="mini"
              type="danger"
              plain
              class="cancel-btn"
              @click="cancel(a)"
            >
              取消
            </van-button>
            <van-button
              v-if="a.reportDownloadUrl"
              size="mini"
              type="primary"
              plain
              class="cancel-btn"
              @click="download(a)"
            >
              下载报告
            </van-button>
          </div>
        </div>
        <div v-if="allTotal > allList.length" class="load-more" @click="loadMore">加载更多</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { showConfirmDialog, showSuccessToast } from 'vant'
import { getMyAppointments, cancelAppointment } from '../../api/appointment'
import { getProfile } from '../../api/auth'
import { useAuthStore } from '../../store/auth'
import { enumText } from '../../utils/format'

const auth = useAuthStore()
const tab = ref('all')
const loading = ref(false)
const allList = ref([])
const allTotal = ref(0)
const allQuery = reactive({ page: 1, size: 10 })

// 本地按状态过滤（后端 5.5 接口不支持 status 参数）
const filtered = computed(() => {
  if (tab.value === 'PENDING') return allList.value.filter((a) => a.status === 'PENDING')
  if (tab.value === 'COMPLETED') return allList.value.filter((a) => a.status === 'COMPLETED')
  return allList.value
})

function statusType(s) {
  return { PENDING: 'warning', CONFIRMED: 'primary', CANCELED: 'default', COMPLETED: 'success' }[s] || 'default'
}

async function loadAll(reset = false) {
  if (reset) allQuery.page = 1
  loading.value = true
  try {
    const data = await getMyAppointments({ page: allQuery.page, size: allQuery.size })
    if (allQuery.page === 1) allList.value = data.list || []
    else allList.value = allList.value.concat(data.list || [])
    allTotal.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function loadMore() {
  allQuery.page += 1
  loadAll()
}

async function refreshPoints() {
  try {
    const profile = await getProfile()
    auth.setUserInfo(profile)
  } catch {
    /* 忽略 */
  }
}

async function cancel(a) {
  try {
    await showConfirmDialog({
      title: '取消预约',
      message: `确定取消「${a.packageName}」的预约吗？取消后将退还 ${a.price} 积分`
    })
  } catch {
    return
  }
  try {
    await cancelAppointment(a.id)
    showSuccessToast('已取消，积分已退还')
    await refreshPoints()
    loadAll(true)
  } catch {
    /* 拦截器已提示 */
  }
}

function download(a) {
  window.open(a.reportDownloadUrl, '_blank')
}

onMounted(() => {
  loadAll()
})
</script>

<style scoped>
.appt-mine-page {
  background: var(--page-bg);
  min-height: 100vh;
}
.appt-list {
  padding-top: 12px;
}
.appt-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--card-bg);
  border-radius: var(--radius-card);
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: var(--card-shadow);
}
.appt-main {
  flex: 1;
  min-width: 0;
  margin-right: 12px;
}
.appt-name {
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
}
.appt-meta {
  margin-top: 6px;
  font-size: 14px;
  color: var(--text-sub);
  display: flex;
  align-items: center;
  gap: 5px;
}
.appt-meta .meta-icon {
  color: var(--brand-deep);
}
.appt-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}
/* 取消/下载按钮加大热区 */
.cancel-btn {
  margin-top: 2px;
  height: 34px !important;
  padding: 0 14px !important;
  font-size: 14px !important;
}
</style>
