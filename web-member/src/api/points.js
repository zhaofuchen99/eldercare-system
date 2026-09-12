import request from './request'

// 积分明细（接口文档 8.1）
export const getPoints = (params) => request.get('/member/points', { params })
