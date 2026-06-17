import { ref, computed } from 'vue'
import type { Ref } from 'vue'
import { SUBREDDIT_URL } from '../lib/format'

interface ReferenceFormFields {
  url: string
  type: string
  user2: string
  gave: string
  got: string
  description: string
  number: number
}

export function useReferenceForm(form: Ref<ReferenceFormFields>) {
  const touched    = ref(false)
  const urlTouched = ref(false)

  const isGiveaway  = computed(() => form.value.type === 'giveaway')
  const isDescType  = computed(() => form.value.type === 'involvement' || form.value.type === 'misc')
  const showPartner = computed(() => !isGiveaway.value)
  const showGaveGot = computed(() => !isGiveaway.value && !isDescType.value)
  const showDesc    = computed(() => isGiveaway.value || isDescType.value)
  const showNumber  = computed(() => isGiveaway.value)

  const urlError     = computed(() => {
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

  return {
    touched, urlTouched,
    showPartner, showGaveGot, showDesc, showNumber,
    urlError, typeError, partnerError, gaveError, gotError, descError, numberError,
    hasErrors,
  }
}
