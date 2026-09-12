<template>
  <div class="msg-list-page">
    <van-nav-bar title="消息中心" left-arrow @click-left="$router.back()" fixed placeholder safe-area-inset-top />

    <div class="page-content">
      <van-empty v-if="!list.length && !loading" description="暂无消息" image-size="80" />
      <div v-for="m in list" :key="m.id" class="msg-card" :class="{ unread: m.isRead === 0 }" @click="goDetail(m)">
        <div class="msg-left">
          <div class="msg-head">
            <van-tag size="small" round :color="typeColor(m.type)">{{ enumText('messageType', m.type) }}</van-tag>
            <span class="msg-title">{{ m.title }}</span>
            <span v-if="m.isRead === 0" class="unread-dot" />
          </div>
          <div class="msg-preview">{{ m.content || '点击查看详情' }}</div>
          <div class="msg-time">{{ formatDateTime(m.createTime) }}</div>
        </div>
        <van-icon name="arrow" class="msg-arrow" />
      </div>

      <div v-if="total > list.length" class="load-more" @click="loadMore">加载更多</div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMessages } from '../../api/message'
import { formatDateTime, enumText } from '../../utils/format'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 20 })

function typeColor(t) {
  return {
    APPOINTMENT: '#41769e',
    ACTIVITY: '#c1731f',
    SYSTEM: '#2e7d5f',
    HEALTH_REMINDER: '#a86d12'
  }[t] || '#7d8187'
}

function goDetail(m) {
  router.push(`/message/${m.id}`)
}

async function load() {
  loading.value = true
  try {
    const data = await getMessages({ page: query.page, size: query.size })
    if (query.page === 1) list.value = data.list || []
    else list.value = list.value.concat(data.list || [])
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function loadMore() {
  query.page += 1
  load()
}

onMounted(load)
</script>

<style scoped>
.msg-list-page {
  background: var(--page-bg);
  min-height: 100vh;
}
.msg-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--card-bg);
  border-radius: var(--radius-card);
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: var(--card-shadow);
}
/* 未读消息：左侧主色强调条 */
.msg-card.unread {
  border-left: 4px solid var(--brand-color);
}
.msg-left {
  flex: 1;
  min-width: 0;
  margin-right: 10px;
}
.msg-head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.msg-title {
  font-size: 16px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}
.unread .msg-title {
  color: var(--brand-deep);
}
.unread-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--warm-color);
  flex-shrink: 0;
}
.msg-preview {
  margin-top: 8px;
  font-size: 15px;
  color: var(--text-sub);
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.msg-time {
  margin-top: 8px;
  font-size: 14px;
  color: var(--text-dim);
}
.msg-arrow {
  color: var(--text-dim);
  flex-shrink: 0;
  font-size: 16px;
}
</style>
