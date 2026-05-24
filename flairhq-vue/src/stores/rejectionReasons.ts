import { ref } from 'vue'
import { defineStore } from 'pinia'
import { apiFetch, API_BASE } from '../lib/apiFetch'

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
      const res = await apiFetch(`${API_BASE}/api/rejection-reasons`)
      if (!res.ok) throw new Error(`${res.status}`)
      reasons.value = await res.json()
      loaded.value  = true
    } finally {
      loading.value = false
    }
  }

  async function create(label: string, reason: string): Promise<string | null> {
    try {
      const res = await apiFetch(`${API_BASE}/api/rejection-reasons`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ label, reason }),
      })
      if (!res.ok) throw new Error(`${res.status}`)
      reasons.value = [...reasons.value, await res.json()]
      return null
    } catch {
      return 'Failed to create rejection reason.'
    }
  }

  async function update(id: string, label: string, reason: string): Promise<string | null> {
    try {
      const res = await apiFetch(`${API_BASE}/api/rejection-reasons/${encodeURIComponent(id)}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ label, reason }),
      })
      if (!res.ok) throw new Error(`${res.status}`)
      const updated: RejectionReason = await res.json()
      reasons.value = reasons.value.map(r => r.id === id ? updated : r)
      return null
    } catch {
      return 'Failed to update rejection reason.'
    }
  }

  async function remove(id: string): Promise<string | null> {
    try {
      const res = await apiFetch(`${API_BASE}/api/rejection-reasons/${encodeURIComponent(id)}`, {
        method: 'DELETE',
      })
      if (!res.ok) throw new Error(`${res.status}`)
      reasons.value = reasons.value.filter(r => r.id !== id)
      return null
    } catch {
      return 'Failed to delete rejection reason.'
    }
  }

  return { reasons, loading, loaded, load, create, update, remove }
})
