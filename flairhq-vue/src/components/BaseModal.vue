<script setup lang="ts">
const props = defineProps<{
  modelValue: boolean
  maxWidth?: string
  maxHeight?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
}>()

function onBackdropClick(e: MouseEvent) {
  if ((e.target as HTMLElement).classList.contains('modal-backdrop')) {
    emit('update:modelValue', false)
  }
}
</script>

<template>
  <Teleport to="body">
    <div v-if="modelValue" class="modal-backdrop" @click="onBackdropClick">
      <div
        class="modal-box"
        role="dialog"
        aria-modal="true"
        :style="{ maxWidth: maxWidth ?? '560px', maxHeight: maxHeight ?? '90vh' }"
      >
        <div class="modal-header">
          <slot name="header" />
          <button class="close-btn" @click="emit('update:modelValue', false)" aria-label="Close">&times;</button>
        </div>
        <div class="modal-body">
          <slot name="body" />
        </div>
        <div class="modal-footer">
          <slot name="footer" />
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style src="../styles/BaseModal.css" scoped></style>
