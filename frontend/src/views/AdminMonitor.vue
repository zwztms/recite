<template>
  <div class="max-w-4xl mx-auto">
    <h1 class="text-xl font-semibold text-text-primary mb-6">运维监控</h1>

    <!-- 统计卡片 -->
    <div class="grid grid-cols-3 gap-4 mb-6">
      <div class="bg-white rounded-xl border border-border p-4 text-center">
        <p class="text-2xl font-bold text-coral">{{ stats.totalRequests ?? '-' }}</p>
        <p class="text-xs text-text-muted mt-1">今日请求数</p>
      </div>
      <div class="bg-white rounded-xl border border-border p-4 text-center">
        <p class="text-2xl font-bold text-text-primary">{{ stats.avgLatencyMs ?? '-' }}<span class="text-sm text-text-muted">ms</span></p>
        <p class="text-xs text-text-muted mt-1">平均耗时</p>
      </div>
      <div class="bg-white rounded-xl border border-border p-4 text-center">
        <p class="text-2xl font-bold" :class="(stats.errorCount || 0) > 0 ? 'text-red-500' : 'text-success-text'">
          {{ stats.errorCount ?? 0 }}
        </p>
        <p class="text-xs text-text-muted mt-1">异常数</p>
      </div>
    </div>

    <!-- 链路列表 -->
    <div class="bg-white rounded-xl border border-border overflow-hidden">
      <div class="px-4 py-3 border-b border-border flex items-center justify-between">
        <h2 class="text-sm font-medium text-text-primary">链路追踪</h2>
        <button @click="fetchTraces"
          class="text-xs px-3 py-1 rounded-lg border border-border text-text-secondary hover:bg-gray-50 transition-colors">
          刷新
        </button>
      </div>

      <!-- 表头 -->
      <div class="grid grid-cols-[1fr_1fr_80px_80px_140px] gap-2 px-4 py-2 bg-warm text-xs text-text-muted font-medium">
        <span>Trace ID</span>
        <span>入口方法</span>
        <span class="text-center">耗时</span>
        <span class="text-center">状态</span>
        <span class="text-right">时间</span>
      </div>

      <div v-if="loading" class="text-center py-8 text-text-muted text-sm">加载中...</div>
      <div v-else-if="!traces.length" class="text-center py-8 text-text-muted text-sm">暂无链路数据</div>

      <template v-else>
        <div v-for="t in traces" :key="t.traceId">
          <div
            @click="toggleTrace(t)"
            class="grid grid-cols-[1fr_1fr_80px_80px_140px] gap-2 px-4 py-2.5 border-b border-border
                   hover:bg-warm/50 cursor-pointer transition-colors text-sm"
          >
            <span class="font-mono text-xs text-coral truncate">{{ t.traceId }}</span>
            <span class="text-text-secondary truncate">{{ t.entryMethod }}</span>
            <span class="text-center text-text-secondary">{{ t.latencyMs }}ms</span>
            <span class="text-center">
              <span class="text-xs px-1.5 py-0.5 rounded-full font-medium"
                :class="t.status === 'SUCCESS'
                  ? 'bg-success-bg text-success-text'
                  : t.status === 'ERROR'
                    ? 'bg-danger-bg text-danger-text'
                    : 'bg-gray-100 text-text-muted'">
                {{ t.status }}
              </span>
            </span>
            <span class="text-right text-text-muted text-xs">{{ formatTime(t.createdAt) }}</span>
          </div>

          <!-- 展开节点详情 -->
          <div v-if="expandedTraceId === t.traceId" class="border-b border-border bg-warm/30">
            <div v-if="expanding" class="px-4 py-4 text-sm text-text-muted">加载节点...</div>
            <div v-else-if="!nodes.length" class="px-4 py-4 text-sm text-text-muted">无节点数据</div>
            <div v-else class="px-4 py-3 space-y-2">
              <div v-for="(n, i) in nodes" :key="i"
                class="flex items-center gap-3 text-sm">
                <!-- 节点类型标签 -->
                <span class="text-xs px-1.5 py-0.5 rounded font-mono shrink-0 w-16 text-center"
                  :class="nodeTypeStyle(n.nodeType)">
                  {{ n.nodeType }}
                </span>
                <!-- 节点名 -->
                <span class="text-text-secondary flex-1">{{ n.nodeName }}</span>
                <!-- 耗时条 -->
                <div class="w-24 h-2 bg-gray-200 rounded-full overflow-hidden shrink-0">
                  <div class="h-full rounded-full"
                    :class="n.status === 'ERROR' ? 'bg-red-400' : 'bg-coral'"
                    :style="{ width: barWidth(n.latencyMs) }"></div>
                </div>
                <span class="text-xs text-text-muted w-12 text-right">{{ n.latencyMs }}ms</span>
                <span class="text-xs w-12 text-center">
                  <span class="px-1 py-0.5 rounded"
                    :class="n.status === 'ERROR' ? 'text-red-500' : 'text-success-text'">
                    {{ n.status === 'ERROR' ? '✕' : '✓' }}
                  </span>
                </span>
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- 分页 -->
      <div v-if="totalPages > 1" class="px-4 py-3 border-t border-border flex items-center justify-between text-sm">
        <span class="text-text-muted">共 {{ total }} 条</span>
        <div class="flex gap-1">
          <button @click="page--; fetchTraces()" :disabled="page <= 1"
            class="px-3 py-1 rounded border border-border text-text-secondary hover:bg-gray-50 disabled:opacity-30 transition-colors">
            上一页
          </button>
          <span class="px-3 py-1 text-text-muted">{{ page }} / {{ totalPages }}</span>
          <button @click="page++; fetchTraces()" :disabled="page >= totalPages"
            class="px-3 py-1 rounded border border-border text-text-secondary hover:bg-gray-50 disabled:opacity-30 transition-colors">
            下一页
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listTraces, getTraceDetail, getMonitorStats } from '../api'

