<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { useAuthStore } from '../stores/auth'
import { apiFetch, API_BASE } from '../lib/apiFetch'
import { formatDate } from '../lib/format'
import Pagination from './Pagination.vue'

interface Comment {
  id: string
  user: string
  user2: string
  message: string
  createdAt: string
  updatedAt: string
}

const props = defineProps<{ profileUser: string }>()

const auth = useAuthStore()

const PAGE_SIZE = 5

const comments    = ref<Comment[]>([])
const loading     = ref(false)
const error       = ref<string | null>(null)
const msgText     = ref('')
const posting     = ref(false)
const deleting    = ref<string[]>([])
const showPreview = ref(false)
const currentPage = ref(1)

const isMod      = computed(() => auth.effectiveIsMod)
const isLoggedIn = computed(() => !!auth.user)

const totalPages    = computed(() => Math.max(1, Math.ceil(comments.value.length / PAGE_SIZE)))
const pagedComments = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return comments.value.slice(start, start + PAGE_SIZE)
})

const renderedPreview = computed(() => renderMarkdown(msgText.value))

onMounted(load)

function renderMarkdown(text: string): string {
  const html = marked.parse(text, { async: false }) as string
  return DOMPurify.sanitize(html, { USE_PROFILES: { html: true } })
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await apiFetch(`${API_BASE}/api/comments?user=${encodeURIComponent(props.profileUser)}`)
    if (!res.ok) throw new Error(`${res.status}`)
    comments.value = await res.json()
  } catch {
    error.value = 'Failed to load comments.'
  } finally {
    loading.value = false
  }
}

async function postComment() {
  const text = msgText.value.trim()
  if (!text) return
  posting.value = true
  error.value = null
  try {
    const res = await apiFetch(`${API_BASE}/api/comments`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ user: props.profileUser, message: text }),
    })
    if (!res.ok) throw new Error(`${res.status}`)
    const created: Comment = await res.json()
    comments.value = [...comments.value, created]
    currentPage.value = totalPages.value
    msgText.value = ''
    showPreview.value = false
  } catch {
    error.value = 'Failed to post comment.'
  } finally {
    posting.value = false
  }
}

async function deleteComment(id: string) {
  deleting.value.push(id)
  try {
    const res = await apiFetch(`${API_BASE}/api/comments/${encodeURIComponent(id)}`, { method: 'DELETE' })
    if (!res.ok) throw new Error(`${res.status}`)
    comments.value = comments.value.filter(c => c.id !== id)
  } catch {
    error.value = 'Failed to delete comment.'
  } finally {
    deleting.value.splice(deleting.value.indexOf(id), 1)
  }
}

function canDelete(comment: Comment) {
  return isMod.value || comment.user2 === auth.user?.name
}
</script>

<template>
  <section class="comment-section">
    <h3 class="comment-heading">Comments</h3>

    <div v-if="loading" class="comment-state">Loading…</div>
    <div v-else-if="error" class="comment-error">{{ error }}</div>
    <div v-else-if="comments.length === 0" class="comment-state">No comments yet.</div>

    <ul v-else class="comment-list">
      <li v-for="c in pagedComments" :key="c.id" class="comment-item">
        <div class="comment-body markdown-body" v-html="renderMarkdown(c.message)" />
        <div class="comment-meta">
          <span class="comment-author">u/{{ c.user2 }}</span>
          <span class="comment-date">{{ formatDate(c.createdAt) }}</span>
          <button
            v-if="canDelete(c)"
            class="btn-delete-comment"
            :disabled="deleting.includes(c.id)"
            @click="deleteComment(c.id)"
          >{{ deleting.includes(c.id) ? '…' : 'Delete' }}</button>
        </div>
      </li>
    </ul>

    <Pagination
      v-if="comments.length > PAGE_SIZE"
      v-model="currentPage"
      :total="totalPages"
      class="comment-pagination"
    />

    <div v-if="isLoggedIn" class="comment-compose">
      <div class="compose-tabs">
        <button
          class="compose-tab"
          :class="{ active: !showPreview }"
          type="button"
          @click="showPreview = false"
        >Write</button>
        <button
          class="compose-tab"
          :class="{ active: showPreview }"
          type="button"
          @click="showPreview = true"
        >Preview</button>
      </div>

      <textarea
        v-if="!showPreview"
        v-model="msgText"
        class="comment-input"
        placeholder="Leave a comment… (Markdown supported)"
        rows="3"
        :disabled="posting"
      />
      <div
        v-else
        class="comment-preview markdown-body"
        :class="{ empty: !msgText.trim() }"
      >
        <span v-if="!msgText.trim()" class="preview-empty">Nothing to preview.</span>
        <div v-else v-html="renderedPreview" />
      </div>

      <button class="btn-post" :disabled="posting || !msgText.trim()" @click="postComment">
        {{ posting ? 'Posting…' : 'Post Comment' }}
      </button>
    </div>
    <p v-else class="comment-login-hint">Log in to leave a comment.</p>
  </section>
</template>

<style src="../styles/CommentSection.css" scoped></style>
