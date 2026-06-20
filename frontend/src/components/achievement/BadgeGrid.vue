<template>
  <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-3">
    <BadgeCard
      v-for="badge in badges"
      :key="badge.key"
      :badge="badge"
      @click="$emit('select', badge)"
    />
  </div>
</template>

<script setup>
import BadgeCard from './BadgeCard.vue'

defineProps({ badges: { type: Array, required: true } })
defineEmits(['select'])
</script>
