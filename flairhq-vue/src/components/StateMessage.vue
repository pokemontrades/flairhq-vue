<script setup lang="ts">
defineProps<{
  loading: boolean
  error: string | null
  empty?: boolean
  emptyText?: string
}>()
</script>

<template>
  <div v-if="loading" class="state-msg">Loading…</div>
  <div v-else-if="error" class="state-msg error">{{ error }}</div>
  <div v-else-if="empty" class="state-msg">{{ emptyText ?? 'No items found.' }}</div>
  <slot v-else />
</template>
