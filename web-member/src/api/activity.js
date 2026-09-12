import request from './request'

// 社区活动（接口文档 6.1~6.6）
export const getActivities = () => request.get('/member/activity')
export const getActivityDetail = (id) => request.get(`/member/activity/${id}`)
export const registerActivity = (id) => request.post(`/member/activity/${id}/register`)
export const getMyActivities = (params) => request.get('/member/activity/mine', { params })
export const getCheckinStatus = (id) => request.get(`/member/activity/${id}/checkin-status`)
export const checkinActivity = (id) => request.post(`/member/activity/${id}/checkin`)
