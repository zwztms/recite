import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getAllBadges, getNewBadges, ackNewBadges } from '../api'

export const useAchievementStore = defineStore('achievement', () => {
  const badges = ref([])
  const toastQueue = ref([])
  const currentToast = ref(null)
  const loading = ref(false)

  const earnedCount = computed(() => badges.value.filter(b => b.earned).length)
  const totalCount = computed(() => badges.value.length)
  const nearCount = computed(() =>
    badges.value.filter(b => !b.earned && b.progress?.percent >= 70).length
  )

  async function fetchBadges() {
    loading.value = true
    try {
      const res = await getAllBadges()
      badges.value = res.data || []
    } finally {
      loading.value = false
    }
  }

  async function pollNewBadges() {
    try {
      const res = await getNewBadges()
      const list = res.data || []
      if (list.length) {
        toastQueue.value.push(...list)
        if (!currentToast.value) showNextToast()
      }
      return list
    } catch (e) { return [] }
  }

  async function ack() {
    try { await ackNewBadges() } catch (e) { /* 静默 */ }
  }

  function showNextToast() {
    if (toastQueue.value.length === 0) { currentToast.value = null; ack(); return }
    currentToast.value = toastQueue.value.shift()
    setTimeout(() => { currentToast.value = null; showNextToast() }, 3000)
  }

  function dismissToast() { currentToast.value = null; showNextToast() }

  return {
    badges, toastQueue, currentToast, loading,
    earnedCount, totalCount, nearCount,
    fetchBadges, pollNewBadges, ack, dismissToast
  }
})
