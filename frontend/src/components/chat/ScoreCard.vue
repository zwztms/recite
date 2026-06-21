<template>
  <div class="mb-5 bg-white border border-border rounded-xl p-5 shadow-sm max-w-full">
    <!-- 阶段 1: 评分中 -->
    <div v-if="data.score == null && !data.done && !data.error" class="flex items-center gap-3 text-text-muted">
      <span class="w-2 h-2 rounded-full bg-coral animate-pulse"></span>
      <span class="text-sm">评分中...</span>
    </div>

    <!-- 阶段 2: 分数圆环 -->
    <div v-if="data.score != null" class="flex items-center gap-4 mb-4">
      <div class="shrink-0 w-16 h-16 rounded-full flex items-center justify-center text-2xl font-bold"
        :class="scoreColor">
        {{ data.score }}
        <span class="text-xs font-normal">/10</span>
      </div>
      <div>
        <p class="text-sm font-medium" :class="scoreTextColor">{{ scoreLabel }}</p>
      </div>
    </div>

    <!-- 阶段 3: 正确要点 -->
    <div v-if="data.corrects.length" class="mb-3">
      <p class="text-sm font-medium text-success-text mb-1.5">✅ 正确</p>
      <ul class="space-y-0.5">
        <li v-for="(p, i) in data.corrects" :key="'c'+i"
          class="text-sm text-text-secondary pl-4 relative
                 before:content-['·'] before:absolute before:left-1 before:text-success-text">
          {{ p }}
        </li>
      </ul>
    </div>

    <!-- 阶段 4: 遗漏 -->
    <div v-if="data.missed.length" class="mb-3">
      <p class="text-sm font-medium text-danger-text mb-1.5">❌ 遗漏</p>
      <ul class="space-y-0.5">
        <li v-for="(p, i) in data.missed" :key="'m'+i"
          class="text-sm text-text-secondary pl-4 relative
                 before:content-['·'] before:absolute before:left-1 before:text-danger-text">
          {{ p }}
        </li>
      </ul>
    </div>

    <!-- 阶段 5: 建议 -->
    <div v-if="data.suggestion" class="mb-3 p-3 bg-warning-bg border border-amber-200 rounded-lg">
      <p class="text-sm text-warning-text">💡 {{ data.suggestion }}</p>
    </div>

    <!-- 阶段 6: 追问 -->
    <div v-if="data.followUpQuestion && !data.done" class="p-3 bg-warning-bg border border-amber-200 rounded-lg">
      <p class="text-sm text-warning-text mb-2">💬 追问：{{ data.followUpQuestion }}</p>
      <div class="flex gap-2">
        <button @click="$emit('acceptFollowUp')"
          class="px-4 py-1.5 text-xs rounded-lg bg-coral text-white
                 hover:bg-orange-600 transition-colors font-medium">
          接受
        </button>
        <button @click="$emit('skipFollowUp')"
          class="px-4 py-1.5 text-xs rounded-lg border border-border text-text-secondary
                 hover:bg-gray-50 transition-colors">
          跳过
        </button>
      </div>
    </div>

    <!-- 追问已完成 -->
    <div v-if="data.followUpQuestion && data.done" class="p-3 bg-gray-50 border border-border rounded-lg">
      <p class="text-sm text-text-muted">💬 追问已跳过：{{ data.followUpQuestion }}</p>
    </div>

    <!-- 阶段 7: 评分出错 → 重试/跳过 -->
    <div v-if="data.error" class="p-3 bg-red-50 border border-red-200 rounded-lg mb-3">
      <p class="text-sm text-red-700 mb-3">⚠️ {{ data.suggestion || '评分出错' }}</p>
      <div class="flex gap-2">
        <button @click="$emit('retry')"
          class="flex-1 px-4 py-1.5 text-sm rounded-lg bg-coral text-white
                 hover:bg-orange-600 transition-colors font-medium">
          重试
        </button>
        <button @click="$emit('skipQuestion')"
          class="flex-1 px-4 py-1.5 text-sm rounded-lg border border-border text-text-secondary
                 hover:bg-gray-50 transition-colors">
          跳过此题
        </button>
      </div>
    </div>

    <!-- 完成标记 + 下一题按钮 -->
    <div v-if="data.done" class="mt-4 pt-3 border-t border-border flex items-center justify-end">
      <button @click="$emit('nextQuestion')"
        class="px-5 py-2 text-sm rounded-lg bg-coral text-white
               hover:bg-orange-600 transition-colors font-medium">
        下一题 →
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  data: { type: Object, required: true }
})

defineEmits(['acceptFollowUp', 'skipFollowUp', 'nextQuestion'])

const scoreColor = computed(() => {
  const s = props.data.score
  if (s == null) return 'bg-gray-100 text-text-muted'
  if (s >= 8) return 'bg-success-bg text-success-text border-2 border-green-300'
  if (s >= 5) return 'bg-warning-bg text-warning-text border-2 border-amber-300'
  return 'bg-danger-bg text-danger-text border-2 border-red-300'
})

const scoreTextColor = computed(() => {
  const s = props.data.score
  if (s == null) return 'text-text-muted'
  if (s >= 8) return 'text-success-text'
  if (s >= 5) return 'text-warning-text'
  return 'text-danger-text'
})

const scoreLabel = computed(() => {
  const s = props.data.score
  if (s == null) return ''
  if (s >= 9) return '优秀！'
  if (s >= 7) return '不错'
  if (s >= 5) return '还行，继续加油'
  return '需要加强'
})
</script>
