<template>
  <div class="w-44 shrink-0 bg-surface rounded-xl border border-border overflow-hidden flex flex-col h-full">
    <div class="px-3 py-2.5 border-b border-border text-xs font-semibold text-text-primary">题目索引</div>
    <div class="flex-1 overflow-y-auto p-1" ref="listRef">
      <div v-for="(q, i) in questions" :key="q.id"
        @click="$emit('select', q.id)"
        :class="['flex items-center gap-2 px-2.5 py-1.5 rounded-md cursor-pointer text-xs transition-colors',
          activeId === q.id ? 'bg-orange-50 border border-orange-200' : 'hover:bg-warm']">
        <span :class="activeId === q.id ? 'text-coral font-semibold' : 'text-text-muted'" class="w-5">{{ i + 1 }}</span>
        <span class="flex-1 truncate" :class="activeId === q.id ? 'text-text-primary font-semibold' : 'text-text-secondary'">{{ q.question }}</span>
        <span class="w-1.5 h-1.5 rounded-full shrink-0"
          :class="q.mastered ? 'bg-green-500' : 'bg-gray-300'"
          :title="q.mastered ? '已掌握' : '未掌握'"></span>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  questions: { type: Array, required: true },
  activeId: { type: String, default: '' }
})
defineEmits(['select'])
</script>
