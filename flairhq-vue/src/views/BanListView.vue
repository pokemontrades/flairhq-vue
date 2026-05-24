<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { apiFetch, API_BASE } from '../lib/apiFetch'
import ModNav from '../components/ModNav.vue'
import StateMessage from '../components/StateMessage.vue'

interface BannedUser {
  id: string
  flair?: { ptrades?: { flairText?: string } }
}

const users   = ref<BannedUser[]>([])
const loading = ref(false)
const error   = ref<string | null>(null)

onMounted(async () => {
  loading.value = true
  error.value   = null
  try {
    const res = await apiFetch(`${API_BASE}/api/users?banned=true`)
    if (!res.ok) throw new Error(`${res.status}`)
    users.value = await res.json()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load ban list'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <ModNav />
  <div class="banlist">
    <StateMessage :loading="loading" :error="error" :empty="users.length === 0" empty-text="No banned users.">
      <div class="user-list">
        <RouterLink
          v-for="user in users"
          :key="user.id"
          :to="{ name: 'userProfile', params: { username: user.id } }"
          class="user-row"
        >
          <span class="username">u/{{ user.id }}</span>
          <span v-if="user.flair?.ptrades?.flairText" class="flair-text">
            {{ user.flair.ptrades.flairText }}
          </span>
        </RouterLink>
      </div>
    </StateMessage>
  </div>
</template>

<style src="../styles/BanListView.css" scoped></style>
