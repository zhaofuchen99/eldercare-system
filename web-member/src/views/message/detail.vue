<template>
  <div class="msg-detail-page">
    <van-nav-bar title="消息详情" left-arrow @click-left="$router.back()" fixed placeholder safe-area-inset-top />

    <div class="page-content" v-if="msg">
      <div class="detail-card">
        <div class="detail-head">
          <van-tag round :color="typeColor(msg.type)">{{ enumText('messageType', msg.type) }}</van-tag>
        </div>
        <div class="detail-title">{{ msg.title }}</div>
        <div class="detail-time">{{ formatDateTime(msg.createTime) }}</div>
        <div class="detail-content">{{ msg.content || '（无正文）' }}</div>
      </div>
    </div>
    <van-empty v-else-if="!loading" description="消息不存在" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getMessageDetail, readMessage } from '../../api/message'
import { formatDateTime, enumText } from '../../utils/format'

const route = useRoute()
const loading = ref(false)
const msg = ref(null)

function typeColor(t) {
  return {
    APPOINTMENT: '#41769e',
    ACTIVITY: '#c1731f',
    SYSTEM: '#2e7d5f',
    HEALTH_REMINDER: '#a86d12'
  }[t] || '#7d8187'
}

onMounted(async () => {
  loading.value = true
  try {
    msg.value = await getMessageDetail(route.params.id)
    // 标记已读（幂等）
    if (msg.value.isRead === 0) {
      try {
        await readMessage(msg.value.id)
        msg.value.isRead = 1
      } catch {
        /* 忽略 */
      }
    }
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.msg-detail-page {
  background: var(--page-bg);
  min-height: 100vh;
}
.detail-card {
  background: var(--card-bg);
  border-radius: var(--radius-card);
  padding: 20px 18px;
  box-shadow: var(--card-shadow);
}
.detail-title {
  margin-top: 14px;
  font-size: 19px;
  font-weight: 700;
  line-height: 1.5;
}
.detail-time {
  margin-top: 8px;
  font-size: 14px;
  color: var(--text-dim);
}
.detail-content {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--line-color);
  font-size: 16px;
  line-height: 1.9;
  color: var(--text-main);
  white-space: pre-wrap;
}
</style>
