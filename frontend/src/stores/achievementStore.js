import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getAllBadges, getNewBadges, ackNewBadges } from '../api'

export const useAchievementStore = defineStore('achievement', () => {
  const badges = ref([])           // 全部徽章（含进度）
  const toastQueue = ref([])       // 待展示的 Toast
  const currentToast = ref(null)   // 当前展示的 Toast
  const loading = ref(false)

  // 统计
  const earnedCount = computed(() => badges.value.filter(b => b.earned).length)
  const totalCount = computed(() => badges.value.filter(b => !b.hidden).length)
  const nearCount = computed(() =>
    badges.value.filter(b => !b.earned && b.progress?.percent >= 70).length
  )

  // 按类别分组
  const groupedBadges = computed(() => {
    const groups = {}
    const order = ['背诵量', '质量', '坚持', '模块通关', '趣味 & 隐藏']

    for (const b of badges.value) {
      const cat = b.category || '其他'
      if (!groups[cat]) groups[cat] = []
      groups[cat].push(b)
    }

    return order.filter(k => groups[k]).map(k => ({
      category: k,
      earned: groups[k].filter(b => b.earned).length,
      total: groups[k].length,
      badges: groups[k]
    }))
  })

  // 拉取全部徽章
  async function fetchBadges() {
    loading.value = true
    try {
      const res = await getAllBadges()
      badges.value = res.data || []
    } finally {
      loading.value = false
    }
  }

  // 轮询新徽章（finishRecite 后调用）
  async function pollNewBadges() {
    try {
      const res = await getNewBadges()
      const list = res.data || []
      if (list.length) {
        toastQueue.value.push(...list)
        if (!currentToast.value) showNextToast()
      }
      return list
    } catch (e) {
      return []
    }
  }

  // 确认已读
  async function ack() {
    try {
      await ackNewBadges()
    } catch (e) {
      // 静默
    }
  }

  // Toast 队列
  function showNextToast() {
    if (toastQueue.value.length === 0) {
      currentToast.value = null
      ack()
      return
    }
    currentToast.value = toastQueue.value.shift()
    setTimeout(() => {
      currentToast.value = null
      showNextToast()
    }, 3000)
  }

  function dismissToast() {
    currentToast.value = null
    showNextToast()
  }

  return {
    badges, toastQueue, currentToast, loading,
    earnedCount, totalCount, nearCount, groupedBadges,
    fetchBadges, pollNewBadges, ack, dismissToast
  }
})
