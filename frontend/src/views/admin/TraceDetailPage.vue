<template>
  <div class="max-w-4xl mx-auto p-6">
    <router-link to="/admin/traces" class="text-blue-600 hover:underline text-sm mb-4 inline-block">
      ← 返回列表
    </router-link>
    <h1 class="text-xl font-bold mb-2">链路详情</h1>
    <p class="text-sm text-gray-500 mb-6">Trace ID: {{ traceId }}</p>

    <!-- Trace 元信息 -->
    <div v-if="trace" class="bg-white rounded-lg shadow p-4 mb-4 text-sm">
      <div class="grid grid-cols-4 gap-3">
        <div><span class="text-gray-500">入口:</span> {{ trace.entryMethod }}</div>
        <div><span class="text-gray-500">状态:</span>
          <span :class="trace.status==='SUCCESS'?'text-green-600':'text-red-500'">{{ trace.status }}</span>
        </div>
        <div><span class="text-gray-500">总耗时:</span> {{ trace.latencyMs }}ms</div>
        <div><span class="text-gray-500">时间:</span> {{ trace.createdAt?.slice(0,16) }}</div>
      </div>
      <div v-if="trace.errorMsg" class="mt-2 text-red-500 text-xs bg-red-50 p-2 rounded">{{ trace.errorMsg }}</div>
    </div>

    <!-- 节点树 -->
    <div class="bg-white rounded-lg shadow p-5">
      <h3 class="font-semibold mb-4">RAG 管线节点树</h3>
      <div v-for="node in treeNodes" :key="node.nodeName + '-' + node.depth"
           class="border-l-2 border-gray-200"
           :style="{marginLeft: (node.depth||0)*20+'px'}">
        <div class="flex items-center py-2 px-3 hover:bg-gray-50 rounded-r cursor-pointer"
             @click="toggleExpand(node)">
          <span class="mr-2 text-xs">{{ expanded.has(node.nodeName + node.depth) ? '▼' : '▶' }}</span>
          <span class="flex-1 text-sm">{{ node.nodeName }}</span>
          <span :class="node.status==='SUCCESS'?'text-green-600':'text-red-500'"
                class="text-xs mr-3">{{ node.latencyMs }}ms</span>
          <span class="text-xs">{{ node.status==='SUCCESS'?'✓':'✗' }}</span>
        </div>
        <!-- 展开的 extraData -->
        <div v-if="expanded.has(node.nodeName + node.depth) && node.extraData"
             class="ml-8 mb-2 p-2 bg-gray-50 rounded text-xs text-gray-600 font-mono">
          {{ formatExtra(node.extraData) }}
        </div>
      </div>
      <div v-if="!treeNodes.length" class="text-center text-gray-400 py-8">暂无节点数据</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { useRoute } from 'vue-router'
import api from '../../api/index.js'

const route = useRoute()
const traceId = route.params.traceId
const trace = ref(null)
const treeNodes = ref([])
const expanded = reactive(new Set())

onMounted(async () => {
  try {
    const r1 = await api.get('/admin/monitor/traces/'+traceId)
    trace.value = r1.data?.trace
    const r2 = await api.get('/admin/monitor/traces/'+traceId+'/tree')
    treeNodes.value = r2.data || []
    // 默认展开 depth=0 的节点
    treeNodes.value.filter(n=>!n.depth||n.depth===0).forEach(n=>expanded.add(n.nodeName + n.depth))
  } catch(e) {}
})

function toggleExpand(node) {
  const key = node.nodeName + node.depth
  if (expanded.has(key)) expanded.delete(key)
  else expanded.add(key)
}

function formatExtra(json) {
  try { const o = JSON.parse(json); return JSON.stringify(o, null, 2) } catch(e) { return json }
}
</script>
