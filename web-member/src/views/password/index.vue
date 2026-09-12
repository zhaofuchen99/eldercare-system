<template>
  <div class="password-page">
    <van-nav-bar title="修改密码" left-arrow @click-left="$router.back()" fixed placeholder safe-area-inset-top />

    <van-form @submit="onSubmit">
      <van-cell-group inset>
        <van-field
          v-model="form.oldPassword"
          type="password"
          label="原密码"
          placeholder="请输入原密码"
          :rules="[{ required: true, message: '请输入原密码' }]"
        />
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
        <van-field
          v-model="confirmPassword"
          type="password"
          label="确认密码"
          placeholder="再次输入新密码"
          :rules="[
            { required: true, message: '请再次输入新密码' },
            { validator: (v) => v === form.newPassword, message: '两次输入不一致' }
          ]"
        />
      </van-cell-group>

      <div class="save-wrap">
        <van-button round block type="primary" native-type="submit" :loading="submitting">确认修改</van-button>
      </div>
      <div class="tip">修改成功后需使用新密码重新登录</div>
    </van-form>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast } from 'vant'
import { changePassword } from '../../api/auth'
import { useAuthStore } from '../../store/auth'

const router = useRouter()
const auth = useAuthStore()
const submitting = ref(false)
const confirmPassword = ref('')
const form = reactive({ oldPassword: '', newPassword: '' })

async function onSubmit() {
  submitting.value = true
  try {
    await changePassword({ oldPassword: form.oldPassword, newPassword: form.newPassword })
    showSuccessToast('密码已修改，请重新登录')
    auth.clear()
    router.replace('/login')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.password-page {
  background: var(--page-bg);
  min-height: 100vh;
}
.password-page :deep(.van-cell-group--inset) {
  margin-top: 14px;
  box-shadow: var(--card-shadow);
}
.save-wrap {
  margin: 24px 20px 14px;
}
.save-wrap :deep(.van-button) {
  height: 52px;
  font-size: 18px;
  font-weight: 600;
}
.tip {
  text-align: center;
  font-size: 15px;
  color: var(--text-sub);
}
</style>
