<script setup lang="ts">
import { onMounted, onUnmounted, computed, ref, reactive, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useReferenceStore, REFERENCE_CATEGORIES } from '../stores/references'
import type { Reference } from '../stores/references'
import { useRejectionReasonStore } from '../stores/rejectionReasons'
import { apiFetch, API_BASE } from '../lib/apiFetch'
import { formatDate } from '../lib/format'
import AddReferenceModal from '../components/AddReferenceModal.vue'
import EditProfileModal from '../components/EditProfileModal.vue'
import EditReferenceModal from '../components/EditReferenceModal.vue'
import FlairApplyModal from '../components/FlairApplyModal.vue'
import FlairTextModal from '../components/FlairTextModal.vue'
import ReasonModal from '../components/ReasonModal.vue'
import ModnotePanel from '../components/ModnotePanel.vue'
import PokeBallSpinner from '../components/PokeBallSpinner.vue'
import IconInfo from '../components/icons/IconInfo.vue'

interface UserProfile {
  iconImg: string | null
  intro: string | null
  friendCodes: string[]
  flairText: string | null
  flairCssClass: string | null
  hideReciprocalSection: boolean
}

const BELOW_POKEBALL = new Set(['default', 'gen2', 'gen21'])

const route           = useRoute()
const router          = useRouter()
const auth            = useAuthStore()
const refStore        = useReferenceStore()
const reasonStore     = useRejectionReasonStore()

const username     = computed(() => (route.params.username as string) || auth.user!.name)
const isOwnProfile = computed(() => username.value === auth.user?.name)

const userProfile        = ref<UserProfile>({ iconImg: null, intro: null, friendCodes: [], flairText: null, flairCssClass: null, hideReciprocalSection: false })
const introEl            = ref<HTMLElement | null>(null)
const introExpanded      = ref(false)
const introOverflows     = ref(false)
const showAddRefModal    = ref(false)
const addRefPrefill      = ref<{ url?: string; user2?: string; type?: string; gave?: string; got?: string } | undefined>(undefined)
const showEditModal      = ref(false)
const showFlairModal     = ref(false)
const showFlairTextModal = ref(false)
const showEditRefModal   = ref(false)
const editingRef         = ref<Reference | null>(null)

function openEditRef(ref: Reference) {
  editingRef.value    = ref
  showEditRefModal.value = true
}

function onRefAdded(created: Reference) {
  refStore.references = [created, ...refStore.references]
  refStore.loadPendingReciprocal()
  addRefPrefill.value = undefined
}

async function toggleReciprocalSection() {
  const next = !userProfile.value.hideReciprocalSection
  userProfile.value.hideReciprocalSection = next
  try {
    await apiFetch(`${API_BASE}/api/users/me`, {
      method:  'PUT',
      headers: { 'Content-Type': 'application/json' },
      body:    JSON.stringify({ hideReciprocalSection: next }),
    })
  } catch { /* non-critical — preference is updated locally regardless */ }
}

function openAddRefForReciprocal(r: Reference) {
  addRefPrefill.value = {
    url:   r.url   ?? undefined,
    user2: r.user  ?? undefined,
    type:  r.type  ?? undefined,
    gave:  r.got   ?? undefined,
    got:   r.gave  ?? undefined,
  }
  showAddRefModal.value = true
}

function onRefSaved(updated: Reference) {
  refStore.references = refStore.references.map(r => r.id === updated.id ? updated : r)
}

async function loadProfile(name: string, own: boolean) {
  try {
    const url = own ? `${API_BASE}/api/users/me` : `${API_BASE}/api/users/${name}`
    const res = await apiFetch(url)
    if (res.ok) {
      const data = await res.json()
      userProfile.value = {
        iconImg:                data.iconImg ?? null,
        intro:                  data.intro ?? null,
        friendCodes:            data.friendCodes ?? [],
        flairText:              data.flair?.ptrades?.flairText ?? null,
        flairCssClass:          data.flair?.ptrades?.flairCssClass ?? null,
        hideReciprocalSection:  data.hideReciprocalSection ?? false,
      }
      initOpenSections()
    }
  } catch { /* non-critical — profile display degrades gracefully */ }
  if (openSections.size === 0) initOpenSections()
}

