<template>
  <div class="space-y-8">
    <div v-for="group in groups" :key="group.category">
      <!-- 分组标题 -->
      <div class="flex items-center gap-3 mb-3">
        <h3 class="text-base font-semibold text-text-primary">{{ group.category }}</h3>
        <span class="text-xs text-text-muted">{{ group.earned }}/{{ group.total }}</span>
      </div>

      <!-- 网格 -->
      <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-3">
        <BadgeCard
          v-for="badge in group.badges"
          :key="badge.key"
          :badge="badge"
          @click="$emit('select', badge)"
        />
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!groups.length" class="text-center py-16 text-text-muted">
      <p class="text-lg mb-2">🏅</p>
      <p>暂无徽章数据</p>
    </div>
  </div>
</template>

<script setup>
import BadgeCard from './BadgeCard.vue'

defineProps({
  groups: { type: Array, required: true }
})

defineEmits(['select'])
</script>
