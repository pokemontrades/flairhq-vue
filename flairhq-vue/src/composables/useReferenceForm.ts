import { ref, computed } from 'vue'

/** Editable fields shared by the Add and Edit reference modals. */
export interface ReferenceFormData {
  url: string
  user2: string
  type: string
  gave: string
  got: string
  description: string
  notes: string
  privateNotes: string
  number: number
}

const SUBREDDIT_URL = /^https?:\/\/(www\.|old\.)?reddit\.com\/r\/pokemontrades\//i

export function emptyReferenceForm(): ReferenceFormData {
  return {
    url:          '',
    user2:        '',
    type:         '',
    gave:         '',
    got:          '',
    description:  '',
    notes:        '',
    privateNotes: '',
    number:       0,
  }
}

/**
 * Form state, per-type field visibility, and validation for reference submission.
 * Shared by AddReferenceModal and EditReferenceModal.
 *
 * Which fields apply depends on the reference type:
 * - giveaway:            description + number given (no partner, no gave/got)
 * - involvement / misc:  partner + description (no gave/got)
 * - everything else:     partner + gave/got
 *
 * Validation messages only appear once the relevant field has been "touched":
 * the URL validates on blur, everything else on first submit attempt.
 */
export function useReferenceForm() {
  const form       = ref(emptyReferenceForm())
  const touched    = ref(false)
  const urlTouched = ref(false)

  const isGiveaway  = computed(() => form.value.type === 'giveaway')
  const isDescType  = computed(() => form.value.type === 'involvement' || form.value.type === 'misc')
  const showPartner = computed(() => !isGiveaway.value)
  const showGaveGot = computed(() => !isGiveaway.value && !isDescType.value)
  const showDesc    = computed(() => isGiveaway.value || isDescType.value)
  const showNumber  = computed(() => isGiveaway.value)

  const urlError = computed(() => {
    if (!urlTouched.value) return null
    const url = form.value.url.trim()
    if (!url) return 'URL is required'
    return SUBREDDIT_URL.test(url) ? null : 'URL must be from pokemontrades subreddit'
  })
  const typeError    = computed(() => touched.value && !form.value.type ? 'Type is required' : null)
  const partnerError = computed(() => touched.value && showPartner.value && !form.value.user2.trim() ? 'Trading partner is required' : null)
  const gaveError    = computed(() => touched.value && showGaveGot.value && !form.value.gave.trim() ? 'Required' : null)
  const gotError     = computed(() => touched.value && showGaveGot.value && !form.value.got.trim() ? 'Required' : null)
  const descError    = computed(() => touched.value && showDesc.value && !form.value.description.trim() ? 'Description is required' : null)
  const numberError  = computed(() => touched.value && showNumber.value && !(form.value.number > 0) ? 'Must be at least 1' : null)

  const hasErrors = computed(() =>
    !!(urlError.value || typeError.value || partnerError.value || gaveError.value || gotError.value || descError.value || numberError.value)
  )

  /** Reinitializes the form (optionally with prefilled values) and clears touched state. */
  function reset(initial?: Partial<ReferenceFormData>) {
    form.value       = { ...emptyReferenceForm(), ...initial }
    touched.value    = false
    urlTouched.value = false
  }

  /** Marks every field touched so all validation errors become visible (called on submit). */
  function markAllTouched() {
    touched.value    = true
    urlTouched.value = true
  }

  /** Builds the API request body, nulling out fields that don't apply to the selected type. */
  function toRequestBody() {
    return {
      url:          form.value.url          || null,
      user2:        showPartner.value  ? (form.value.user2       || null) : null,
      type:         form.value.type         || null,
      gave:         showGaveGot.value  ? (form.value.gave        || null) : null,
      got:          showGaveGot.value  ? (form.value.got         || null) : null,
      description:  showDesc.value     ? (form.value.description || null) : null,
      notes:        form.value.notes        || null,
      privateNotes: form.value.privateNotes || null,
      number:       showNumber.value   ? (form.value.number      || null) : null,
    }
  }

  return {
    form, touched, urlTouched,
    showPartner, showGaveGot, showDesc, showNumber,
    urlError, typeError, partnerError, gaveError, gotError, descError, numberError, hasErrors,
    reset, markAllTouched, toRequestBody,
  }
}
