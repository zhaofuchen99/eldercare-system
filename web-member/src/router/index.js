import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../store/auth'

// TabBar 布局（四个主 tab）
const TabBarLayout = () => import('../layout/TabBarLayout.vue')

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/login/index.vue'), meta: { title: '登录' } },
  { path: '/register', name: 'register', component: () => import('../views/register/index.vue'), meta: { title: '注册' } },
  { path: '/forgot', name: 'forgot', component: () => import('../views/forgot/index.vue'), meta: { title: '找回密码' } },
  {
    path: '/',
    component: TabBarLayout,
    redirect: '/home',
    children: [
      { path: 'home', name: 'home', component: () => import('../views/home/index.vue'), meta: { title: '首页' } },
      { path: 'health', name: 'health', component: () => import('../views/health/index.vue'), meta: { title: '健康' } },
      { path: 'activity', name: 'activity', component: () => import('../views/activity/index.vue'), meta: { title: '活动' } },
      { path: 'mine', name: 'mine', component: () => import('../views/mine/index.vue'), meta: { title: '我的' } }
    ]
  },
  // 子页面（无 TabBar）
  { path: '/assessment', name: 'assessment-list', component: () => import('../views/assessment/list.vue'), meta: { title: '健康评测' } },
  { path: '/assessment/history', name: 'assessment-history', component: () => import('../views/assessment/history.vue'), meta: { title: '我的评测记录' } },
  { path: '/assessment/:id', name: 'assessment-detail', component: () => import('../views/assessment/detail.vue'), meta: { title: '答题' } },
  { path: '/assessment/result/:id', name: 'assessment-result', component: () => import('../views/assessment/result.vue'), meta: { title: '评测报告' } },
  { path: '/appointment', name: 'appointment-list', component: () => import('../views/appointment/list.vue'), meta: { title: '体检预约' } },
  { path: '/appointment/book/:id', name: 'appointment-book', component: () => import('../views/appointment/book.vue'), meta: { title: '选择时段' } },
  { path: '/appointment/mine', name: 'appointment-mine', component: () => import('../views/appointment/mine.vue'), meta: { title: '我的预约' } },
  { path: '/chat', name: 'chat', component: () => import('../views/chat/index.vue'), meta: { title: 'AI 助手' } },
  { path: '/message', name: 'message-list', component: () => import('../views/message/list.vue'), meta: { title: '消息中心' } },
  { path: '/message/:id', name: 'message-detail', component: () => import('../views/message/detail.vue'), meta: { title: '消息详情' } },
  { path: '/points', name: 'points', component: () => import('../views/points/index.vue'), meta: { title: '积分明细' } },
  { path: '/profile', name: 'profile', component: () => import('../views/profile/index.vue'), meta: { title: '个人信息' } },
  { path: '/password', name: 'password', component: () => import('../views/password/index.vue'), meta: { title: '修改密码' } },
  { path: '/:pathMatch(.*)*', redirect: '/home' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 守卫：未登录一律跳登录页（登录/注册/找回放行）
router.beforeEach((to) => {
  const auth = useAuthStore()
  const whiteList = ['login', 'register', 'forgot']
  if (!auth.isLoggedIn && !whiteList.includes(to.name)) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

// 页面标题
router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 智慧养老社区` : '智慧养老社区'
})

export default router
