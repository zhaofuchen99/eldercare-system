import request from './request'

// 仪表盘（接口文档 9.1）
export const getDashboard = () => request.get('/admin/dashboard')