onMounted(async () => {
  document.addEventListener('click', closeMenu)
  refStore.load(username.value)
  if (isOwnProfile.value) refStore.loadPendingReciprocal()
  await loadProfile(username.value, isOwnProfile.value)
})

onUnmounted(() => document.removeEventListener('click', closeMenu))

const isMod     = computed(() => auth.effectiveIsMod)
const totalRefs = computed(() =>
  refStore.references.filter(r => !r.rejected && (!r.mustFix || isMod.value || isOwnProfile.value)).length
)

const verifiedRefs = computed(() =>
  refStore.references.filter(r => !r.rejected && r.verified).length
)

const totalGiven = computed(() =>
  refStore.references
    .filter(r => r.type === 'giveaway' && !r.rejected)
    .reduce((sum, r) => sum + (r.number || 0), 0)
)

function visibleRefs(type: import('../stores/references').ReferenceType) {
  return refStore.byType[type].filter(r => !r.mustFix || isMod.value || isOwnProfile.value)
}

const hasPokeBallOrHigher = computed(() => {
  const css = userProfile.value.flairCssClass
  if (!css) return false
  const primary = (css.split(' ')[0] ?? '').replace(/1$/, '')
  return !BELOW_POKEBALL.has(primary) && primary !== ''
})

const defaultOpenSection = computed(() =>
  hasPokeBallOrHigher.value ? 'event' : 'casual'
)

const openSections = reactive<Set<string>>(new Set())

function initOpenSections() {
  openSections.clear()
  openSections.add(defaultOpenSection.value)
}

function toggleSection(type: string) {
  if (openSections.has(type)) openSections.delete(type)
  else openSections.add(type)
}

const postTitles = reactive<Record<string, string>>({})

async function fetchGiveawayTitles(refs: typeof refStore.references) {
  const giveaways = refs.filter(r => r.type === 'giveaway' && r.url && !(r.id in postTitles))
  await Promise.allSettled(giveaways.map(async r => {
    try {
      const jsonUrl = r.url.replace(/\/$/, '') + '.json'
      const res = await fetch(jsonUrl, { headers: { Accept: 'application/json' } })
      if (!res.ok) return
      const data = await res.json()
      const title = data?.[0]?.data?.children?.[0]?.data?.title
      if (title) postTitles[r.id] = title
    } catch { /* title stays absent — URL will still link */ }
  }))
}

watch(() => route.query.action, (action) => {
  if (action === 'addRef' && isOwnProfile.value) {
    showAddRefModal.value = true
    router.replace({ query: { ...route.query, action: undefined } })
  }
  if (action === 'applyFlair' && isOwnProfile.value) {
    showFlairModal.value = true
    router.replace({ query: { ...route.query, action: undefined } })
  }
  if (action === 'setFlairText' && isOwnProfile.value) {
    showFlairTextModal.value = true
    router.replace({ query: { ...route.query, action: undefined } })
  }
}, { immediate: true })

watch(() => userProfile.value.intro, async () => {
  introExpanded.value = false
  await nextTick()
  introOverflows.value = !!introEl.value && introEl.value.scrollHeight > introEl.value.clientHeight
})

watch(username, async (newUsername) => {
  refStore.load(newUsername)
  userProfile.value = { iconImg: null, intro: null, friendCodes: [], flairText: null, flairCssClass: null, hideReciprocalSection: false }
  await loadProfile(newUsername, newUsername === auth.user?.name)
})

watch(() => refStore.references, (refs) => {
  if (refs.some(r => r.type === 'giveaway')) fetchGiveawayTitles(refs)
}, { immediate: true })

watch(isMod, (mod) => { if (mod) reasonStore.load() }, { immediate: true })

const approving         = reactive<Set<string>>(new Set())
const actioning         = reactive<Set<string>>(new Set())
const openMenuId        = ref<string | null>(null)
const expandedReasons   = reactive<Set<string>>(new Set())
const expandedNotes     = reactive<Set<string>>(new Set())

function toggleReason(id: string) {
  if (expandedReasons.has(id)) expandedReasons.delete(id)
  else expandedReasons.add(id)
}

