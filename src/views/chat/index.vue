<template>
  <div class="chat-page">
    <!-- 会话列表 -->
    <template v-if="view === 'list'">
      <van-nav-bar title="健康咨询" fixed placeholder safe-area-inset-top />
      <div class="page-content">
        <van-empty v-if="!sessions.length && !loading" description="还没有会话，开始一段健康咨询吧" image-size="80" />
        <div v-for="s in sessions" :key="s.id" class="session-card" @click="openSession(s)">
          <div class="session-name">{{ s.sessionName || '新对话' }}</div>
          <div class="session-time">{{ formatDateTime(s.updateTime) }}</div>
        </div>
        <div class="new-session">
          <van-button round block type="primary" icon="plus" :loading="creating" @click="newSession">
            开启新对话
          </van-button>
        </div>
      </div>
    </template>

    <!-- 聊天窗口 -->
    <template v-else>
      <van-nav-bar
        :title="currentSessionName"
        left-arrow
        @click-left="backToList"
        fixed
        placeholder
        safe-area-inset-top
      >
        <template #right>
          <van-icon name="delete-o" size="20" class="del-icon" @click="removeSession" />
        </template>
      </van-nav-bar>

      <div class="msg-list" ref="msgRef">
        <div v-for="m in messages" :key="m.id" class="msg-row" :class="m.role === 'USER' ? 'user' : 'assistant'">
          <div class="msg-bubble" :class="{ streaming: m.streaming }">
            <div v-if="!m.message && m.streaming" class="typing">正在为您整理回答…</div>
            <div v-else class="msg-text" v-html="renderMarkdown(m.message)"></div>
            <div v-if="m.status === 'FAILED'" class="msg-failed">回复失败，请检查网络后重试</div>
          </div>
        </div>
      </div>

      <div class="chat-input-bar">
        <van-field
          v-model="input"
          placeholder="咨询健康问题，如：高血压平时要注意什么？"
          :border="false"
          class="chat-input"
          @keyup.enter="send"
        />
        <van-button type="primary" round size="small" :disabled="streaming || !input.trim()" @click="send">
          发送
        </van-button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showFailToast } from 'vant'
import { createSession, getSessions, deleteSession, getMessages } from '../../api/chat'
import { useAuthStore } from '../../store/auth'
import { formatDateTime } from '../../utils/format'
import { renderMarkdown } from '../../utils/markdown'

const router = useRouter()
const auth = useAuthStore()

const view = ref('list')
const loading = ref(false)
const sessions = ref([])
const creating = ref(false)

const currentSessionId = ref(null)
const currentSessionName = ref('')
const messages = ref([])
const input = ref('')
const streaming = ref(false)
const msgRef = ref(null)

const chatTitle = computed(() => currentSessionName.value || '新对话')

async function loadSessions() {
  loading.value = true
  try {
    const data = await getSessions({ page: 1, size: 50 })
    sessions.value = data.list || []
  } finally {
    loading.value = false
  }
}

async function newSession() {
  creating.value = true
  try {
    const id = await createSession()
    currentSessionId.value = id
    currentSessionName.value = '新对话'
    messages.value = []
    view.value = 'chat'
    await loadSessions()
  } finally {
    creating.value = false
  }
}

async function openSession(s) {
  currentSessionId.value = s.id
  currentSessionName.value = s.sessionName || '新对话'
  view.value = 'chat'
  try {
    const data = await getMessages(s.id, { page: 1, size: 50 })
    // 历史倒序 → 正序显示
    messages.value = (data.list || []).slice().reverse()
    scrollToBottom()
  } catch {
    /* 拦截器已提示 */
  }
}

function backToList() {
  view.value = 'list'
  loadSessions()
}

async function removeSession() {
  try {
    await showConfirmDialog({ title: '删除会话', message: '删除后该会话与全部消息不可恢复，确定删除吗？' })
  } catch {
    return
  }
  try {
    await deleteSession(currentSessionId.value)
    view.value = 'list'
    loadSessions()
  } catch {
    /* 拦截器已提示 */
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (msgRef.value) msgRef.value.scrollTop = msgRef.value.scrollHeight
  })
}

// 解析 SSE：SseEmitter 每 chunk 发送 data:xxx\n\n
async function* parseSSE(resp) {
  const reader = resp.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let readerDone = false
  while (!readerDone) {
    const { done, value } = await reader.read()
    readerDone = done
    if (value) buffer += decoder.decode(value, { stream: true })
    let idx
    while ((idx = buffer.indexOf('\n\n')) !== -1) {
      const raw = buffer.slice(0, idx)
      buffer = buffer.slice(idx + 2)
      const line = raw.split('\n').find((l) => l.startsWith('data:'))
      if (line) yield extractData(line)
    }
  }
  if (buffer.trim()) {
    const line = buffer.split('\n').find((l) => l.startsWith('data:'))
    if (line) yield extractData(line)
  }
}

function extractData(line) {
  let text = line.slice(5).trim()
  if (text.startsWith('"') && text.endsWith('"')) {
    try {
      text = JSON.parse(text)
    } catch {
      /* 保持原文 */
    }
  }
  return text
}

