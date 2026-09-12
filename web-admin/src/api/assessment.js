import request from './request'

// 评测管理（接口文档 3.6~3.14）
export const createQuestionnaire = (data) => request.post('/admin/assessment/questionnaire', data)
export const updateQuestionnaire = (id, data) => request.put(`/admin/assessment/questionnaire/${id}`, data)
export const deleteQuestionnaire = (id) => request.delete(`/admin/assessment/questionnaire/${id}`)
export const updateQuestionnaireStatus = (id, status) =>
  request.put(`/admin/assessment/questionnaire/${id}/status`, null, { params: { status } })
export const getQuestionnairePage = (params) => request.get('/admin/assessment/questionnaires', { params })
export const getQuestions = (id) => request.get(`/admin/assessment/questionnaire/${id}/questions`)
export const saveQuestions = (id, data) => request.post(`/admin/assessment/questionnaire/${id}/questions`, data)
export const updateQuestion = (id, data) => request.put(`/admin/assessment/question/${id}`, data)
export const deleteQuestion = (id) => request.delete(`/admin/assessment/question/${id}`)
