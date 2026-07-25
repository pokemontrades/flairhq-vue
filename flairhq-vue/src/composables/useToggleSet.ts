import { reactive } from 'vue'

/**
 * A reactive Set plus a toggle() helper — for expand/collapse UI state keyed by id
 * (open sections, expanded notes, etc.).
 */
export function useToggleSet<T = string>() {
  const set = reactive(new Set<T>()) as Set<T>

  function toggle(item: T) {
    if (set.has(item)) set.delete(item)
    else set.add(item)
  }

  return { set, toggle }
}