function toggleNote(id: string) {
  if (expandedNotes.has(id)) expandedNotes.delete(id)
  else expandedNotes.add(id)
}
const mustFixTargetId  = ref<string | null>(null)
const mustFixReason    = ref('')
const rejectTargetId   = ref<string | null>(null)
const rejectReason     = ref('')

function toggleMenu(id: string) {
  openMenuId.value = openMenuId.value === id ? null : id
}

function closeMenu() {
  openMenuId.value = null
}

async function approveRef(id: string) {
  approving.add(id)
  try {
    await refStore.approve(id)
  } finally {
    approving.delete(id)
  }
}

function rejectRef(id: string) {
  closeMenu()
  rejectTargetId.value = id
  rejectReason.value   = ''
}

async function submitReject() {
  const id = rejectTargetId.value
  if (!id) return
  rejectTargetId.value = null
  actioning.add(id)
  try {
    await refStore.reject(id, rejectReason.value || undefined)
  } finally {
    actioning.delete(id)
  }
}

function mustFixRef(id: string) {
  closeMenu()
  mustFixTargetId.value = id
  mustFixReason.value   = ''
}

async function submitMustFix() {
  const id = mustFixTargetId.value
  if (!id) return
  mustFixTargetId.value = null
  actioning.add(id)
  try {
    await refStore.markMustFix(id, mustFixReason.value)
  } finally {
    actioning.delete(id)
  }
}

async function setPendingRef(id: string) {
  closeMenu()
  actioning.add(id)
  try {
    await refStore.setPending(id)
  } finally {
    actioning.delete(id)
  }
}

async function unapproveRef(id: string) {
  actioning.add(id)
  try {
    await refStore.unapprove(id)
  } finally {
    actioning.delete(id)
  }
}

async function removeRef(id: string) {
  closeMenu()
  actioning.add(id)
  try {
    await refStore.remove(id)
  } finally {
    actioning.delete(id)
  }
}

async function approveAll(type: string) {
  const pending = visibleRefs(type as import('../stores/references').ReferenceType)
    .filter(r => !r.approved && !r.rejected && !r.mustFix)
  await Promise.allSettled(pending.map(r => approveRef(r.id)))
}

function otherParty(ref: Reference) {
  return ref.user === username.value ? ref.user2 : ref.user
}

function onProfileSaved(data: { intro: string; friendCodes: string[] }) {
  userProfile.value = { ...userProfile.value, intro: data.intro || null, friendCodes: data.friendCodes }
}

function onFlairTextSaved() {
  // Refetch to get the full updated flairText (server prepends emoji prefix)
  apiFetch(`${API_BASE}/api/users/me`)
    .then(r => r.ok ? r.json() : null)
    .then(data => {
      if (data) userProfile.value = { ...userProfile.value, flairText: data.flair?.ptrades?.flairText ?? null }
    })
}
</script>

