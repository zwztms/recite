<template>
  <div class="mb-5 bg-white border border-border rounded-xl p-5 shadow-sm max-w-full">
    <h3 class="text-lg font-semibold text-text-primary mb-4">📊 背诵报告</h3>

    <!-- 四栏统计 -->
    <div class="grid grid-cols-4 gap-3 mb-5">
      <div class="text-center p-3 bg-warm rounded-lg">
        <p class="text-2xl font-bold text-coral">{{ data.totalScore || 0 }}</p>
        <p class="text-xs text-text-muted mt-0.5">总分</p>
      </div>
      <div class="text-center p-3 bg-warm rounded-lg">
        <p class="text-2xl font-bold text-text-primary">{{ data.averageScore || '-' }}</p>
        <p class="text-xs text-text-muted mt-0.5">均分</p>
      </div>
      <div class="text-center p-3 bg-warm rounded-lg">
        <p class="text-2xl font-bold text-text-primary">{{ data.totalQuestions || 0 }}</p>
        <p class="text-xs text-text-muted mt-0.5">题数</p>
      </div>
      <div class="text-center p-3 bg-warm rounded-lg">
        <p class="text-2xl font-bold text-text-primary">-</p>
        <p class="text-xs text-text-muted mt-0.5">连续天数</p>
      </div>
    </div>

    <!-- 强项 / 弱项 -->
    <div v-if="data.strengths?.length" class="mb-3">
      <p class="text-sm font-medium text-success-text mb-1">✅ 强项</p>
      <div class="flex flex-wrap gap-1.5">
        <span v-for="(s, i) in data.strengths" :key="'s'+i"
          class="text-xs px-2 py-0.5 rounded-full bg-success-bg text-success-text border border-green-200">
          {{ s }}
        </span>
      </div>
    </div>

    <div v-if="data.weaknesses?.length" class="mb-3">
      <p class="text-sm font-medium text-danger-text mb-1">⚠️ 薄弱</p>
      <div class="flex flex-wrap gap-1.5">
        <span v-for="(w, i) in data.weaknesses" :key="'w'+i"
          class="text-xs px-2 py-0.5 rounded-full bg-danger-bg text-danger-text border border-red-200">
          {{ w }}
        </span>
      </div>
    </div>

    <!-- AI 建议 -->
    <div v-if="data.advice" class="p-3 bg-warm rounded-lg mb-4">
      <p class="text-sm text-text-secondary whitespace-pre-wrap">{{ data.advice }}</p>
    </div>

    <!-- 返回主页 -->
    <div class="text-center">
      <button @click="$emit('goHome')"
        class="px-6 py-2 text-sm rounded-lg bg-coral text-white
               hover:bg-orange-600 transition-colors font-medium">
        返回主页
      </button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  data: { type: Object, required: true }
})
defineEmits(['goHome'])
</script>
