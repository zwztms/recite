<template>
  <Teleport to="body">
    <div v-if="badge" class="fixed inset-0 z-50 flex items-center justify-center px-4"
      @click.self="$emit('close')">
      <!-- 遮罩 -->
      <div class="absolute inset-0 bg-black/40 backdrop-blur-sm"></div>

      <!-- 弹窗卡片 -->
      <div class="relative bg-white rounded-2xl shadow-xl p-6 max-w-sm w-full">
        <button @click="$emit('close')"
          class="absolute top-3 right-3 w-8 h-8 flex items-center justify-center
                 rounded-full hover:bg-gray-100 text-text-muted transition-colors">
          ✕
        </button>

        <!-- icon -->
        <div class="w-20 h-20 mx-auto mb-4 rounded-full flex items-center justify-center text-4xl"
          :class="badge.earned
            ? 'bg-gradient-to-br from-yellow-400 to-amber-500 text-white shadow-lg'
            : 'bg-gray-100 grayscale'">
          {{ badge.icon || '🏅' }}
        </div>

        <!-- 名称 -->
        <h2 class="text-xl font-bold text-center text-text-primary mb-1">
          {{ badge.name }}
        </h2>

        <!-- 获得状态 -->
        <p v-if="badge.earned" class="text-center text-sm text-amber-600 mb-3">
          🎉 已获得 · {{ badge.earnedAt ? formatDate(badge.earnedAt) : '' }}
        </p>
        <p v-else class="text-center text-sm text-text-muted mb-3">
          暂未获得
        </p>

        <!-- 描述 -->
        <p class="text-sm text-text-secondary text-center mb-3">
          {{ badge.hidden && !badge.earned ? '🔒 隐藏徽章，条件不公开' : badge.description }}
        </p>

        <!-- 进度条（未获得时） -->
        <div v-if="!badge.earned && badge.progress && !badge.hidden" class="mt-4">
          <div class="flex justify-between text-xs text-text-muted mb-1">
            <span>进度</span>
            <span>{{ badge.progress.current }}/{{ badge.progress.target }}</span>
          </div>
          <div class="h-2 bg-gray-100 rounded-full overflow-hidden">
            <div class="h-full bg-coral rounded-full transition-all"
              :style="{ width: Math.min(badge.progress.percent || 0, 100) + '%' }"></div>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
defineProps({
  badge: { type: Object, default: null }
})

defineEmits(['close'])

function formatDate(str) {
  if (!str) return ''
  const d = new Date(str)
  return `${d.getFullYear()}/${d.getMonth() + 1}/${d.getDate()}`
}
</script>
