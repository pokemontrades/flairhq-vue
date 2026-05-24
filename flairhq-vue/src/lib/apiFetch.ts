import router from '../router'
import { useAuthStore } from '../stores/auth'

export const API_BASE = import.meta.env.VITE_API_BASE_URL as string

/**
 * Thin fetch wrapper that adds credentials and handles session expiry.
 * On a 401 response, if the user was authenticated, clears the session
 * and redirects to the landing page so they aren't silently stuck.
 */
export async function apiFetch(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
  const res = await fetch(input, { credentials: 'include', ...init })
  if (res.status === 401) {
    const auth = useAuthStore()
    if (auth.isAuthenticated) {
      auth.clearSession()
      router.push({ name: 'landing' })
    }
  }
  return res
}
