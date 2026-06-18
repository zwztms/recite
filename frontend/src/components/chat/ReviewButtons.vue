<template>
  <div class="mb-5 flex items-center gap-3 justify-center">
    <button @click="$emit('rate', 'remembered')"
      class="px-5 py-2.5 rounded-xl text-sm font-medium
             bg-success-bg text-success-text border border-green-300
             hover:bg-green-100 transition-colors">
      ✓ 想起来了
    </button>
    <button @click="$emit('rate', 'uncertain')"
      class="px-5 py-2.5 rounded-xl text-sm font-medium
             bg-warning-bg text-warning-text border border-amber-300
             hover:bg-amber-100 transition-colors">
      ? 不确定
    </button>
    <button @click="$emit('rate', 'forgot')"
      class="px-5 py-2.5 rounded-xl text-sm font-medium
             bg-danger-bg text-danger-text border border-red-300
             hover:bg-red-100 transition-colors">
      ✗ 忘了
    </button>
  </div>
</template>

<script setup>
defineEmits(['rate'])
</script>
