<template>
  <div class="assess-result-page">
    <van-nav-bar title="评测报告" left-arrow @click-left="$router.back()" fixed placeholder safe-area-inset-top />

    <div v-if="result">
      <!-- 评分卡片 -->
      <div class="score-card">
        <div class="score-grade">{{ result.grade || '良好' }}</div>
        <div class="score-num">{{ result.aiScore }}<span class="score-total"> 分</span></div>
        <div class="score-sub">
          规则评分 {{ result.ruleScore }} 分 · {{ formatDate(result.createTime) }}
        </div>
      </div>

      <van-cell-group inset class="suggest-card">
        <van-cell title="AI 健康建议" icon="chat-o">
          <template #value>
            <span class="suggest-text">{{ result.aiSuggestion || '暂无建议' }}</span>
          </template>
        </van-cell>
      </van-cell-group>

      <!-- 逐题解析 -->
      <van-cell-group inset title="逐题解析">
        <div v-for="(it, i) in result.items" :key="it.qid" class="answer-item">
          <div class="answer-q">{{ i + 1 }}. {{ it.content }}</div>
          <div class="answer-line"><span class="label">我的答案：</span>{{ it.answerText }}</div>
          <div v-if="it.meaning" class="answer-line"><span class="label">解析：</span>{{ it.meaning }}</div>
          <div v-if="it.score !== undefined && it.score !== null" class="answer-score">
            <van-tag type="warning" round>本题 {{ it.score }} 分</van-tag>
          </div>
        </div>
      </van-cell-group>

      <div class="result-actions">
        <van-button round block type="primary" @click="goList">返回评测列表</van-button>
      </div>
      <div class="empty-pad" />
    </div>

    <van-empty v-else-if="!loading" description="报告不存在" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getAssessmentResult } from '../../api/assessment'
import { formatDate } from '../../utils/format'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const result = ref(null)

function goList() {
  router.replace('/assessment')
}

onMounted(async () => {
  loading.value = true
  try {
    result.value = await getAssessmentResult(route.params.id)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.assess-result-page {
  background: var(--page-bg);
  min-height: 100vh;
}
/* 评分卡片：白底清晰，不做大面积渲染色块 */
.score-card {
  margin: 14px;
  padding: 26px 22px;
  border-radius: var(--radius-card);
  text-align: center;
  background: var(--card-bg);
  box-shadow: var(--card-shadow);
}
.score-grade {
  font-size: 17px;
  font-weight: 600;
  color: var(--brand-deep);
}
.score-num {
  margin-top: 8px;
  font-size: 52px;
  font-weight: 700;
  line-height: 1.1;
  color: var(--brand-deep);
}
.score-total {
  font-size: 18px;
  font-weight: 400;
  color: var(--text-sub);
}
.score-sub {
  margin-top: 8px;
  font-size: 14px;
  color: var(--text-dim);
}
.suggest-card {
  margin-top: 14px;
  box-shadow: var(--card-shadow);
}
.suggest-text {
  white-space: pre-wrap;
  text-align: left;
  font-size: 15px;
  color: var(--text-sub);
  line-height: 1.8;
}
/* 逐题解析 */
.assess-result-page :deep(.van-cell-group--inset) {
  box-shadow: var(--card-shadow);
}
.answer-item {
  padding: 16px;
  border-bottom: 1px solid var(--line-color);
}
.answer-item:last-child {
  border-bottom: none;
}
.answer-q {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
  line-height: 1.5;
}
.answer-line {
  font-size: 15px;
  color: var(--text-sub);
  line-height: 1.8;
}
.answer-line .label {
  color: var(--text-dim);
}
.answer-score {
  margin-top: 8px;
}
.result-actions {
  margin: 22px 20px;
}
.result-actions :deep(.van-button) {
  height: 52px;
  font-size: 18px;
  font-weight: 600;
}
</style>
