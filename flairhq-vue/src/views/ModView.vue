<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useApplicationStore } from '../stores/applications'
import type { Application } from '../stores/applications'
import { useToast } from '../composables/useToast'
import { formatDate } from '../lib/format'
import ModNav from '../components/ModNav.vue'
import BaseModal from '../components/BaseModal.vue'
import StateMessage from '../components/StateMessage.vue'
import UserLink from '../components/UserLink.vue'

const appStore = useApplicationStore()
const { show: showToast } = useToast()

onMounted(() => appStore.load())

const maxTradesForUser = computed(() => {
  const map: Record<string, number> = {}
  for (const app of appStore.applications) {
    if (app.requiredTrades > 0) {
      map[app.user] = Math.max(map[app.user] ?? 0, app.requiredTrades)
    }
  }
  return map
})

const pendingDeny = ref<Application | null>(null)
const denyNote    = ref('')
const denying     = ref(false)

async function handleApprove(id: string) {
  const err = await appStore.approve(id)
  if (err) showToast(err, 'error')
}

function openDenyModal(app: Application) {
  pendingDeny.value = app
  denyNote.value    = ''
}

function closeDenyModal() {
  pendingDeny.value = null
  denyNote.value    = ''
}

async function confirmDeny() {
  if (!pendingDeny.value) return
  denying.value = true
  try {
    await appStore.deny(pendingDeny.value.id, denyNote.value.trim() || undefined)
    closeDenyModal()
  } finally {
    denying.value = false
  }
}
</script>

<template>
  <ModNav />
  <div class="mod-view">
    <StateMessage
      :loading="appStore.loading"
      :error="appStore.error"
      :empty="appStore.applications.length === 0"
      empty-text="No pending applications."
    >
      <div class="app-table">
        <div class="app-row header-row">
          <span>User</span>
          <span>Flair</span>
          <span>Subreddit</span>
          <span>Progress</span>
          <span>Submitted</span>
          <span></span>
        </div>
        <div
          v-for="app in appStore.applications"
          :key="app.id"
          class="app-row"
        >
          <span class="app-user">
            <UserLink :username="app.user" />
          </span>
          <span class="app-flair">{{ app.flair }}</span>
          <span class="app-sub">r/{{ app.sub }}</span>
          <span class="app-progress">
            <span v-if="app.flair !== 'involvement' && app.requiredTrades > 0" class="progress-badge" :class="{ met: app.approvedTrades >= app.requiredTrades }">
              {{ app.approvedTrades }} / {{ app.requiredTrades }}
            </span>
          </span>
          <span class="app-date">{{ formatDate(app.createdAt) }}</span>
          <span class="app-actions">
            <button
              class="btn-approve"
              :disabled="app.requiredTrades > 0 && (app.approvedTrades < app.requiredTrades || app.requiredTrades < (maxTradesForUser[app.user] ?? 0))"
              @click="handleApprove(app.id)"
            >Approve</button>
            <button class="btn-deny" @click="openDenyModal(app)">Deny</button>
          </span>
        </div>
      </div>
    </StateMessage>
  </div>

  <BaseModal
    :model-value="!!pendingDeny"
    @update:model-value="!$event && closeDenyModal()"
    max-width="480px"
    max-height="none"
  >
    <template #header>
      <h2>Deny Application</h2>
    </template>
    <template #body>
      <p class="deny-summary">
        Denying <strong>u/{{ pendingDeny?.user }}</strong>'s application for
        <strong>{{ pendingDeny?.flair }}</strong> flair on r/{{ pendingDeny?.sub }}.
      </p>
      <div class="field">
        <label class="field-label" for="deny-note">
          Additional Moderator Notes <span class="field-hint">(optional — included in the PM to the user)</span>
        </label>
        <textarea
          id="deny-note"
          v-model="denyNote"
          class="field-textarea"
          rows="4"
          placeholder="Add any notes for the user…"
        />
      </div>
    </template>
    <template #footer>
      <button class="btn-cancel" @click="closeDenyModal" :disabled="denying">Cancel</button>
      <button class="btn-deny-confirm" @click="confirmDeny" :disabled="denying">
        {{ denying ? 'Denying…' : 'Confirm Deny' }}
      </button>
    </template>
  </BaseModal>
</template>

<style src="../styles/ModView.css" scoped></style>
