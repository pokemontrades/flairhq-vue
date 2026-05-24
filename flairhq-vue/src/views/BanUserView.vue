<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiFetch, API_BASE } from '../lib/apiFetch'
import ModNav from '../components/ModNav.vue'

const route  = useRoute()
const router = useRouter()

const APPEALS_SUFFIX = '\n\n***\n**Please review [this page](https://www.reddit.com/r/pokemontrades/wiki/appeals) for important information before replying or taking any other action.**'
const USERNAME_RE    = /^[A-Za-z0-9_-]{1,20}$/
const FC_RE          = /(\d{4}-){2}\d{4}/g

// Form fields
const username      = ref('')
const banNote       = ref('')
const banMessage    = ref(APPEALS_SUFFIX)
const banlistEntry  = ref('')
const tradeNote     = ref('')
const duration      = ref('')
const knownAlt      = ref('')
const additionalFCs = ref('')

// State
const submitting  = ref(false)
const error       = ref<string | null>(null)
const success     = ref(false)

onMounted(() => {
  const q = route.query.username
  if (typeof q === 'string' && q) username.value = q
})

// ── Derived ─────────────────────────────────────────────────────────────────

const isPermanent = computed(() => !duration.value.trim())

const parsedFCs = computed((): string[] => {
  const matches = additionalFCs.value.match(FC_RE)
  return matches ?? []
})

// ── Validation ───────────────────────────────────────────────────────────────

