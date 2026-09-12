import request from './request'

// 消息通知（接口文档 7.1~7.4）
export const getMessages = (params) => request.get('/member/message', { params })
export const getMessageDetail = (id) => request.get(`/member/message/${id}`)
export const readMessage = (id) => request.put(`/member/message/${id}/read`)
export const getUnreadCount = () => request.get('/member/message/unread-count')
