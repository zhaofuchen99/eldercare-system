<template>
  <div class="book-page">
    <van-nav-bar title="选择时段" left-arrow @click-left="$router.back()" fixed placeholder safe-area-inset-top />

    <div class="page-content" v-if="pkgName">
      <div class="book-head">
        <div class="book-name">{{ pkgName }}</div>
        <div class="book-tip">选择日期与时段后提交预约，将按套餐积分价扣减积分</div>
      </div>

      <!-- 日期选择 -->
      <div class="date-bar">
        <div
          v-for="d in days"
          :key="d.date"
          class="date-chip"
          :class="{ active: d.date === selectedDate }"
          @click="selectDate(d.date)"
        >
          <div class="date-week">{{ d.week }}</div>
          <div class="date-day">{{ d.day }}</div>
        </div>
      </div>

      <!-- 时段列表 -->
      <van-empty v-if="!slotLoading && !slots.length" description="该日期暂无可用时段，请换一天试试" image-size="80" />
      <van-skeleton :row="3" v-if="slotLoading" />
      <div v-for="s in slots" :key="s.id" class="slot-card" :class="{ active: selectedSlot?.id === s.id }" @click="selectedSlot = s">
        <div class="slot-time">{{ s.timeRange }}</div>
        <div class="slot-left" :class="{ full: s.currentCount >= s.maxCount }">
          {{ s.currentCount >= s.maxCount ? '已约满' : `余 ${s.maxCount - s.currentCount} 名额` }}
        </div>
        <div class="slot-status">
          <van-tag :type="s.status === 'AVAILABLE' ? 'success' : 'default'" round>
            {{ s.status === 'AVAILABLE' ? '可预约' : '不可约' }}
          </van-tag>
        </div>
      </div>
    </div>

    <!-- 底部提交栏 -->
    <van-submit-bar
      v-if="pkgName"
      :price="priceCent"
      button-text="提交预约"
      :disabled="!selectedSlot"
      :loading="submitting"
      @submit="submit"
    >
      <span class="submit-label">{{ selectedSlot ? `已选 ${selectedSlot.timeRange}` : '请选择时段' }}</span>
    </van-submit-bar>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showSuccessToast } from 'vant'
import { getPackages, getSlots, createAppointment } from '../../api/appointment'

const route = useRoute()
const router = useRouter()
const pkgName = ref(route.query.name || '')
const pkgPrice = ref(0)
const selectedDate = ref('')
const selectedSlot = ref(null)
const slots = ref([])
const slotLoading = ref(false)
const submitting = ref(false)

// 未来 14 天
const days = computed(() => {
  const weekNames = ['日', '一', '二', '三', '四', '五', '六']
  const arr = []
  const pad = (n) => String(n).padStart(2, '0')
  for (let i = 0; i < 14; i++) {
    const d = new Date()
    d.setDate(d.getDate() + i)
    arr.push({
      date: `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`,
      week: `周${weekNames[d.getDay()]}`,
      day: `${d.getMonth() + 1}/${d.getDate()}`
    })
  }
  return arr
})

// Vant 金额单位是分
const priceCent = computed(() => pkgPrice.value * 100)

async function loadPackagePrice() {
  try {
    const pkgs = await getPackages()
    const p = pkgs.find((x) => String(x.id) === String(route.params.id))
    if (p) {
      pkgName.value = p.name
      pkgPrice.value = p.price || 0
    }
  } catch {
    /* 忽略 */
  }
}

async function selectDate(date) {
  selectedDate.value = date
  selectedSlot.value = null
  slotLoading.value = true
  try {
    slots.value = await getSlots(route.params.id, date)
  } finally {
    slotLoading.value = false
  }
}

async function submit() {
  submitting.value = true
  try {
    const id = await createAppointment(selectedSlot.value.id)
    showSuccessToast('预约成功')
    router.replace('/appointment/mine')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadPackagePrice()
  if (days.value.length) {
    selectDate(days.value[0].date)
  }
})
</script>

<style scoped>
.book-page {
  background: var(--page-bg);
  min-height: 100vh;
}
.book-head {
  padding: 6px 4px 16px;
}
.book-name {
  font-size: 20px;
  font-weight: 700;
  line-height: 1.4;
}
.book-tip {
  margin-top: 8px;
  font-size: 15px;
  color: var(--text-sub);
  line-height: 1.6;
}
/* 日期横条 */
.date-bar {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding: 4px 0 14px;
  -webkit-overflow-scrolling: touch;
}
.date-chip {
  flex-shrink: 0;
  width: 64px;
  padding: 10px 0;
  text-align: center;
  background: var(--card-bg);
  border-radius: 12px;
  border: 1.5px solid transparent;
  color: var(--text-sub);
  box-shadow: var(--card-shadow);
}
.date-chip.active {
  background: var(--brand-color);
  color: #fff;
  font-weight: 600;
}
.date-week {
  font-size: 13px;
}
.date-day {
  margin-top: 5px;
  font-size: 16px;
  font-weight: 600;
}
/* 时段卡 */
.slot-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--card-bg);
  border-radius: var(--radius-card);
  padding: 16px;
  margin-bottom: 12px;
  border: 1.5px solid transparent;
  box-shadow: var(--card-shadow);
}
.slot-card.active {
  border-color: var(--brand-color);
  background: var(--brand-bg);
}
.slot-time {
  font-size: 17px;
  font-weight: 600;
}
.slot-left {
  font-size: 15px;
  color: var(--good-color);
  flex: 1;
  margin: 0 12px;
}
.slot-left.full {
  color: var(--bad-color);
}
.submit-label {
  color: var(--text-main);
  font-size: 15px;
  font-weight: 500;
}
</style>
