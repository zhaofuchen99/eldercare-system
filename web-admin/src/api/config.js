import request from './request'

// 系统配置（接口文档 11.1~11.3）
export const getConfigList = () => request.get('/admin/config')
export const getConfig = (key) => request.get(`/admin/config/${key}`)
export const updateConfig = (key, configValue) => request.put(`/admin/config/${key}`, { configValue })
