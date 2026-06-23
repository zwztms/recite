<template>
  <div class="max-w-4xl mx-auto px-4 py-6 space-y-5">
    <!-- 问候 + 统计 -->
    <div class="bg-surface rounded-2xl p-6 border border-border shadow-sm flex items-center gap-5">
      <div class="w-16 h-16 rounded-full bg-gradient-to-br from-coral to-orange-600 flex items-center justify-center text-white text-2xl font-bold shrink-0">
        {{ initial }}
      </div>
      <div class="flex-1" v-if="data">
        <p class="text-lg font-bold text-text-primary mb-2">{{ data.user?.nickname }}，{{ greeting }} ☀️</p>
        <div class="flex gap-5 flex-wrap">
          <div class="text-center">
            <span class="block text-xl font-bold text-coral">{{ data.stats?.streakDays }}</span>
            <span class="text-xs text-text-muted">连续天数</span>
          </div>
          <div class="w-px bg-border"></div>
          <div class="text-center">
            <span class="block text-xl font-bold text-text-primary">{{ data.stats?.totalRecites }}</span>
            <span class="text-xs text-text-muted">累计背诵</span>
          </div>
          <div class="w-px bg-border"></div>
          <div class="text-center">
            <span class="block text-xl font-bold text-green-600">{{ data.stats?.masteredCount }}</span>
            <span class="text-xs text-text-muted">已掌握</span>
          </div>
          <div class="w-px bg-border"></div>
          <div class="text-center">
            <span class="block text-xl font-bold text-coral">{{ data.stats?.totalProgress }}%</span>
            <span class="text-xs text-text-muted">总进度</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 双 CTA -->
    <div class="grid grid-cols-2 gap-4">
      <button @click="$router.push('/learn')"
        class="p-5 bg-gradient-to-br from-coral to-orange-600 text-white rounded-2xl font-bold text-base shadow-lg shadow-orange-200 hover:shadow-xl transition-shadow">
        📖 翻卡学习
        <span class="block text-xs font-normal opacity-85 mt-1">浏览题目 · 自由选择 · 随手标记</span>
      </button>
      <button @click="$router.push('/recite')"
        class="p-5 bg-white text-coral border-2 border-coral rounded-2xl font-bold text-base hover:bg-orange-50 transition-colors">
        ✍️ 对话背诵
        <span class="block text-xs font-normal text-text-muted mt-1">AI 出题 · 回答评分 · 追问深入</span>
      </button>
    </div>

    <!-- 三列卡片 -->
    <div class="grid grid-cols-3 gap-4" v-if="data">
      <!-- 模块掌握 -->
      <div class="bg-surface rounded-2xl p-5 border border-border">
        <span class="font-semibold text-sm text-text-primary block mb-4">📊 模块掌握</span>
        <div class="space-y-3">
          <div v-for="m in (data.moduleMastery || []).slice(0,5)" :key="m.moduleKey">
            <div class="flex justify-between text-xs mb-1">
              <span class="text-text-primary">{{ m.moduleName }}</span>
              <span :class="m.mastered > m.total/2 ? 'text-coral font-semibold' : 'text-text-muted'">
                {{ m.mastered }} / {{ m.total }}
              </span>
            </div>
            <div class="h-2 bg-warm rounded-full overflow-hidden">
              <div class="h-full rounded-full transition-all"
                :style="{
                  width: (m.total > 0 ? m.mastered * 100 / m.total : 0) + '%',
                  background: m.mastered > m.total/2
                    ? 'linear-gradient(90deg,#f97316,#fb923c)'
                    : '#fed7aa'
                }"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 趋势 -->
      <div class="bg-surface rounded-2xl p-5 border border-border">
        <span class="font-semibold text-sm text-text-primary block mb-4">📈 近 7 天背诵</span>
        <div class="flex items-end justify-between h-28 gap-2 px-1">
          <div v-for="t in (data.trend || [])" :key="t.dayLabel" class="flex flex-col items-center flex-1">
            <span class="text-2xs text-text-muted mb-1">{{ t.count }}</span>
            <div class="w-full rounded-t-sm"
              :style="{
                height: Math.min(Math.max(t.count * 2.5, 4), 100) + 'px',
                background: t.count >= 20 ? '#f97316' : '#fed7aa'
              }"></div>
            <span class="text-2xs text-text-muted mt-2">{{ t.dayLabel }}</span>
          </div>
        </div>
      </div>

      <!-- 徽章 + 标签 + 建议 -->
      <div class="bg-surface rounded-2xl p-5 border border-border flex flex-col gap-4">
        <div>
          <span class="font-semibold text-sm text-text-primary block mb-3">🏆 徽章</span>
          <div class="flex gap-2 items-center">
            <div v-for="b in (data.badges || [])" :key="b.key"
              class="w-10 h-10 rounded-full bg-gradient-to-br from-amber-400 to-amber-600 flex items-center justify-center text-lg"
              :title="b.name">⭐</div>
            <span v-if="!(data.badges || []).length" class="text-xs text-text-muted">暂无徽章</span>
          </div>
        </div>
        <div class="border-t border-border pt-4">
          <span class="font-semibold text-sm text-text-primary block mb-3">🏷️ 薄弱标签</span>
          <div class="flex flex-wrap gap-1">
            <span v-for="t in (data.weakTags || [])" :key="t"
              class="px-2 py-0.5 bg-orange-50 text-orange-700 border border-orange-200 rounded-full text-2xs">{{ t }}</span>
            <span v-if="!(data.weakTags || []).length" class="text-xs text-text-muted">暂无</span>
          </div>
        </div>
        <div class="border-t border-border pt-4">
          <span class="font-semibold text-sm text-text-primary block mb-2">💡 建议</span>
          <p class="text-xs text-text-secondary leading-relaxed">{{ data.advice || '开始你的学习之旅！' }}</p>
        </div>
      </div>
    </div>

    <!-- 最近背诵 -->
    <div class="bg-surface rounded-2xl p-5 border border-border" v-if="data">
      <span class="font-semibold text-sm text-text-primary block mb-4">📝 最近背诵</span>
      <div class="space-y-1">
        <div v-for="r in (data.recentRecites || [])" :key="r.sessionId"
          @click="openReport(r.sessionId)"
          class="flex items-center gap-3 px-3 py-2.5 rounded-xl cursor-pointer hover:bg-warm transition-colors border border-transparent hover:border-border">
          <span class="text-xs text-text-muted w-20 shrink-0">{{ r.dateLabel }}</span>
          <span class="flex-1 text-sm text-text-primary">
            {{ r.moduleName || r.moduleKey }} · {{ r.questionCount }} 题 · 均分
            <b :class="r.avgScore >= 7 ? 'text-green-600' : 'text-coral'">{{ r.avgScore }}</b>
          </span>
          <span class="text-xs text-coral font-semibold shrink-0">查看报告 →</span>
        </div>
        <p v-if="!(data.recentRecites || []).length" class="text-sm text-text-muted text-center py-4">暂无背诵记录</p>
      </div>
    </div>

    <!-- 报告弹窗 -->
    <ReportModal :visible="reportVisible" :sessionId="selectedSid" @close="reportVisible = false" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getHomeDashboard } from '../api'
import ReportModal from '../components/home/ReportModal.vue'

const data = ref(null)
const reportVisible = ref(false)
const selectedSid = ref('')

const initial = computed(() => (data.value?.user?.nickname || 'U')[0].toUpperCase())
const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '上午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

onMounted(async () => {
  try {
    const res = await getHomeDashboard()
    data.value = res.data
  } catch (e) {
    // 加载失败静默处理
  }
})

function openReport(sid) {
  selectedSid.value = sid
  reportVisible.value = true
}
</script>
