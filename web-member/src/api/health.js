import request from './request'

// 健康记录（接口文档 2.1~2.3）
export const recordHealth = (data) => request.post('/member/health', data)
export const getHealthHistory = (params) => request.get('/member/health/history', { params })
export const getHealthTrend = (params) => request.get('/member/health/trend', { params })
