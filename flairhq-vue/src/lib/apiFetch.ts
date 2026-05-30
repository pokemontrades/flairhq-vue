import router from '../router'
import { useAuthStore } from '../stores/auth'

export const API_BASE = import.meta.env.VITE_API_BASE_URL as string

const CSRF_HEADER = 'X-XSRF-TOKEN'
const MUTATING_METHODS = new Set(['POST', 'PUT', 'DELETE', 'PATCH'])

// Cached CSRF token — populated from the X-XSRF-TOKEN response header on any API call
let csrfToken: string | null = null

/**
 * Thin fetch wrapper that adds credentials, manages the CSRF token, and handles session expiry.
 *
 * The backend exposes the current CSRF token in the X-XSRF-TOKEN response header on every
 * response. We cache it here and include it as a request header on state-changing requests.
 * This works for cross-origin setups where document.cookie cannot read the API's XSRF-TOKEN cookie.
 *
 * On a 401 response, if the user was authenticated, clears the session and redirects to the
 * landing page so they aren't silently stuck.
 */
export async function apiFetch(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
  const method = (init?.method ?? 'GET').toUpperCase()

  const extraHeaders: Record<string, string> = {}
  if (MUTATING_METHODS.has(method) && csrfToken) {
    extraHeaders[CSRF_HEADER] = csrfToken
  }

  const res = await fetch(input, {
    credentials: 'include',
    ...init,
    headers: { ...extraHeaders, ...(init?.headers as Record<string, string>) },
  })

  const newToken = res.headers.get(CSRF_HEADER)
  if (newToken) csrfToken = newToken

  if (res.status === 401) {
    const auth = useAuthStore()
    if (auth.isAuthenticated) {
      auth.clearSession()
      router.push({ name: 'landing' })
    }
  }

  return res
}
