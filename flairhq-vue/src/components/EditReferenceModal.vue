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

const urlError = computed(() => {
  const url = form.value.url.trim()
  if (!url) return null
  return SUBREDDIT_URL.test(url) ? null : 'URL must be from pokemontrades subreddit'
})

const isGiveaway  = computed(() => form.value.type === 'giveaway')
const isDescType  = computed(() => form.value.type === 'involvement' || form.value.type === 'misc')
const showPartner = computed(() => !isGiveaway.value)
const showGaveGot = computed(() => !isGiveaway.value && !isDescType.value)
const showDesc    = computed(() => isGiveaway.value || isDescType.value)
const showNumber  = computed(() => isGiveaway.value)

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
    error.value = null
  }
})

function close() { emit('update:modelValue', false) }

async function save() {
  if (!props.reference || urlError.value) return
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
    if (!res.ok) throw new Error(`${res.status}`)
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
        <input id="ref-url" v-model="form.url" type="url" class="field-input" :class="{ 'field-input--invalid': urlError }" placeholder="https://reddit.com/r/pokemontrades/comments/…" />
        <p v-if="urlError" class="field-error">{{ urlError }}</p>
      </div>

      <div class="field-row">
        <div class="field">
          <label class="field-label" for="ref-type">Type</label>
          <select id="ref-type" v-model="form.type" class="field-input field-select">
            <option v-for="{ type, label } in REFERENCE_CATEGORIES" :key="type" :value="type">{{ label }}</option>
          </select>
        </div>
        <div v-if="showPartner" class="field">
          <label class="field-label" for="ref-partner">Trading Partner</label>
          <input id="ref-partner" v-model="form.user2" type="text" class="field-input" placeholder="u/username" />
        </div>
        <div v-if="showNumber" class="field">
          <label class="field-label" for="ref-number">Number Given</label>
          <input id="ref-number" v-model.number="form.number" type="number" min="0" class="field-input" />
        </div>
      </div>

      <div v-if="showGaveGot" class="field-row">
        <div class="field">
          <label class="field-label" for="ref-gave">Gave</label>
          <input id="ref-gave" v-model="form.gave" type="text" class="field-input" placeholder="Pokémon you gave" />
        </div>
        <div class="field">
          <label class="field-label" for="ref-got">Got</label>
          <input id="ref-got" v-model="form.got" type="text" class="field-input" placeholder="Pokémon you received" />
        </div>
      </div>

      <div v-if="showDesc" class="field">
        <label class="field-label" for="ref-description">Description</label>
        <input id="ref-description" v-model="form.description" type="text" class="field-input" placeholder="Brief description…" />
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
      <button class="btn-save" @click="save" :disabled="saving || !!urlError">
        {{ saving ? 'Saving…' : 'Save Changes' }}
      </button>
    </template>
  </BaseModal>
</template>
