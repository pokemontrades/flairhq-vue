<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { apiFetch, API_BASE } from '../lib/apiFetch'
import BaseModal from './BaseModal.vue'

interface Flair {
  id: string
  name: string
  sub: string
  trades: number
  shinyEvents: number
  events: number
  eggs: number
  giveaways: number
  involvement: number
}

interface PendingApp {
  id: string
  flair: string
  sub: string
}

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'applied'): void
}>()


interface RefCounts { casual: number; giveaway: number; involvement: number }

const flairs      = ref<Flair[]>([])
const myApps      = ref<PendingApp[]>([])
const myCssClass  = ref('')
const refCounts   = ref<RefCounts>({ casual: 0, giveaway: 0, involvement: 0 })
const loading     = ref(false)
const selectedId  = ref<string | null>(null)
const submitting  = ref(false)
const error       = ref<string | null>(null)

watch(() => props.modelValue, async (open) => {
  if (!open) return
  selectedId.value = null
  error.value = null
  flairs.value = []
  loading.value = true
  try {
    const [flairRes, appRes, meRes, countsRes] = await Promise.all([
      apiFetch(`${API_BASE}/api/flairs`),
      apiFetch(`${API_BASE}/api/applications/me`),
      apiFetch(`${API_BASE}/api/users/me`),
      apiFetch(`${API_BASE}/api/references/me/counts`),
    ])
    if (!flairRes.ok) throw new Error('Failed to load flairs')
    flairs.value    = await flairRes.json()
    if (appRes.ok)     myApps.value    = await appRes.json()
    if (countsRes.ok)  refCounts.value = await countsRes.json()
    if (meRes.ok) {
      const me = await meRes.json()
      myCssClass.value = me.flair?.ptrades?.flairCssClass ?? ''
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load flairs'
  } finally {
    loading.value = false
  }
})

const ptTrader = computed(() =>
  flairs.value
    .filter(f => f.sub === 'pokemontrades' && !f.involvement && !f.giveaways)
    .sort((a, b) => a.trades - b.trades)
)
const ptHelper = computed(() =>
  flairs.value
    .filter(f => f.sub === 'pokemontrades' && (f.involvement > 0 || f.giveaways > 0))
    .sort((a, b) => a.involvement - b.involvement)
)

const selectedFlair = computed(() => flairs.value.find(f => f.id === selectedId.value) ?? null)

function isPending(flair: Flair) {
  return myApps.value.some(a => a.flair === flair.name)
}

function hasFlair(flair: Flair) {
  if (flair.sub !== 'pokemontrades' || !myCssClass.value) return false
  if (flair.name === 'involvement') return myCssClass.value.includes('1')
  return myCssClass.value.replace(/1/g, '').split(' ').includes(flair.name)
}

function isQualified(flair: Flair) {
  if (flair.trades      > 0 && refCounts.value.casual      < flair.trades)      return false
  if (flair.giveaways   > 0 && refCounts.value.giveaway    < flair.giveaways)   return false
  if (flair.involvement > 0 && refCounts.value.involvement < flair.involvement) return false
  return true
}

function progressLabel(flair: Flair): string | null {
  if (flair.trades > 0 && refCounts.value.casual < flair.trades)
    return `${refCounts.value.casual} / ${flair.trades}`
  if (flair.giveaways > 0 && refCounts.value.giveaway < flair.giveaways)
    return `${refCounts.value.giveaway} / ${flair.giveaways}`
  if (flair.involvement > 0 && refCounts.value.involvement < flair.involvement)
    return `${refCounts.value.involvement} / ${flair.involvement}`
  return null
}

function isSelectable(flair: Flair) {
  return !isPending(flair) && !hasFlair(flair) && isQualified(flair)
}

function formattedName(name: string) {
  if (!name) return ''
  let suffix = ''
  let end: number | undefined
  if (name.includes('ball'))       { suffix = 'Ball';   end = -4 }
  else if (name === 'gen2')        { suffix = 'II Ball'; end = -1 }
  else if (name.includes('charm')) { suffix = 'Charm';  end = -5 }
  else if (name.includes('ribbon')){ suffix = 'Ribbon'; end = -6 }
  else if (name === 'eggcup')      { suffix = 'Cup';    end = -3 }
  else if (name !== 'egg' && name !== 'involvement') { suffix = 'Egg' }
  const base = name.charAt(0).toUpperCase() + name.slice(1, end)
  return suffix ? `${base} ${suffix}` : base
}

function formattedRequirements(flair: Flair) {
  const parts: string[] = []
  if (flair.trades)     parts.push(`${flair.trades} trade${flair.trades !== 1 ? 's' : ''}`)
  if (flair.involvement) parts.push(`${flair.involvement} free tradeback${flair.involvement !== 1 ? 's' : ''}/redemption${flair.involvement !== 1 ? 's' : ''}`)
  if (flair.giveaways) parts.push(`${flair.giveaways} giveaway${flair.giveaways !== 1 ? 's' : ''}`)
  if (flair.eggs) parts.push(`${flair.eggs} hatch${flair.eggs !== 1 ? 'es' : ''}`)
  return parts.join(', ') || 'No requirements'
}

function select(flair: Flair) {
  if (!isSelectable(flair)) return
  selectedId.value = selectedId.value === flair.id ? null : flair.id
}

function close() { emit('update:modelValue', false) }

async function submit() {
  if (!selectedFlair.value) return
  submitting.value = true
  error.value = null
  try {
    const res = await apiFetch(`${API_BASE}/api/applications/me`, {
      method:  'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ flair: selectedFlair.value.name, sub: selectedFlair.value.sub }),
    })
    if (res.status === 409) throw new Error('You already have a pending application for this flair.')
    if (!res.ok) throw new Error(`${res.status}`)
    const created: PendingApp = await res.json()
    myApps.value.push(created)
    selectedId.value = null
    emit('applied')
    close()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Submission failed'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <BaseModal :model-value="modelValue" @update:model-value="emit('update:modelValue', $event)" max-width="700px">
    <template #header>
      <h2 id="modal-title">Apply for Flair</h2>
    </template>

    <template #body>
      <div v-if="loading" class="state-msg">Loading flairs…</div>

      <template v-else>
        <div class="notice">
          Flair applications are reviewed manually by moderators. Higher flairs take longer to process.
        </div>

        <div class="columns">
          <div v-if="ptTrader.length" class="column">
            <p class="group-label">Trader</p>
            <button
              v-for="flair in ptTrader"
              :key="flair.id"
              class="flair-btn"
              :class="{ active: selectedId === flair.id, disabled: !isSelectable(flair) }"
              :title="formattedRequirements(flair)"
              @click="select(flair)"
            >
              <span :class="`flair-${flair.name}`" class="flair-sprite"></span>
              <span class="flair-name">{{ formattedName(flair.name) }}</span>
              <span v-if="hasFlair(flair)"                         class="tag have">✓</span>
              <span v-else-if="isPending(flair)"                   class="tag pending">Pending</span>
              <span v-else-if="progressLabel(flair)"               class="tag progress">{{ progressLabel(flair) }}</span>
            </button>
          </div>

          <div v-if="ptHelper.length" class="column">
            <p class="group-label">Helper</p>
            <button
              v-for="flair in ptHelper"
              :key="flair.id"
              class="flair-btn"
              :class="{ active: selectedId === flair.id, disabled: !isSelectable(flair) }"
              :title="formattedRequirements(flair)"
              @click="select(flair)"
            >
              <span :class="`flair-${flair.name}`" class="flair-sprite"></span>
              <span class="flair-name">{{ formattedName(flair.name) }}</span>
              <span v-if="hasFlair(flair)"                         class="tag have">✓</span>
              <span v-else-if="isPending(flair)"                   class="tag pending">Pending</span>
              <span v-else-if="progressLabel(flair)"               class="tag progress">{{ progressLabel(flair) }}</span>
            </button>
          </div>
        </div>

        <p v-if="error" class="save-error">{{ error }}</p>
      </template>
    </template>

    <template #footer>
      <div class="footer-hint" v-if="selectedFlair">
        Applying for:
        <span :class="`flair-${selectedFlair.name}`" class="flair-sprite"></span>
        <strong>{{ formattedName(selectedFlair.name) }}</strong>
      </div>
      <div class="footer-hint muted" v-else>Select a flair above to apply.</div>
      <div class="footer-actions">
        <button class="btn-cancel" @click="close" :disabled="submitting">Cancel</button>
        <button
          class="btn-submit"
          @click="submit"
          :disabled="!selectedFlair || submitting"
        >
          {{ submitting ? 'Submitting…' : 'Apply' }}
        </button>
      </div>
    </template>
  </BaseModal>
</template>

<style src="../styles/FlairApplyModal.css" scoped></style>
