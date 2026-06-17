<script setup lang="ts">
import { API_BASE } from '../lib/apiFetch'

function signInWithReddit() {
  window.location.href = `${API_BASE}/oauth2/authorization/reddit`
}
</script>

<template>
  <div class="landing">
    <section class="hero">
      <div class="hero-content">
        <img src="/images/fhq-500.png" alt="FlairHQ" class="logo" />
        <h1 class="visually-hidden">FlairHQ</h1>
        <p class="tagline">Flair tracking for <span class="sub-link">/r/pokemontrades</span></p>
        <p class="description">
          Log your trades, giveaways, and tradebacks. Apply for flair once you've earned it.
          Moderators review every application so your flair means something.
        </p>
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
      </div>
    </section>

    <section class="steps">
      <img src="/images/steps.png" alt="Step 1: Trade or Hatch. Step 2: Submit to Reference. Step 3: Send Flair Application. Step 4: Get Flair." class="steps-img" />
    </section>

    <section class="features">
      <div class="feature-grid">
        <div class="feature-card">
          <div class="feature-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M9 12l2 2 4-4" />
              <path d="M12 2a10 10 0 1 0 0 20A10 10 0 0 0 12 2z" />
            </svg>
          </div>
          <h3>Reference Tracking</h3>
          <p>Submit trade, giveaway, and tradeback references with Reddit thread links. Approved references count toward your flair thresholds.</p>
        </div>

        <div class="feature-card">
          <div class="feature-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
            </svg>
          </div>
          <h3>Flair Applications</h3>
          <p>Once you hit the required number of approved references, apply for your next trader or helper flair level directly through FlairHQ.</p>
        </div>

        <div class="feature-card">
          <div class="feature-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <circle cx="12" cy="8" r="4" />
              <path d="M4 20c0-4 3.58-7 8-7s8 3 8 7" />
              <path d="M19 8l2 2-4 4" />
            </svg>
          </div>
          <h3>Moderator Review</h3>
          <p>Every application goes through manual review by /r/pokemontrades moderators. Your flair reflects real, verified community participation.</p>
        </div>

        <div class="feature-card">
          <div class="feature-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
              <line x1="16" y1="2" x2="16" y2="6" />
              <line x1="8" y1="2" x2="8" y2="6" />
              <line x1="3" y1="10" x2="21" y2="10" />
            </svg>
          </div>
          <h3>Trade History</h3>
          <p>View your full reference history, track verification status, and see exactly how many approved trades count toward each flair level.</p>
        </div>
      </div>
    </section>

    <footer class="landing-footer">
      <p>&copy; 2026 FlairHQ &mdash; A community tool for <a href="https://reddit.com/r/pokemontrades" target="_blank" rel="noopener">/r/pokemontrades</a></p>
      <p class="footer-links">
        <RouterLink to="/info">About</RouterLink>
        <RouterLink to="/privacy">Privacy Policy</RouterLink>
      </p>
    </footer>
  </div>
</template>

<style src="../styles/LandingView.css" scoped></style>
