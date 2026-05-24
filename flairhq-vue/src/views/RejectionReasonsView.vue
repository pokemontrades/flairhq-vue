<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRejectionReasonStore } from '../stores/rejectionReasons'
import ModNav from '../components/ModNav.vue'

const store = useRejectionReasonStore()

onMounted(() => store.load())

const error = ref<string | null>(null)

// Add form
const addLabel  = ref('')
const addReason = ref('')
const adding    = ref(false)

async function submitAdd() {
  const label  = addLabel.value.trim()
  const reason = addReason.value.trim()
  if (!label || !reason) return
  adding.value = true
  error.value  = null
  const err = await store.create(label, reason)
  if (err) error.value = err
  else { addLabel.value = ''; addReason.value = '' }
  adding.value = false
}

// Edit state
const editId     = ref<string | null>(null)
const editLabel  = ref('')
const editReason = ref('')
const saving     = ref(false)

function startEdit(id: string, label: string, reason: string) {
  editId.value     = id
  editLabel.value  = label
  editReason.value = reason
  error.value      = null
}

function cancelEdit() {
  editId.value = null
}

async function submitEdit() {
  const id = editId.value
  if (!id) return
  saving.value = true
  error.value  = null
  const err = await store.update(id, editLabel.value.trim(), editReason.value.trim())
  if (err) error.value = err
  else editId.value = null
  saving.value = false
}

// Delete
const confirmDeleteId = ref<string | null>(null)
const deleting        = ref(false)

async function confirmDelete() {
  const id = confirmDeleteId.value
  if (!id) return
  deleting.value = true
  error.value    = null
  const err = await store.remove(id)
  if (err) error.value = err
  else confirmDeleteId.value = null
  deleting.value = false
}
</script>

<template>
  <ModNav />

  <main class="page">
    <h2 class="page-title">Rejection Reasons</h2>
    <p class="page-desc">These appear as quick-select presets when marking a reference as Must Fix or Rejected.</p>

    <p v-if="error" class="error-msg">{{ error }}</p>

    <div v-if="store.loading" class="state-msg">Loading…</div>

    <ul v-else class="reason-list">
      <li v-for="r in store.reasons" :key="r.id" class="reason-item">
        <template v-if="editId === r.id">
          <div class="edit-form">
            <input v-model="editLabel" class="field-input" placeholder="Label" />
            <textarea v-model="editReason" class="field-textarea" rows="3" placeholder="Reason text…" />
            <div class="edit-actions">
              <button class="btn-save" :disabled="saving || !editLabel.trim() || !editReason.trim()" @click="submitEdit">
                {{ saving ? 'Saving…' : 'Save' }}
              </button>
              <button class="btn-cancel" :disabled="saving" @click="cancelEdit">Cancel</button>
            </div>
          </div>
        </template>
        <template v-else>
          <div class="reason-body">
            <span class="reason-label">{{ r.label }}</span>
            <p class="reason-text">{{ r.reason }}</p>
          </div>
          <div class="reason-actions">
            <button class="btn-edit" @click="startEdit(r.id, r.label, r.reason)">Edit</button>
            <template v-if="confirmDeleteId === r.id">
              <span class="confirm-label">Delete?</span>
              <button class="btn-confirm-yes" :disabled="deleting" @click="confirmDelete">
                {{ deleting ? '…' : 'Yes' }}
              </button>
              <button class="btn-cancel-sm" :disabled="deleting" @click="confirmDeleteId = null">Cancel</button>
            </template>
            <button v-else class="btn-delete" @click="confirmDeleteId = r.id">Delete</button>
          </div>
        </template>
      </li>

      <li v-if="!store.loading && store.reasons.length === 0" class="state-msg">
        No rejection reasons yet.
      </li>
    </ul>

    <section class="add-section">
      <h3 class="add-title">Add Reason</h3>
      <div class="add-form">
        <input v-model="addLabel" class="field-input" placeholder="Label (e.g. Wrong Permalink)" :disabled="adding" />
        <textarea
          v-model="addReason"
          class="field-textarea"
          rows="4"
          placeholder="Full reason text shown to the trader…"
          :disabled="adding"
        />
        <button
          class="btn-add"
          :disabled="adding || !addLabel.trim() || !addReason.trim()"
          @click="submitAdd"
        >{{ adding ? 'Adding…' : 'Add Reason' }}</button>
      </div>
    </section>
  </main>
</template>

<style src="../styles/RejectionReasonsView.css" scoped></style>
