import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

export interface RedditUser {
  name: string
  icon_img: string
  total_karma: number
  isMod: boolean
}

const API_BASE = import.meta.env.VITE_API_BASE_URL as string

export const useAuthStore = defineStore('auth', () => {
  const user       = ref<RedditUser | null>(null)
  const loading    = ref(false)
  const error      = ref<string | null>(null)
  const viewAsUser = ref(false)

  const isAuthenticated = computed(() => user.value !== null)
  const effectiveIsMod  = computed(() => (user.value?.isMod ?? false) && !viewAsUser.value)

  /**
   * Check the backend session. Called once on app startup and after the
   * OAuth redirect lands back on the frontend.
   */
  async function fetchMe() {
    loading.value = true
    error.value = null

    try {
      const res = await fetch(`${API_BASE}/api/auth/me`, {
        credentials: 'include', // send the JSESSIONID cookie cross-origin
      })

      if (res.status === 401) {
        user.value = null
        return
      }

      if (!res.ok) throw new Error(`Unexpected response: ${res.status}`)

      user.value = await res.json()
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to load session'
      user.value = null
    } finally {
      loading.value = false
    }
  }

  function clearSession() {
    user.value = null
    initPromise = null
  }

  async function logout() {
    try {
      await fetch(`${API_BASE}/api/auth/logout`, {
        method: 'POST',
        credentials: 'include',
      })
    } finally {
      clearSession()
    }
  }

  let initPromise: Promise<void> | null = null

  function init(): Promise<void> {
    if (!initPromise) {
      initPromise = fetchMe()
    }
    return initPromise
  }

  return { user, loading, error, isAuthenticated, effectiveIsMod, viewAsUser, fetchMe, init, logout, clearSession }
})
