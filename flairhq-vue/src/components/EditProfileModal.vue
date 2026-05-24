<script setup lang="ts">
import { ref, watch } from 'vue'
import BaseModal from './BaseModal.vue'

const props = defineProps<{
  modelValue: boolean
  intro: string
  friendCodes: string[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'saved', data: { intro: string; friendCodes: string[] }): void
}>()

const API_BASE = import.meta.env.VITE_API_BASE_URL as string

const localIntro = ref(props.intro)
const localFcs   = ref<string[]>([...props.friendCodes])
const saving     = ref(false)
const error      = ref<string | null>(null)

watch(() => props.modelValue, (open) => {
  if (open) {
    localIntro.value = props.intro
    localFcs.value   = [...props.friendCodes]
    error.value      = null
  }
})

function addFc()           { localFcs.value.push('') }
function removeFc(i: number) { localFcs.value.splice(i, 1) }

function close() { emit('update:modelValue', false) }

async function save() {
  saving.value = true
  error.value  = null
  try {
    const res = await fetch(`${API_BASE}/api/users/me`, {
      method:      'PUT',
      credentials: 'include',
      headers:     { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        intro:       localIntro.value,
        friendCodes: localFcs.value.filter(fc => fc.trim()),
      }),
    })
    if (!res.ok) throw new Error(`${res.status}`)
    emit('saved', {
      intro:       localIntro.value,
      friendCodes: localFcs.value.filter(fc => fc.trim()),
    })
    close()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Save failed'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <BaseModal :model-value="modelValue" @update:model-value="emit('update:modelValue', $event)" max-width="520px">
    <template #header>
      <h2 id="modal-title">Edit Profile</h2>
    </template>

    <template #body>
      <div class="field">
        <label class="field-label" for="intro-input">Introduction</label>
        <textarea
          id="intro-input"
          v-model="localIntro"
          class="field-textarea intro-textarea"
          maxlength="10000"
          rows="5"
          placeholder="Tell traders a bit about yourself…"
        />
        <span class="char-count">{{ localIntro.length }}/10000</span>
      </div>

      <div class="field">
        <label class="field-label">Friend Codes</label>
        <div class="fc-list">
          <div v-for="(_, i) in localFcs" :key="i" class="fc-row">
            <input
              v-model="localFcs[i]"
              type="text"
              class="field-input"
              placeholder="0000-0000-0000"
              maxlength="14"
            />
            <button class="fc-remove" @click="removeFc(i)" aria-label="Remove">&times;</button>
          </div>
          <button class="fc-add" @click="addFc">+ Add Friend Code</button>
        </div>
      </div>

      <p v-if="error" class="save-error">{{ error }}</p>
    </template>

    <template #footer>
      <button class="btn-cancel" @click="close" :disabled="saving">Cancel</button>
      <button class="btn-save" @click="save" :disabled="saving">
        {{ saving ? 'Saving…' : 'Save Changes' }}
      </button>
    </template>
  </BaseModal>
</template>

<style src="../styles/EditProfileModal.css" scoped></style>