<template>
  <div class="profile">
    <div class="profile-header">
      <img v-if="userProfile.iconImg" :src="userProfile.iconImg" class="avatar" :alt="`u/${username} avatar`" />
      <div class="profile-header-info">
        <div class="profile-header-top">
          <h1 class="username">u/{{ username }}</h1>
          <a
            :href="`https://www.reddit.com/user/${username}`"
            target="_blank"
            rel="noopener noreferrer"
            class="reddit-link"
            :aria-label="`View u/${username} on Reddit`"
          >
            <svg class="reddit-logo" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
              <circle cx="10" cy="10" r="10" fill="#FF4500"/>
              <path fill="#fff" d="M16.67 10a1.46 1.46 0 0 0-2.47-1 7.12 7.12 0 0 0-3.85-1.23l.65-3.08 2.13.45a1 1 0 1 0 .18-.93l-2.38-.5a.27.27 0 0 0-.32.2l-.73 3.44a7.14 7.14 0 0 0-3.89 1.23 1.46 1.46 0 1 0-1.61 2.39 2.87 2.87 0 0 0 0 .44c0 2.24 2.61 4.06 5.83 4.06s5.83-1.82 5.83-4.06a2.87 2.87 0 0 0 0-.44 1.46 1.46 0 0 0 .55-1.97zM7.27 11a1 1 0 1 1 1 1 1 1 0 0 1-1-1zm5.58 2.71a3.58 3.58 0 0 1-2.85.86 3.58 3.58 0 0 1-2.85-.86.19.19 0 0 1 .27-.27 3.24 3.24 0 0 0 2.58.72 3.24 3.24 0 0 0 2.58-.72.19.19 0 1 1 .27.27zm-.14-1.71a1 1 0 1 1 1-1 1 1 0 0 1-1 1z"/>
            </svg>
          </a>
          <a
            v-if="isOwnProfile && userProfile.flairText"
            :href="`${API_BASE}/api/discord/authorize`"
            class="btn-discord"
          >
            <svg class="discord-logo" viewBox="0 0 71 55" xmlns="http://www.w3.org/2000/svg" aria-hidden="true" fill="currentColor">
              <path d="M60.1 4.9A58.6 58.6 0 0 0 45.5.6a.2.2 0 0 0-.3.1 40.8 40.8 0 0 0-1.8 3.7 54.1 54.1 0 0 0-16.3 0 37.6 37.6 0 0 0-1.8-3.7.2.2 0 0 0-.3-.1A58.4 58.4 0 0 0 10.9 4.9a.2.2 0 0 0-.1.1C1.6 18.4-.9 31.5.3 44.5a.2.2 0 0 0 .1.2 58.9 58.9 0 0 0 17.7 9 .2.2 0 0 0 .3-.1c1.4-1.9 2.6-3.9 3.6-6a.2.2 0 0 0-.1-.3 38.7 38.7 0 0 1-5.5-2.6.2.2 0 0 1 0-.4l1.1-.9a.2.2 0 0 1 .2 0c11.5 5.3 24 5.3 35.4 0a.2.2 0 0 1 .2 0l1.1.9a.2.2 0 0 1 0 .4 36.2 36.2 0 0 1-5.5 2.6.2.2 0 0 0-.1.3 41.7 41.7 0 0 0 3.6 6 .2.2 0 0 0 .3.1 58.7 58.7 0 0 0 17.8-9 .2.2 0 0 0 .1-.2C73 29.5 69.3 16.6 60.2 5a.1.1 0 0 0-.1-.1zM23.7 36.8c-3.5 0-6.4-3.2-6.4-7.2s2.8-7.2 6.4-7.2c3.6 0 6.5 3.3 6.4 7.2 0 3.9-2.8 7.2-6.4 7.2zm23.6 0c-3.5 0-6.4-3.2-6.4-7.2s2.8-7.2 6.4-7.2c3.6 0 6.5 3.3 6.4 7.2 0 3.9-2.8 7.2-6.4 7.2z"/>
            </svg>
            Join r/pokemontrades Discord
          </a>
          <button
            v-if="isMod && !isOwnProfile"
            class="btn-ban-user"
            @click="router.push({ name: 'banUser', query: { username: username } })"
          >Ban User</button>
        </div>
        <div v-if="isOwnProfile" class="profile-actions">
          <button class="btn-apply-flair" @click="showFlairModal = true">Apply for Flair</button>
          <button class="btn-edit-profile" @click="showFlairTextModal = true">Set Flair Text</button>
          <button class="btn-edit-profile" @click="showEditModal = true">Edit Profile</button>
          <button class="btn-edit-profile" @click="showAddRefModal = true">Add Reference</button>
        </div>
        <p class="ref-count">{{ totalRefs }} reference{{ totalRefs !== 1 ? 's' : '' }}</p>
        <p class="ref-count">{{ verifiedRefs }} verified reference{{ verifiedRefs !== 1 ? 's' : '' }}</p>
        <p v-if="totalGiven > 0" class="ref-count">{{ totalGiven }} Pokémon given away</p>
        <p v-if="userProfile.flairText" class="flair-text-display">{{ userProfile.flairText }}</p>
        <div v-if="userProfile.intro" class="profile-intro-wrap">
          <p
            ref="introEl"
            class="profile-intro"
            :class="{ clamped: !introExpanded }"
          >{{ userProfile.intro }}</p>
          <button
            v-if="introOverflows || introExpanded"
            class="btn-intro-toggle"
            @click="introExpanded = !introExpanded"
          >{{ introExpanded ? 'Show less' : 'Read more' }}</button>
        </div>
        <div v-if="userProfile.friendCodes.length" class="friend-codes">
          <span class="fc-label">Friend Codes:</span>
          <span v-for="fc in userProfile.friendCodes" :key="fc" class="fc-chip">{{ fc }}</span>
        </div>
      </div>
    </div>

    <AddReferenceModal
      v-model="showAddRefModal"
      :prefill="addRefPrefill"
      @added="onRefAdded"
    />

    <ReasonModal
      v-if="mustFixTargetId"
      title="Mark as Must Fix"
      placeholder="Describe what needs to be fixed…"
      action-label="Mark Must Fix"
      :presets="reasonStore.reasons"
      :reason="mustFixReason"
      @update:reason="mustFixReason = $event"
      @close="mustFixTargetId = null"
      @submit="submitMustFix"
    />

    <ReasonModal
      v-if="rejectTargetId"
      title="Reject Reference"
      placeholder="Describe why this reference is being rejected…"
      action-label="Reject"
      :presets="reasonStore.reasons"
      :reason="rejectReason"
      @update:reason="rejectReason = $event"
      @close="rejectTargetId = null"
      @submit="submitReject"
    />

    <EditProfileModal
      v-model="showEditModal"
      :intro="userProfile.intro ?? ''"
      :friend-codes="userProfile.friendCodes"
      @saved="onProfileSaved"
    />

    <FlairApplyModal
      v-model="showFlairModal"
    />

    <FlairTextModal
      v-model="showFlairTextModal"
      :current-flair-text="userProfile.flairText ?? ''"
      @saved="onFlairTextSaved"
    />

    <EditReferenceModal
      v-model="showEditRefModal"
      :reference="editingRef"
      @saved="onRefSaved"
    />

    <section v-if="isOwnProfile && refStore.pendingReciprocal.length > 0" class="reciprocal-section" :class="{ 'reciprocal-section--collapsed': userProfile.hideReciprocalSection }">
      <div class="reciprocal-header">
        <div>
          <h2 class="reciprocal-title">Add Your Reference</h2>
          <p v-if="!userProfile.hideReciprocalSection" class="reciprocal-subtitle">Your partner already submitted — add your side to complete the record.</p>
        </div>
        <button class="btn-reciprocal-toggle" @click="toggleReciprocalSection" :title="userProfile.hideReciprocalSection ? 'Show' : 'Hide'">
          {{ userProfile.hideReciprocalSection ? 'Show' : 'Hide' }}
        </button>
      </div>
      <div v-if="!userProfile.hideReciprocalSection" class="ref-list">
        <div
          v-for="r in refStore.pendingReciprocal"
          :key="r.id"
          class="ref-row reciprocal-row"
        >
          <RouterLink
            :to="{ name: 'userProfile', params: { username: r.user } }"
            class="ref-partner-link"
          >u/{{ r.user }}</RouterLink>
          <span class="ref-type-tag">{{ REFERENCE_CATEGORIES.find(c => c.type === r.type)?.label ?? r.type }}</span>
          <span v-if="r.gave || r.got" class="ref-trade">{{ r.gave }} → {{ r.got }}</span>
          <span v-else-if="r.description" class="ref-trade">{{ r.description }}</span>
          <div class="ref-meta">
            <a v-if="r.url" :href="r.url" target="_blank" rel="noopener" class="reciprocal-permalink">thread</a>
            <button class="btn-approve" @click="openAddRefForReciprocal(r)">Add Reference</button>
          </div>
        </div>
      </div>
    </section>

    <PokeBallSpinner v-if="refStore.loading" />
    <div v-else-if="refStore.error" class="state-msg error">{{ refStore.error }}</div>
    <div v-else-if="totalRefs === 0" class="state-msg">
      No references yet.
      <br v-if="isOwnProfile" />
      <button v-if="isOwnProfile" class="btn-edit-profile" style="margin-top: 12px;" @click="showAddRefModal = true">Add Reference</button>
    </div>

    <template v-else>
      <section
        v-for="{ type, label } in REFERENCE_CATEGORIES"
        :key="type"
      >
        <template v-if="visibleRefs(type).length > 0">
          <h2 class="category-title" @click="toggleSection(type)">
            <span class="section-chevron" :class="{ open: openSections.has(type) }">▶</span>
            {{ label }}
            <span class="category-count">{{ visibleRefs(type).length }}</span>
            <button
              v-if="isMod && !isOwnProfile && visibleRefs(type).some(r => !r.approved)"
              class="btn-approve-all"
              @click.stop="approveAll(type)"
            >Approve All</button>
          </h2>
          <div v-if="openSections.has(type)" class="ref-list">
            <div
              v-for="(ref, i) in visibleRefs(type)"
              :key="ref.id"
              class="ref-row"
              :class="{ unapproved: isMod && !ref.approved && !ref.verified && !ref.mustFix, 'must-fix': ref.mustFix && (isMod || isOwnProfile) }"
            >
              <span class="ref-num">{{ i + 1 }}</span>
              <RouterLink
                v-if="otherParty(ref)"
                :to="{ name: 'userProfile', params: { username: otherParty(ref) } }"
                class="ref-partner-link"
              >{{ ref.type === 'giveaway' ? otherParty(ref) : 'u/' + otherParty(ref) }}</RouterLink>
              <span v-else class="ref-partner-link ref-partner-anon">
                <template v-if="ref.type === 'giveaway'">
                  {{ ref.number ? ref.number + ' given' : '' }}{{ ref.number && ref.description ? ' · ' : '' }}{{ ref.description }}
                </template>
                <template v-else>(unknown)</template>
              </span>
              <a :href="ref.url ?? undefined" target="_blank" rel="noopener" class="ref-link">
                <span v-if="ref.type === 'giveaway' && postTitles[ref.id]" class="ref-post-title">
                  {{ postTitles[ref.id] }}
                </span>
                <span v-else-if="ref.gave || ref.got" class="ref-trade">
                  {{ ref.gave }} → {{ ref.got }}
                </span>
                <span v-else-if="ref.description" class="ref-trade">
                  {{ ref.description }}
                </span>
              </a>
              <span class="ref-meta">
                <span
                  v-if="ref.mustFix && (isMod || isOwnProfile)"
                  class="badge must-fix-badge reason-badge"
                  @click.stop="toggleReason(ref.id)"
                >must fix</span>
                <span v-else-if="ref.verified"                       class="badge verified">verified</span>
                <span v-else-if="isMod && ref.approved"                               class="badge approved">approved</span>
                <span v-else-if="isMod && !ref.approved && ref.reciprocalApproved" class="badge reciprocal-approved">partner approved</span>
                <span v-else-if="isMod && !ref.approved"                           class="badge pending-mod">pending</span>
                <span
                  v-if="ref.notes || (ref.privateNotes && isOwnProfile)"
                  class="note-badge"
                  title="Show notes"
                  @click.stop="toggleNote(ref.id)"
                ><IconInfo /></span>
                <span class="ref-date">{{ formatDate(ref.createdAt) }}</span>
                <button
                  v-if="isOwnProfile"
                  class="btn-edit-ref"
                  @click.stop="openEditRef(ref)"
                >Edit</button>
                <template v-if="isMod">
                  <template v-if="ref.approved && !isOwnProfile">
                    <button
                      class="btn-unapprove"
                      :disabled="actioning.has(ref.id)"
                      @click.stop="unapproveRef(ref.id)"
                    >{{ actioning.has(ref.id) ? '…' : 'Unapprove' }}</button>
                  </template>
                  <template v-else>
                    <button
                      v-if="!ref.mustFix && auth.user?.name !== ref.user && auth.user?.name !== ref.user2"
                      class="btn-approve"
                      :disabled="approving.has(ref.id)"
                      @click.stop="approveRef(ref.id)"
                    >{{ approving.has(ref.id) ? '…' : 'Approve' }}</button>
                    <div class="action-menu" @click.stop>
                      <button
                        class="btn-more"
                        :disabled="actioning.has(ref.id)"
                        @click="toggleMenu(ref.id)"
                      >{{ actioning.has(ref.id) ? '…' : '▾' }}</button>
                      <div v-if="openMenuId === ref.id" class="action-dropdown">
                        <template v-if="ref.mustFix">
                          <button v-if="!isOwnProfile" class="dropdown-item" @click="approveRef(ref.id)">Approve</button>
                          <button v-if="!isOwnProfile" class="dropdown-item" @click="setPendingRef(ref.id)">Set to Pending</button>
                          <button v-if="!isOwnProfile" class="dropdown-item danger" @click="rejectRef(ref.id)">Reject</button>
                          <button class="dropdown-item danger" @click="removeRef(ref.id)">Remove</button>
                        </template>
                        <template v-else>
                          <button v-if="!isOwnProfile" class="dropdown-item" @click="rejectRef(ref.id)">Reject</button>
                          <button v-if="!isOwnProfile" class="dropdown-item" @click="mustFixRef(ref.id)">Must Fix</button>
                          <button class="dropdown-item danger" @click="removeRef(ref.id)">Remove</button>
                        </template>
                      </div>
                    </div>
                  </template>
                </template>
              </span>
              <span
                v-if="expandedReasons.has(ref.id) && ref.mustFix && (isMod || isOwnProfile)"
                class="ref-reason-line"
              >{{ ref.mustFixReason ?? 'No reason provided' }}</span>
              <span
                v-if="expandedNotes.has(ref.id)"
                class="ref-notes-line"
              >
                <span v-if="ref.notes">{{ ref.notes }}</span>
                <span v-if="ref.privateNotes && isOwnProfile" class="ref-notes-private">(private) {{ ref.privateNotes }}</span>
              </span>
            </div>
          </div>
        </template>
      </section>
    </template>

    <section v-if="(isMod || isOwnProfile) && refStore.rejectedRefs.length > 0">
      <h2 class="category-title rejected-title" @click="toggleSection('__rejected__')">
        <span class="section-chevron" :class="{ open: openSections.has('__rejected__') }">▶</span>
        Rejected
        <span class="category-count rejected-count">{{ refStore.rejectedRefs.length }}</span>
      </h2>
      <div v-if="openSections.has('__rejected__')" class="ref-list">
        <div
          v-for="(ref, i) in refStore.rejectedRefs"
          :key="ref.id"
          class="ref-row rejected-row"
        >
          <span class="ref-num">{{ i + 1 }}</span>
          <RouterLink
            v-if="otherParty(ref)"
            :to="{ name: 'userProfile', params: { username: otherParty(ref) } }"
            class="ref-partner-link"
          >{{ ref.type === 'giveaway' ? otherParty(ref) : 'u/' + otherParty(ref) }}</RouterLink>
          <span v-else class="ref-partner-link ref-partner-anon">
            <template v-if="ref.type === 'giveaway'">
              {{ ref.number ? ref.number + ' given' : '' }}{{ ref.number && ref.description ? ' · ' : '' }}{{ ref.description }}
            </template>
            <template v-else>(unknown)</template>
          </span>
          <a :href="ref.url ?? undefined" target="_blank" rel="noopener" class="ref-link">
            <span class="ref-type-tag">{{ ref.type }}</span>
            <span v-if="ref.gave || ref.got" class="ref-trade">{{ ref.gave }} → {{ ref.got }}</span>
          </a>
          <span class="ref-meta">
            <span class="badge rejected-badge reason-badge" @click.stop="toggleReason(ref.id)">rejected</span>
            <span
              v-if="ref.notes || (ref.privateNotes && isOwnProfile)"
              class="note-badge"
              title="Show notes"
              @click.stop="toggleNote(ref.id)"
            ><IconInfo /></span>
            <span class="ref-date">{{ formatDate(ref.createdAt) }}</span>
            <button
              v-if="isMod && !isOwnProfile"
              class="btn-approve"
              :disabled="actioning.has(ref.id)"
              @click.stop="setPendingRef(ref.id)"
            >{{ actioning.has(ref.id) ? '…' : 'Undo' }}</button>
          </span>
          <span v-if="expandedReasons.has(ref.id)" class="ref-reason-line">
            {{ ref.rejectedReason ?? 'No reason provided' }}
          </span>
          <span
            v-if="expandedNotes.has(ref.id)"
            class="ref-notes-line"
          >
            <span v-if="ref.notes">{{ ref.notes }}</span>
            <span v-if="ref.privateNotes && isOwnProfile" class="ref-notes-private">(private) {{ ref.privateNotes }}</span>
          </span>
        </div>
      </div>
    </section>

    <ModnotePanel v-if="isMod && !isOwnProfile" :key="username" :ref-user="username" />
  </div>
</template>

<style src="../styles/ProfileView.css" scoped></style>
