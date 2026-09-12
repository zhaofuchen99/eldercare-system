<template>
  <div class="forgot-page">
    <van-nav-bar title="找回密码" left-arrow @click-left="$router.back()" fixed placeholder safe-area-inset-top />

    <van-form @submit="onSubmit">
      <van-cell-group inset>
        <van-field
          v-model="form.phone"
          type="tel"
          label="手机号"
          placeholder="请输入手机号"
          maxlength="11"
          :rules="[{ required: true, message: '请输入手机号' }, { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }]"
        />
        <van-field
          v-model="form.code"
          center
          label="验证码"
          placeholder="请输入短信验证码"
          maxlength="6"
          :rules="[{ required: true, message: '请输入验证码' }]"
        >
          <template #button>
            <van-button size="small" type="primary" plain :disabled="countdown > 0" @click="sendCode">
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </van-button>
          </template>
        </van-field>
        <van-field
          v-model="form.newPassword"
          type="password"
          label="新密码"
          placeholder="至少 8 位，含字母和数字"
          :rules="[
            { required: true, message: '请输入新密码' },
            { pattern: /^(?=.*[A-Za-z])(?=.*\d).{8,}$/, message: '至少 8 位且包含字母和数字' }
          ]"
        />
      </van-cell-group>
      <div class="forgot-actions">
        <van-button round block type="primary" native-type="submit" :loading="loading">重置密码</van-button>
      </div>
    </van-form>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast, showFailToast } from 'vant'
import { sendSmsCode, forgotPassword } from '../../api/auth'

const router = useRouter()
const loading = ref(false)
const countdown = ref(0)
let timer = null
const form = reactive({ phone: '', code: '', newPassword: '' })

async function sendCode() {
  if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    return showFailToast('请输入正确的手机号')
  }
  try {
    await sendSmsCode(form.phone)
    showSuccessToast('验证码已发送')
    countdown.value = 60
    timer && clearInterval(timer)
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch {
    /* 拦截器已提示 */
  }
}

async function onSubmit() {
  loading.value = true
  try {
    await forgotPassword(form)
    showSuccessToast('密码已重置，请重新登录')
    router.replace('/login')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.forgot-page {
  min-height: 100vh;
  background: var(--page-bg);
}
.forgot-actions {
  margin: 30px 20px 0;
}
.forgot-actions :deep(.van-button--primary) {
  height: 52px;
  font-size: 18px;
  font-weight: 600;
}
/* 验证码小按钮加大热区 */
.forgot-page :deep(.van-field__button .van-button) {
  height: 38px;
  padding: 0 14px;
  font-size: 14px;
}
</style>
