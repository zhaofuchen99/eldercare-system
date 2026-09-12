import { defineStore } from 'pinia'
import { login as apiLogin, logout as apiLogout, getPermissions } from '../api/auth'

const TOKEN_PREFIX = 'eldercare_admin_'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: localStorage.getItem(TOKEN_PREFIX + 'access') || '',
    refreshToken: localStorage.getItem(TOKEN_PREFIX + 'refresh') || '',
    userInfo: null,
    permissions: [],
    buttons: [],
    menus: [],
    authLoaded: false
  }),
  getters: {
    isLoggedIn: (s) => !!s.accessToken,
    hasPerm: (s) => (code) => s.permissions.includes(code)
  },
  actions: {
    setTokens(access, refresh) {
      this.accessToken = access
      this.refreshToken = refresh
      localStorage.setItem(TOKEN_PREFIX + 'access', access)
      localStorage.setItem(TOKEN_PREFIX + 'refresh', refresh)
    },
    /** 登录：拿 token + 用户信息，再拉取 RBAC 权限/菜单/按钮 */
    async login(phone, password) {
      const data = await apiLogin({ phone, password })
      this.setTokens(data.accessToken, data.refreshToken)
      this.userInfo = data.userInfo
      await this.fetchAuth()
    },
    /** 拉取当前用户权限（GET /api/member/permissions，RBAC 菜单/按钮数据源） */
    async fetchAuth() {
      const data = await getPermissions()
      this.permissions = data.permissions || []
      this.buttons = data.buttons || []
      this.menus = data.menus || []
      this.authLoaded = true
    },
    async logout() {
      try {
        await apiLogout({ refreshToken: this.refreshToken })
      } catch (e) {
        // 登出接口失败不阻塞本地清理
      }
      this.clear()
    },
    clear() {
      this.accessToken = ''
      this.refreshToken = ''
      this.userInfo = null
      this.permissions = []
      this.buttons = []
      this.menus = []
      this.authLoaded = false
      localStorage.removeItem(TOKEN_PREFIX + 'access')
      localStorage.removeItem(TOKEN_PREFIX + 'refresh')
    }
  }
})
