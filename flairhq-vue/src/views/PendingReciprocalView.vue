<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { apiFetch, API_BASE } from '../lib/apiFetch'
import { formatDate } from '../lib/format'
import { REFERENCE_CATEGORIES } from '../stores/references'
import type { Reference } from '../stores/references'
import StateMessage from '../components/StateMessage.vue'

const router = useRouter()

const pending = ref<Reference[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

onMounted(async () => {
  try {
    const res = await apiFetch(`${API_BASE}/api/references/pending-reciprocal`)
    if (!res.ok) throw new Error(`${res.status}`)
    pending.value = await res.json()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load'
  } finally {
    loading.value = false
  }
})

function labelFor(type: string) {
  return REFERENCE_CATEGORIES.find(c => c.type === type)?.label ?? type
}

function addReference(ref: Reference) {
  router.push({ name: 'profile', query: { addRef: '1', url: ref.url, user2: ref.user2, type: ref.type } })
}
</script>

<template>
  <div class="pending-reciprocal">
    <h1 class="page-title">Pending Reciprocal References</h1>
    <p class="subtitle">These trades were logged about you and approved, but you haven't submitted a reference back yet.</p>

    <StateMessage
      :loading="loading"
      :error="error"
      :empty="pending.length === 0"
      empty-text="You're all caught up — no pending reciprocal references."
    >
      <div class="ref-list">
        <div v-for="ref in pending" :key="ref.id" class="ref-row">
          <a :href="ref.url" target="_blank" rel="noopener" class="ref-link">
            <span class="ref-partner">u/{{ ref.user }}</span>
            <span class="ref-type">{{ labelFor(ref.type) }}</span>
            <span v-if="ref.gave || ref.got" class="ref-trade">{{ ref.gave }} → {{ ref.got }}</span>
          </a>
          <span class="ref-meta">
            <span class="ref-date">{{ formatDate(ref.createdAt) }}</span>
            <button class="btn-add" @click="addReference(ref)">Add Reference</button>
          </span>
        </div>
      </div>
    </StateMessage>
  </div>
</template>

<style src="../styles/PendingReciprocalView.css" scoped></style>
