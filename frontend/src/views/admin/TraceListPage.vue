<template>
  <div class="max-w-6xl mx-auto p-6">
    <h1 class="text-2xl font-bold text-text-primary mb-4">链路追踪</h1>

    <!-- 统计卡片 -->
    <div class="grid grid-cols-4 gap-3 mb-4">
      <div v-for="s in stats" :key="s.label"
           class="bg-surface rounded-2xl border border-border p-4 text-center">
        <div class="text-xl font-bold text-text-primary">{{ s.value }}</div>
        <div class="text-xs text-text-secondary">{{ s.label }}</div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="flex gap-3 mb-4">
      <select v-model="filterStatus" @change="loadTraces"
              class="border border-border rounded-lg px-3 py-1.5 text-sm bg-warm text-text-primary focus:outline-none focus:border-coral">
        <option value="">全部状态</option>
        <option value="SUCCESS">成功</option>
        <option value="ERROR">失败</option>
      </select>
    </div>

    <!-- 链路列表 -->
    <div class="bg-surface rounded-2xl border border-border overflow-hidden">
      <table class="w-full text-sm">
        <thead>
          <tr class="border-b border-border bg-warm">
            <th class="text-left px-4 py-3 text-text-secondary font-medium">Trace ID</th>
            <th class="text-left px-4 py-3 text-text-secondary font-medium">入口方法</th>
            <th class="text-center px-4 py-3 text-text-secondary font-medium">状态</th>
            <th class="text-right px-4 py-3 text-text-secondary font-medium">耗时</th>
            <th class="text-left px-4 py-3 text-text-secondary font-medium">时间</th>
            <th class="px-4 py-3"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="t in traces" :key="t.traceId" class="border-b border-border last:border-0 hover:bg-warm/50 transition-colors">
            <td class="px-4 py-3 font-mono text-xs text-text-primary">{{ t.traceId?.slice(0,12) }}</td>
            <td class="px-4 py-3 text-xs text-text-primary">{{ t.entryMethod }}</td>
            <td class="px-4 py-3 text-center">
              <span :class="t.status==='SUCCESS'?'text-green-600':'text-red-500'" class="text-xs font-medium">
                {{ t.status==='SUCCESS'?'✓':'✗' }}
              </span>
            </td>
            <td class="px-4 py-3 text-right text-xs text-text-muted">{{ t.latencyMs }}ms</td>
            <td class="px-4 py-3 text-xs text-text-muted">{{ t.createdAt?.slice(0,16) }}</td>
            <td class="px-4 py-3">
              <router-link :to="'/admin/traces/'+t.traceId"
                           class="text-coral hover:underline text-xs">详情</router-link>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../../api/index.js'

const traces = ref([])
const filterStatus = ref('')

const stats = computed(() => {
  const all = traces.value
  const success = all.filter(t=>t.status==='SUCCESS').length
  const fail = all.filter(t=>t.status==='ERROR').length
  const avgLat = all.length ? Math.round(all.reduce((s,t)=>s+(t.latencyMs||0),0)/all.length) : 0
  return [
    {label:'成功',value:success},
    {label:'失败',value:fail},
    {label:'成功率',value:all.length?Math.round(success/all.length*100)+'%':'-'},
    {label:'平均延迟',value:avgLat+'ms'}]
})

onMounted(() => loadTraces())

async function loadTraces() {
  let url = '/admin/monitor/traces'
  if (filterStatus.value) url = '/admin/monitor/traces/filter?status='+filterStatus.value
  try { const r = await api.get(url); traces.value = r.data?.records || [] } catch(e) {}
}
</script>
