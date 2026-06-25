<template>
  <div class="max-w-6xl mx-auto p-6">
    <h1 class="text-2xl font-bold text-text-primary mb-6">仪表盘</h1>

    <!-- KPI 卡片 -->
    <div class="grid grid-cols-4 gap-4 mb-6">
      <div v-for="kpi in kpis" :key="kpi.label"
           class="bg-surface rounded-2xl border border-border p-5 text-center">
        <div class="text-3xl font-bold text-text-primary">{{ kpi.value }}</div>
        <div class="text-sm text-text-secondary mt-1">{{ kpi.label }}</div>
        <div class="text-xs text-text-muted mt-1">{{ kpi.trend }}</div>
      </div>
    </div>

    <div class="grid grid-cols-3 gap-4">
      <!-- 趋势图（简化柱状图） -->
      <div class="col-span-2 bg-surface rounded-2xl border border-border p-5">
        <h3 class="font-semibold text-text-primary mb-4">背诵趋势 (近{{ days }}天)</h3>
        <div class="flex items-end gap-2 h-40">
          <div v-for="(t,i) in trends" :key="i" class="flex-1 flex flex-col items-center">
            <div class="w-full bg-coral rounded-t" :style="{height: barHeight(t.sessions)+'px'}"></div>
            <span class="text-xs text-text-muted mt-1">{{ t.date }}</span>
          </div>
        </div>
      </div>

      <!-- 热门模块 -->
      <div class="bg-surface rounded-2xl border border-border p-5">
        <h3 class="font-semibold text-text-primary mb-4">热门模块</h3>
        <div v-for="(m,i) in overview?.topModules" :key="m.moduleKey"
             class="flex justify-between py-2 border-b border-border last:border-0 text-sm">
          <span class="text-text-secondary">{{ i+1 }}. {{ m.moduleName }}</span>
          <span class="text-text-muted">{{ m.sessionCount }}次</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import api from '../../api/index.js'

const overview = ref(null)
const trends = ref([])
const days = ref(7)

const kpis = computed(() => {
  const o = overview.value
  if (!o) return [{label:'加载中...',value:'-',trend:''}]
  return [
    {label:'总用户',value:o.totalUsers,trend:''},
    {label:'背诵会话',value:o.totalSessions,trend:''},
    {label:'平均评分',value:o.avgScore,trend:'/10'},
    {label:'掌握率',value:o.masteryRate+'%',trend:'≥8分'}]
})

function barHeight(v) {
  const max = Math.max(...trends.value.map(t=>t.sessions), 1)
  return Math.max(8, (v / max) * 140)
}

onMounted(async () => {
  try { const r1 = await api.get('/admin/dashboard/overview'); overview.value = r1.data }
       catch(e){}
  try { const r2 = await api.get('/admin/dashboard/trends?days='+days.value); trends.value = r2.data||[] }
       catch(e){}
})
</script>
