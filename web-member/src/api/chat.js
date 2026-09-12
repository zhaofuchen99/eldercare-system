import request from './request'

// AI 对话（接口文档 4.1~4.6）
export const createSession = () => request.post('/member/chat/session')
export const getSessions = (params) => request.get('/member/chat/sessions', { params })
export const deleteSession = (id) => request.delete(`/member/chat/session/${id}`)
export const getMessages = (id, params) => request.get(`/member/chat/session/${id}/messages`, { params })
export const sendMessage = (data) => request.post('/member/chat/message', data)
