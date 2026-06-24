<template>
  <div class="max-w-4xl mx-auto p-6">
    <h1 class="text-2xl font-bold mb-6">系统设置</h1>

    <div v-if="settings" class="space-y-4">
      <!-- LLM -->
      <div class="bg-white rounded-lg shadow p-5">
        <h3 class="font-semibold mb-3 text-blue-600">LLM 配置</h3>
        <div class="grid grid-cols-2 gap-2 text-sm">
          <div><span class="text-gray-500">提供商:</span> {{ settings.llm.provider }}</div>
          <div><span class="text-gray-500">模型:</span> {{ settings.llm.model }}</div>
          <div><span class="text-gray-500">URL:</span> {{ settings.llm.url }}</div>
          <div><span class="text-gray-500">Key:</span> {{ settings.llm.keyMasked }}</div>
        </div>
      </div>

      <!-- Embedding -->
      <div class="bg-white rounded-lg shadow p-5">
        <h3 class="font-semibold mb-3 text-green-600">Embedding 配置</h3>
        <div class="grid grid-cols-2 gap-2 text-sm">
          <div><span class="text-gray-500">提供商:</span> {{ settings.embedding.provider }}</div>
          <div><span class="text-gray-500">模型:</span> {{ settings.embedding.model }}</div>
          <div><span class="text-gray-500">维度:</span> {{ settings.embedding.dimension }}</div>
          <div><span class="text-gray-500">Key:</span> {{ settings.embedding.keyMasked }}</div>
        </div>
      </div>

      <!-- RAG 管线 -->
      <div class="bg-white rounded-lg shadow p-5">
        <h3 class="font-semibold mb-3 text-purple-600">RAG 管线状态</h3>
        <div class="grid grid-cols-4 gap-3 text-sm">
          <div v-for="(enabled, name) in settings.ragPipeline" :key="name"
               class="flex items-center gap-2">
            <span :class="enabled?'text-green-600':'text-red-400'">{{ enabled ? '●' : '○' }}</span>
            {{ name }}
          </div>
        </div>
      </div>

      <!-- 并发 + SSE -->
      <div class="grid grid-cols-2 gap-4 text-sm">
        <div class="bg-white rounded-lg shadow p-5">
          <h3 class="font-semibold mb-3 text-orange-600">并发控制</h3>
          <div><span class="text-gray-500">评分槽:</span> {{ settings.concurrency.scoreSlot }}</div>
          <div><span class="text-gray-500">类型:</span> {{ settings.concurrency.type }}</div>
        </div>
        <div class="bg-white rounded-lg shadow p-5">
          <h3 class="font-semibold mb-3 text-teal-600">SSE 配置</h3>
          <div><span class="text-gray-500">超时:</span> {{ settings.sse.timeout }}</div>
          <div><span class="text-gray-500">事件类型:</span> {{ settings.sse.eventTypes }}</div>
        </div>
      </div>
    </div>

    <div v-else class="text-center text-gray-400 py-12">加载中...</div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../../api/index.js'

const settings = ref(null)
onMounted(async () => {
  try { const r = await api.get('/admin/settings'); settings.value = r.data } catch(e) {}
})
</script>
