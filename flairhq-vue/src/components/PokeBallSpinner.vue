<template>
  <div class="pokeball-spinner-wrap">
    <svg class="pokeball-spinner" viewBox="0 0 40 40" xmlns="http://www.w3.org/2000/svg" aria-label="Loading" role="img">
      <!-- Top half -->
      <path d="M 2 20 A 18 18 0 0 1 38 20 Z" fill="var(--color-primary)"/>
      <!-- Bottom half -->
      <path d="M 2 20 A 18 18 0 0 0 38 20 Z" fill="var(--color-background)"/>
      <!-- Outer ring -->
      <circle cx="20" cy="20" r="18" fill="none" stroke="var(--color-heading)" stroke-width="2"/>
      <!-- Center band -->
      <rect x="2" y="18.75" width="36" height="2.5" fill="var(--color-heading)"/>
      <!-- Button ring -->
      <circle cx="20" cy="20" r="5.5" fill="var(--color-background)" stroke="var(--color-heading)" stroke-width="2"/>
      <!-- Button dot -->
      <circle cx="20" cy="20" r="2.5" fill="var(--color-background-mute)"/>
    </svg>
  </div>
</template>

<style scoped>
.pokeball-spinner-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 3rem 0;
}

.pokeball-spinner {
  width: 56px;
  height: 56px;
  animation: pokeball-spin 0.9s linear infinite;
  filter: drop-shadow(0 2px 6px rgba(0, 0, 0, 0.15));
}

@keyframes pokeball-spin {
  from { transform: rotate(0deg); }
  to   { transform: rotate(360deg); }
}
</style>
