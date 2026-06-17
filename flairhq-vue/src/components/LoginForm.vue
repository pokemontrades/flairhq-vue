<script setup lang="ts">
import { API_BASE } from '../lib/apiFetch'

function signInWithReddit() {
  // Hand off to the backend — Spring Security handles the full OAuth2 flow
  window.location.href = `${API_BASE}/oauth2/authorization/reddit`
}
</script>

<template>
  <div class="login-form">
    <h2 class="form-title">Welcome back</h2>
    <p class="form-subtitle">Sign in to your FlairHQ account</p>

    <button class="reddit-btn" @click="signInWithReddit">
      <svg class="reddit-icon" viewBox="0 0 20 20" aria-hidden="true">
        <circle cx="10" cy="10" r="10" fill="#FF4500" />
        <path
          fill="#fff"
          d="M16.67 10a1.46 1.46 0 0 0-2.47-1 7.12 7.12 0 0 0-3.85-1.23l.65-3.08 2.13.45a1 1 0 1 0 .14-.73l-2.38-.5a.27.27 0 0 0-.31.2l-.73 3.44a7.14 7.14 0 0 0-3.89 1.23 1.46 1.46 0 1 0-1.61 2.39 2.87 2.87 0 0 0 0 .44c0 2.24 2.61 4.06 5.83 4.06s5.83-1.82 5.83-4.06a2.87 2.87 0 0 0 0-.44 1.46 1.46 0 0 0 .57-1.17ZM7.27 11a1 1 0 1 1 1 1 1 1 0 0 1-1-1Zm5.58 2.71a3.58 3.58 0 0 1-2.85.87 3.58 3.58 0 0 1-2.85-.87.19.19 0 0 1 .27-.27 3.23 3.23 0 0 0 2.58.69 3.23 3.23 0 0 0 2.58-.69.19.19 0 1 1 .27.27Zm-.13-1.71a1 1 0 1 1 1-1 1 1 0 0 1-1 1Z"
        />
      </svg>
      <span>Continue with Reddit</span>
    </button>

    <p class="form-footer">
      By signing in you agree to our <a href="#">Terms of Service</a>.
    </p>
  </div>
</template>

<style src="../styles/LoginForm.css" scoped></style>
