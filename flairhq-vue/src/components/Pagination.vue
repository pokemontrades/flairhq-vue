<script setup lang="ts">
const props = defineProps<{ modelValue: number; total: number }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: number): void }>()
</script>

<template>
  <div class="pagination">
    <button class="page-btn" :disabled="modelValue === 1" @click="emit('update:modelValue', modelValue - 1)">Prev</button>
    <span class="page-info">{{ modelValue }} / {{ total }}</span>
    <button class="page-btn" :disabled="modelValue === total" @click="emit('update:modelValue', modelValue + 1)">Next</button>
  </div>
</template>

<style src="../styles/Pagination.css" scoped></style>
