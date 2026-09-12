<template>
  <div class="assessment-page">
    <el-card shadow="never">
      <div class="toolbar">
        <div class="toolbar-title">问卷列表</div>
        <el-button v-perm="'admin:assessment:manage'" type="primary" @click="openEdit()">新建问卷</el-button>
      </div>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="totalScore" label="总分" width="70" align="center" />
        <el-table-column prop="passScore" label="及格分" width="80" align="center" />
        <el-table-column label="等级规则" min-width="180">
          <template #default="{ row }">
            <template v-if="row.gradeRules && row.gradeRules.length">
              <el-tag v-for="(g, i) in row.gradeRules" :key="i" size="small" class="rule-tag">
                {{ g.label }}(≥{{ g.min }})
              </el-tag>
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'">
              {{ enumText('questionnaireStatus', row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="250">
          <template #default="{ row }">
            <el-button v-perm="'admin:assessment:manage'" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-perm="'admin:assessment:manage'" link type="primary" @click="openQuestions(row)">题目</el-button>
            <el-button
              v-perm="'admin:assessment:manage'"
              link
              :type="row.status === 'PUBLISHED' ? 'warning' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 'PUBLISHED' ? '下架' : '发布' }}
            </el-button>
            <el-button v-perm="'admin:assessment:manage'" link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pager"
        background
        layout="total, prev, pager, next, sizes"
        :total="total"
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :page-sizes="[10, 20, 50]"
        @current-change="load"
        @size-change="handleSizeChange"
      />
    </el-card>

    <!-- 新建 / 编辑问卷 -->
    <el-dialog v-model="editVisible" :title="form.id ? '编辑问卷' : '新建问卷'" width="640px" destroy-on-close>
      <el-form :model="form" label-width="90px" ref="formRef" :rules="formRules">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="2000" />
        </el-form-item>
        <el-form-item label="总分">
          <el-input-number v-model="form.totalScore" :min="0" />
        </el-form-item>
        <el-form-item label="及格分">
          <el-input-number v-model="form.passScore" :min="0" />
        </el-form-item>
        <el-form-item label="等级规则">
          <div class="rules-wrap">
            <div v-for="(g, i) in form.gradeRules" :key="i" class="rule-row">
              <el-input v-model="g.label" placeholder="等级名称" style="width: 110px" />
              <el-input-number v-model="g.min" :min="0" placeholder="最低分" />
              <el-input v-model="g.description" placeholder="描述" />
              <el-button link type="danger" @click="removeRule(i)">删除</el-button>
            </div>
            <el-button type="primary" plain size="small" @click="addRule">添加等级</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 题目管理 -->
    <el-drawer v-model="questionVisible" size="760px" :title="`题目管理 · ${questionTitle}`" destroy-on-close>
      <el-alert
        type="info"
        :closable="false"
        title="编辑后点击「保存全部」整体重建（覆盖式）；添加新题也走同一入口"
        class="drawer-alert"
      />
      <div v-for="(q, qi) in questionList" :key="qi" class="question-card">
        <el-form :model="q" label-width="90px">
          <div class="question-head">
            <span class="question-index">第 {{ qi + 1 }} 题</span>
            <el-button link type="danger" @click="removeQuestion(qi)">移除</el-button>
          </div>
          <el-form-item label="题目内容">
            <el-input v-model="q.content" maxlength="500" />
          </el-form-item>
          <el-form-item label="题型">
            <el-select v-model="q.type" style="width: 160px">
              <el-option v-for="(t, v) in enumMaps.questionType" :key="v" :label="t" :value="v" />
            </el-select>
          </el-form-item>
          <el-form-item label="计分模式">
            <el-select v-model="q.scoreMode" style="width: 160px">
              <el-option v-for="(t, v) in enumMaps.scoreMode" :key="v" :label="t" :value="v" />
            </el-select>
          </el-form-item>
          <el-form-item label="最大分值">
            <el-input-number v-model="q.maxScore" :min="0" />
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="q.sortOrder" :min="1" />
          </el-form-item>
          <template v-if="q.type !== 'TEXT'">
            <el-form-item label="选项">
              <div class="opt-wrap">
                <div v-for="(o, oi) in q.options" :key="oi" class="opt-row">
                  <el-input v-model="o.text" placeholder="选项文案" style="width: 170px" />
                  <el-input v-model="o.meaning" placeholder="含义（非计分题展示）" style="width: 170px" />
                  <el-input-number
                    v-if="q.scoreMode === 'SCORED'"
                    v-model="o.score"
                    :min="0"
                    placeholder="分值"
                    style="width: 100px"
                  />
                  <el-button link type="danger" @click="q.options.splice(oi, 1)">删除</el-button>
                </div>
                <el-button type="primary" plain size="small" @click="q.options.push({ text: '', meaning: '', score: 0 })">
                  添加选项
                </el-button>
              </div>
            </el-form-item>
          </template>
        </el-form>
      </div>
      <el-button v-perm="'admin:assessment:manage'" type="primary" plain class="add-question" @click="addQuestion">
        添加题目
      </el-button>
      <template #footer>
        <el-button @click="questionVisible = false">关闭</el-button>
        <el-button
          v-perm="'admin:assessment:manage'"
          type="primary"
          :loading="savingQuestions"
          @click="saveAllQuestions"
        >
          保存全部
        </el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createQuestionnaire,
  updateQuestionnaire,
  deleteQuestionnaire,
  updateQuestionnaireStatus,
  getQuestionnairePage,
  getQuestions,
  saveQuestions
} from '../../api/assessment'
import { formatDateTime, enumMaps, enumText } from '../../utils/format'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10 })

