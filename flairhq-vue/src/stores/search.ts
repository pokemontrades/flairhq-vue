import { ref } from 'vue'
import { defineStore } from 'pinia'
import { apiFetch, API_BASE } from '../lib/apiFetch'
import type { Reference } from './references'
import { withLoading } from '../composables/useAsyncLoad'

export type SearchType = 'users' | 'references' | 'logs' | 'modmails'

export interface UserResult {
  id: string
  isMod?: boolean
  banned?: boolean
  flair?: { ptrades?: { flairText?: string; flairCssClass?: string } }
}

export interface LogResult {
  id: string
  type: string
  user: string
  content: string
  createdAt: string
  updatedAt: string
}

export interface ModmailResult {
  id: string
  subject: string
  body: string
  author: string
  subreddit: string
  createdAt: string
  updatedAt: string
}

export const useSearchStore = defineStore('search', () => {
  const userResults      = ref<UserResult[]>([])
  const referenceResults = ref<Reference[]>([])
  const logResults       = ref<LogResult[]>([])
  const modmailResults   = ref<ModmailResult[]>([])
  const loading          = ref(false)
  const error            = ref<string | null>(null)
  const lastQuery        = ref('')
  const lastType         = ref<SearchType>('users')

  async function search(q: string, type: SearchType) {
    if (!q.trim()) return
    lastQuery.value = q
    lastType.value  = type
    await withLoading(loading, error, async () => {
      const res = await apiFetch(`${API_BASE}/api/search/${type}?q=${encodeURIComponent(q)}`)
      if (!res.ok) throw new Error(`${res.status}`)
      const data = await res.json()
      if (type === 'users')      userResults.value      = data
      if (type === 'references') referenceResults.value = data
      if (type === 'logs')       logResults.value       = data
      if (type === 'modmails')   modmailResults.value   = data
    }, 'Search failed')
  }

  function clear() {
    userResults.value      = []
    referenceResults.value = []
    logResults.value       = []
    modmailResults.value   = []
    error.value            = null
  }

  return { userResults, referenceResults, logResults, modmailResults, loading, error, lastQuery, lastType, search, clear }
})
