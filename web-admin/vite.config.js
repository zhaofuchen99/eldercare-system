import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 管理端开发配置（文档 4.2 / 10.1）：端口 5174，/api 代理到后端 8080
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5174,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
