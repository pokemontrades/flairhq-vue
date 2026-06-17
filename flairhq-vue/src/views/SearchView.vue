<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useSearchStore } from '../stores/search'
import { REFERENCE_CATEGORIES } from '../stores/references'
import type { SearchType, ModmailResult } from '../stores/search'
import { formatDate } from '../lib/format'
import BaseModal from '../components/BaseModal.vue'

const route = useRoute()
const store = useSearchStore()

const selectedModmail = ref<ModmailResult | null>(null)

function run() {
  const q    = (route.query.q    as string)     || ''
  const type = (route.query.type as SearchType) || 'users'
  if (q) store.search(q, type)
}

onMounted(run)
watch(() => route.query, run)

function labelFor(type: string) {
  return REFERENCE_CATEGORIES.find(c => c.type === type)?.label ?? type
}
</script>

<template>
  <div class="search-view">
    <p class="search-meta" v-if="store.lastQuery">
      Results for <strong>{{ store.lastQuery }}</strong> in {{ store.lastType }}
    </p>

    <div v-if="store.loading" class="state-msg">Searching...</div>
    <div v-else-if="store.error" class="state-msg error">{{ store.error }}</div>

    <!-- Users -->
    <template v-else-if="store.lastType === 'users'">
      <div v-if="store.userResults.length === 0" class="state-msg">No users found.</div>
      <div v-else class="result-list">
        <RouterLink
          v-for="user in store.userResults"
          :key="user.id"
          :to="{ name: 'userProfile', params: { username: user.id } }"
          class="result-row"
        >
          <span class="result-primary">u/{{ user.id }}</span>
          <span v-if="user.flair?.ptrades?.flairText" class="result-secondary">
            {{ user.flair.ptrades.flairText }}
          </span>
          <span v-if="user.isMod" class="badge mod">mod</span>
          <span v-if="user.banned" class="badge banned">banned</span>
        </RouterLink>
      </div>
    </template>

    <!-- References -->
    <template v-else-if="store.lastType === 'references'">
      <div v-if="store.referenceResults.length === 0" class="state-msg">No references found.</div>
      <div v-else class="result-list">
        <RouterLink
          v-for="ref in store.referenceResults"
          :key="ref.id"
          :to="{ name: 'userProfile', params: { username: ref.user } }"
          class="result-row"
        >
          <span class="result-primary">u/{{ ref.user }}</span>
          <span v-if="ref.user2" class="result-secondary">and u/{{ ref.user2 }}</span>
          <span class="result-secondary">{{ labelFor(ref.type) }}</span>
          <span v-if="ref.gave || ref.got" class="result-trade">
            {{ ref.gave }} → {{ ref.got }}
          </span>
          <span v-if="ref.verified" class="result-meta">
            <span class="badge verified">verified</span>
          </span>
        </RouterLink>
      </div>
    </template>

    <!-- Logs -->
    <template v-else-if="store.lastType === 'logs'">
      <div v-if="store.logResults.length === 0" class="state-msg">No logs found.</div>
      <div v-else class="result-list">
        <div
          v-for="log in store.logResults"
          :key="log.id"
          class="result-row log-row"
        >
          <span class="log-type badge">{{ log.type }}</span>
          <span class="result-primary">u/{{ log.user }}</span>
          <span class="result-secondary log-content">{{ log.content }}</span>
          <span class="result-date result-meta">{{ formatDate(log.createdAt) }}</span>
        </div>
      </div>
    </template>

    <!-- Modmails -->
    <template v-else-if="store.lastType === 'modmails'">
      <div v-if="store.modmailResults.length === 0" class="state-msg">No modmails found.</div>
      <div v-else class="result-list">
        <div
          v-for="mail in store.modmailResults"
          :key="mail.id"
          class="result-row modmail-row"
          @click="selectedModmail = mail"
        >
          <div class="modmail-header">
            <span class="result-primary">u/{{ mail.author }}</span>
            <span v-if="mail.subreddit" class="modmail-sub">r/{{ mail.subreddit }}</span>
            <span class="result-date result-meta">{{ formatDate(mail.createdAt) }}</span>
          </div>
          <span class="modmail-subject">{{ mail.subject }}</span>
          <span class="modmail-body">{{ mail.body }}</span>
        </div>
      </div>
    </template>
  </div>

  <BaseModal
    :model-value="!!selectedModmail"
    @update:model-value="!$event && (selectedModmail = null)"
    max-width="680px"
    max-height="80vh"
  >
    <template #header>
      <div class="modal-title-group">
        <h2>{{ selectedModmail?.subject }}</h2>
        <span class="modal-meta">
          u/{{ selectedModmail?.author }}
          <template v-if="selectedModmail?.subreddit"> · r/{{ selectedModmail.subreddit }}</template>
          · {{ selectedModmail ? formatDate(selectedModmail.createdAt) : '' }}
        </span>
      </div>
    </template>
    <template #body>
      <pre class="mail-body-text">{{ selectedModmail?.body }}</pre>
    </template>
  </BaseModal>
</template>

<style src="../styles/SearchView.css" scoped></style>
