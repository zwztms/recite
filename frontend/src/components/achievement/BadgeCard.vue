<template>
  <div
    @click="$emit('click', badge)"
    class="relative p-4 rounded-xl border transition-all cursor-pointer
           hover:shadow-md hover:-translate-y-0.5"
    :class="badge.earned
      ? 'bg-gradient-to-br from-yellow-50 to-orange-50 border-amber-300'
      : badge.hidden
        ? 'bg-gray-50 border-border opacity-60'
        : 'bg-white border-border'"
  >
    <!-- 隐藏徽章：问号 -->
    <div v-if="badge.hidden && !badge.earned" class="text-center">
      <div class="w-12 h-12 mx-auto mb-2 rounded-full bg-gray-200 flex items-center justify-center">
        <span class="text-xl text-gray-400">?</span>
      </div>
      <p class="text-xs text-text-muted">🔒 隐藏条件</p>
    </div>

    <!-- 正常徽章 -->
    <template v-else>
      <!-- icon -->
      <div class="w-12 h-12 mx-auto mb-2 rounded-full flex items-center justify-center text-2xl"
        :class="badge.earned
          ? 'bg-gradient-to-br from-yellow-400 to-amber-500 text-white shadow-md'
          : 'bg-gray-100 grayscale'">
        {{ badge.icon || '🏅' }}
      </div>

      <!-- 名称 -->
      <p class="text-sm font-medium text-center mb-1"
        :class="badge.earned ? 'text-text-primary' : 'text-text-muted'">
        {{ badge.name }}
      </p>

      <!-- 描述 -->
      <p class="text-xs text-text-muted text-center line-clamp-2 mb-2">
        {{ badge.description }}
      </p>

      <!-- 获得时间 / 进度条 -->
      <div v-if="badge.earned && badge.earnedAt" class="text-center">
        <span class="text-xs text-amber-600">
          {{ formatDate(badge.earnedAt) }}
        </span>
      </div>
      <div v-else-if="!badge.hidden && badge.progress" class="mt-1">
        <div class="h-1.5 bg-gray-100 rounded-full overflow-hidden">
          <div class="h-full bg-coral rounded-full transition-all"
            :style="{ width: Math.min(badge.progress.percent || 0, 100) + '%' }"></div>
        </div>
        <p class="text-xs text-text-muted text-center mt-1">
          {{ badge.progress.current }}/{{ badge.progress.target }}
        </p>
      </div>
    </template>
  </div>
</template>

<script setup>
defineProps({
  badge: { type: Object, required: true }
})

defineEmits(['click'])

function formatDate(str) {
  if (!str) return ''
  const d = new Date(str)
  const m = d.getMonth() + 1
  const day = d.getDate()
  return `${m}月${day}日`
}
</script>
