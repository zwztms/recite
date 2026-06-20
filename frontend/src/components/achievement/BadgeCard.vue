<template>
  <div
    @click="$emit('click', badge)"
    class="badge-card relative rounded-2xl p-4 text-center cursor-pointer transition-all duration-300 hover:-translate-y-1.5"
    :class="cardClass"
  >
    <!-- ✓ 角标 -->
    <span v-if="badge.earned" class="check-mark">✓</span>

    <!-- 圆形金属 icon -->
    <div class="icon-circle" :class="iconClass">
      {{ badge.icon || '?' }}
    </div>

    <!-- 名称 -->
    <p class="badge-name" :class="nameClass">{{ badge.name }}</p>

    <!-- 描述 -->
    <p class="badge-desc" :class="descClass">{{ badge.description }}</p>

    <!-- 获得日期 / 进度 -->
    <p v-if="badge.earned" class="badge-date" :class="dateClass">
      🎉 {{ formatDate(badge.earnedAt) }}
    </p>
    <div v-else-if="progress.current > 0" class="progress-wrap">
      <div class="progress-bar"><div class="progress-fill" :style="{width:progress.pct+'%'}"></div></div>
      <p class="progress-text">{{ progress.current }}/{{ progress.target }}</p>
    </div>
    <p v-else class="badge-date locked-text">暂未解锁</p>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ badge: { type: Object, required: true } })
defineEmits(['click'])

const progress = computed(() => {
  const p = props.badge.progress
  if (!p) return { current: 0, target: 0, pct: 0 }
  return { current: p.current || 0, target: p.target || 0, pct: p.percent || 0 }
})

// ═══ 四档配色 ═══
const tier = computed(() => {
  const gold = ['jvm','juc','mysql','redis','spring']
  const dark = ['java-basics','java-collections','os','ds-algo','network','ai-finetune','ai-openclaw']
  const copper = ['ai-rag','ai-prompt','ai-eval','ai-security']
  // legend: ai-spring, ai-agent, ai-design (fallthrough)
  if (gold.includes(props.badge.key)) return 'gold'
  if (dark.includes(props.badge.key)) return 'darkgold'
  if (copper.includes(props.badge.key)) return 'copper'
  return 'legend'
})

const cardClass = computed(() => {
  if (!props.badge.earned) return 'locked'
  return `card-${tier.value}`
})
const iconClass = computed(() => props.badge.earned ? `icon-${tier.value}` : 'icon-locked')
const nameClass = computed(() => props.badge.earned ? `name-${tier.value}` : 'name-locked')
const descClass = computed(() => props.badge.earned ? `desc-${tier.value}` : 'desc-locked')
const dateClass = computed(() => `date-${tier.value}`)

function formatDate(str) {
  if (!str) return ''
  const d = new Date(str)
  return `${d.getMonth() + 1}月${d.getDate()}日`
}
</script>

