<script setup lang="ts">
import { ref, watch } from 'vue'
import { ADDABLE_REFERENCE_CATEGORIES } from '../stores/references'
import type { Reference } from '../stores/references'
import { apiJson } from '../lib/apiFetch'
import { useReferenceForm } from '../composables/useReferenceForm'
import BaseModal from './BaseModal.vue'
import ReferenceFormFields from './ReferenceFormFields.vue'

const props = defineProps<{
  modelValue: boolean
  prefill?: { url?: string; user2?: string; type?: string; gave?: string; got?: string; description?: string }
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'added', ref: Reference): void
}>()

const rf = useReferenceForm()
const { form, touched, urlError, hasErrors } = rf

const saving = ref(false)
const error  = ref<string | null>(null)

watch(() => props.modelValue, (open) => {
  if (open) {
    rf.reset(props.prefill)
    error.value = null
  }
})

function close() { emit('update:modelValue', false) }

async function submit() {
  rf.markAllTouched()
  if (hasErrors.value) return
  saving.value = true
  error.value  = null
  try {
    const created = await apiJson<Reference>('/api/references', {
      method: 'POST',
      json: rf.toRequestBody(),
    })
    emit('added', created)
    close()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Submission failed'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <BaseModal :model-value="modelValue" @update:model-value="emit('update:modelValue', $event)" max-width="560px">
    <template #header>
      <h2 id="modal-title">Add Reference</h2>
    </template>

    <template #body>
      <ReferenceFormFields
        :form="form"
        :rf="rf"
        :categories="ADDABLE_REFERENCE_CATEGORIES"
        type-placeholder="Select a type"
      />
      <p v-if="error" class="save-error">{{ error }}</p>
    </template>

    <template #footer>
      <button class="btn-cancel" @click="close" :disabled="saving">Cancel</button>
      <button class="btn-save" @click="submit" :disabled="saving || (touched && hasErrors) || !!urlError">
        {{ saving ? 'Submitting…' : 'Submit Reference' }}
      </button>
    </template>
  </BaseModal>
</template>
