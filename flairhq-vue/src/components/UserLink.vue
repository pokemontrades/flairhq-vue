<script setup lang="ts">
defineProps<{ username: string }>()
</script>

<template>
  <RouterLink :to="{ name: 'userProfile', params: { username } }" class="user-link">
    u/{{ username }}
  </RouterLink>
</template>
