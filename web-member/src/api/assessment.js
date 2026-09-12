import request from './request'

// 健康评测（接口文档 3.1~3.5）
export const getQuestionnaires = () => request.get('/member/assessment/questionnaires')
export const getQuestionnaire = (id) => request.get(`/member/assessment/questionnaire/${id}`)
export const submitAssessment = (data) => request.post('/member/assessment/submit', data)
export const getAssessmentHistory = (params) => request.get('/member/assessment/history', { params })
export const getAssessmentResult = (id) => request.get(`/member/assessment/result/${id}`)
