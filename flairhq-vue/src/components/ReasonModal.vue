<script setup lang="ts">
import BaseModal from './BaseModal.vue'

interface Preset {
  label: string
  reason: string
}

const props = defineProps<{
  title: string
  placeholder: string
  actionLabel: string
  presets: Preset[]
  reason: string
}>()

const emit = defineEmits<{
  (e: 'update:reason', value: string): void
  (e: 'close'): void
  (e: 'submit'): void
}>()

function togglePreset(preset: string) {
  emit('update:reason', props.reason === preset ? '' : preset)
}
</script>

<template>
  <BaseModal :model-value="true" @update:model-value="emit('close')" max-width="440px" max-height="none">
    <template #header>
      <h2>{{ title }}</h2>
    </template>

    <template #body>
      <div v-if="presets.length" class="field">
        <label class="field-label">Quick reasons</label>
        <div class="preset-chips">
          <button
            v-for="preset in presets"
            :key="preset.label"
            class="preset-chip"
            :class="{ active: reason === preset.reason }"
            type="button"
            @click="togglePreset(preset.reason)"
          >{{ preset.label }}</button>
        </div>
      </div>
      <div class="field">
        <label class="field-label">Reason</label>
        <textarea
          class="field-textarea"
          rows="3"
          :placeholder="placeholder"
          :value="reason"
          autofocus
          @input="emit('update:reason', ($event.target as HTMLTextAreaElement).value)"
        />
      </div>
    </template>

    <template #footer>
      <button class="btn-cancel" @click="emit('close')">Cancel</button>
      <button class="btn-action" @click="emit('submit')">{{ actionLabel }}</button>
    </template>
  </BaseModal>
</template>

<style src="../styles/ReasonModal.css" scoped></style>
