<template>
  <el-timeline class="recent-timeline">
    <el-timeline-item
      v-for="(item, i) in items"
      :key="i"
      :color="i === 0 ? 'var(--ec-primary)' : '#c9cdd4'"
      :hollow="i !== 0"
      :timestamp="item.time"
      placement="top"
    >
      <div class="activity-item">
        <span class="activity-text">{{ item.text }}</span>
        <el-tag v-if="item.tag" size="small" :type="item.tagType || 'info'">
          {{ item.tag }}
        </el-tag>
      </div>
    </el-timeline-item>
  </el-timeline>
</template>

<script setup>
defineProps({
  /** items: [{ text, time, tag?, tagType? }] */
  items: { type: Array, required: true }
})
</script>

<style scoped>
.recent-timeline {
  padding-top: 4px;
}
.recent-timeline :deep(.el-timeline-item__timestamp) {
  font-size: 12px;
  color: var(--ec-text-secondary);
}
.recent-timeline :deep(.el-timeline-item) {
  padding-bottom: 18px;
}
.activity-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.activity-text {
  font-size: 13px;
  color: var(--ec-text-regular);
}
</style>
