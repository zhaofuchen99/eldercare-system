<template>
  <div class="todo-list">
    <div
      v-for="(item, i) in items"
      :key="i"
      class="todo-item"
      :class="{ clickable: item.route }"
      @click="item.route && $router.push(item.route)"
    >
      <div class="todo-icon" :style="{ color: item.color, background: item.colorBg }">
        <el-icon :size="16"><component :is="item.icon" /></el-icon>
      </div>
      <div class="todo-body">
        <div class="todo-title">
          {{ item.title }}
          <span v-if="item.mock" class="todo-mock">示例</span>
        </div>
        <div class="todo-desc">{{ item.description }}</div>
      </div>
      <el-badge v-if="item.count > 0" :value="item.count" :max="99" />
      <span v-else class="todo-empty">无</span>
      <el-icon v-if="item.route" class="todo-arrow" :size="14"><ArrowRight /></el-icon>
    </div>
  </div>
</template>

<script setup>
import { ArrowRight } from '@element-plus/icons-vue'

defineProps({
  /**
   * items: [{ title, description, icon, color, colorBg, count, route?, mock? }]
   * route 存在时点击跳转；mock 为 true 时标注示例数据
   */
  items: { type: Array, required: true }
})
</script>

<style scoped>
.todo-list {
  display: flex;
  flex-direction: column;
}
.todo-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 12px;
  border-radius: var(--ec-radius-md);
  transition: background 0.2s;
}
.todo-item + .todo-item {
  border-top: 1px dashed var(--ec-border);
}
.todo-item.clickable {
  cursor: pointer;
}
.todo-item.clickable:hover {
  background: var(--ec-bg-page);
}
.todo-icon {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 8px;
}
.todo-body {
  flex: 1;
  min-width: 0;
}
.todo-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--ec-text-primary);
}
.todo-mock {
  margin-left: 6px;
  padding: 0 4px;
  font-size: 11px;
  font-weight: 400;
  color: var(--ec-text-placeholder);
  border: 1px solid var(--ec-border);
  border-radius: var(--ec-radius-sm);
}
.todo-desc {
  margin-top: 2px;
  font-size: 12px;
  color: var(--ec-text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.todo-empty {
  font-size: 12px;
  color: var(--ec-text-placeholder);
}
.todo-arrow {
  color: var(--ec-text-placeholder);
}
</style>
