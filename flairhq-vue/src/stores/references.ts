import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { apiFetch, API_BASE } from '../lib/apiFetch'
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

  async function load(username: string) {
    references.value = []
    await withLoading(loading, error, async () => {
      const res = await apiFetch(`${API_BASE}/api/references?user=${encodeURIComponent(username)}`)
      if (!res.ok) throw new Error(`${res.status}`)
      references.value = await res.json()
    }, 'Failed to load references')
  }

  async function approve(id: string) {
    const res = await apiFetch(`${API_BASE}/api/references/${id}/approve`, { method: 'POST' })
    if (!res.ok) throw new Error(`${res.status}`)
    const updated: Reference = await res.json()
    references.value = references.value.map(r => r.id === updated.id ? updated : r)
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
    const res = await apiFetch(`${API_BASE}/api/references/${id}/unapprove`, { method: 'POST' })
    if (!res.ok) throw new Error(`${res.status}`)
    const updated: Reference = await res.json()
    references.value = references.value.map(r => r.id === updated.id ? updated : r)
  }

  async function reject(id: string, reason?: string) {
    const res = await apiFetch(`${API_BASE}/api/references/${id}/reject`, {
      method: 'POST',
      headers: reason ? { 'Content-Type': 'application/json' } : {},
      body: reason ? JSON.stringify({ reason }) : undefined,
    })
    if (!res.ok) throw new Error(`${res.status}`)
    const updated: Reference = await res.json()
    references.value = references.value.map(r => r.id === updated.id ? updated : r)
  }

  async function setPending(id: string) {
    const res = await apiFetch(`${API_BASE}/api/references/${id}/pending`, { method: 'POST' })
    if (!res.ok) throw new Error(`${res.status}`)
    const updated: Reference = await res.json()
    references.value = references.value.map(r => r.id === updated.id ? updated : r)
  }

  async function markMustFix(id: string, reason: string) {
    const res = await apiFetch(`${API_BASE}/api/references/${id}/must-fix`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ reason: reason || null }),
    })
    if (!res.ok) throw new Error(`${res.status}`)
    const updated: Reference = await res.json()
    references.value = references.value.map(r => r.id === updated.id ? updated : r)
  }

  async function update(id: string, body: {
    url?: string; user2?: string; gave?: string; got?: string
    description?: string; type?: string; notes?: string; privateNotes?: string; number?: number
  }) {
    const res = await apiFetch(`${API_BASE}/api/references/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    })
    if (!res.ok) throw new Error(`${res.status}`)
    const updated: Reference = await res.json()
    references.value = references.value.map(r => r.id === updated.id ? updated : r)
    return updated
  }

  async function loadPendingReciprocal() {
    try {
      const res = await apiFetch(`${API_BASE}/api/references/pending-reciprocal`)
      if (!res.ok) return
      pendingReciprocal.value = await res.json()
    } catch { /* non-critical */ }
  }

  async function remove(id: string) {
    const res = await apiFetch(`${API_BASE}/api/references/${id}/remove`, { method: 'POST' })
    if (!res.ok) throw new Error(`${res.status}`)
    references.value = references.value.filter(r => r.id !== id)
  }

  return { references, loading, error, byType, rejectedRefs, pendingReciprocal, load, loadPendingReciprocal, approve, unapprove, reject, markMustFix, setPending, update, remove }
})