async function send() {
  const content = input.value.trim()
  if (!content || streaming.value) return
  input.value = ''

  messages.value.push({ id: `u-${Date.now()}`, role: 'USER', message: content, status: 'SUCCESS' })
  const aiMsg = { id: `a-${Date.now()}`, role: 'ASSISTANT', message: '', streaming: true, status: 'SUCCESS' }
  messages.value.push(aiMsg)
  streaming.value = true
  scrollToBottom()

  try {
    const resp = await fetch('/api/member/chat/message/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${auth.accessToken}`
      },
      body: JSON.stringify({ sessionId: currentSessionId.value, content })
    })

    if (!resp.ok) {
      let msg = 'AI 服务暂不可用'
      try {
        const j = await resp.json()
        msg = j.message || msg
      } catch {
        /* 忽略 */
      }
      throw new Error(msg)
    }

    for await (const chunk of parseSSE(resp)) {
      aiMsg.message += chunk
      scrollToBottom()
    }

    if (!aiMsg.message) {
      throw new Error('AI 未返回内容')
    }
    aiMsg.streaming = false
  } catch (e) {
    aiMsg.streaming = false
    aiMsg.status = 'FAILED'
    showFailToast(e.message || 'AI 服务暂不可用')
  } finally {
    streaming.value = false
    loadSessions() // 会话标题可能已自动生成
  }
}

onMounted(loadSessions)
</script>

<style scoped>
.chat-page {
  background: var(--page-bg);
  min-height: 100vh;
}
/* 会话列表卡片 */
.session-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--card-bg);
  border-radius: var(--radius-card);
  padding: 18px 16px;
  margin-bottom: 12px;
  box-shadow: var(--card-shadow);
}
.session-card:active {
  transform: scale(0.985);
  transition: transform 0.15s;
}
.session-name {
  font-size: 17px;
  font-weight: 600;
}
.session-time {
  font-size: 14px;
  color: var(--text-dim);
  flex-shrink: 0;
  margin-left: 10px;
}
.new-session {
  margin-top: 16px;
}
.new-session :deep(.van-button) {
  height: 50px;
  font-size: 17px;
  font-weight: 600;
}
.del-icon {
  color: var(--text-dim);
  font-size: 20px;
}
/* 消息流 */
.msg-list {
  position: fixed;
  top: 52px;
  bottom: 60px;
  left: 0;
  right: 0;
  overflow-y: auto;
  padding: 14px;
  background: var(--page-bg);
}
.msg-row {
  display: flex;
  margin-bottom: 16px;
}
.msg-row.user {
  justify-content: flex-end;
}
.msg-row.assistant {
  justify-content: flex-start;
}
.msg-bubble {
  max-width: 82%;
  padding: 12px 16px;
  border-radius: 14px;
  font-size: 17px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}
.msg-row.user .msg-bubble {
  background: var(--brand-color);
  color: #fff;
  border-top-right-radius: 4px;
}
.msg-row.assistant .msg-bubble {
  background: var(--card-bg);
  color: var(--text-main);
  border-top-left-radius: 4px;
  border: 1px solid var(--line-color);
  box-shadow: var(--card-shadow);
}
.msg-bubble.streaming {
  min-width: 70px;
}
.typing {
  color: var(--text-dim);
  font-size: 16px;
}
.msg-failed {
  margin-top: 8px;
  font-size: 14px;
  color: var(--bad-color);
}
/* AI 回复的 Markdown 渲染样式（v-html 生成的内容需用 :deep 命中） */
.msg-text {
  white-space: normal;
}
.msg-text :deep(p) {
  margin: 4px 0;
}
.msg-text :deep(ul),
.msg-text :deep(ol) {
  margin: 4px 0;
  padding-left: 20px;
}
.msg-text :deep(li) {
  margin: 2px 0;
  line-height: 1.8;
}
.msg-text :deep(h3),
.msg-text :deep(h4),
.msg-text :deep(h5),
.msg-text :deep(h6) {
  margin: 8px 0 4px;
  font-weight: 600;
}
.msg-text :deep(strong) {
  font-weight: 600;
}
.msg-text :deep(code) {
  display: inline-block;
  padding: 1px 5px;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.06);
  font-size: 13px;
}
.msg-text :deep(em) {
  font-style: italic;
}
/* 底部输入栏：输入框与发送按钮都足够大 */
.chat-input-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  padding-bottom: calc(12px + env(safe-area-inset-bottom));
  background: var(--card-bg);
  box-shadow: 0 -1px 8px rgba(70, 65, 55, 0.08);
}
.chat-input {
  flex: 1;
  background: var(--page-bg);
  border-radius: 24px;
  padding: 12px 16px;
}
.chat-input :deep(.van-field__control) {
  font-size: 16px;
}
.chat-input-bar :deep(.van-button) {
  height: 48px;
  min-width: 84px;
  font-size: 17px;
  font-weight: 600;
}
</style>
