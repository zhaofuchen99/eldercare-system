import request from './request'

// RBAC 授权管理（接口文档 14.x）
// 角色
export const getRolePage = (params) => request.get('/admin/role/page', { params })
export const getRoleDetail = (id) => request.get(`/admin/role/${id}`)
export const getRolePermissions = (id) => request.get(`/admin/role/${id}/permissions`)
export const assignRolePermissions = (id, permissionIds) =>
  request.put(`/admin/role/${id}/permissions`, { permissionIds })
export const getRoleUsers = (id, params) => request.get(`/admin/role/${id}/users`, { params })
export const assignRoleUsers = (id, userIds) => request.put(`/admin/role/${id}/users`, { userIds })
export const createRole = (data) => request.post('/admin/role', data)
export const updateRole = (id, data) => request.put(`/admin/role/${id}`, data)
export const deleteRole = (id) => request.delete(`/admin/role/${id}`)

// 权限
export const getPermissionPage = (params) => request.get('/admin/permission/page', { params })
export const getPermissionDetail = (id) => request.get(`/admin/permission/${id}`)
export const getPermissionResources = (id) => request.get(`/admin/permission/${id}/resources`)
export const assignPermissionResources = (id, resourceIds) =>
  request.put(`/admin/permission/${id}/resources`, { resourceIds })
export const createPermission = (data) => request.post('/admin/permission', data)
export const updatePermission = (id, data) => request.put(`/admin/permission/${id}`, data)
export const deletePermission = (id) => request.delete(`/admin/permission/${id}`)

// 资源
export const getResourcePage = (params) => request.get('/admin/resource/page', { params })
export const getResourceDetail = (id) => request.get(`/admin/resource/${id}`)
export const createResource = (data) => request.post('/admin/resource', data)
export const updateResource = (id, data) => request.put(`/admin/resource/${id}`, data)
export const deleteResource = (id) => request.delete(`/admin/resource/${id}`)
