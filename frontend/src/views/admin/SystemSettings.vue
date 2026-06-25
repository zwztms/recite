<template>
  <div class="max-w-4xl mx-auto p-6">
    <h1 class="text-2xl font-bold text-text-primary mb-6">系统设置</h1>

    <div v-if="settings" class="space-y-4">
      <!-- LLM -->
      <div class="bg-surface rounded-2xl border border-border p-5">
        <h3 class="font-semibold mb-3 text-coral">LLM 配置</h3>
        <div class="grid grid-cols-2 gap-2 text-sm">
          <div><span class="text-text-muted">提供商:</span> <span class="text-text-primary">{{ settings.llm.provider }}</span></div>
          <div><span class="text-text-muted">模型:</span> <span class="text-text-primary">{{ settings.llm.model }}</span></div>
          <div><span class="text-text-muted">URL:</span> <span class="text-text-primary">{{ settings.llm.url }}</span></div>
          <div><span class="text-text-muted">Key:</span> <span class="text-text-primary">{{ settings.llm.keyMasked }}</span></div>
        </div>
      </div>

      <!-- Embedding -->
      <div class="bg-surface rounded-2xl border border-border p-5">
        <h3 class="font-semibold mb-3 text-coral">Embedding 配置</h3>
        <div class="grid grid-cols-2 gap-2 text-sm">
          <div><span class="text-text-muted">提供商:</span> <span class="text-text-primary">{{ settings.embedding.provider }}</span></div>
          <div><span class="text-text-muted">模型:</span> <span class="text-text-primary">{{ settings.embedding.model }}</span></div>
          <div><span class="text-text-muted">维度:</span> <span class="text-text-primary">{{ settings.embedding.dimension }}</span></div>
          <div><span class="text-text-muted">Key:</span> <span class="text-text-primary">{{ settings.embedding.keyMasked }}</span></div>
        </div>
      </div>

      <!-- RAG 管线 -->
      <div class="bg-surface rounded-2xl border border-border p-5">
        <h3 class="font-semibold mb-3 text-coral">RAG 管线状态</h3>
        <div class="grid grid-cols-4 gap-3 text-sm">
          <div v-for="(enabled, name) in settings.ragPipeline" :key="name"
               class="flex items-center gap-2">
            <span :class="enabled?'text-green-600':'text-text-muted'">{{ enabled ? '●' : '○' }}</span>
            <span class="text-text-primary">{{ name }}</span>
          </div>
        </div>
      </div>

      <!-- 并发 + SSE -->
      <div class="grid grid-cols-2 gap-4 text-sm">
        <div class="bg-surface rounded-2xl border border-border p-5">
          <h3 class="font-semibold mb-3 text-coral">并发控制</h3>
          <div><span class="text-text-muted">评分槽:</span> <span class="text-text-primary">{{ settings.concurrency.scoreSlot }}</span></div>
          <div><span class="text-text-muted">类型:</span> <span class="text-text-primary">{{ settings.concurrency.type }}</span></div>
        </div>
        <div class="bg-surface rounded-2xl border border-border p-5">
          <h3 class="font-semibold mb-3 text-coral">SSE 配置</h3>
          <div><span class="text-text-muted">超时:</span> <span class="text-text-primary">{{ settings.sse.timeout }}</span></div>
          <div><span class="text-text-muted">事件类型:</span> <span class="text-text-primary">{{ settings.sse.eventTypes }}</span></div>
        </div>
      </div>
    </div>

    <div v-else class="text-center text-text-muted py-12">加载中...</div>
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
