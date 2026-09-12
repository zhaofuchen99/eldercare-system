<template>
  <div class="assess-list-page">
    <van-nav-bar title="健康评测" left-arrow @click-left="$router.back()" fixed placeholder safe-area-inset-top />
    <div class="page-content">
      <van-notice-bar left-icon="info-o" text="完成一份评测可获得 20 积分，提交后生成健康评分与 AI 建议" />

      <van-cell title="我的评测记录" icon="records-o" is-link class="history-link" @click="goHistory" />

      <van-skeleton title :row="3" v-if="loading" />
      <van-empty v-else-if="!list.length" description="暂无已发布的问卷" image-size="80" />

      <div v-for="q in list" :key="q.id" class="questionnaire-card" @click="go(q)">
        <div class="q-title">{{ q.title }}</div>
        <div class="q-desc">{{ q.description || '完成问卷，了解您的健康状况' }}</div>
        <div class="q-footer">
          <van-button size="small" round type="primary">开始评测</van-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getQuestionnaires } from '../../api/assessment'

const router = useRouter()
const loading = ref(false)
const list = ref([])

function go(q) {
  router.push(`/assessment/${q.id}`)
}

function goHistory() {
  router.push('/assessment/history')
}

onMounted(async () => {
  loading.value = true
  try {
    list.value = await getQuestionnaires()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.assess-list-page {
  background: var(--page-bg);
  min-height: 100vh;
}
.assess-list-page :deep(.van-notice-bar) {
  border-radius: 10px;
  margin-bottom: 12px;
}
.history-link {
  border-radius: 10px;
  background: var(--card-bg);
  box-shadow: var(--card-shadow);
  overflow: hidden;
  margin-bottom: 12px;
}
.history-link :deep(.van-cell__title) {
  font-size: 16px;
  font-weight: 600;
}
.questionnaire-card {
  background: var(--card-bg);
  border-radius: var(--radius-card);
  padding: 18px 16px;
  margin-bottom: 12px;
  box-shadow: var(--card-shadow);
}
.q-title {
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
}
.q-desc {
  margin-top: 8px;
  font-size: 15px;
  color: var(--text-sub);
  line-height: 1.7;
}
.q-footer {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
.q-footer :deep(.van-button) {
  height: 42px;
  min-width: 116px;
  font-size: 15px;
  font-weight: 600;
}
</style>
