import request from './request'

// 体检管理（接口文档 5.7~5.16）
// 套餐
export const createPackage = (data) => request.post('/admin/appointment/package', data)
export const updatePackage = (id, data) => request.put(`/admin/appointment/package/${id}`, data)
export const deletePackage = (id) => request.delete(`/admin/appointment/package/${id}`)
export const getPackagePage = (params) => request.get('/admin/appointment/package/page', { params })
export const getPackageDetail = (id) => request.get(`/admin/appointment/package/${id}`)
// 时段
export const createSlotsBatch = (data) => request.post('/admin/appointment/slot/batch', data)
export const getSlots = (params) => request.get('/admin/appointment/slot', { params })
// 预约
export const getAppointmentPage = (params) => request.get('/admin/appointment/page', { params })
export const updateAppointmentStatus = (id, status) => request.put(`/admin/appointment/${id}/status`, { status })
// 上传报告（multipart/form-data，file 字段，PDF ≤20MB）
export const uploadReport = (id, file) => {
  const form = new FormData()
  form.append('file', file)
  return request.post(`/admin/appointment/${id}/report`, form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
