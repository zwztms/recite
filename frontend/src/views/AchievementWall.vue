<template>
  <div class="max-w-5xl mx-auto">
    <h1 class="text-2xl font-bold text-text-primary mb-6">🏅 模块徽章</h1>

    <!-- 统计 -->
    <div class="grid grid-cols-3 gap-4 mb-8">
      <div class="bg-white rounded-xl border border-border p-4 text-center">
        <p class="text-3xl font-bold text-coral">{{ store.earnedCount }}</p>
        <p class="text-xs text-text-muted mt-1">已获得</p>
      </div>
      <div class="bg-white rounded-xl border border-border p-4 text-center">
        <p class="text-3xl font-bold text-text-primary">{{ store.totalCount }}</p>
        <p class="text-xs text-text-muted mt-1">总徽章</p>
      </div>
      <div class="bg-white rounded-xl border border-border p-4 text-center">
        <p class="text-3xl font-bold text-amber-500">{{ store.nearCount }}</p>
        <p class="text-xs text-text-muted mt-1">接近达成</p>
      </div>
    </div>

    <!-- 网格 -->
    <div v-if="store.loading" class="text-center py-16 text-text-muted">加载中...</div>
    <BadgeGrid v-else :badges="store.badges" @select="onSelect" />

    <!-- 详情弹窗 -->
    <BadgeDetail v-if="selectedBadge" :badge="selectedBadge" @close="selectedBadge = null" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAchievementStore } from '../stores/achievementStore'
import BadgeGrid from '../components/achievement/BadgeGrid.vue'
import BadgeDetail from '../components/achievement/BadgeDetail.vue'

const store = useAchievementStore()
const selectedBadge = ref(null)

onMounted(() => store.fetchBadges())

function onSelect(badge) { selectedBadge.value = badge }
</script>
