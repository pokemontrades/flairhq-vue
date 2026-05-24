import { ref } from 'vue'
import { defineStore } from 'pinia'
import { apiFetch, API_BASE } from '../lib/apiFetch'
import { withLoading } from '../composables/useAsyncLoad'

export interface Application {
  id: string
  user: string
  flair: string
  sub: string
  approvedTrades: number
  requiredTrades: number
  createdAt: string
  updatedAt: string
}

export const useApplicationStore = defineStore('applications', () => {
  const applications = ref<Application[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function load() {
    await withLoading(loading, error, async () => {
      const res = await apiFetch(`${API_BASE}/api/applications`)
      if (!res.ok) throw new Error(`${res.status}`)
      applications.value = await res.json()
    }, 'Failed to load applications')
  }

  async function approve(id: string): Promise<string | null> {
    const res = await apiFetch(`${API_BASE}/api/applications/${id}/approve`, { method: 'POST' })
    if (!res.ok) {
      const body = await res.json().catch(() => null)
      return (body?.message as string) || `Error ${res.status}`
    }
    await load()
    return null
  }

  async function deny(id: string, note?: string) {
    await apiFetch(`${API_BASE}/api/applications/${id}/deny`, {
      method: 'POST',
      headers: note ? { 'Content-Type': 'application/json' } : {},
      body: note ? JSON.stringify({ note }) : undefined,
    })
    applications.value = applications.value.filter(a => a.id !== id)
  }

  return { applications, loading, error, load, approve, deny }
})
