<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { apiFetch, API_BASE } from '../lib/apiFetch'
import { formatDate } from '../lib/format'
import Pagination from './Pagination.vue'

interface Modnote {
  id: string
  user: string
  refUser: string
  note: string
  createdAt: string
  updatedAt: string
}

const props = defineProps<{ refUser: string }>()

const PAGE_SIZE = 5

const notes       = ref<Modnote[]>([])
const loading     = ref(false)
const error       = ref<string | null>(null)
const deleting    = ref<string[]>([])
const confirmId   = ref<string | null>(null)
const currentPage = ref(1)

const totalPages = computed(() => Math.max(1, Math.ceil(notes.value.length / PAGE_SIZE)))
const pagedNotes = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return notes.value.slice(start, start + PAGE_SIZE)
})

onMounted(load)

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await apiFetch(`${API_BASE}/api/modnotes?refUser=${encodeURIComponent(props.refUser)}`)
    if (!res.ok) throw new Error(`${res.status}`)
    notes.value = await res.json()
  } catch {
    error.value = 'Failed to load mod notes.'
  } finally {
    loading.value = false
  }
}

async function deleteNote(id: string) {
  deleting.value.push(id)
  try {
    const res = await apiFetch(`${API_BASE}/api/modnotes/${encodeURIComponent(id)}`, { method: 'DELETE' })
    if (!res.ok) throw new Error(`${res.status}`)
    notes.value = notes.value.filter(n => n.id !== id)
  } catch {
    error.value = 'Failed to delete note.'
  } finally {
    deleting.value.splice(deleting.value.indexOf(id), 1)
  }
}
</script>

<template>
  <section class="modnote-panel">
    <h3 class="modnote-heading">Mod Notes</h3>

    <p v-if="error" class="modnote-error">{{ error }}</p>

    <div v-if="loading" class="modnote-state">Loading…</div>
    <div v-else-if="notes.length === 0" class="modnote-state">No notes yet.</div>

    <ul v-else class="modnote-list">
      <li v-for="note in pagedNotes" :key="note.id" class="modnote-item">
        <div class="modnote-body">{{ note.note }}</div>
        <div class="modnote-meta">
          <span class="modnote-author">u/{{ note.user }}</span>
          <span class="modnote-date">{{ formatDate(note.createdAt) }}</span>
          <template v-if="confirmId === note.id">
            <span class="modnote-confirm-label">Delete?</span>
            <button
              class="btn-confirm-yes"
              :disabled="deleting.includes(note.id)"
              @click="deleteNote(note.id); confirmId = null"
            >{{ deleting.includes(note.id) ? '…' : 'Yes' }}</button>
            <button class="btn-confirm-cancel" @click="confirmId = null">Cancel</button>
          </template>
          <button
            v-else
            class="btn-delete-note"
            :disabled="deleting.includes(note.id)"
            @click="confirmId = note.id"
          >Delete</button>
        </div>
      </li>
    </ul>

    <Pagination
      v-if="notes.length > PAGE_SIZE"
      v-model="currentPage"
      :total="totalPages"
      class="modnote-pagination"
    />
  </section>
</template>

<style src="../styles/ModnotePanel.css" scoped></style>