<style scoped>
/* ═══ 公共 ═══ */
.badge-card { width: 100% }
.icon-circle {
  width: 52px; height: 52px; border-radius: 50%;
  margin: 0 auto 10px; display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 800; letter-spacing: 0.5px; color: #fff;
  text-shadow: 0 1px 2px rgba(0,0,0,.15); position: relative;
}
.badge-name { font-size: 12px; font-weight: 700; margin-bottom: 2px }
.badge-desc { font-size: 10px; line-height: 1.3; margin-bottom: 4px }
.badge-date { font-size: 10px; font-weight: 500 }
.check-mark {
  position: absolute; top: 8px; right: 8px; width: 18px; height: 18px;
  border-radius: 50%; background: #10b981; display: flex; align-items: center;
  justify-content: center; font-size: 10px; color: #fff; font-weight: 700;
  box-shadow: 0 1px 4px rgba(16,185,129,.4); z-index: 1;
}
.progress-wrap { margin-top: 2px }
.progress-bar { height: 4px; background: #e5e3e0; border-radius: 2px; overflow: hidden }
.progress-fill { height: 100%; border-radius: 2px; background: #d4a853; transition: width .5s }
.progress-text { font-size: 9px; color: #a8a29e; margin-top: 2px }

/* ═══ 金色 ═══ */
.card-gold {
  background: linear-gradient(150deg,#fffef5 0%,#fff9e0 25%,#fef0c0 55%,#f5deb3 85%,#fffef5 100%);
  box-shadow: 0 2px 16px rgba(180,140,20,.18), inset 0 1px 0 rgba(255,255,255,.8);
}
.icon-gold { background: linear-gradient(145deg,#ffd700,#f0a500,#e89600); box-shadow: 0 3px 12px rgba(255,180,0,.35), inset 0 1px 0 rgba(255,255,255,.35) }
.name-gold { color: #6b4c00 } .desc-gold { color: #a07818 } .date-gold { color: #b8860b }

/* ═══ 暗金 ═══ */
.card-darkgold {
  background: linear-gradient(150deg,#fefaf3 0%,#fdf3e0 25%,#f5deb3 55%,#e8cfa0 85%,#fefaf3 100%);
  box-shadow: 0 2px 14px rgba(160,120,40,.2), inset 0 1px 0 rgba(255,255,255,.75);
}
.icon-darkgold { background: linear-gradient(145deg,#d4a853,#b8860b,#9b7408); box-shadow: 0 3px 10px rgba(160,120,20,.3), inset 0 1px 0 rgba(255,255,255,.22) }
.name-darkgold { color: #5c4008 } .desc-darkgold { color: #a08030 } .date-darkgold { color: #b8860b }

/* ═══ 铜色 ═══ */
.card-copper {
  background: linear-gradient(150deg,#fefaf5 0%,#fdf0e0 25%,#f5d5b0 55%,#e8c49a 85%,#fefaf5 100%);
  box-shadow: 0 2px 14px rgba(180,110,30,.15), inset 0 1px 0 rgba(255,255,255,.7);
}
.icon-copper { background: linear-gradient(145deg,#d4a060,#b87333,#a0602a); box-shadow: 0 3px 10px rgba(180,110,40,.28), inset 0 1px 0 rgba(255,255,255,.25) }
.name-copper { color: #5c3010 } .desc-copper { color: #a07040 } .date-copper { color: #b87333 }

/* ═══ 紫金 ═══ */
.card-legend {
  background: linear-gradient(150deg,#fefafe 0%,#fdf5ff 25%,#f0e0ff 55%,#e8d0f8 85%,#fefafe 100%);
  box-shadow: 0 2px 20px rgba(160,60,240,.22), inset 0 1px 0 rgba(255,255,255,.8);
  animation: leglow 3s ease-in-out infinite;
}
.icon-legend { background: linear-gradient(145deg,#d080f8,#a040e8,#8020d0); box-shadow: 0 3px 16px rgba(160,60,240,.45), inset 0 1px 0 rgba(255,255,255,.2) }
.name-legend { color: #4a1080 } .desc-legend { color: #7b3fb8 } .date-legend { color: #9333ea }
@keyframes leglow {
  0%,100% { box-shadow: 0 2px 20px rgba(160,60,240,.22), inset 0 1px 0 rgba(255,255,255,.8) }
  50% { box-shadow: 0 2px 28px rgba(160,60,240,.35), inset 0 1px 0 rgba(255,255,255,.8) }
}

/* ═══ 未解锁 ═══ */
.locked { background: #f4f2ef; box-shadow: 0 1px 6px rgba(0,0,0,.04); opacity: .6 }
.icon-locked { background: #ddd9d5; box-shadow: none; text-shadow: none; font-size: 16px !important }
.name-locked { color: #a8a29e } .desc-locked { color: #c0bdb8 } .locked-text { font-size: 10px; color: #c0bdb8 }
</style>
