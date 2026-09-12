<template>
  <div class="stat-card" :class="{ clickable: to }" @click="to && $router.push(to)">
    <div class="stat-head">
      <span class="stat-title">{{ title }}</span>
      <el-icon class="stat-icon" :size="18"><component :is="icon" /></el-icon>
    </div>
    <div class="stat-value">{{ value }}</div>
    <div class="stat-desc">{{ description }}</div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  value: { type: Number, default: 0 },
  icon: { type: String, default: 'DataAnalysis' },
  description: { type: String, default: '' },
  /** 点击跳转的路由路径，可选 */
  to: { type: String, default: '' }
})
</script>

<style scoped>
.stat-card {
  height: 100%;
  padding: 18px 20px;
  background: #fff;
  border: 1px solid var(--ec-border);
  border-radius: var(--ec-radius-lg);
  box-shadow: var(--ec-shadow-card);
}
.stat-card.clickable {
  cursor: pointer;
  transition: border-color 0.2s;
}
.stat-card.clickable:hover {
  border-color: var(--ec-primary-light);
}
.stat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.stat-title {
  font-size: 13px;
  color: var(--ec-text-secondary);
}
.stat-icon {
  color: var(--ec-text-placeholder);
}
.stat-value {
  margin-top: 10px;
  font-size: 28px;
  font-weight: 600;
  line-height: 1;
  color: var(--ec-text-primary);
  font-variant-numeric: tabular-nums;
}
.stat-desc {
  margin-top: 10px;
  font-size: 12px;
  color: var(--ec-text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
