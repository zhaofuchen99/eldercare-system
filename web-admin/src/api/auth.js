import request from './request'

// 认证与当前用户权限接口（接口文档 1.x / 14.1）
export const sendSmsCode = (phone) => request.post('/sms/code', { phone })
export const register = (data) => request.post('/auth/register', data)
export const login = (data) => request.post('/auth/login', data)
export const logout = (data) => request.post('/auth/logout', data)
export const refresh = (data) => request.post('/auth/refresh', data)
export const forgotPassword = (data) => request.post('/auth/forgot-password', data)
export const changePassword = (data) => request.post('/auth/change-password', data)
// 当前用户权限/菜单（RBAC，文档 8.2 / 14.1）
export const getPermissions = () => request.get('/member/permissions')
