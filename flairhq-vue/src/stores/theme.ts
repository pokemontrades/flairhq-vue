import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

export type Theme = 'pokeball' | 'pokedex' | 'porygon'

export const THEME_LABELS: Record<Theme, string> = {
  pokeball: 'PokéBall',
  pokedex:  'Dark Pokédex',
  porygon:  'Porygon',
}

export const THEME_CYCLE: Theme[] = ['pokeball', 'pokedex', 'porygon']

export const useThemeStore = defineStore('theme', () => {
  const theme = ref<Theme>((localStorage.getItem('fhq-theme') as Theme) ?? 'pokeball')

  function setTheme(t: Theme) {
    theme.value = t
    localStorage.setItem('fhq-theme', t)
    document.documentElement.setAttribute('data-theme', t)
  }

  return { theme, setTheme }
})
