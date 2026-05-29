<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { REFERENCE_CATEGORIES } from '../stores/references'
import type { Reference } from '../stores/references'
import BaseModal from './BaseModal.vue'

const props = defineProps<{
  modelValue: boolean
  reference: Reference | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'saved', ref: Reference): void
}>()

const API_BASE = import.meta.env.VITE_API_BASE_URL as string

const form = ref({
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
const saving = ref(false)
const error  = ref<string | null>(null)

const SUBREDDIT_URL = /^https?:\/\/(www\.|old\.)?reddit\.com\/r\/pokemontrades\//i

const isGiveaway  = computed(() => form.value.type === 'giveaway')
const isDescType  = computed(() => form.value.type === 'involvement' || form.value.type === 'misc')
const showPartner = computed(() => !isGiveaway.value)
const showGaveGot = computed(() => !isGiveaway.value && !isDescType.value)
const showDesc    = computed(() => isGiveaway.value || isDescType.value)
const showNumber  = computed(() => isGiveaway.value)

const touched    = ref(false)
const urlTouched = ref(false)

const urlError     = computed(() => {
  if (!urlTouched.value) return null
  const url = form.value.url.trim()
  if (!url) return 'URL is required'
  return SUBREDDIT_URL.test(url) ? null : 'URL must be from pokemontrades subreddit'
})
const typeError    = computed(() => touched.value && !form.value.type ? 'Type is required' : null)
const partnerError = computed(() => touched.value && showPartner.value && !form.value.user2.trim() ? 'Trading partner is required' : null)
const gaveError    = computed(() => touched.value && showGaveGot.value && !form.value.gave.trim() ? 'Required' : null)
const gotError     = computed(() => touched.value && showGaveGot.value && !form.value.got.trim() ? 'Required' : null)
const descError    = computed(() => touched.value && showDesc.value && !form.value.description.trim() ? 'Description is required' : null)
const numberError  = computed(() => touched.value && showNumber.value && !(form.value.number > 0) ? 'Must be at least 1' : null)

const hasErrors = computed(() =>
  !!(urlError.value || typeError.value || partnerError.value || gaveError.value || gotError.value || descError.value || numberError.value)
)

watch(() => props.modelValue, (open) => {
  if (open && props.reference) {
    const r = props.reference
    form.value = {
      url:          r.url              ?? '',
      user2:        r.user2            ?? '',
      type:         r.type             ?? '',
      gave:         r.gave             ?? '',
      got:          r.got              ?? '',
      description:  r.description      ?? '',
      notes:        r.notes            ?? '',
      privateNotes: (r as any).privateNotes ?? '',
      number:       r.number           ?? 0,
    }
    error.value      = null
    touched.value    = false
    urlTouched.value = false
  }
})

function close() { emit('update:modelValue', false) }

async function save() {
  touched.value    = true
  urlTouched.value = true
  if (!props.reference || hasErrors.value) return
  saving.value = true
  error.value  = null
  try {
    const res = await fetch(`${API_BASE}/api/references/${props.reference.id}`, {
      method:      'PUT',
      credentials: 'include',
      headers:     { 'Content-Type': 'application/json' },
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
      throw new Error(body?.message ?? `Error ${res.status}`)
    }
    const updated: Reference = await res.json()
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
      <div class="field">
        <label class="field-label" for="ref-url">Permalink URL</label>
        <input id="ref-url" v-model="form.url" type="url" class="field-input" :class="{ 'field-input--invalid': urlError }" placeholder="https://reddit.com/r/pokemontrades/comments/…" @blur="urlTouched = true" />
        <p v-if="urlError" class="field-error">{{ urlError }}</p>
      </div>

      <div class="field-row">
        <div class="field">
          <label class="field-label" for="ref-type">Type</label>
          <select id="ref-type" v-model="form.type" class="field-input field-select" :class="{ 'field-input--invalid': typeError }">
            <option v-for="{ type, label } in REFERENCE_CATEGORIES" :key="type" :value="type">{{ label }}</option>
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
      <button class="btn-save" @click="save" :disabled="saving || (touched && hasErrors) || !!urlError">
        {{ saving ? 'Saving…' : 'Save Changes' }}
      </button>
    </template>
  </BaseModal>
</template>
