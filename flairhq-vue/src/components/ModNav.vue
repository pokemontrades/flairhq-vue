<script setup lang="ts">
import { onMounted } from 'vue'
import { useApplicationStore } from '../stores/applications'

const appStore = useApplicationStore()

onMounted(() => { if (!appStore.applications.length) appStore.load() })
</script>

<template>
  <header class="mod-header">
    <h1>Moderation</h1>
    <nav class="mod-nav">
      <RouterLink :to="{ name: 'mod' }"       class="mod-nav-link" :end="true">
        Pending Applications
        <span v-if="appStore.applications.length" class="count-badge">
          {{ appStore.applications.length }}
        </span>
      </RouterLink>
      <RouterLink :to="{ name: 'banlist' }"   class="mod-nav-link">Ban List</RouterLink>
      <RouterLink :to="{ name: 'banUser' }"   class="mod-nav-link">Ban User</RouterLink>
      <RouterLink :to="{ name: 'rejectionReasons' }" class="mod-nav-link">Rejection Reasons</RouterLink>
      <RouterLink :to="{ name: 'eventLog' }"         class="mod-nav-link">Event Log</RouterLink>
    </nav>
  </header>
</template>

<style src="../styles/ModNav.css" scoped></style>
