<template>
  <div ref="containerRef" class="flex-1 overflow-y-auto px-4 py-6">
    <div class="max-w-3xl mx-auto space-y-1">
      <template v-for="m in messages" :key="m.id">
        <!-- 系统消息 -->
        <div v-if="m.type === 'system'" class="flex items-center gap-3 my-6">
          <div class="flex-1 h-px bg-border"></div>
          <span class="text-xs text-text-muted whitespace-nowrap">{{ m.data.text }}</span>
          <div class="flex-1 h-px bg-border"></div>
        </div>

        <!-- AI 消息 -->
        <AiMessage v-else-if="m.type === 'ai'" :data="m.data" />

        <!-- 用户消息 -->
        <UserMessage v-else-if="m.type === 'user'" :data="m.data" />

        <!-- SSE 评分卡片 — 由父组件插槽处理 -->
        <slot v-else-if="m.type === 'scoreCard'" name="scoreCard" :message="m" />

        <!-- 追问 — 由父组件插槽处理 -->
        <slot v-else-if="m.type === 'followUp'" name="followUp" :message="m" />

        <!-- 复习自评 — 由父组件插槽处理 -->
        <slot v-else-if="m.type === 'review'" name="review" :message="m" />

        <!-- 报告 — 由父组件插槽处理 -->
        <slot v-else-if="m.type === 'report'" name="report" :message="m" />
      </template>

      <!-- SSE 流式加载指示器 -->
      <div v-if="streaming" class="flex items-center gap-1 py-2">
        <span class="w-2 h-2 rounded-full bg-coral animate-bounce" style="animation-delay: 0s"></span>
        <span class="w-2 h-2 rounded-full bg-coral animate-bounce" style="animation-delay: 0.15s"></span>
        <span class="w-2 h-2 rounded-full bg-coral animate-bounce" style="animation-delay: 0.3s"></span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import AiMessage from './AiMessage.vue'
import UserMessage from './UserMessage.vue'

const props = defineProps({
  messages: { type: Array, required: true },
  streaming: { type: Boolean, default: false }
})

const containerRef = ref(null)

// 消息变化时自动滚底
watch(
  () => props.messages.length,
  () => {
    nextTick(() => {
      const el = containerRef.value
      if (el) el.scrollTop = el.scrollHeight
    })
  }
)

// SSE 数据更新时也滚底（监听 messages 深度变化可能频繁，用 streaming 作副 trigger）
watch(
  () => props.streaming,
  () => {
    nextTick(() => {
      const el = containerRef.value
      if (el) el.scrollTop = el.scrollHeight
    })
  }
)
</script>
