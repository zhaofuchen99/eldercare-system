/**
 * 轻量 Markdown 渲染器（AI 对话消息用）
 *
 * 设计：
 * 1. 先对原文做 HTML 转义（escapeHtml），再套 markdown 标签 —— 防止 AI 输出注入脚本（XSS）。
 * 2. 覆盖 AI 回复最常用的语法：加粗、斜体、行内代码、标题、无序/有序列表、段落换行。
 *    不需要引入第三方库，零依赖。
 */
function escapeHtml(str) {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

/** 文本 → 安全的 HTML 片段 */
export function renderMarkdown(text) {
  if (text === null || text === undefined) return ''
  const raw = String(text).replace(/\r\n/g, '\n')
  if (!raw.trim()) return escapeHtml(raw)

  // ---- 行内语法（先转义再套标签） ----
  let html = escapeHtml(raw)
  // 行内代码 `code`
  html = html.replace(/`([^`\n]+)`/g, '<code>$1</code>')
  // 加粗 **bold**
  html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
  // 斜体 *italic*（前面已去掉 **，此处只处理单星号）
  html = html.replace(/(^|[^*])\*([^*\n]+)\*/g, '$1<em>$2</em>')

  // ---- 块级语法：按行组装 ----
  const lines = html.split('\n')
  const out = []
  let list = null // 'ul' | 'ol' | null

  const closeList = () => {
    if (list) {
      out.push('</' + list + '>')
      list = null
    }
  }

  for (const rawLine of lines) {
    const line = rawLine.trim()
    if (line === '') {
      closeList()
      continue
    }

    // 标题 # ~ ####
    let m = line.match(/^(#{1,4})\s+(.+)$/)
    if (m) {
      closeList()
      const lv = Math.min(m[1].length + 2, 6) // # → h3，#### → h6
      out.push('<' + lv + '>' + m[2] + '</' + lv + '>')
      continue
    }

    // 无序列表 - / *
    m = line.match(/^[-*]\s+(.+)$/)
    if (m) {
      if (list !== 'ul') {
        closeList()
        out.push('<ul>')
        list = 'ul'
      }
      out.push('<li>' + m[1] + '</li>')
      continue
    }

    // 有序列表 1. 2. ...
    m = line.match(/^\d+\.\s+(.+)$/)
    if (m) {
      if (list !== 'ol') {
        closeList()
        out.push('<ol>')
        list = 'ol'
      }
      out.push('<li>' + m[1] + '</li>')
      continue
    }

    // 普通段落
    closeList()
    out.push('<p>' + line + '</p>')
  }
  closeList()

  return out.join('\n')
}
