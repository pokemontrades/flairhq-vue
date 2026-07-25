import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { apiJson } from '../lib/apiFetch'
import { withLoading } from '../composables/useAsyncLoad'

export type ReferenceType =
  | 'event' | 'shiny' | 'casual' | 'bank' | 'egg'
  | 'giveaway' | 'involvement' | 'eggcheck' | 'misc'

export interface Reference {
  id: string
  url: string
  user: string
  user2: string
  gave: string
  got: string
  description: string
  type: ReferenceType
  number: number
  verified: boolean
  approved: boolean
  rejected: boolean
  rejectedReason: string | null
  mustFix: boolean
  mustFixReason: string | null
  reciprocalApproved: boolean
  notes: string
  privateNotes?: string
  createdAt: string
  updatedAt: string
}

export const REFERENCE_CATEGORIES: { type: ReferenceType; label: string }[] = [
  { type: 'casual',      label: 'Casual Trades' },
  { type: 'shiny',       label: 'Shiny Trades' },
  { type: 'event',       label: 'Event Trades' },
  { type: 'bank',        label: 'Bank Services' },
  { type: 'egg',         label: 'Egg Trades' },
  { type: 'giveaway',    label: 'Giveaway / Contest' },
  { type: 'involvement', label: 'Free Tradeback / Free Redemption' },
  { type: 'eggcheck',    label: 'Egg Checks' },
  { type: 'misc',        label: 'Miscellaneous' },
]

export const ADDABLE_REFERENCE_CATEGORIES = REFERENCE_CATEGORIES.filter(
  c => c.type !== 'egg' && c.type !== 'eggcheck'
)

/** Display label for a reference type, falling back to the raw type string. */
export function referenceTypeLabel(type: string): string {
  return REFERENCE_CATEGORIES.find(c => c.type === type)?.label ?? type
}

/**
 * Extracts a canonical comparison key from a Reddit permalink so that two references
 * for the same trade can be matched even when their URLs differ cosmetically.
 * Mirrors UrlNormalizer.permalinkBase in the API.
 *
 * Strips query params, then returns the URL up to and including the comment ID
 * (2nd segment after /comments/) when present; otherwise up to the post ID only.
 * Non-Reddit URLs (no /comments/ segment) are returned as-is.
 *
 * Examples:
 *   .../comments/abc123/title-slug/xyz789/  →  .../comments/abc123/title-slug/xyz789/
 *   .../comments/abc123/title-slug/         →  .../comments/abc123/
 *   .../comments/abc123/                    →  .../comments/abc123/
 */
function permalinkBase(url: string | null | undefined): string | null {
  if (!url) return null
  const s = url.includes('?') ? url.substring(0, url.indexOf('?')) : url
  const idx = s.indexOf('/comments/')
  if (idx < 0) return s

  // Segment 1: post ID
  const idStart = idx + '/comments/'.length
  const idEnd   = s.indexOf('/', idStart)
  if (idEnd < 0) return s + '/'

  // Segment 2: title slug
  const titleEnd = s.indexOf('/', idEnd + 1)
  if (titleEnd < 0 || titleEnd === idEnd + 1) return s.substring(0, idEnd + 1)

  // Segment 3: comment ID
  const commentStart = titleEnd + 1
  if (commentStart >= s.length) return s.substring(0, idEnd + 1)

  const commentEnd = s.indexOf('/', commentStart)
  if (commentEnd < 0)              return s + '/'
  if (commentEnd === commentStart) return s.substring(0, idEnd + 1)

  return s.substring(0, commentEnd + 1)
}

export const useReferenceStore = defineStore('references', () => {
  const references = ref<Reference[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const pendingReciprocal = ref<Reference[]>([])

  const byType = computed(() => {
    const map = {} as Record<ReferenceType, Reference[]>
    for (const { type } of REFERENCE_CATEGORIES) {
      map[type] = references.value.filter(r => r.type === type && !r.rejected)
    }
    return map
  })

  const rejectedRefs = computed(() => references.value.filter(r => r.rejected))

  /** Swaps the matching reference in the list with the server's updated copy. */
  function replaceRef(updated: Reference) {
    references.value = references.value.map(r => r.id === updated.id ? updated : r)
  }

  /** POSTs a mod action for one reference and applies the returned update locally. */
  async function postAction(id: string, action: string, body?: unknown): Promise<Reference> {
    const updated = await apiJson<Reference>(`/api/references/${id}/${action}`, {
      method: 'POST',
      ...(body !== undefined ? { json: body } : {}),
    })
    replaceRef(updated)
    return updated
  }

  async function load(username: string) {
    references.value = []
    await withLoading(loading, error, async () => {
      const pageUrl = (page: number) =>
        `/api/references?user=${encodeURIComponent(username)}&page=${page}&size=200`

      // Fetch page 0 first to learn the total, then fan out the rest in parallel.
      const firstPage = await apiJson<{ items: Reference[]; totalPages: number }>(pageUrl(0))
      references.value = firstPage.items

      if (firstPage.totalPages > 1) {
        const remaining = Array.from({ length: firstPage.totalPages - 1 }, (_, i) =>
          apiJson<{ items: Reference[] }>(pageUrl(i + 1)).then(p => p.items)
        )
        const pages = await Promise.all(remaining)
        references.value = [...references.value, ...pages.flat()]
      }
    }, 'Failed to load references')
  }

  async function approve(id: string) {
    const updated = await postAction(id, 'approve')

    // When approval verified the trade, the server also verified the partner's
    // reciprocal reference — mirror that on any matching local copy.
    if (updated.verified) {
      const updatedBase = permalinkBase(updated.url)
      references.value = references.value.map(r => {
        if (r.id !== updated.id &&
            updatedBase !== null && permalinkBase(r.url) === updatedBase &&
            r.user === updated.user2 && r.user2 === updated.user) {
          return { ...r, verified: true }
        }
        return r
      })
    }
  }

  async function unapprove(id: string) {
    await postAction(id, 'unapprove')
  }

  async function reject(id: string, reason?: string) {
    await postAction(id, 'reject', reason ? { reason } : undefined)
  }

  async function setPending(id: string) {
    await postAction(id, 'pending')
  }

  async function markMustFix(id: string, reason: string) {
    await postAction(id, 'must-fix', { reason: reason || null })
  }

  async function update(id: string, body: {
    url?: string; user2?: string; gave?: string; got?: string
    description?: string; type?: string; notes?: string; privateNotes?: string; number?: number
  }) {
    const updated = await apiJson<Reference>(`/api/references/${id}`, { method: 'PUT', json: body })
    replaceRef(updated)
    return updated
  }

  async function loadPendingReciprocal() {
    try {
      pendingReciprocal.value = await apiJson<Reference[]>('/api/references/pending-reciprocal')
    } catch { /* non-critical */ }
  }

  async function remove(id: string) {
    await apiJson(`/api/references/${id}/remove`, { method: 'POST' })
    references.value = references.value.filter(r => r.id !== id)
  }

  return { references, loading, error, byType, rejectedRefs, pendingReciprocal, load, loadPendingReciprocal, approve, unapprove, reject, markMustFix, setPending, update, remove }
})
