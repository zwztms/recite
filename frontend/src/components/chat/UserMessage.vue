<template>
  <div class="flex justify-end mb-5">
    <div class="max-w-[75%] px-4 py-2.5 rounded-2xl rounded-br-md
                bg-coral-light text-orange-900 border border-coral-border
                text-sm leading-relaxed whitespace-pre-wrap">
      {{ data.text }}
    </div>
  </div>
</template>

<script setup>
defineProps({
  data: { type: Object, required: true }
})
</script>
