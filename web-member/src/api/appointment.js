import request from './request'

// 体检预约（接口文档 5.1~5.6）
export const getPackages = () => request.get('/member/appointment/packages')
export const getSlots = (packageId, date) =>
  request.get(`/member/appointment/packages/${packageId}/slots`, { params: { date } })
export const createAppointment = (slotId) => request.post('/member/appointment', { slotId })
export const cancelAppointment = (id) => request.post(`/member/appointment/${id}/cancel`)
export const getMyAppointments = (params) => request.get('/member/appointment', { params })
