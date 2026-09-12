import request from './request'

// 消息管理（接口文档 7.5~7.9）
export const getMessagePage = (params) => request.get('/admin/message/page', { params })
export const getMessageDetail = (id) => request.get(`/admin/message/${id}`)
export const pushMessage = (data) => request.post('/admin/message', data)
export const pushBatchMessage = (data) => request.post('/admin/message/batch', data)
export const deleteMessage = (id) => request.delete(`/admin/message/${id}`)
