<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import BaseModal from './BaseModal.vue'

const CONSOLES = ['Switch', '3DS'] as const
const GAMES = ['X','Y','ΩR','αS','S','M','US','UM','LGP','LGE','SW','SH','BD','SP','PLA','SC','VI'] as const

type ConsoleName = typeof CONSOLES[number]
interface FcEntry   { console: ConsoleName | ''; value: string }
interface GameEntry { ign: string; game: string }

const props = defineProps<{
  modelValue: boolean
  currentFlairText: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'saved'): void
}>()

const API_BASE = import.meta.env.VITE_API_BASE_URL as string

const fcs    = ref<FcEntry[]>([{ console: '', value: '' }])
const games  = ref<GameEntry[]>([{ ign: '', game: '' }])
const saving = ref(false)
const error  = ref<string | null>(null)

watch(() => props.modelValue, (open) => {
  if (open) {
    error.value  = null
    saving.value = false
    parseCurrentFlair(props.currentFlairText)
  }
})

function parseCurrentFlair(text: string) {
  if (!text) {
    fcs.value   = [{ console: '', value: '' }]
    games.value = [{ ign: '', game: '' }]
    return
  }
  const stripped = text.replace(/^(:[a-zA-Z0-9_-]+:)+/, '').trim()
  const parts    = stripped.split(' || ')

  if (parts[0]) {
    const rawFcs = parts[0].split(', ').map(s => s.trim()).filter(Boolean)
    fcs.value = rawFcs.length
      ? rawFcs.map(fc => ({ console: fc.startsWith('SW-') ? 'Switch' : '3DS' as ConsoleName, value: fc }))
      : [{ console: '', value: '' }]
  } else {
    fcs.value = [{ console: '', value: '' }]
  }

  if (parts[1]) {
    games.value = parseGamesString(parts[1])
  } else {
    games.value = [{ ign: '', game: '' }]
  }
}

function parseGamesString(str: string): GameEntry[] {
  const result: GameEntry[] = []
  const re = /([^,(]+?)(?:\s*\(([^)]+)\))?(?:,\s*|$)/g
  let m: RegExpExecArray | null
  while ((m = re.exec(str)) !== null) {
    const ign = m[1].trim()
    if (!ign) continue
    const gameList = m[2] ? m[2].split(',').map(g => g.trim()) : ['']
    for (const game of gameList) result.push({ ign, game })
  }
  return result.length ? result : [{ ign: '', game: '' }]
}

function addFc()   { fcs.value.push({ console: '', value: '' }) }
function removeFc(i: number) { fcs.value.splice(i, 1) }
function addGame() { games.value.push({ ign: '', game: '' }) }
function removeGame(i: number) { games.value.splice(i, 1) }

function onConsoleChange(i: number) {
  const entry = fcs.value[i]
  const raw   = entry.value.replace(/^SW-/, '')
  if (entry.console === 'Switch') {
    entry.value = raw ? `SW-${raw}` : ''
  } else {
    entry.value = raw
  }
}

function formatFc(i: number) {
  const entry   = fcs.value[i]
  const isSwitch = entry.console === 'Switch'
  const digits  = entry.value.replace(/^SW-/, '').replace(/\D/g, '').slice(0, 12)
  const parts   = digits.match(/.{1,4}/g) ?? []
  const formatted = parts.join('-')
  entry.value   = isSwitch && formatted ? `SW-${formatted}` : formatted
}

const FC_RE_3DS    = /^\d{4}-\d{4}-\d{4}$/
const FC_RE_SWITCH = /^SW-\d{4}-\d{4}-\d{4}$/

function isValidFc(entry: FcEntry) {
  if (entry.console === 'Switch') return FC_RE_SWITCH.test(entry.value)
  if (entry.console === '3DS')    return FC_RE_3DS.test(entry.value)
  return false
}

function formatGames(list: GameEntry[]): string {
  const byIgn = new Map<string, string[]>()
  for (const e of list) {
    if (!e.ign.trim()) continue
    if (!byIgn.has(e.ign)) byIgn.set(e.ign, [])
    if (e.game.trim()) byIgn.get(e.ign)!.push(e.game.trim())
  }
  return [...byIgn.entries()]
    .map(([ign, gs]) => gs.length ? `${ign} (${gs.join(', ')})` : ign)
    .join(', ')
}

const preview = computed(() => {
  const fcStr   = fcs.value.filter(isValidFc).map(f => f.value).join(', ')
  const gameStr = formatGames(games.value)
  if (!fcStr && !gameStr) return ''
  return `${fcStr} || ${gameStr}`
})

