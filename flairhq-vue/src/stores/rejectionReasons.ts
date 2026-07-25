import { ref } from 'vue'
import { defineStore } from 'pinia'
import { apiJson } from '../lib/apiFetch'

export interface RejectionReason {
  id: string
  label: string
  reason: string
  createdAt: string
  updatedAt: string
}

export const useRejectionReasonStore = defineStore('rejectionReasons', () => {
  const reasons  = ref<RejectionReason[]>([])
  const loading  = ref(false)
  const loaded   = ref(false)

  async function load() {
    if (loaded.value) return
    loading.value = true
    try {
      reasons.value = await apiJson<RejectionReason[]>('/api/rejection-reasons')
      loaded.value  = true
    } finally {
      loading.value = false
    }
  }

  /** Runs a mutation, returning an error message on failure or null on success. */
  async function attempt(fn: () => Promise<void>, failMessage: string): Promise<string | null> {
    try {
      await fn()
      return null
    } catch {
      return failMessage
    }
  }

  function create(label: string, reason: string) {
    return attempt(async () => {
      const created = await apiJson<RejectionReason>('/api/rejection-reasons', {
        method: 'POST',
        json: { label, reason },
      })
      reasons.value = [...reasons.value, created]
    }, 'Failed to create rejection reason.')
  }

  function update(id: string, label: string, reason: string) {
    return attempt(async () => {
      const updated = await apiJson<RejectionReason>(`/api/rejection-reasons/${encodeURIComponent(id)}`, {
        method: 'PUT',
        json: { label, reason },
      })
      reasons.value = reasons.value.map(r => r.id === id ? updated : r)
    }, 'Failed to update rejection reason.')
  }

  function remove(id: string) {
    return attempt(async () => {
      await apiJson(`/api/rejection-reasons/${encodeURIComponent(id)}`, { method: 'DELETE' })
      reasons.value = reasons.value.filter(r => r.id !== id)
    }, 'Failed to delete rejection reason.')
  }

  return { reasons, loading, loaded, load, create, update, remove }
})
