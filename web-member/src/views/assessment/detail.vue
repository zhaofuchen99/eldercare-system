<template>
  <div class="assess-detail-page">
    <van-nav-bar title="健康评测" left-arrow @click-left="$router.back()" fixed placeholder safe-area-inset-top />

    <div v-if="questionnaire">
      <div class="assess-head">
        <div class="assess-title">{{ questionnaire.title }}</div>
        <div class="assess-desc">{{ questionnaire.description }}</div>
        <div class="assess-count">共 {{ questionnaire.questions.length }} 题 · 全部必答</div>
      </div>

      <van-cell-group
        v-for="(q, qi) in questionnaire.questions"
        :key="q.id"
        inset
        class="q-card"
        :title="`${qi + 1}. ${q.content}`"
      >
        <!-- 单选 -->
        <van-radio-group v-if="q.type === 'SINGLE'" v-model="answers[q.id]">
          <van-cell v-for="(opt, oi) in q.options" :key="oi" :title="opt" clickable @click="answers[q.id] = oi">
            <template #right-icon>
              <van-radio :name="oi" />
            </template>
          </van-cell>
        </van-radio-group>

        <!-- 多选 -->
        <van-checkbox-group v-else-if="q.type === 'MULTIPLE'" v-model="answers[q.id]">
          <van-cell v-for="(opt, oi) in q.options" :key="oi" :title="opt" clickable @click="toggleMultiple(q.id, oi)">
            <template #right-icon>
              <van-checkbox :name="oi" />
            </template>
          </van-cell>
        </van-checkbox-group>

        <!-- 文本 -->
        <van-field
          v-else
          v-model="answers[q.id]"
          type="textarea"
          rows="2"
          autosize
          maxlength="500"
          show-word-limit
          placeholder="请输入您的回答"
        />
      </van-cell-group>

      <div class="submit-wrap">
        <van-button round block type="primary" :loading="submitting" @click="submit">提交评测</van-button>
      </div>
      <div class="empty-pad" />
    </div>

    <van-empty v-else-if="!loading" description="问卷不存在或已下线" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast, showFailToast } from 'vant'
import { getQuestionnaire, submitAssessment } from '../../api/assessment'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const questionnaire = ref(null)
const answers = reactive({})
const submitting = ref(false)

function toggleMultiple(qid, oi) {
  const arr = answers[qid] || []
  const idx = arr.indexOf(oi)
  if (idx >= 0) {
    arr.splice(idx, 1)
  } else {
    arr.push(oi)
  }
  answers[qid] = arr
}

async function submit() {
  const qs = questionnaire.value.questions
  for (const q of qs) {
    const v = answers[q.id]
    if (q.type === 'TEXT') {
      if (!v || !String(v).trim()) return showFailToast(`请回答第 ${qs.indexOf(q) + 1} 题`)
    } else if (q.type === 'MULTIPLE') {
      if (!v || !v.length) return showFailToast(`请选择第 ${qs.indexOf(q) + 1} 题`)
    } else {
      if (v === undefined || v === null || v === '') return showFailToast(`请选择第 ${qs.indexOf(q) + 1} 题`)
    }
  }

  submitting.value = true
  try {
    const items = qs.map((q) => ({ qid: q.id, value: answers[q.id] }))
    const resultId = await submitAssessment({ questionnaireId: questionnaire.value.id, items })
    showSuccessToast('评测提交成功，+20 积分')
    router.replace(`/assessment/result/${resultId}`)
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    questionnaire.value = await getQuestionnaire(route.params.id)
    // 初始化答案结构
    questionnaire.value.questions.forEach((q) => {
      if (q.type === 'MULTIPLE') answers[q.id] = []
      else if (q.type === 'TEXT') answers[q.id] = ''
      else answers[q.id] = undefined
    })
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.assess-detail-page {
  background: var(--page-bg);
  min-height: 100vh;
}
.assess-head {
  padding: 20px 20px 8px;
}
.assess-title {
  font-size: 21px;
  font-weight: 700;
  line-height: 1.4;
}
.assess-desc {
  margin-top: 8px;
  font-size: 15px;
  color: var(--text-sub);
  line-height: 1.6;
}
.assess-count {
  margin-top: 12px;
  font-size: 14px;
  color: var(--brand-deep);
  font-weight: 500;
}
/* 题卡：左侧橙色强调条 */
.q-card {
  margin-top: 14px;
  box-shadow: var(--card-shadow);
  overflow: hidden;
}
.q-card :deep(.van-cell-group__title) {
  background: var(--brand-bg);
  color: var(--text-main);
  font-size: 16px;
  font-weight: 600;
  padding: 14px 16px;
  line-height: 1.5;
  border-left: 4px solid var(--brand-color);
}
.submit-wrap {
  margin: 22px 20px;
}
.submit-wrap :deep(.van-button) {
  height: 52px;
  font-size: 18px;
  font-weight: 600;
}
</style>
