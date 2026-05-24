<script setup lang="ts">
import policyHtml from '../assets/privacy_policy.html?raw'
</script>

<template>
  <div class="privacy-page">
    <nav class="top-nav">
      <RouterLink to="/" class="back-link">← FlairHQ</RouterLink>
    </nav>

    <main class="content">
      <div v-html="policyHtml" />

      <div class="footer-links">
        <RouterLink to="/info">About FlairHQ</RouterLink>
      </div>
    </main>
  </div>
</template>

<style src="../styles/PrivacyView.css" scoped></style>
