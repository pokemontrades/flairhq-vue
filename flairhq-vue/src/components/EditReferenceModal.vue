<script setup lang="ts">
import { ref, watch } from 'vue'
import { REFERENCE_CATEGORIES } from '../stores/references'
import type { Reference } from '../stores/references'
import { apiJson } from '../lib/apiFetch'
import { useReferenceForm } from '../composables/useReferenceForm'
import BaseModal from './BaseModal.vue'
import ReferenceFormFields from './ReferenceFormFields.vue'

const props = defineProps<{
  modelValue: boolean
  reference: Reference | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'saved', ref: Reference): void
}>()

const rf = useReferenceForm()
const { form, touched, urlError, hasErrors } = rf

const saving = ref(false)
const error  = ref<string | null>(null)

watch(() => props.modelValue, (open) => {
  if (open && props.reference) {
    const r = props.reference
    rf.reset({
      url:          r.url          ?? '',
      user2:        r.user2        ?? '',
      type:         r.type         ?? '',
      gave:         r.gave         ?? '',
      got:          r.got          ?? '',
      description:  r.description  ?? '',
      notes:        r.notes        ?? '',
      privateNotes: r.privateNotes ?? '',
      number:       r.number       ?? 0,
    })
    error.value = null
  }
})

function close() { emit('update:modelValue', false) }

async function save() {
  rf.markAllTouched()
  if (!props.reference || hasErrors.value) return
  saving.value = true
  error.value  = null
  try {
    const updated = await apiJson<Reference>(`/api/references/${props.reference.id}`, {
      method: 'PUT',
      json: rf.toRequestBody(),
    })
    emit('saved', updated)
    close()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Save failed'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <BaseModal
    :model-value="modelValue && !!reference"
    @update:model-value="emit('update:modelValue', $event)"
    max-width="560px"
  >
    <template #header>
      <h2 id="modal-title">Edit Reference</h2>
    </template>

    <template #body>
      <ReferenceFormFields :form="form" :rf="rf" :categories="REFERENCE_CATEGORIES" />
      <p v-if="error" class="save-error">{{ error }}</p>
    </template>

    <template #footer>
      <button class="btn-cancel" @click="close" :disabled="saving">Cancel</button>
      <button class="btn-save" @click="save" :disabled="saving || (touched && hasErrors) || !!urlError">
        {{ saving ? 'Saving…' : 'Save Changes' }}
      </button>
    </template>
  </BaseModal>
</template>
