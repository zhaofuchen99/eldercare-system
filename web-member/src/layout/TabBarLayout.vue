<template>
  <div class="tabbar-layout">
    <van-nav-bar :title="title" fixed placeholder safe-area-inset-top />
    <div class="tabbar-body">
      <router-view />
    </div>
    <van-tabbar v-model="active" route safe-area-inset-bottom>
      <van-tabbar-item replace to="/home" icon="wap-home-o">首页</van-tabbar-item>
      <van-tabbar-item replace to="/health" icon="chart-trending-o">健康</van-tabbar-item>
      <van-tabbar-item replace to="/activity" icon="flag-o">活动</van-tabbar-item>
      <van-tabbar-item replace to="/mine" icon="friends-o">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const active = computed(() => route.path)
const title = computed(() => route.meta.title || '')
</script>

<style scoped>
.tabbar-layout {
  background: var(--page-bg);
}
.tabbar-layout :deep(.van-tabbar) {
  box-shadow: 0 -1px 8px rgba(70, 65, 55, 0.06);
}
.tabbar-layout :deep(.van-tabbar-item) {
  color: var(--text-sub);
}
.tabbar-layout :deep(.van-tabbar-item--active) {
  font-weight: 600;
}
.tabbar-body {
  min-height: 100vh;
  padding-bottom: 60px;
}
</style>
