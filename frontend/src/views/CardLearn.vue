<template>
  <div class="h-[calc(100vh-3.5rem)] flex flex-col -mx-4 -my-8">
    <!-- 控制栏 -->
    <div class="flex items-center gap-3 px-5 py-3 bg-surface border-b border-border flex-wrap shrink-0">
      <select v-model="moduleKey" @change="fetchQuestions"
        class="px-3 py-2 border border-border rounded-lg text-sm bg-white text-text-primary">
        <option value="">全部模块</option>
        <option v-for="m in modules" :key="m.moduleKey" :value="m.moduleKey">
          {{ m.moduleName }} ({{ m.questionCount }}题)
        </option>
      </select>
      <div class="flex gap-0 bg-gray-100 rounded-lg p-0.5">
        <button v-for="o in [{v:'seq',l:'按顺序'},{v:'random',l:'乱序'}]" :key="o.v"
          @click="order = o.v; fetchQuestions()"
          :class="['px-3 py-1.5 rounded-md text-xs font-semibold transition-colors',
            order === o.v ? 'bg-coral text-white' : 'text-text-muted']">{{ o.l }}</button>
      </div>
      <div class="flex gap-0 bg-gray-100 rounded-lg p-0.5 ml-auto">
        <button v-for="f in [{v:'all',l:'全部'},{v:'unmastered',l:'未掌握'},{v:'mastered',l:'已掌握'}]" :key="f.v"
          @click="filter = f.v; fetchQuestions()"
          :class="['px-3 py-1.5 rounded-md text-xs font-semibold transition-colors',
            filter === f.v ? 'bg-white text-coral' : 'text-text-muted']">{{ f.l }}</button>
      </div>
      <span class="text-xs text-text-muted">已掌握 {{ masteredCount }}/{{ questions.length }}</span>
    </div>

    <!-- 主体 -->
    <div class="flex-1 flex gap-0 overflow-hidden">
      <SideIndex :questions="questions" :activeId="activeId" @select="scrollTo" />
      <div class="flex-1 overflow-y-auto px-4 py-4 space-y-3" ref="contentRef" @scroll="onScroll">
        <QuestionCard v-for="(q, i) in questions" :key="q.id" :question="q" :index="i" @mark="onMark" />
        <p v-if="questions.length === 0" class="text-center text-text-muted py-12">暂无题目</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getLearnQuestions, markMastery, listModules } from '../api'
import SideIndex from '../components/learn/SideIndex.vue'
import QuestionCard from '../components/learn/QuestionCard.vue'

const modules = ref([])
const questions = ref([])
const moduleKey = ref('')
const order = ref('seq')
const filter = ref('all')
const activeId = ref('')
const contentRef = ref(null)

const masteredCount = computed(() => questions.value.filter(q => q.mastered).length)

async function fetchQuestions() {
  try {
    const res = await getLearnQuestions(moduleKey.value || null, order.value, filter.value)
    questions.value = res.data || []
    activeId.value = ''
  } catch (e) {
    // 静默处理
  }
}

function scrollTo(id) {
  activeId.value = id
  const el = document.getElementById(`q-${id}`)
  if (el) el.scrollIntoView({ block: 'start', behavior: 'smooth' })
}

function onScroll() {
  if (!contentRef.value) return
  const top = contentRef.value.getBoundingClientRect().top + 100
  for (const q of questions.value) {
    const el = document.getElementById(`q-${q.id}`)
    if (el) {
      const rect = el.getBoundingClientRect()
      if (rect.bottom > top) { activeId.value = q.id; break }
    }
  }
}

async function onMark(questionId, mastered) {
  try {
    await markMastery(questionId, mastered)
    const q = questions.value.find(q => q.id === questionId)
    if (q) q.mastered = mastered
  } catch (e) {
    // 静默处理
  }
}

onMounted(async () => {
  try {
    const res = await listModules()
    modules.value = (res.data || []).filter(m => m.status === 'ONLINE')
  } catch (e) {
    // 静默处理
  }
  await fetchQuestions()
})
</script>
