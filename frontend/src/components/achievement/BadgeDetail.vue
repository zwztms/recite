<template>
  <Teleport to="body">
    <div v-if="badge" class="fixed inset-0 z-50 flex items-center justify-center px-4" @click.self="$emit('close')">
      <div class="absolute inset-0 bg-black/40 backdrop-blur-sm"></div>
      <div class="relative bg-white rounded-2xl shadow-xl p-6 max-w-sm w-full modal-in">
        <button @click="$emit('close')" class="absolute top-3 right-3 w-8 h-8 flex items-center justify-center rounded-full hover:bg-stone-100 text-text-muted">✕</button>

        <div class="text-center">
          <!-- icon -->
          <div class="w-20 h-20 mx-auto mb-4 rounded-full flex items-center justify-center text-2xl font-extrabold text-white shadow-lg"
            :class="iconClass">
            {{ badge.icon || '?' }}
            <span v-if="badge.earned" class="absolute -top-1 -right-1 w-5 h-5 bg-green-500 rounded-full flex items-center justify-center">
              <svg class="w-3 h-3 text-white" fill="none" stroke="currentColor" stroke-width="4" viewBox="0 0 24 24"><path d="M5 13l4 4L19 7"/></svg>
            </span>
          </div>

          <h2 class="text-xl font-bold text-text-primary mb-1">{{ badge.name }}</h2>
          <p class="text-sm text-text-secondary mb-3">{{ badge.description }}</p>

          <p v-if="badge.earned" class="text-sm font-medium" :class="earnedColor">
            🎉 已获得 · {{ formatDate(badge.earnedAt) }}
          </p>
          <div v-else-if="progress.pct > 0" class="mt-3">
            <div class="flex justify-between text-xs text-text-muted mb-1">
              <span>进度</span><span>{{ progress.current }}/{{ progress.target }}</span>
            </div>
            <div class="h-2 bg-stone-200 rounded-full overflow-hidden">
              <div class="h-full rounded-full transition-all" :style="{width:progress.pct+'%',background:tierColor}"></div>
            </div>
            <p class="text-xs text-text-muted mt-2">还差 {{ progress.target - progress.current }} 题即可解锁</p>
          </div>
          <p v-else class="text-sm text-text-muted">暂未解锁</p>

          <p class="text-xs mt-3 px-2 py-1 rounded-full inline-block font-medium" :style="{background:tierColor+'20',color:tierColor}">
            {{ tierLabel }}
          </p>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ badge: { type: Object, default: null } })
defineEmits(['close'])

const progress = computed(() => {
  const p = props.badge?.progress
  if (!p) return { current: 0, target: 0, pct: 0 }
  return { current: p.current || 0, target: p.target || 0, pct: p.percent || 0 }
})

const tiers = [
  { keys: ['jvm','juc','mysql','redis','spring'], label: '🥇 金色 · 核心', color: '#d4a853' },
  { keys: ['java-basics','java-collections','os','ds-algo','network','ai-finetune','ai-openclaw'], label: '🏅 暗金 · Java生态', color: '#b8860b' },
  { keys: ['ai-rag','ai-prompt','ai-eval','ai-security'], label: '🥉 铜色 · AI子域', color: '#b87333' },
  { keys: ['ai-spring','ai-agent','ai-design'], label: '💎 紫金 · AI综合', color: '#9333ea' },
]
const tierInfo = computed(() => {
  const k = props.badge?.key
  return tiers.find(t => t.keys.includes(k)) || tiers[1]
})
const tierLabel = computed(() => tierInfo.value.label)
const tierColor = computed(() => tierInfo.value.color)

const iconClass = computed(() => {
  if (!props.badge?.earned) return 'bg-stone-300 text-stone-400'
  const k = props.badge.key
  if (['jvm','juc','mysql','redis','spring'].includes(k)) return 'icon-gold'
  if (['ai-spring','ai-agent','ai-design'].includes(k)) return 'icon-legend'
  if (['ai-rag','ai-prompt','ai-eval','ai-security'].includes(k)) return 'icon-copper'
  return 'icon-darkgold'
})
const earnedColor = computed(() => `text-[${tierColor.value}]`)

function formatDate(str) {
  if (!str) return ''
  const d = new Date(str)
  return `${d.getFullYear()}/${d.getMonth()+1}/${d.getDate()}`
}
</script>

<style scoped>
.modal-in { animation: modalIn .2s ease-out }
@keyframes modalIn { from { opacity: 0; transform: scale(.9) } to { opacity: 1; transform: scale(1) } }
.icon-gold { background: linear-gradient(145deg,#ffd700,#f0a500,#e89600); box-shadow: 0 3px 12px rgba(255,180,0,.35) }
.icon-darkgold { background: linear-gradient(145deg,#d4a853,#b8860b,#9b7408); box-shadow: 0 3px 10px rgba(160,120,20,.3) }
.icon-copper { background: linear-gradient(145deg,#d4a060,#b87333,#a0602a); box-shadow: 0 3px 10px rgba(180,110,40,.28) }
.icon-legend { background: linear-gradient(145deg,#d080f8,#a040e8,#8020d0); box-shadow: 0 3px 16px rgba(160,60,240,.45) }
</style>