// 问卷表单
const editVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const emptyForm = () => ({
  id: null,
  title: '',
  description: '',
  totalScore: 0,
  passScore: 0,
  gradeRules: []
})
const form = reactive(emptyForm())
const formRules = { title: [{ required: true, message: '请输入标题', trigger: 'blur' }] }

// 题目管理
const questionVisible = ref(false)
const questionTitle = ref('')
const currentQuestionnaireId = ref(null)
const questionList = ref([])
const savingQuestions = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await getQuestionnairePage(query)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function handleSizeChange() {
  query.page = 1
  load()
}

function openEdit(row) {
  Object.assign(form, emptyForm())
  if (row) {
    Object.assign(form, {
      id: row.id,
      title: row.title,
      description: row.description,
      totalScore: row.totalScore,
      passScore: row.passScore,
      gradeRules: (row.gradeRules || []).map((g) => ({ ...g }))
    })
  }
  editVisible.value = true
}

function addRule() {
  form.gradeRules.push({ min: 0, label: '', description: '' })
}

function removeRule(i) {
  form.gradeRules.splice(i, 1)
}

async function submitEdit() {
  await formRef.value.validate().catch(() => Promise.reject())
  submitting.value = true
  try {
    const payload = {
      title: form.title,
      description: form.description,
      totalScore: form.totalScore,
      passScore: form.passScore,
      gradeRules: form.gradeRules
    }
    if (form.id) {
      await updateQuestionnaire(form.id, payload)
      ElMessage.success('问卷已更新')
    } else {
      await createQuestionnaire(payload)
      ElMessage.success('问卷已创建（草稿）')
    }
    editVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row) {
  const to = row.status === 'PUBLISHED' ? 'DRAFT' : 'PUBLISHED'
  const label = to === 'PUBLISHED' ? '发布' : '下架'
  try {
    await ElMessageBox.confirm(`确定要${label}问卷「${row.title}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await updateQuestionnaireStatus(row.id, to)
    ElMessage.success(`${label}成功`)
    load()
  } catch {
    /* 拦截器已提示 */
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `删除后问卷及其题目不可恢复，确定删除「${row.title}」吗？`,
      '删除确认',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await deleteQuestionnaire(row.id)
    ElMessage.success('已删除')
    load()
  } catch {
    /* 拦截器已提示 */
  }
}

async function openQuestions(row) {
  questionTitle.value = row.title
  currentQuestionnaireId.value = row.id
  questionVisible.value = true
  try {
    const qs = await getQuestions(row.id)
    questionList.value = (qs || []).map((q) => ({
      ...q,
      options: (q.options || []).map((o) => ({ text: o.text || '', meaning: o.meaning || '', score: o.score ?? 0 }))
    }))
  } catch {
    questionList.value = []
  }
}

function addQuestion() {
  questionList.value.push({
    id: null,
    content: '',
    type: 'SINGLE',
    scoreMode: 'NON_SCORED',
    options: [{ text: '', meaning: '', score: 0 }],
    maxScore: 0,
    sortOrder: questionList.value.length + 1
  })
}

function removeQuestion(i) {
  questionList.value.splice(i, 1)
}

async function saveAllQuestions() {
  if (!questionList.value.length) return ElMessage.warning('至少保留一道题目')
  for (const q of questionList.value) {
    if (!q.content.trim()) return ElMessage.warning('存在未填写内容的题目')
    if (q.type !== 'TEXT' && !q.options.length) return ElMessage.warning(`题目「${q.content}」缺少选项`)
    if (q.type !== 'TEXT' && q.scoreMode === 'SCORED') {
      const invalid = q.options.some((o) => o.score === '' || o.score === null || o.score === undefined)
      if (invalid) return ElMessage.warning(`计分题「${q.content}」选项分值未填全`)
    }
  }
  savingQuestions.value = true
  try {
    await saveQuestions(currentQuestionnaireId.value, questionList.value)
    ElMessage.success('题目已保存')
  } finally {
    savingQuestions.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}
.toolbar-title {
  font-size: 15px;
  font-weight: 600;
}
.pager {
  margin-top: 14px;
  justify-content: flex-end;
}
.rule-tag {
  margin: 2px 4px 2px 0;
}
.rules-wrap {
  width: 100%;
}
.rule-row {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
.drawer-alert {
  margin-bottom: 14px;
}
.question-card {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 14px;
}
.question-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.question-index {
  font-weight: 600;
  color: #409eff;
}
.opt-wrap {
  width: 100%;
}
.opt-row {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
.add-question {
  margin-bottom: 10px;
}
</style>
