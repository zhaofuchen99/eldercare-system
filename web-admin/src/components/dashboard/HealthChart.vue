<template>
  <div ref="chartRef" class="health-chart" :style="{ height }"></div>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

// 按需注册所需图表类型，控制包体
echarts.use([
  LineChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  CanvasRenderer
])

const props = defineProps({
  /** ECharts option，由页面侧构建 */
  option: { type: Object, required: true },
  height: { type: String, default: '280px' }
})

const chartRef = ref(null)
let chart = null
let resizeObserver = null

onMounted(() => {
  chart = echarts.init(chartRef.value)
  chart.setOption(props.option)
  // 容器尺寸变化（侧栏折叠等）时自适应
  resizeObserver = new ResizeObserver(() => chart?.resize())
  resizeObserver.observe(chartRef.value)
})

watch(
  () => props.option,
  (opt) => chart?.setOption(opt, true),
  { deep: true }
)

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.health-chart {
  width: 100%;
}
</style>
