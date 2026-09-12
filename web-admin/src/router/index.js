import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../store/auth'

// 管理端静态路由；侧边菜单可见性由 RBAC menus（menu:admin:*）决定
const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/login/index.vue')
  },
  {
    path: '/',
    component: () => import('../layout/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'dashboard', component: () => import('../views/dashboard/index.vue'), meta: { title: '工作台' } },
      { path: 'member', name: 'member', component: () => import('../views/member/index.vue'), meta: { title: '会员管理' } },
      { path: 'health', name: 'health', component: () => import('../views/health/index.vue'), meta: { title: '健康档案' } },
      { path: 'assessment', name: 'assessment', component: () => import('../views/assessment/index.vue'), meta: { title: '评测管理' } },
      { path: 'appointment', name: 'appointment', component: () => import('../views/appointment/index.vue'), meta: { title: '体检管理' } },
      { path: 'activity', name: 'activity', component: () => import('../views/activity/index.vue'), meta: { title: '活动管理' } },
      { path: 'message', name: 'message', component: () => import('../views/message/index.vue'), meta: { title: '消息管理' } },
      { path: 'config', name: 'config', component: () => import('../views/config/index.vue'), meta: { title: '系统配置' } },
      { path: 'rbac/role', name: 'rbacRole', component: () => import('../views/rbac/role.vue'), meta: { title: '角色管理' } },
      { path: 'rbac/permission', name: 'rbacPermission', component: () => import('../views/rbac/permission.vue'), meta: { title: '权限管理' } },
      { path: 'rbac/resource', name: 'rbacResource', component: () => import('../views/rbac/resource.vue'), meta: { title: '资源管理' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.path === '/login') {
    return auth.isLoggedIn ? { path: '/dashboard' } : true
  }
  if (!auth.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  // 已登录但未拉过权限：先拉取 RBAC 菜单/权限
  if (!auth.authLoaded) {
    try {
      await auth.fetchAuth()
    } catch (e) {
      auth.clear()
      return { path: '/login' }
    }
  }
  return true
})

export default router
