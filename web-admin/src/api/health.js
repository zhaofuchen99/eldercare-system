import request from './request'

// 健康记录管理（接口文档 2.4~2.5）
export const getHealthRecordPage = (params) => request.get('/admin/health-record', { params })
export const getHealthTrend = (params) => request.get('/admin/health-record/trend', { params })
