<script setup lang="ts">
import { ref, watch } from 'vue'
import { ADDABLE_REFERENCE_CATEGORIES } from '../stores/references'
import type { Reference } from '../stores/references'
import { apiFetch, API_BASE } from '../lib/apiFetch'
import { useReferenceForm } from '../composables/useReferenceForm'
import BaseModal from './BaseModal.vue'

const props = defineProps<{
  modelValue: boolean
  prefill?: { url?: string; user2?: string; type?: string; gave?: string; got?: string; description?: string }
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'added', ref: Reference): void
}>()


const defaultForm = () => ({
  url:          '',
  user2:        '',
  type:         '',
  gave:         '',
  got:          '',
  description:  '',
  notes:        '',
  privateNotes: '',
  number:       0,
})

const form    = ref(defaultForm())
const saving  = ref(false)
const error   = ref<string | null>(null)


const {
  touched, urlTouched,
  showPartner, showGaveGot, showDesc, showNumber,
  urlError, typeError, partnerError, gaveError, gotError, descError, numberError,
  hasErrors,
} = useReferenceForm(form)

watch(() => props.modelValue, (open) => {
  if (open) {
    form.value       = props.prefill ? { ...defaultForm(), ...props.prefill } : defaultForm()
    error.value      = null
    touched.value    = false
    urlTouched.value = false
  }
})

function close() { emit('update:modelValue', false) }

async function submit() {
  touched.value    = true
  urlTouched.value = true
  if (hasErrors.value) return
  saving.value = true
  error.value  = null
  try {
    const res = await apiFetch(`${API_BASE}/api/references`, {
      method:  'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        url:          form.value.url          || null,
        user2:        showPartner.value  ? (form.value.user2       || null) : null,
        type:         form.value.type         || null,
        gave:         showGaveGot.value  ? (form.value.gave        || null) : null,
        got:          showGaveGot.value  ? (form.value.got         || null) : null,
        description:  showDesc.value     ? (form.value.description || null) : null,
        notes:        form.value.notes        || null,
        privateNotes: form.value.privateNotes || null,
        number:       showNumber.value   ? (form.value.number      || null) : null,
      }),
    })
    if (!res.ok) {
      const body = await res.json().catch(() => null)
      throw new Error(body?.message ?? (res.status === 400 ? 'A reference with that URL already exists.' : `Error ${res.status}`))
    }
    const created: Reference = await res.json()
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
      <div class="field">
        <label class="field-label" for="ref-url">Permalink URL</label>
        <input id="ref-url" v-model="form.url" type="url" class="field-input" :class="{ 'field-input--invalid': urlError }" placeholder="https://reddit.com/r/pokemontrades/comments/…" @blur="urlTouched = true" />
        <p v-if="urlError" class="field-error">{{ urlError }}</p>
      </div>

      <div class="field-row">
        <div class="field">
          <label class="field-label" for="ref-type">Type</label>
          <select id="ref-type" v-model="form.type" class="field-input field-select" :class="{ 'field-input--invalid': typeError }">
            <option value="" disabled>Select a type</option>
            <option v-for="{ type, label } in ADDABLE_REFERENCE_CATEGORIES" :key="type" :value="type">{{ label }}</option>
          </select>
          <p v-if="typeError" class="field-error">{{ typeError }}</p>
        </div>
        <div v-if="showPartner" class="field">
          <label class="field-label" for="ref-partner">Trading Partner</label>
          <input id="ref-partner" v-model="form.user2" type="text" class="field-input" :class="{ 'field-input--invalid': partnerError }" placeholder="u/username" />
          <p v-if="partnerError" class="field-error">{{ partnerError }}</p>
        </div>
        <div v-if="showNumber" class="field">
          <label class="field-label" for="ref-number">Number Given</label>
          <input id="ref-number" v-model.number="form.number" type="number" min="1" class="field-input" :class="{ 'field-input--invalid': numberError }" />
          <p v-if="numberError" class="field-error">{{ numberError }}</p>
        </div>
      </div>

      <div v-if="showGaveGot" class="field-row">
        <div class="field">
          <label class="field-label" for="ref-gave">Gave</label>
          <input id="ref-gave" v-model="form.gave" type="text" class="field-input" :class="{ 'field-input--invalid': gaveError }" placeholder="Pokémon you gave" />
          <p v-if="gaveError" class="field-error">{{ gaveError }}</p>
        </div>
        <div class="field">
          <label class="field-label" for="ref-got">Got</label>
          <input id="ref-got" v-model="form.got" type="text" class="field-input" :class="{ 'field-input--invalid': gotError }" placeholder="Pokémon you received" />
          <p v-if="gotError" class="field-error">{{ gotError }}</p>
        </div>
      </div>

      <div v-if="showDesc" class="field">
        <label class="field-label" for="ref-description">Description</label>
        <input id="ref-description" v-model="form.description" type="text" class="field-input" :class="{ 'field-input--invalid': descError }" placeholder="Brief description…" />
        <p v-if="descError" class="field-error">{{ descError }}</p>
      </div>

      <div class="field">
        <label class="field-label" for="ref-notes">Notes</label>
        <textarea id="ref-notes" v-model="form.notes" class="field-textarea" rows="3" placeholder="Public notes visible to everyone…" />
      </div>

      <div class="field">
        <label class="field-label" for="ref-private-notes">
          Private Notes <span class="field-hint">(only visible to you)</span>
        </label>
        <textarea id="ref-private-notes" v-model="form.privateNotes" class="field-textarea" rows="2" placeholder="Private notes only you can see…" />
      </div>

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

<style src="../styles/AddReferenceModal.css" scoped></style>
