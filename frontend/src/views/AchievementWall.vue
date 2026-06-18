<template>
  <div class="max-w-4xl mx-auto">
    <!-- 顶部统计 -->
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

    <!-- 分组网格 -->
    <div v-if="store.loading" class="text-center py-16 text-text-muted">
      <p>加载中...</p>
    </div>
    <BadgeGrid v-else :groups="store.groupedBadges" @select="onSelect" />

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

onMounted(() => {
  store.fetchBadges()
})

function onSelect(badge) {
  selectedBadge.value = badge
}
</script>
