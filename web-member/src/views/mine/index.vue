<template>
  <div class="mine-page">
    <!-- 个人资料卡 -->
    <div class="card user-card" @click="go('/profile')">
      <van-image round width="58" height="58" :src="auth.userInfo?.avatar || ''">
        <template #error>
          <div class="avatar-fallback">🧓</div>
        </template>
      </van-image>
      <div class="user-info">
        <div class="user-name">{{ auth.realName || auth.phone }}</div>
        <div class="user-sub">{{ auth.phone }} · {{ enumText('memberLevel', auth.memberLevel) }}会员</div>
      </div>
      <van-icon name="arrow" class="user-arrow" />
    </div>

    <!-- 健康服务 -->
    <van-cell-group inset title="健康服务">
      <van-cell icon="bar-chart-o" title="健康档案" is-link @click="go('/health')" />
      <van-cell icon="todo-list-o" title="健康评测" is-link @click="go('/assessment')" />
      <van-cell icon="shop-o" title="体检预约" is-link @click="go('/appointment')" />
      <van-cell icon="calendar-o" title="我的预约" is-link @click="go('/appointment/mine')" />
      <van-cell icon="chat-o" title="健康咨询" is-link @click="go('/chat')" />
    </van-cell-group>

    <!-- 活动与消息 -->
    <van-cell-group inset title="活动与消息">
      <van-cell icon="flag-o" title="我的活动" is-link @click="go('/activity')" />
      <van-cell icon="volume-o" title="消息中心" is-link @click="go('/message')">
        <template #right-icon>
          <van-badge v-if="unread > 0" :content="unread > 99 ? '99+' : unread" />
          <van-icon name="arrow" class="cell-arrow" />
        </template>
      </van-cell>
      <van-cell icon="gem-o" title="积分明细" is-link @click="go('/points')">
        <template #value>
          <span class="points-num">{{ auth.points }}</span>
        </template>
      </van-cell>
    </van-cell-group>

    <!-- 设置 -->
    <van-cell-group inset title="设置">
      <van-cell icon="manager-o" title="个人信息" is-link @click="go('/profile')" />
      <van-cell icon="lock" title="修改密码" is-link @click="go('/password')" />
    </van-cell-group>

    <div class="logout-wrap">
      <van-button round block plain type="danger" :loading="logoutLoading" @click="doLogout">退出登录</van-button>
    </div>

    <div class="empty-pad" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showSuccessToast } from 'vant'
import { logout } from '../../api/auth'
import { getUnreadCount } from '../../api/message'
import { useAuthStore } from '../../store/auth'
import { enumText } from '../../utils/format'

const router = useRouter()
const auth = useAuthStore()
const unread = ref(0)
const logoutLoading = ref(false)

function go(path) {
  router.push(path)
}

async function doLogout() {
  try {
    await showConfirmDialog({ title: '提示', message: '确定要退出登录吗？' })
  } catch {
    return
  }
  logoutLoading.value = true
  try {
    if (auth.refreshToken) {
      try {
        await logout(auth.refreshToken)
      } catch {
        /* 登出接口失败不影响本地清除 */
      }
    }
    auth.clear()
    showSuccessToast('已退出登录')
    router.replace('/login')
  } finally {
    logoutLoading.value = false
  }
}

onMounted(async () => {
  try {
    const d = await getUnreadCount()
    unread.value = d.unread || 0
  } catch {
    /* 忽略 */
  }
})
</script>

<style scoped>
.mine-page {
  background: var(--page-bg);
  min-height: 100vh;
}
/* 个人资料卡：白底清爽，点击进入个人信息 */
.user-card {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 14px;
  padding: 20px 16px;
  cursor: pointer;
}
.user-card:active {
  background: #f5f3ee;
}
.avatar-fallback {
  width: 58px;
  height: 58px;
  border-radius: 50%;
  background: var(--brand-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
}
.user-info {
  flex: 1;
  min-width: 0;
}
.user-name {
  font-size: 21px;
  font-weight: 700;
  color: var(--text-main);
}
.user-sub {
  margin-top: 6px;
  font-size: 14px;
  color: var(--text-sub);
}
.user-arrow {
  font-size: 18px;
  color: var(--text-dim);
}
/* 分组菜单卡片 */
.mine-page :deep(.van-cell-group--inset) {
  box-shadow: var(--card-shadow);
  margin-top: 14px;
}
.mine-page :deep(.van-cell__title) {
  font-size: 16px;
}
.points-num {
  color: var(--warm-deep);
  font-size: 20px;
  font-weight: 700;
}
.cell-arrow {
  margin-left: 6px;
  color: var(--text-dim);
}
.logout-wrap {
  margin: 28px 20px;
}
.logout-wrap :deep(.van-button) {
  height: 50px;
  font-size: 17px;
  font-weight: 600;
}
</style>
