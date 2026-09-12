<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <div class="brand-panel">
      <div class="brand-inner">
        <div class="brand-logo">
          <div class="brand-mark">
            <el-icon :size="22"><FirstAidKit /></el-icon>
          </div>
          <span class="brand-name">养老社区管理后台</span>
        </div>
        <h1 class="brand-title">养老社区运营管理系统</h1>
        <p class="brand-desc">面向养老机构的一体化运营管理平台，覆盖会员、健康、体检、活动与权限管理。</p>
        <ul class="brand-points">
          <li>
            <el-icon :size="16"><User /></el-icon>
            <span>会员档案与等级积分管理</span>
          </li>
          <li>
            <el-icon :size="16"><TrendCharts /></el-icon>
            <span>健康记录跟踪与异常预警</span>
          </li>
          <li>
            <el-icon :size="16"><Calendar /></el-icon>
            <span>体检预约与活动运营</span>
          </li>
        </ul>
      </div>
      <div class="brand-footer">© 养老社区运营平台 · 管理端</div>
    </div>

    <!-- 右侧登录表单 -->
    <div class="form-panel">
      <div class="form-box">
        <h2 class="form-title">管理员登录</h2>
        <p class="form-sub">请使用管理员账号登录系统</p>
        <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="submit">
          <el-form-item prop="phone">
            <el-input v-model="form.phone" placeholder="手机号" clearable :prefix-icon="Iphone" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="密码" show-password :prefix-icon="Lock" />
          </el-form-item>
          <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="submit">
            登 录
          </el-button>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Iphone, Lock, FirstAidKit, User, TrendCharts, Calendar } from '@element-plus/icons-vue'
import { useAuthStore } from '../../store/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const formRef = ref()
const loading = ref(false)
const form = reactive({ phone: '', password: '' })
const rules = {
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await auth.login(form.phone, form.password)
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/dashboard')
  } catch (e) {
    // 错误信息已由 axios 拦截器提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
}

/* ===== 左侧品牌区：深墨蓝实底，无渐变 ===== */
.brand-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 48px 56px;
  background: #101d31;
  color: #fff;
}
.brand-logo {
  display: flex;
  align-items: center;
  gap: 10px;
}
.brand-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--ec-radius-md);
  background: var(--ec-primary);
}
.brand-name {
  font-size: 15px;
  font-weight: 600;
}
.brand-inner {
  max-width: 480px;
}
.brand-title {
  margin-top: 96px;
  font-size: 30px;
  font-weight: 600;
  line-height: 1.4;
}
.brand-desc {
  margin-top: 16px;
  font-size: 14px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.65);
}
.brand-points {
  margin-top: 40px;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.brand-points li {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
}
.brand-points .el-icon {
  color: rgba(255, 255, 255, 0.55);
}
.brand-footer {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.35);
}

/* ===== 右侧表单区 ===== */
.form-panel {
  flex: none;
  width: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
}
.form-box {
  width: 340px;
}
.form-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--ec-text-primary);
}
.form-sub {
  margin: 8px 0 28px;
  font-size: 13px;
  color: var(--ec-text-secondary);
}
.login-btn {
  width: 100%;
  margin-top: 4px;
}

/* 窄屏下隐藏品牌区，仅保留表单 */
@media (max-width: 960px) {
  .brand-panel {
    display: none;
  }
  .form-panel {
    flex: 1;
    width: auto;
    background: var(--ec-bg-page);
  }
  .form-box {
    padding: 40px 32px;
    background: #fff;
    border: 1px solid var(--ec-border);
    border-radius: var(--ec-radius-lg);
  }
}
</style>
