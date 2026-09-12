import request from './request'

// 会员管理（接口文档 10.1~10.7）
export const getMemberPage = (params) => request.get('/admin/members', { params })
export const getMemberDetail = (id) => request.get(`/admin/members/${id}`)
export const enableMember = (id) => request.put(`/admin/members/${id}/enable`)
export const disableMember = (id) => request.put(`/admin/members/${id}/disable`)
export const updateMemberLevel = (id, memberLevel) => request.put(`/admin/members/${id}/level`, { memberLevel })
export const adjustMemberPoints = (id, delta) => request.put(`/admin/members/${id}/points`, { delta })
export const resetMemberPassword = (id, password) => request.put(`/admin/members/${id}/password`, { password })
