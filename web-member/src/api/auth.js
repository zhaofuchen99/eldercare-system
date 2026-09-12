import request from './request'

// 认证（接口文档 1.x）
export const sendSmsCode = (phone) => request.post('/sms/code', { phone })
export const register = (data) => request.post('/auth/register', data)
export const login = (data) => request.post('/auth/login', data)
export const logout = (refreshToken) => request.post('/auth/logout', { refreshToken })
export const forgotPassword = (data) => request.post('/auth/forgot-password', data)
export const changePassword = (data) => request.post('/auth/change-password', data)

// 会员中心（接口文档 12.x）
export const getProfile = () => request.get('/member/profile')
export const updateProfile = (data) => request.put('/member/profile', data)
