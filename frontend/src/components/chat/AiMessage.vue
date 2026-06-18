<template>
  <div class="flex flex-col mb-5 max-w-full">
    <!-- 标签行：模块 tag + 难度 + 题号 -->
    <div v-if="data.moduleKey || data.questionIndex" class="flex items-center gap-2 mb-2">
      <span v-if="data.moduleKey"
        class="text-xs px-2 py-0.5 rounded-full bg-coral-light text-coral border border-coral-border">
        {{ data.moduleKey }}
      </span>
      <span v-if="data.difficulty"
        class="text-xs text-text-muted">
        {{ '★'.repeat(data.difficulty || 0) }}
      </span>
      <span v-if="data.questionIndex"
        class="text-xs text-text-muted ml-auto">
        第 {{ data.questionIndex }}/{{ data.totalQuestions }} 题
      </span>
    </div>

    <!-- 题目正文（无气泡，纯文本） -->
    <div class="text-text-primary text-base leading-relaxed whitespace-pre-wrap">
      {{ data.text }}
    </div>

    <!-- 复习模式：答案展开区 -->
    <div v-if="data.answer" class="mt-3 p-4 bg-success-bg border border-green-200 rounded-lg">
      <p class="text-sm font-medium text-success-text mb-1">📖 参考答案</p>
      <p class="text-sm text-text-secondary whitespace-pre-wrap">{{ data.answer }}</p>
    </div>
  </div>
</template>

<script setup>
defineProps({
  data: { type: Object, required: true }
})
</script>
