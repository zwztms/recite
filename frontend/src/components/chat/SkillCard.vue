<template>
  <div class="skill-card my-3 mx-2 border border-amber-500/30 rounded-lg overflow-hidden
              bg-amber-500/5 transition-all duration-200">
    <button @click="expanded = !expanded"
      class="w-full flex items-center justify-between px-4 py-2.5
             hover:bg-amber-500/10 transition-colors text-left">
      <div class="flex items-center gap-2">
        <span class="text-sm">📋</span>
        <span class="text-sm font-semibold text-amber-400">{{ data.label || data.name }}</span>
        <span class="text-[10px] text-amber-500/60 px-1.5 py-0.5 bg-amber-500/10 rounded">AI 技能</span>
      </div>
      <span class="text-xs text-amber-500/60 transition-transform duration-200"
            :class="expanded ? 'rotate-180' : ''">▼</span>
    </button>
    <div v-if="expanded" class="px-4 pb-3 pt-1 text-sm leading-relaxed">
      <div v-if="result.analysis" class="mb-3 text-gray-300">
        <span class="font-medium text-gray-200">📝 分析：</span>{{ result.analysis }}
      </div>
      <div v-if="result.strengths?.length" class="mb-2">
        <div class="font-medium text-green-400 text-xs mb-1">✅ 优势</div>
        <ul class="list-disc list-inside text-gray-300 text-xs space-y-0.5">
          <li v-for="(s,i) in result.strengths" :key="'s'+i">{{ s }}</li>
        </ul>
      </div>
      <div v-if="result.weaknesses?.length" class="mb-2">
        <div class="font-medium text-red-400 text-xs mb-1">⚠️ 待改进</div>
        <ul class="list-disc list-inside text-gray-300 text-xs space-y-0.5">
          <li v-for="(w,i) in result.weaknesses" :key="'w'+i">{{ w }}</li>
        </ul>
      </div>
      <div v-if="result.missingConcepts?.length" class="mb-2">
        <div class="font-medium text-orange-400 text-xs mb-1">🔍 遗漏概念</div>
        <ul class="list-disc list-inside text-gray-300 text-xs space-y-0.5">
          <li v-for="(m,i) in result.missingConcepts" :key="'m'+i">{{ m }}</li>
        </ul>
      </div>
      <div v-if="otherFields.length" class="mt-2 pt-2 border-t border-amber-500/20">
        <div v-for="kv in otherFields" :key="kv.key" class="mb-1 text-xs">
          <span class="text-gray-400">{{ kv.key }}：</span>
          <span class="text-gray-300">{{ kv.value }}</span>
        </div>
      </div>
      <div v-if="!hasStdFields" class="text-xs text-gray-400 font-mono bg-black/20 rounded p-2
                   overflow-x-auto max-h-32">{{ data.resultJson }}</div>
    </div>
    <div v-else class="px-4 pb-2.5">
      <p class="text-xs text-gray-400 truncate">{{ summary }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  data: { type: Object, required: true }
})

const expanded = ref(true)

const result = computed(() => props.data.result || {})

const STD = ['analysis', 'strengths', 'weaknesses', 'missingConcepts', 'overallAssessment', 'error']

const hasStdFields = computed(() =>
  Object.keys(result.value).some(k => STD.includes(k)))

const otherFields = computed(() =>
  Object.entries(result.value)
    .filter(([k]) => !STD.includes(k))
    .map(([k, v]) => ({
      key: k,
      value: Array.isArray(v) ? v.join(', ') : typeof v === 'object' ? JSON.stringify(v) : String(v)
    })))

const summary = computed(() => {
  if (result.value.analysis) return result.value.analysis.substring(0, 60) + '...'
  if (result.value.error) return '分析失败：' + result.value.error
  return '点击展开查看分析详情'
})
</script>
