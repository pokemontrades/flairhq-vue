import { ref } from 'vue'
import { defineStore } from 'pinia'
import { apiJson } from '../lib/apiFetch'
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
      const data = await apiJson(`/api/search/${type}?q=${encodeURIComponent(q)}`)
      if (type === 'users')      userResults.value      = data as UserResult[]
      if (type === 'references') referenceResults.value = data as Reference[]
      if (type === 'logs')       logResults.value       = data as LogResult[]
      if (type === 'modmails')   modmailResults.value   = data as ModmailResult[]
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
