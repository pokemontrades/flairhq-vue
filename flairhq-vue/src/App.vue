<script setup lang="ts">
import { RouterView, RouterLink } from 'vue-router'
import { useAuthStore } from './stores/auth'
import AppHeader from './components/AppHeader.vue'
import ToastContainer from './components/ToastContainer.vue'

const auth = useAuthStore()
auth.init()
</script>

<template>
  <AppHeader v-if="auth.isAuthenticated" />
  <RouterView />
  <ToastContainer />
  <footer class="app-footer">
    <RouterLink :to="{ name: 'info' }" class="footer-link">About</RouterLink>
    <span class="footer-sep">·</span>
    <RouterLink :to="{ name: 'privacy' }" class="footer-link">Privacy Policy</RouterLink>
  </footer>
</template>

<style src="./styles/App.css" scoped></style>