function stripPrefix(val: string) {
  return val.replace(/^\/?u\//, '')
}

function validate(): string | null {
  const user = stripPrefix(username.value.trim())
  if (!user) return 'Username is required.'
  if (!USERNAME_RE.test(user)) return 'Invalid username.'

  const alt = stripPrefix(knownAlt.value.trim())
  if (alt && !USERNAME_RE.test(alt)) return 'Invalid known alt username.'

  if (banNote.value.length > 300) return 'Ban note cannot exceed 300 characters.'
  if (banMessage.value.length > 1000) return 'Ban message cannot exceed 1000 characters.'

  if (duration.value.trim()) {
    const d = parseInt(duration.value, 10)
    if (isNaN(d) || d < 0) return 'Duration must be a positive number of days.'
    if (d > 999) return 'Duration cannot exceed 999 days. Leave blank for a permanent ban.'
  }

  if (additionalFCs.value.trim() && parsedFCs.value.length === 0) {
    return 'Could not parse any valid friend codes from the Additional FCs field.'
  }

  return null
}

// ── Submit ───────────────────────────────────────────────────────────────────

async function submit() {
  error.value = validate()
  if (error.value) return

  submitting.value = true
  success.value    = false

  const user = stripPrefix(username.value.trim())
  const alt  = stripPrefix(knownAlt.value.trim())

  const body: Record<string, unknown> = {
    banNote:      banNote.value,
    banMessage:   banMessage.value,
    banlistEntry: banlistEntry.value,
    tradeNote:    tradeNote.value,
    knownAlt:     alt || null,
    additionalFCs: parsedFCs.value,
  }

  if (duration.value.trim()) {
    body.duration = parseInt(duration.value, 10)
  }

  try {
    const res = await apiFetch(`${API_BASE}/api/users/${encodeURIComponent(user)}/ban`, {
      method:  'POST',
      headers: { 'Content-Type': 'application/json' },
      body:    JSON.stringify(body),
    })
    if (!res.ok) {
      const text = await res.text().catch(() => '')
      throw new Error(text || `${res.status}`)
    }
    success.value = true
    resetForm()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Ban failed'
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  username.value     = ''
  banNote.value      = ''
  banMessage.value   = APPEALS_SUFFIX
  banlistEntry.value = ''
  tradeNote.value    = ''
  duration.value     = ''
  knownAlt.value     = ''
  additionalFCs.value = ''
}
</script>

<template>
  <ModNav />
  <div class="ban-view">
    <p class="page-subtitle">
      Permanent bans update Reddit flair, usernotes, AutoModerator, and the public banlist.
      Temporary bans only ban from the subreddit.
    </p>

    <div v-if="success" class="success-banner">
      <strong>Ban submitted successfully.</strong>
      <button class="btn-ban-another" @click="success = false">Ban another user</button>
    </div>

    <form v-else class="ban-form" @submit.prevent="submit">

      <!-- Username -->
      <div class="field-group">
        <label class="field-label required" for="ban-username">Username</label>
        <input
          id="ban-username"
          v-model="username"
          type="text"
          class="field-input"
          placeholder="reddit_username"
          autocomplete="off"
          spellcheck="false"
        />
        <span class="field-hint">The Reddit username of the person being banned. The <code>u/</code> prefix is stripped automatically.</span>
      </div>

      <div class="field-row">
        <!-- Duration -->
        <div class="field-group">
          <label class="field-label" for="ban-duration">Duration (days)</label>
          <input
            id="ban-duration"
            v-model="duration"
            type="text"
            class="field-input"
            placeholder="Leave blank for permanent"
          />
          <span class="field-hint">
            <span v-if="isPermanent" class="perm-badge">Permanent ban</span>
            <span v-else class="temp-badge">Temp ban for {{ duration }} day{{ duration === '1' ? '' : 's' }}</span>
          </span>
        </div>

        <!-- Known Alt -->
        <div class="field-group">
          <label class="field-label" for="ban-alt">Known Alt</label>
          <input
            id="ban-alt"
            v-model="knownAlt"
            type="text"
            class="field-input"
            placeholder="main_account (optional)"
            autocomplete="off"
          />
          <span class="field-hint">If this is an alt of a banned user, enter the main account's username to combine banlist entries.</span>
        </div>
      </div>

      <!-- Ban Note (internal) -->
      <div class="field-group">
        <label class="field-label" for="ban-note">
          Ban Note
          <span class="char-counter" :class="{ warn: banNote.length > 270, over: banNote.length > 300 }">
            {{ banNote.length }}/300
          </span>
        </label>
        <input
          id="ban-note"
          v-model="banNote"
          type="text"
          class="field-input"
          placeholder="Internal mod note — not shown to the user"
        />
        <span class="field-hint">Visible to mods on Reddit; not shown to the banned user.</span>
      </div>

      <!-- Banlist Entry (public) -->
      <div class="field-group">
        <label class="field-label" for="ban-entry">Banlist Entry</label>
        <input
          id="ban-entry"
          v-model="banlistEntry"
          type="text"
          class="field-input"
          placeholder='e.g. "Scamming"'
        />
        <span class="field-hint">The ban reason shown on the public banlist. Ignored if this user is an alt of another banned user.</span>
      </div>

      <!-- Trade Note (public) -->
      <div class="field-group">
        <label class="field-label" for="ban-trade">Retrading Allowed?</label>
        <input
          id="ban-trade"
          v-model="tradeNote"
          type="text"
          class="field-input"
          placeholder='e.g. "No"'
        />
        <span class="field-hint">Shown under "Retrading allowed?" on the public banlist.</span>
      </div>

      <!-- Additional FCs -->
      <div class="field-group">
        <label class="field-label" for="ban-fcs">Additional Friend Codes</label>
        <input
          id="ban-fcs"
          v-model="additionalFCs"
          type="text"
          class="field-input"
          placeholder="0000-0000-0000, 1111-1111-1111"
        />
        <span class="field-hint">
          FCs are automatically extracted from the user's flair history. List any extras here.
          <span v-if="parsedFCs.length" class="parsed-fcs">
            Parsed: {{ parsedFCs.join(', ') }}
          </span>
        </span>
      </div>

      <!-- Ban Message (PM) -->
      <div class="field-group">
        <label class="field-label" for="ban-message">
          Ban Message
          <span class="char-counter" :class="{ warn: banMessage.length > 900, over: banMessage.length > 1000 }">
            {{ banMessage.length }}/1000
          </span>
        </label>
        <textarea
          id="ban-message"
          v-model="banMessage"
          class="field-textarea"
          rows="8"
          placeholder="Message PM'd to the banned user"
        />
        <span class="field-hint">This is PM'd to the user. The appeals link at the bottom is standard — type your message above it.</span>
      </div>

      <p v-if="error" class="form-error">{{ error }}</p>

      <div class="form-actions">
        <RouterLink :to="{ name: 'mod' }" class="btn-cancel">Cancel</RouterLink>
        <button
          type="submit"
          class="btn-ban"
          :disabled="submitting"
        >
          {{ submitting ? 'Banning…' : isPermanent ? 'Permanently Ban' : `Ban for ${duration} day${duration === '1' ? '' : 's'}` }}
        </button>
      </div>
    </form>
  </div>
</template>

<style src="../styles/BanUserView.css" scoped></style>
