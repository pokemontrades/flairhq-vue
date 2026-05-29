<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useThemeStore, THEME_LABELS, THEME_CYCLE } from '../stores/theme'
import type { Theme } from '../stores/theme'
import type { SearchType } from '../stores/search'
import NavMenuItems from './NavMenuItems.vue'

const router = useRouter()
const route  = useRoute()
const auth   = useAuthStore()
const themeStore = useThemeStore()

const query       = ref((route.query.q as string) || '')
const searchType  = ref<SearchType>((route.query.type as SearchType) || 'users')
const menuOpen    = ref(false)
const themeOpen   = ref(false)
const sidebarOpen = ref(false)

watch(() => route.query, (q) => {
  if (q.q)    query.value      = q.q as string
  if (q.type) searchType.value = q.type as SearchType
})

watch(() => route.fullPath, () => {
  menuOpen.value    = false
  sidebarOpen.value = false
})

function handleSearch() {
  if (!query.value.trim()) return
  router.push({ name: 'search', query: { q: query.value.trim(), type: searchType.value } })
  sidebarOpen.value = false
}

function homeDest() {
  return auth.user?.isMod ? { name: 'mod' } : { name: 'profile' }
}

function onMenuBlur(e: FocusEvent) {
  const related = e.relatedTarget as HTMLElement | null
  if (!related?.closest('.user-menu')) menuOpen.value = false
}

function onThemeBlur(e: FocusEvent) {
  const related = e.relatedTarget as HTMLElement | null
  if (!related?.closest('.theme-menu')) themeOpen.value = false
}

function selectTheme(t: Theme) {
  themeStore.setTheme(t)
  themeOpen.value   = false
  menuOpen.value    = false
  sidebarOpen.value = false
}

function closeMenus() {
  menuOpen.value    = false
  sidebarOpen.value = false
}

function toggleViewAs() {
  auth.viewAsUser = !auth.viewAsUser
  closeMenus()
}

async function handleLogout() {
  await auth.logout()
  window.location.replace('/')
}
</script>

<template>
  <header class="app-header">
    <RouterLink :to="homeDest()" class="brand">FlairHQ</RouterLink>

    <form class="search-form" @submit.prevent="handleSearch">
      <select v-model="searchType" class="type-select">
        <option value="users">Users</option>
        <option value="references">References</option>
        <option v-if="auth.user?.isMod" value="logs">Logs</option>
        <option v-if="auth.user?.isMod" value="modmails">Modmails</option>
      </select>
      <input
        v-model="query"
        class="search-input"
        placeholder="Search..."
        type="search"
      />
      <button type="submit" class="search-btn">Search</button>
    </form>

    <span v-if="auth.viewAsUser" class="view-as-banner">Viewing as user</span>

    <div class="theme-menu" @focusout="onThemeBlur">
      <button
        class="theme-trigger"
        :title="`Theme: ${THEME_LABELS[themeStore.theme]}`"
        @click="themeOpen = !themeOpen"
      >
        <svg v-if="themeStore.theme === 'pokeball'" width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" aria-hidden="true">
          <circle cx="8" cy="8" r="6.5"/>
          <line x1="1.5" y1="8" x2="14.5" y2="8"/>
          <circle cx="8" cy="8" r="2" fill="currentColor" stroke="none"/>
        </svg>
        <svg v-else-if="themeStore.theme === 'pokedex'" width="16" height="16" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
          <path d="M14 10.5A6 6 0 0 1 5.5 2a6.5 6.5 0 1 0 8.5 8.5z"/>
        </svg>
        <svg v-else-if="themeStore.theme === 'porygon'" width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" aria-hidden="true">
          <polygon points="8,1.5 13.5,4.75 13.5,11.25 8,14.5 2.5,11.25 2.5,4.75"/>
        </svg>
      </button>
      <div v-if="themeOpen" class="theme-dropdown">
        <button
          v-for="t in THEME_CYCLE"
          :key="t"
          class="theme-option"
          :class="{ active: themeStore.theme === t }"
          @click="selectTheme(t)"
        >
          <svg v-if="t === 'pokeball'" width="14" height="14" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" aria-hidden="true">
            <circle cx="8" cy="8" r="6.5"/>
            <line x1="1.5" y1="8" x2="14.5" y2="8"/>
            <circle cx="8" cy="8" r="2" fill="currentColor" stroke="none"/>
          </svg>
          <svg v-else-if="t === 'pokedex'" width="14" height="14" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
            <path d="M14 10.5A6 6 0 0 1 5.5 2a6.5 6.5 0 1 0 8.5 8.5z"/>
          </svg>
          <svg v-else-if="t === 'porygon'" width="14" height="14" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" aria-hidden="true">
            <polygon points="8,1.5 13.5,4.75 13.5,11.25 8,14.5 2.5,11.25 2.5,4.75"/>
          </svg>
          {{ THEME_LABELS[t] }}
        </button>
      </div>
    </div>

    <!-- Desktop user menu -->
    <div class="user-menu" @focusout="onMenuBlur">
      <button class="user-trigger" :class="{ 'viewing-as': auth.viewAsUser }" @click="menuOpen = !menuOpen">
        <img v-if="auth.user?.icon_img" :src="auth.user.icon_img" class="avatar" alt="avatar" />
        <span class="username">u/{{ auth.user?.name }}</span>
        <span class="chevron" :class="{ open: menuOpen }">▾</span>
      </button>
      <div v-if="menuOpen" class="user-dropdown">
        <NavMenuItems
          variant="dropdown"
          @close="menuOpen = false"
          @toggle-view-as="toggleViewAs"
          @logout="handleLogout"
        />
      </div>
    </div>

    <!-- Mobile hamburger button -->
    <button class="hamburger-btn" @click="sidebarOpen = true" aria-label="Open menu">☰</button>
  </header>

  <!-- Mobile sidebar -->
  <Teleport to="body">
    <Transition name="sidebar">
      <div v-if="sidebarOpen" class="sidebar-backdrop" @click.self="sidebarOpen = false">
        <nav class="sidebar">
          <div class="sidebar-user">
            <img v-if="auth.user?.icon_img" :src="auth.user.icon_img" class="sidebar-avatar" alt="avatar" />
            <div class="sidebar-user-info">
              <span class="sidebar-username">u/{{ auth.user?.name }}</span>
              <span v-if="auth.viewAsUser" class="sidebar-view-as">Viewing as user</span>
            </div>
            <button class="sidebar-close" @click="sidebarOpen = false" aria-label="Close">&times;</button>
          </div>

          <div class="sidebar-divider"></div>

          <NavMenuItems
            variant="sidebar"
            @close="sidebarOpen = false"
              @toggle-view-as="toggleViewAs"
            @logout="handleLogout"
          />
        </nav>
      </div>
    </Transition>
  </Teleport>
</template>

<style src="../styles/AppHeader.css" scoped></style>
