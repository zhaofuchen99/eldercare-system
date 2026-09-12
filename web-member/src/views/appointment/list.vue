<template>
  <div class="appt-list-page">
    <van-nav-bar title="体检预约" left-arrow @click-left="$router.back()" fixed placeholder safe-area-inset-top />
    <div class="page-content">
      <van-notice-bar
        left-icon="info-o"
        text="预约需消耗积分（按套餐积分价 FIFO 扣减），取消预约将自动退还"
      />
      <van-cell title="我的预约" is-link icon="calendar-o" @click="goMine" class="mine-link" />

      <van-skeleton title :row="3" v-if="loading" />
      <van-empty v-else-if="!list.length" description="暂无可预约套餐" image-size="80" />

      <div v-for="p in list" :key="p.id" class="pkg-card" @click="goBook(p)">
        <div class="pkg-head">
          <div class="pkg-name">{{ p.name }}</div>
          <div class="pkg-price"><span class="price-icon">🪙</span> {{ p.price }}</div>
        </div>
        <div v-if="p.suitablePeople" class="pkg-line">适合：{{ p.suitablePeople }}</div>
        <div v-if="p.description" class="pkg-line dim">{{ p.description }}</div>
        <div v-if="p.items && p.items.length" class="pkg-items">
          <van-tag v-for="(it, i) in p.items" :key="i" round plain color="#2e7d5f" class="pkg-tag">{{ it }}</van-tag>
        </div>
        <div class="pkg-footer">
          <van-button size="small" round type="primary">选择时段预约</van-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getPackages } from '../../api/appointment'

const router = useRouter()
const loading = ref(false)
const list = ref([])

function goBook(p) {
  router.push({ path: `/appointment/book/${p.id}`, query: { name: p.name } })
}

function goMine() {
  router.push('/appointment/mine')
}

onMounted(async () => {
  loading.value = true
  try {
    list.value = await getPackages()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.appt-list-page {
  background: var(--page-bg);
  min-height: 100vh;
}
.appt-list-page :deep(.van-notice-bar) {
  border-radius: 10px;
}
.mine-link {
  margin: 12px 0;
  border-radius: 10px;
  background: var(--card-bg);
  box-shadow: var(--card-shadow);
  overflow: hidden;
}
.mine-link :deep(.van-cell__title) {
  font-size: 16px;
  font-weight: 600;
}
/* noinspection CssUnusedSymbol */ /* van-icon 为 Vant 组件内部渲染节点，:deep 运行时生效 */
.mine-link :deep(.van-icon) {
  color: var(--brand-deep);
  font-size: 20px;
}
.pkg-card {
  background: var(--card-bg);
  border-radius: var(--radius-card);
  padding: 18px 16px;
  margin-bottom: 12px;
  box-shadow: var(--card-shadow);
}
.pkg-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.pkg-name {
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
}
.pkg-price {
  color: var(--warm-deep);
  font-size: 20px;
  font-weight: 700;
  flex-shrink: 0;
  margin-left: 10px;
}
.price-icon {
  font-size: 16px;
}
.pkg-line {
  margin-top: 8px;
  font-size: 15px;
  color: var(--text-sub);
  line-height: 1.7;
}
.pkg-line.dim {
  color: var(--text-dim);
  font-size: 14px;
}
.pkg-items {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.pkg-tag {
  padding: 3px 10px;
  font-size: 14px;
}
.pkg-footer {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
.pkg-footer :deep(.van-button) {
  height: 42px;
  min-width: 132px;
  font-size: 15px;
  font-weight: 600;
}
</style>
