<template>
  <div class="mb-5 p-4 bg-warning-bg border border-amber-200 rounded-xl max-w-full">
    <p class="text-sm text-warning-text mb-3">💬 追问：{{ message.data.question }}</p>
    <div v-if="!message.data.answered" class="flex gap-2">
      <button @click="$emit('accept', message.data.recordId)"
        class="px-4 py-1.5 text-xs rounded-lg bg-coral text-white
               hover:bg-orange-600 transition-colors font-medium">
        接受追问
      </button>
      <button @click="$emit('skip', message.data.recordId)"
        class="px-4 py-1.5 text-xs rounded-lg border border-border text-text-secondary
               hover:bg-gray-50 transition-colors">
        跳过
      </button>
    </div>
    <p v-else class="text-xs text-text-muted">✓ 已回答</p>
  </div>
</template>

<script setup>
defineProps({
  message: { type: Object, required: true }
})

defineEmits(['accept', 'skip'])
</script>
