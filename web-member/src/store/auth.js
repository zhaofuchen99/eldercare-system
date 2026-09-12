import { defineStore } from 'pinia'

const ACCESS_KEY = 'eldercare_member_access'
const REFRESH_KEY = 'eldercare_member_refresh'
const USER_KEY = 'eldercare_member_user'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: localStorage.getItem(ACCESS_KEY) || '',
    refreshToken: localStorage.getItem(REFRESH_KEY) || '',
    userInfo: JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  }),
  getters: {
    isLoggedIn: (s) => !!s.accessToken,
    points: (s) => s.userInfo?.points ?? 0,
    memberLevel: (s) => s.userInfo?.memberLevel || 'NORMAL',
    realName: (s) => s.userInfo?.realName || '',
    phone: (s) => s.userInfo?.phone || ''
  },
  actions: {
    setTokens(accessToken, refreshToken) {
      this.accessToken = accessToken
      this.refreshToken = refreshToken
      localStorage.setItem(ACCESS_KEY, accessToken)
      localStorage.setItem(REFRESH_KEY, refreshToken)
    },
    setUserInfo(userInfo) {
      this.userInfo = userInfo
      localStorage.setItem(USER_KEY, JSON.stringify(userInfo))
    },
    setPoints(points) {
      if (this.userInfo) {
        this.userInfo.points = points
        localStorage.setItem(USER_KEY, JSON.stringify(this.userInfo))
      }
    },
    clear() {
      this.accessToken = ''
      this.refreshToken = ''
      this.userInfo = null
      localStorage.removeItem(ACCESS_KEY)
      localStorage.removeItem(REFRESH_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }
})
