import request from './request'

// 活动管理（接口文档 6.7~6.12）
export const createActivity = (data) => request.post('/admin/activity', data)
export const updateActivity = (id, data) => request.put(`/admin/activity/${id}`, data)
export const deleteActivity = (id) => request.delete(`/admin/activity/${id}`)
export const getActivityPage = (params) => request.get('/admin/activity/page', { params })
export const getActivityDetail = (id) => request.get(`/admin/activity/${id}`)
export const getActivityRegistrations = (id) => request.get(`/admin/activity/${id}/registrations`)
