<template>
  <div class="bg-white/80 backdrop-blur border-b border-border px-4 py-2.5">
    <div class="max-w-3xl mx-auto flex items-center gap-3">
      <!-- 模式标签 -->
      <span class="text-sm font-medium text-coral bg-coral-light px-3 py-1 rounded-full">
        {{ modeLabel }}
      </span>

      <!-- 进度 -->
      <span class="text-xs text-text-muted">
        {{ current }} / {{ total }} 题
      </span>

      <!-- 占位 -->
      <div class="flex-1"></div>

      <!-- 结束按钮（未完成时显示） -->
      <button v-if="!finished" @click="$emit('finish')"
        class="px-3 py-1 text-xs rounded-lg border border-red-200 text-red-500
               hover:bg-red-50 transition-colors">
        结束背诵
      </button>

      <!-- 已完成标记 -->
      <span v-else class="text-xs text-text-muted">✅ 已完成</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  mode: { type: String, default: 'CATEGORY' },
  current: { type: Number, default: 0 },
  total: { type: Number, default: 0 },
  finished: { type: Boolean, default: false }
})

defineEmits(['finish'])

const modeLabel = computed(() => {
  switch (props.mode) {
    case 'CATEGORY': return '模块背诵'
    case 'RANDOM': return '随机背诵'
    case 'REVIEW': return '今日复习'
    default: return props.mode
  }
})
</script>