const MAX_LEN = 55

const validationError = computed<string | null>(() => {
  if (fcs.value.some(f => !f.console)) return 'Please select a console type for each friend code.'
  if (fcs.value.some(f => !isValidFc(f))) return 'One or more friend codes are invalid.'
  if (!games.value.some(g => g.ign.trim())) return 'Please enter at least one in-game name.'
  for (const g of games.value) {
    if (!g.ign.trim()) continue
    if (g.ign.length > 12) return 'In-game names must be 12 characters or fewer.'
    if (/[()|\,:]/g.test(g.ign)) return 'In-game name contains an illegal character: ( ) | , or :'
  }
  if (preview.value.length > MAX_LEN) return `Flair text is too long (${preview.value.length}/${MAX_LEN}). Remove a friend code or game.`
  return null
})

const canSave = computed(() => !validationError.value && !!preview.value)

function close() { emit('update:modelValue', false) }

async function save() {
  if (!canSave.value) return
  saving.value = true
  error.value  = null
  try {
    const res = await fetch(`${API_BASE}/api/users/me/flair`, {
      method:      'PUT',
      credentials: 'include',
      headers:     { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ptrades: preview.value }),
    })
    if (res.status === 429) throw new Error('You can only update your flair text once every 2 minutes.')
    if (!res.ok) throw new Error(`${res.status}`)
    emit('saved')
    close()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to save'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <BaseModal :model-value="modelValue" @update:model-value="emit('update:modelValue', $event)" max-width="540px">
    <template #header>
      <h2 id="modal-title">Set Flair Text</h2>
    </template>

    <template #body>
      <!-- Friend Codes -->
      <div class="section">
        <div class="section-header">
          <span class="section-label">Friend Codes</span>
          <button class="btn-add-row" @click="addFc">+ Add</button>
        </div>
        <div class="row-list">
          <div v-for="(fc, i) in fcs" :key="i" class="entry-row">
            <select v-model="fc.console" class="field-select console-select" @change="onConsoleChange(i)">
              <option value="" disabled>Console</option>
              <option v-for="c in CONSOLES" :key="c" :value="c">{{ c }}</option>
            </select>
            <input
              v-model="fc.value"
              type="text"
              class="field-input fc-input"
              :class="{ invalid: fc.value && !isValidFc(fc) }"
              :placeholder="fc.console === 'Switch' ? 'SW-0000-0000-0000' : '0000-0000-0000'"
              @input="formatFc(i)"
            />
            <button
              class="btn-remove-row"
              :disabled="fcs.length === 1"
              @click="removeFc(i)"
              aria-label="Remove"
            >&times;</button>
          </div>
        </div>
      </div>

      <!-- In-Game Names -->
      <div class="section">
        <div class="section-header">
          <span class="section-label">In-Game Names</span>
          <button class="btn-add-row" @click="addGame">+ Add</button>
        </div>
        <div class="row-list">
          <div v-for="(g, i) in games" :key="i" class="entry-row">
            <input
              v-model="g.ign"
              type="text"
              class="field-input ign-input"
              placeholder="IGN"
              maxlength="12"
            />
            <select v-model="g.game" class="field-select game-select">
              <option value="">— Game —</option>
              <option v-for="gm in GAMES" :key="gm" :value="gm">{{ gm }}</option>
            </select>
            <button
              class="btn-remove-row"
              :disabled="games.length === 1"
              @click="removeGame(i)"
              aria-label="Remove"
            >&times;</button>
          </div>
        </div>
      </div>

      <!-- Preview -->
      <div class="preview-block">
        <span class="preview-label">Preview</span>
        <span class="preview-text" :class="{ over: preview.length > MAX_LEN }">
          {{ preview || '—' }}
        </span>
        <span class="preview-len" :class="{ warn: preview.length > MAX_LEN - 10, over: preview.length > MAX_LEN }">
          {{ preview.length }}/{{ MAX_LEN }}
        </span>
      </div>

      <p v-if="validationError" class="validation-error">{{ validationError }}</p>
      <p v-if="error" class="save-error">{{ error }}</p>
      <p class="hint-text">Your flair level emoji is added automatically.</p>
    </template>

    <template #footer>
      <button class="btn-cancel" @click="close" :disabled="saving">Cancel</button>
      <button class="btn-save" @click="save" :disabled="saving || !canSave">
        {{ saving ? 'Saving…' : 'Set Flair Text' }}
      </button>
    </template>
  </BaseModal>
</template>

<style src="../styles/FlairTextModal.css" scoped></style>
