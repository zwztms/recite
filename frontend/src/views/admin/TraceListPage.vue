<template>
  <div class="max-w-6xl mx-auto p-6">
    <h1 class="text-2xl font-bold mb-4">链路追踪</h1>

    <!-- 统计卡片 -->
    <div class="grid grid-cols-4 gap-3 mb-4">
      <div v-for="s in stats" :key="s.label"
           class="bg-white rounded shadow p-4 text-center">
        <div class="text-xl font-bold" :style="{color:s.color}">{{ s.value }}</div>
        <div class="text-xs text-gray-500">{{ s.label }}</div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="flex gap-3 mb-4">
      <select v-model="filterStatus" @change="loadTraces"
              class="border rounded px-3 py-1 text-sm">
        <option value="">全部状态</option>
        <option value="SUCCESS">成功</option>
        <option value="ERROR">失败</option>
      </select>
    </div>

    <!-- 链路列表 -->
    <div class="bg-white rounded-lg shadow">
      <table class="w-full text-sm">
        <thead class="bg-gray-50 border-b">
          <tr>
            <th class="p-2 text-left">Trace ID</th>
            <th class="p-2 text-left">入口方法</th>
            <th class="p-2 text-center">状态</th>
            <th class="p-2 text-right">耗时</th>
            <th class="p-2 text-left">时间</th>
            <th class="p-2"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="t in traces" :key="t.traceId" class="border-b hover:bg-gray-50">
            <td class="p-2 font-mono text-xs">{{ t.traceId?.slice(0,12) }}</td>
            <td class="p-2 text-xs">{{ t.entryMethod }}</td>
            <td class="p-2 text-center">
              <span :class="t.status==='SUCCESS'?'text-green-600':'text-red-500'" class="text-xs font-medium">
                {{ t.status==='SUCCESS'?'✓':'✗' }}
              </span>
            </td>
            <td class="p-2 text-right text-xs">{{ t.latencyMs }}ms</td>
            <td class="p-2 text-xs text-gray-400">{{ t.createdAt?.slice(0,16) }}</td>
            <td class="p-2">
              <router-link :to="'/admin/traces/'+t.traceId"
                           class="text-blue-600 hover:underline text-xs">详情</router-link>
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
    {label:'成功',value:success,color:'#10b981'},
    {label:'失败',value:fail,color:'#ef4444'},
    {label:'成功率',value:all.length?Math.round(success/all.length*100)+'%':'-',color:'#f59e0b'},
    {label:'平均延迟',value:avgLat+'ms',color:'#6366f1'}]
})

onMounted(() => loadTraces())

async function loadTraces() {
  let url = '/admin/monitor/traces'
  if (filterStatus.value) url = '/admin/monitor/traces/filter?status='+filterStatus.value
  try { const r = await api.get(url); traces.value = r.data?.records || [] } catch(e) {}
}
</script>
