<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { apiFetch, API_BASE } from '../lib/apiFetch'
import ModNav from '../components/ModNav.vue'
import UserLink from '../components/UserLink.vue'

interface EventEntry {
  id: string
  type: string
  user: string
  content: string
  createdAt: string
}

const router = useRouter()

const events     = ref<EventEntry[]>([])
const total      = ref(0)
const totalPages = ref(0)
const loading    = ref(false)
const error      = ref<string | null>(null)

const page  = ref(0)
const userQ = ref('')
const typeQ = ref('')

const PAGE_SIZE = 50

const EVENT_TYPES = [
  'flairTextChange',
  'flairCssChange',
  'banUser',
  'discordJoin',
]

const TYPE_LABELS: Record<string, string> = {
  flairTextChange: 'Flair Text',
  flairCssChange:  'Flair CSS',
  banUser:         'Ban',
  discordJoin:     'Discord',
}

const TYPE_COLORS: Record<string, string> = {
  flairTextChange: 'green',
  flairCssChange:  'blue',
  banUser:         'red',
  discordJoin:     'purple',
}

let debounceTimer: ReturnType<typeof setTimeout>

function scheduleLoad() {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    page.value = 0
    load()
  }, 300)
}

watch(userQ, scheduleLoad)
watch(typeQ, () => { page.value = 0; load() })

async function load() {
  loading.value = true
  error.value   = null
  try {
    const params = new URLSearchParams()
    if (userQ.value.trim()) params.set('user', userQ.value.trim())
    if (typeQ.value)         params.set('type', typeQ.value)
    params.set('page', String(page.value))
    params.set('size', String(PAGE_SIZE))

    const res = await apiFetch(`${API_BASE}/api/events?${params}`)
    if (res.status === 403) { router.push({ name: 'mod' }); return }
    if (!res.ok) throw new Error(`${res.status}`)
    const data = await res.json()
    events.value     = data.events
    total.value      = data.total
    totalPages.value = data.totalPages
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load events'
  } finally {
    loading.value = false
  }
}

function prevPage() { if (page.value > 0) { page.value--; load() } }
function nextPage() { if (page.value < totalPages.value - 1) { page.value++; load() } }

function typeLabel(type: string) { return TYPE_LABELS[type] ?? type }
function typeColor(type: string) { return TYPE_COLORS[type] ?? 'default' }

function formatDate(iso: string) {
  return new Date(iso).toLocaleString('en-US', { month: 'short', day: 'numeric', year: 'numeric', hour: 'numeric', minute: '2-digit' })
}

const rangeStart = computed(() => page.value * PAGE_SIZE + 1)
const rangeEnd   = computed(() => Math.min((page.value + 1) * PAGE_SIZE, total.value))

onMounted(load)
</script>

<template>
  <ModNav />
  <div class="event-log">
    <div class="toolbar">
      <span class="total-badge" v-if="total > 0">{{ total.toLocaleString() }} events</span>
      <div class="filters">
        <input
          v-model="userQ"
          type="text"
          class="filter-input"
          placeholder="Filter by user…"
        />
        <select v-model="typeQ" class="filter-select">
          <option value="">All types</option>
          <option v-for="t in EVENT_TYPES" :key="t" :value="t">{{ typeLabel(t) }}</option>
        </select>
        <button class="btn-refresh" @click="load" :disabled="loading">
          {{ loading ? '…' : 'Refresh' }}
        </button>
      </div>
    </div>

    <div v-if="error" class="state-msg error">{{ error }}</div>
    <div v-else-if="loading && events.length === 0" class="state-msg">Loading…</div>
    <div v-else-if="events.length === 0" class="state-msg">No events found.</div>

    <template v-else>
      <div class="event-table">
        <div class="event-row header-row">
          <span class="col-type">Type</span>
          <span class="col-user">User</span>
          <span class="col-content">Details</span>
          <span class="col-time">Time</span>
        </div>

        <div
          v-for="ev in events"
          :key="ev.id"
          class="event-row"
        >
          <span class="col-type">
            <span class="type-badge" :class="`type-${typeColor(ev.type)}`">
              {{ typeLabel(ev.type) }}
            </span>
          </span>
          <span class="col-user">
            <UserLink :username="ev.user" />
          </span>
          <span class="col-content">{{ ev.content }}</span>
          <span class="col-time">{{ formatDate(ev.createdAt) }}</span>
        </div>
      </div>

      <div class="pagination">
        <span class="page-info">
          {{ rangeStart }}–{{ rangeEnd }} of {{ total.toLocaleString() }}
        </span>
        <div class="page-btns">
          <button class="btn-page" @click="prevPage" :disabled="page === 0 || loading">← Prev</button>
          <button class="btn-page" @click="nextPage" :disabled="page >= totalPages - 1 || loading">Next →</button>
        </div>
      </div>
    </template>
  </div>
</template>

<style src="../styles/EventLogView.css" scoped></style>
