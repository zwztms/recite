<template>
  <Teleport to="body">
    <div v-if="visible" class="fixed inset-0 z-50 flex items-center justify-center"
         @click.self="$emit('close')">
      <div class="absolute inset-0 bg-black/40 backdrop-blur-sm"></div>
      <div class="relative bg-white rounded-2xl shadow-2xl max-w-lg w-full mx-4 max-h-[80vh] overflow-y-auto">
        <!-- 标题栏 -->
        <div class="sticky top-0 bg-white border-b border-border px-6 py-4 flex items-center justify-between rounded-t-2xl">
          <h3 class="font-semibold text-text-primary">背诵报告</h3>
          <button @click="$emit('close')"
            class="text-text-muted hover:text-text-primary text-xl leading-none">&times;</button>
        </div>

        <!-- 加载中 -->
        <div v-if="loading" class="p-8 text-center text-text-muted">加载中...</div>

        <!-- 报告内容 -->
        <div v-else-if="report" class="p-6 space-y-5">
          <!-- 三栏统计 -->
          <div class="grid grid-cols-3 gap-3">
            <div class="text-center p-3 bg-warm rounded-xl">
              <span class="block text-2xl font-bold text-coral">{{ report.totalScore }}</span>
              <span class="text-xs text-text-muted">总分</span>
            </div>
            <div class="text-center p-3 bg-warm rounded-xl">
              <span class="block text-2xl font-bold text-text-primary">{{ report.averageScore }}</span>
              <span class="text-xs text-text-muted">均分</span>
            </div>
            <div class="text-center p-3 bg-warm rounded-xl">
              <span class="block text-2xl font-bold text-text-primary">{{ report.totalQuestions }}</span>
              <span class="text-xs text-text-muted">题数</span>
            </div>
          </div>

          <!-- 优势模块 -->
          <div v-if="report.strengths?.length">
            <p class="text-sm font-medium text-success-text mb-2">✅ 优势模块</p>
            <div class="flex flex-wrap gap-2">
              <span v-for="s in report.strengths" :key="s"
                class="px-3 py-1 bg-green-50 text-green-700 rounded-full text-xs">{{ s }}</span>
            </div>
          </div>

          <!-- 薄弱模块 -->
          <div v-if="report.weaknesses?.length">
            <p class="text-sm font-medium text-danger-text mb-2">❗ 薄弱模块</p>
            <div class="flex flex-wrap gap-2">
              <span v-for="w in report.weaknesses" :key="w"
                class="px-3 py-1 bg-red-50 text-red-700 rounded-full text-xs">{{ w }}</span>
            </div>
          </div>

          <!-- AI 建议 -->
          <div v-if="report.advice">
            <p class="text-sm font-medium text-text-primary mb-2">💡 建议</p>
            <p class="text-sm text-text-secondary leading-relaxed">{{ report.advice }}</p>
          </div>
        </div>

        <!-- 报告生成中 -->
        <div v-else class="p-8 text-center text-text-muted">报告生成中，请稍后查看</div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, watch } from 'vue'
import { getReport } from '../../api'

const props = defineProps({
  visible: { type: Boolean, default: false },
  sessionId: { type: String, default: '' }
})
defineEmits(['close'])

const report = ref(null)
const loading = ref(false)

watch(() => props.sessionId, async (sid) => {
  if (!sid) return
  loading.value = true
  report.value = null
  try {
    const res = await getReport(sid)
    if (res.data?.status === 'done' && res.data.journal) {
      report.value = res.data.journal
    }
  } catch (e) {
    // 获取失败静默处理
  }
  loading.value = false
})
</script>
