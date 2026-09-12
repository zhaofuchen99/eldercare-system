<template>
  <div class="login-page">
    <div class="login-header">
      <div class="login-logo">🏡</div>
      <div class="login-title">智慧养老社区</div>
      <div class="login-subtitle">让每位长者乐享智慧晚年</div>
    </div>

    <van-form @submit="onSubmit">
      <van-cell-group inset>
        <van-field
          v-model="form.phone"
          type="tel"
          label="手机号"
          placeholder="请输入手机号"
          maxlength="11"
          :rules="[{ required: true, message: '请输入手机号' }]"
        />
        <van-field
          v-model="form.password"
          type="password"
          label="密码"
          placeholder="请输入密码"
          :rules="[{ required: true, message: '请输入密码' }]"
        />
      </van-cell-group>
      <div class="login-actions">
        <van-button round block type="primary" native-type="submit" :loading="loading">登 录</van-button>
      </div>
    </van-form>

    <div class="login-links">
      <span @click="goRegister">注册账号</span>
      <span @click="goForgot">忘记密码</span>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast } from 'vant'
import { login } from '../../api/auth'
import { useAuthStore } from '../../store/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const loading = ref(false)
const form = reactive({ phone: '13800138000', password: '' })

async function onSubmit() {
  loading.value = true
  try {
    const data = await login(form)
    auth.setTokens(data.accessToken, data.refreshToken)
    auth.setUserInfo(data.userInfo)
    showSuccessToast('登录成功')
    const redirect = route.query.redirect || '/home'
    router.replace(redirect)
  } finally {
    loading.value = false
  }
}

function goRegister() {
  router.push('/register')
}

function goForgot() {
  router.push('/forgot')
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: var(--page-bg);
}
.login-header {
  padding: 56px 0 36px;
  text-align: center;
}
.login-logo {
  width: 88px;
  height: 88px;
  margin: 0 auto;
  border-radius: 26px;
  background: var(--card-bg);
  box-shadow: var(--card-shadow);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  line-height: 1;
}
.login-title {
  margin-top: 18px;
  font-size: 24px;
  font-weight: 700;
  color: var(--brand-deep);
  letter-spacing: 1px;
}
.login-subtitle {
  margin-top: 10px;
  font-size: 15px;
  color: var(--text-sub);
}
.login-actions {
  margin: 30px 20px 0;
}
.login-actions :deep(.van-button) {
  height: 52px;
  font-size: 18px;
  font-weight: 600;
}
.login-links {
  display: flex;
  justify-content: space-between;
  padding: 22px 44px;
  font-size: 16px;
  font-weight: 500;
  color: var(--brand-deep);
}
.login-links span {
  padding: 6px 4px;
}
</style>
