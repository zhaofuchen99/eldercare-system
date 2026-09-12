/** 日期格式化为 yyyy-MM-dd HH:mm:ss */
export function formatDateTime(value) {
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

/** 日期格式化为 yyyy-MM-dd */
export function formatDate(value) {
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/** 枚举编码 → 中文映射（文档约定：数据库存英文编码，前端展示层映射中文） */
export const enumMaps = {
  memberLevel: {
    NORMAL: '普通', SILVER: '白银', GOLD: '黄金', PLATINUM: '铂金', DIAMOND: '钻石'
  },
  userStatus: { ENABLED: '启用', DISABLED: '禁用' },
  appointmentStatus: {
    PENDING: '待确认', CONFIRMED: '已确认', CANCELED: '已取消', COMPLETED: '已完成'
  },
  activityStatus: {
    DRAFT: '草稿', REGISTRATING: '报名中', IN_PROGRESS: '进行中', ENDED: '已结束'
  },
  messageType: {
    APPOINTMENT: '预约通知', ACTIVITY: '活动通知', SYSTEM: '系统消息', HEALTH_REMINDER: '健康提醒'
  },
  slotStatus: { AVAILABLE: '可预约', FULL: '已满', CLOSED: '已关闭' },
  resourceType: { API: '接口', MENU: '菜单', BUTTON: '按钮' },
  questionnaireStatus: { DRAFT: '草稿', PUBLISHED: '已发布' },
  scoreMode: { SCORED: '计分', NON_SCORED: '非计分' },
  questionType: { SINGLE: '单选', MULTIPLE: '多选', TEXT: '文本' },
  checkInStatus: { NOT_CHECKED_IN: '未签到', CHECKED_IN: '已签到' },
  roleStatus: { 1: '启用', 0: '停用' }
}

/** 取枚举中文；映射不到返回原值 */
export function enumText(mapKey, value) {
  const map = enumMaps[mapKey]
  if (!map) return value ?? '-'
  return map[value] ?? value ?? '-'
}
