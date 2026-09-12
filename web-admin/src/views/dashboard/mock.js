/**
 * Dashboard 模拟数据（图表、待办、动态模块暂无后端接口）。
 * 后端提供对应接口后，仅需将本文件的导出替换为 api 调用即可，页面结构无需改动。
 */

/** 老人健康状态分布（人） */
export const healthDistribution = [
  { name: '健康良好', value: 286 },
  { name: '血压偏高', value: 64 },
  { name: '血糖偏高', value: 38 },
  { name: '心率异常', value: 17 },
  { name: '重点关注', value: 12 }
]

/** 近 7 天血压异常记录趋势（人次） */
export const bpTrend = {
  days: ['08-12', '08-13', '08-14', '08-15', '08-16', '08-17', '08-18'],
  systolic: [8, 12, 6, 9, 14, 11, 7],
  diastolic: [5, 7, 4, 6, 8, 6, 3]
}

/** 体检完成率（%） */
export const checkupCompletion = 78

/** 健康异常提醒条数（待办） */
export const healthAlertCount = 5

/** 未处理消息条数（待办） */
export const unreadMessageCount = 3

/** 最近动态 */
export const recentActivities = [
  { text: '张桂芳 完成了「基础体检套餐」，报告已上传', time: '10 分钟前', color: '#34b382', tag: '体检', tagType: 'success' },
  { text: '李建国 预约了「心血管专项筛查」（明日 09:30）', time: '32 分钟前', color: '#2e7cf6', tag: '预约', tagType: 'primary' },
  { text: '王秀英 报名参加社区活动「重阳太极晨练班」', time: '1 小时前', color: '#f5a623', tag: '活动', tagType: 'warning' },
  { text: '系统完成 128 位会员的每日健康数据巡检', time: '2 小时前', color: '#8a919c', tag: '巡检', tagType: 'info' },
  { text: '陈德海 的健康评测「认知能力量表」已出结果', time: '3 小时前', color: '#22b8cf', tag: '评测' },
  { text: '赵玉梅 血压连续 2 日偏高，已生成健康提醒', time: '昨天 16:20', color: '#f56c6c', tag: '预警', tagType: 'danger' }
]