const stats = ref({})
const traces = ref([])
const total = ref(0)
const page = ref(1)
const totalPages = ref(1)
const loading = ref(false)

const expandedTraceId = ref(null)
const nodes = ref([])
const expanding = ref(false)

async function fetchStats() {
  try {
    const res = await getMonitorStats()
    stats.value = res.data || {}
  } catch (e) { /* 静默 */ }
}

async function fetchTraces() {
  loading.value = true
  try {
    const res = await listTraces(page.value, 10)
    traces.value = res.data?.records || []
    total.value = res.data?.total || 0
    totalPages.value = Math.ceil(total.value / 10) || 1
  } finally {
    loading.value = false
  }
}

async function toggleTrace(t) {
  if (expandedTraceId.value === t.traceId) {
    expandedTraceId.value = null
    nodes.value = []
    return
  }
  expandedTraceId.value = t.traceId
  expanding.value = true
  nodes.value = []
  try {
    const res = await getTraceDetail(t.traceId)
    nodes.value = res.data?.nodes || []
  } catch (e) {
    nodes.value = []
  } finally {
    expanding.value = false
  }
}

function formatTime(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

function barWidth(ms) {
  const max = Math.max(...nodes.value.map(n => n.latencyMs || 0), 1)
  return Math.min((ms / max) * 100, 100) + '%'
}

function nodeTypeStyle(type) {
  const map = {
    LLM: 'bg-purple-50 text-purple-600',
    DB: 'bg-blue-50 text-blue-600',
    CACHE: 'bg-green-50 text-green-600',
    MQ: 'bg-orange-50 text-orange-600',
    AUTH: 'bg-gray-100 text-gray-600',
    VALIDATE: 'bg-yellow-50 text-yellow-600',
    BUSINESS: 'bg-indigo-50 text-indigo-600',
    SSE: 'bg-cyan-50 text-cyan-600'
  }
  return map[type] || 'bg-gray-100 text-text-muted'
}

onMounted(() => {
  fetchStats()
  fetchTraces()
})
</script>
