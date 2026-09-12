import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../store/auth'
import router from '../router'

// axios 实例：baseURL=/api，开发期经 Vite 代理到后端 8080
const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 401 无感刷新（单飞）：同一时刻只刷新一次，其余请求排队等待后重放
let isRefreshing = false
let waiters = []

function onRefreshed(err) {
  waiters.forEach((w) => (err ? w.reject(err) : w.resolve()))
  waiters = []
}

request.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 成功：直接返回 data 字段
    if (res.code === 200) {
      return res.data
    }

    // 401：尝试用 Refresh Token 无感刷新后重放原请求
    if (res.code === 401) {
      const config = response.config
      // 刷新接口本身 401 或已重放过 → 登录失效
      if (config._retried || (config.url && config.url.includes('/auth/refresh'))) {
        useAuthStore().clear()
        router.replace('/login')
        ElMessage.error('登录已失效，请重新登录')
        return Promise.reject(new Error('登录已失效'))
      }
      const auth = useAuthStore()
      if (!auth.refreshToken) {
        auth.clear()
        router.replace('/login')
        ElMessage.error('登录已失效，请重新登录')
        return Promise.reject(new Error('登录已失效'))
      }

      if (!isRefreshing) {
        isRefreshing = true
        axios
          .post('/api/auth/refresh', { refreshToken: auth.refreshToken })
          .then((r) => {
            const d = r.data
            if (d.code === 200) {
              auth.setTokens(d.data.accessToken, d.data.refreshToken)
              onRefreshed(null)
            } else {
              onRefreshed(new Error('刷新失败'))
            }
          })
          .catch(() => {
            onRefreshed(new Error('刷新失败'))
          })
          .finally(() => {
            isRefreshing = false
          })
      }

      // 标记重放，避免无限循环
      config._retried = true
      return new Promise((resolve, reject) => {
        waiters.push({
          resolve: () => resolve(request(config)),
          reject
        })
      })
    }

    // 其余错误码：统一提示并 reject
    if (res.code === 403) {
      ElMessage.error('无权限访问')
    } else if (res.code === 429) {
      ElMessage.error('操作过于频繁，请稍后再试')
    } else {
      ElMessage.error(res.message || '请求失败')
    }
    return Promise.reject(new Error(res.message || `请求失败(${res.code})`))
  },
  (error) => {
    ElMessage.error(error.message || '网络错误，请检查后端服务是否启动')
    return Promise.reject(error)
  }
)

export default request
