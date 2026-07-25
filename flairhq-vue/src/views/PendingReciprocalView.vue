<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { apiJson } from '../lib/apiFetch'
import { formatDate } from '../lib/format'
import { referenceTypeLabel } from '../stores/references'
import type { Reference } from '../stores/references'
import StateMessage from '../components/StateMessage.vue'

const router = useRouter()

const pending = ref<Reference[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

onMounted(async () => {
  try {
    pending.value = await apiJson<Reference[]>('/api/references/pending-reciprocal')
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load'
  } finally {
    loading.value = false
  }
})

// Send the user to their profile with the Add Reference modal open, prefilled
// with their side of the trade (partner is the ref's author; gave/got swapped).
function addReference(ref: Reference) {
  router.push({
    name: 'profile',
    query: { action: 'addRef', url: ref.url, user2: ref.user, type: ref.type, gave: ref.got, got: ref.gave },
  })
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
            <span class="ref-type">{{ referenceTypeLabel(ref.type) }}</span>
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
