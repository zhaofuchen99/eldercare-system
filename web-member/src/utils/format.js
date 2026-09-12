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

/** 枚举编码 → 中文映射（数据库存英文编码，前端展示层映射中文） */
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
  checkInStatus: { NOT_CHECKED_IN: '未签到', CHECKED_IN: '已签到' },
  questionType: { SINGLE: '单选', MULTIPLE: '多选', TEXT: '文本' }
}

/** 取枚举中文；映射不到返回原值 */
export function enumText(mapKey, value) {
  const map = enumMaps[mapKey]
  if (!map) return value ?? '-'
  return map[value] ?? value ?? '-'
}

/** 性别：M/1/男 → 男，F/0/女 → 女，其它原值 */
export function genderText(v) {
  if (v === 'M' || v === '1' || v === '男') return '男'
  if (v === 'F' || v === '0' || v === '女') return '女'
  return v || '-'
}

/**
 * 健康指标展示解读（仅前端展示用，供长者快速看懂状态）
 * 返回 [{ label, value, unit, status, level }]，level：good 正常 / warn 需关注
 */
export function healthMetrics(r) {
  if (!r) return []
  const rows = []
  if (r.systolic != null && r.diastolic != null) {
    let status = '正常'
    let level = 'good'
    if (r.systolic >= 140 || r.diastolic >= 90) {
      status = '偏高'
      level = 'warn'
    } else if (r.systolic < 90 || r.diastolic < 60) {
      status = '偏低'
      level = 'warn'
    }
    rows.push({ label: '血压', value: `${r.systolic}/${r.diastolic}`, unit: 'mmHg', status, level })
  }
  if (r.bloodSugar !== null && r.bloodSugar !== undefined && r.bloodSugar !== '') {
    const v = Number(r.bloodSugar)
    let status = '正常'
    let level = 'good'
    if (v > 6.1) {
      status = '偏高'
      level = 'warn'
    } else if (v < 3.9) {
      status = '偏低'
      level = 'warn'
    }
    rows.push({ label: '血糖', value: String(r.bloodSugar), unit: 'mmol/L', status, level })
  }
  if (r.heartRate !== null && r.heartRate !== undefined && r.heartRate !== '') {
    const v = Number(r.heartRate)
    let status = '正常'
    let level = 'good'
    if (v > 100) {
      status = '偏快'
      level = 'warn'
    } else if (v < 60) {
      status = '偏缓'
      level = 'warn'
    }
    rows.push({ label: '心率', value: String(r.heartRate), unit: '次/分', status, level })
  }
  return rows
}

/** 异常指标的温和提醒文案（无异常返回空串） */
export function healthTip(r) {
  const warns = healthMetrics(r).filter((m) => m.level === 'warn')
  if (!warns.length) return ''
  return `${warns.map((m) => m.label).join('、')}有些波动，建议留意休息；如有不适请及时就医`
}
