<script setup lang="ts">
import { useAuthStore } from '../stores/auth'

const props = defineProps<{
  variant: 'dropdown' | 'sidebar'
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'logout'): void
  (e: 'toggleViewAs'): void
}>()

const auth = useAuthStore()

function itemClass(modifier?: string) {
  const base = props.variant === 'dropdown' ? 'dropdown-item' : 'sidebar-item'
  return modifier ? `${base} ${base}--${modifier}` : base
}

function dividerClass() {
  return props.variant === 'dropdown' ? 'dropdown-divider' : 'sidebar-divider'
}
</script>

<template>
  <RouterLink :to="{ name: 'profile' }" :class="itemClass()" @click="emit('close')">Profile</RouterLink>
  <RouterLink :to="{ name: 'profile', query: { action: 'setFlairText' } }" :class="itemClass()" @click="emit('close')">Set Flair Text</RouterLink>
  <RouterLink :to="{ name: 'profile', query: { action: 'applyFlair' } }" :class="itemClass()" @click="emit('close')">Apply for Flair</RouterLink>
  <RouterLink :to="{ name: 'profile', query: { action: 'addRef' } }" :class="itemClass()" @click="emit('close')">Add Reference</RouterLink>

  <div :class="dividerClass()" />

  <button
    v-if="auth.user?.isMod"
    :class="itemClass('toggle')"
    @click="emit('toggleViewAs')"
  >{{ auth.viewAsUser ? 'Exit user view' : 'View as user' }}</button>

  <div :class="dividerClass()" />

  <button :class="itemClass('danger')" @click="emit('logout')">Log out</button>
</template>
