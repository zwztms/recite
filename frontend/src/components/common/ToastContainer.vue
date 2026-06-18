<template>
  <Teleport to="body">
    <Transition name="toast">
      <div v-if="store.currentToast"
        class="fixed top-4 right-4 z-[100] max-w-xs
               bg-gradient-to-r from-yellow-400 to-amber-500
               text-white rounded-xl shadow-lg px-5 py-4
               flex items-center gap-3 cursor-pointer
               hover:shadow-xl transition-shadow"
        @click="store.dismissToast">
        <!-- icon -->
        <div class="shrink-0 w-10 h-10 rounded-full bg-white/20 flex items-center justify-center text-xl">
          {{ store.currentToast.icon || '🏅' }}
        </div>
        <!-- text -->
        <div class="min-w-0">
          <p class="text-sm font-bold truncate">🎉 获得新徽章！</p>
          <p class="text-xs text-white/90 truncate">{{ store.currentToast.name }}</p>
          <p class="text-xs text-white/70 truncate">{{ store.currentToast.description }}</p>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { useAchievementStore } from '../../stores/achievementStore'

const store = useAchievementStore()
</script>

<style scoped>
.toast-enter-active { transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275); }
.toast-leave-active { transition: all 0.25s ease-in; }
.toast-enter-from { opacity: 0; transform: translateX(100px); }
.toast-leave-to { opacity: 0; transform: translateX(100px); }
</style>
