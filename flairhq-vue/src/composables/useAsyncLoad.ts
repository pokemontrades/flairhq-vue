import type { Ref } from 'vue'

export async function withLoading<T>(
  loading: Ref<boolean>,
  error: Ref<string | null>,
  fn: () => Promise<T>,
  fallback = 'An error occurred'
): Promise<T | undefined> {
  loading.value = true
  error.value = null
  try {
    return await fn()
  } catch (e) {
    error.value = e instanceof Error ? e.message : fallback
  } finally {
    loading.value = false
  }
}
