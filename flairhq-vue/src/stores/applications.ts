import { ref } from 'vue'
import { defineStore } from 'pinia'
import { apiJson } from '../lib/apiFetch'
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
      applications.value = await apiJson<Application[]>('/api/applications')
    }, 'Failed to load applications')
  }

  /** Returns an error message on failure, or null on success. */
  async function approve(id: string): Promise<string | null> {
    try {
      await apiJson(`/api/applications/${id}/approve`, { method: 'POST' })
    } catch (e) {
      const msg = e instanceof Error ? e.message : ''
      return /^\d+$/.test(msg) ? `Error ${msg}` : msg || 'Approval failed'
    }
    await load()
    return null
  }

  async function deny(id: string, note?: string) {
    await apiJson(`/api/applications/${id}/deny`, {
      method: 'POST',
      ...(note ? { json: { note } } : {}),
    })
    applications.value = applications.value.filter(a => a.id !== id)
  }

  return { applications, loading, error, load, approve, deny }
})
