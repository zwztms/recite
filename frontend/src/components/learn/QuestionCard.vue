<template>
  <div :class="['bg-surface rounded-xl border transition-all',
    expanded ? 'border-coral/30 shadow-sm' : 'border-border hover:border-coral/20']"
    :id="`q-${question.id}`">
    <!-- 题目行 -->
    <div @click="toggle" class="flex items-center gap-3 px-4 py-3 cursor-pointer select-none">
      <span :class="['w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold shrink-0',
        question.mastered ? 'bg-green-500 text-white' : 'bg-gray-200 text-text-muted']">
        {{ index + 1 }}
      </span>
      <div class="flex-1 min-w-0">
        <p class="text-sm text-text-primary truncate">{{ question.question }}</p>
        <span class="text-2xs text-text-muted">
          {{ question.moduleName }} · {{ stars(question.difficulty) }} · {{ question.category || '' }}
        </span>
      </div>
      <span v-if="question.mastered" class="text-xs text-green-600 font-medium shrink-0">✅</span>
      <span class="text-text-muted text-sm shrink-0 transition-transform" :class="expanded && 'rotate-180'">▼</span>
    </div>

    <!-- 答案区 -->
    <div v-if="expanded" class="px-4 pb-4 border-t border-border pt-4">
      <div class="bg-warm rounded-xl p-4 mb-4">
        <p class="text-sm text-text-primary leading-relaxed whitespace-pre-wrap">{{ question.content }}</p>
      </div>
      <div class="flex gap-3">
        <button v-if="!question.mastered"
          @click.stop="$emit('mark', question.id, true)"
          class="flex-1 py-2.5 bg-green-500 text-white rounded-lg text-sm font-semibold hover:bg-green-600 transition-colors">
          ✅ 已掌握
        </button>
        <button v-if="question.mastered"
          @click.stop="$emit('mark', question.id, false)"
          class="flex-1 py-2.5 bg-white border border-border text-text-secondary rounded-lg text-sm hover:bg-gray-50 transition-colors">
          标记为未掌握
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  question: { type: Object, required: true },
  index: { type: Number, default: 0 }
})
defineEmits(['mark'])

const expanded = ref(false)

function toggle() { expanded.value = !expanded.value }
function stars(n) { return '⭐'.repeat(Math.min(n || 1, 5)) }
</script>
