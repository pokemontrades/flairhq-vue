<script setup lang="ts">
import type { ReferenceType } from '../stores/references'
import type { ReferenceFormData, useReferenceForm } from '../composables/useReferenceForm'

// The parent owns the form state (created via useReferenceForm) and passes it
// down; this component only renders the shared fields and mutates `form` in place.
const props = defineProps<{
  form: ReferenceFormData
  rf: Omit<ReturnType<typeof useReferenceForm>, 'form'>
  categories: { type: ReferenceType; label: string }[]
  typePlaceholder?: string
}>()

// Destructured refs are auto-unwrapped in the template below. Safe because the
// composable object is created once and never replaced.
const {
  urlTouched,
  showPartner, showGaveGot, showDesc, showNumber,
  urlError, typeError, partnerError, gaveError, gotError, descError, numberError,
} = props.rf
</script>

<template>
  <!-- eslint-disable vue/no-mutating-props -- the parent deliberately shares
       its useReferenceForm state; mutating `form` in place is the contract. -->
  <div class="field">
    <label class="field-label" for="ref-url">Permalink URL</label>
    <input id="ref-url" v-model="form.url" type="url" class="field-input" :class="{ 'field-input--invalid': urlError }" placeholder="https://reddit.com/r/pokemontrades/comments/…" @blur="urlTouched = true" />
    <p v-if="urlError" class="field-error">{{ urlError }}</p>
  </div>

  <div class="field-row">
    <div class="field">
      <label class="field-label" for="ref-type">Type</label>
      <select id="ref-type" v-model="form.type" class="field-input field-select" :class="{ 'field-input--invalid': typeError }">
        <option v-if="typePlaceholder" value="" disabled>{{ typePlaceholder }}</option>
        <option v-for="{ type, label } in categories" :key="type" :value="type">{{ label }}</option>
      </select>
      <p v-if="typeError" class="field-error">{{ typeError }}</p>
    </div>
    <div v-if="showPartner" class="field">
      <label class="field-label" for="ref-partner">Trading Partner</label>
      <input id="ref-partner" v-model="form.user2" type="text" class="field-input" :class="{ 'field-input--invalid': partnerError }" placeholder="u/username" />
      <p v-if="partnerError" class="field-error">{{ partnerError }}</p>
    </div>
    <div v-if="showNumber" class="field">
      <label class="field-label" for="ref-number">Number Given</label>
      <input id="ref-number" v-model.number="form.number" type="number" min="1" class="field-input" :class="{ 'field-input--invalid': numberError }" />
      <p v-if="numberError" class="field-error">{{ numberError }}</p>
    </div>
  </div>

  <div v-if="showGaveGot" class="field-row">
    <div class="field">
      <label class="field-label" for="ref-gave">Gave</label>
      <input id="ref-gave" v-model="form.gave" type="text" class="field-input" :class="{ 'field-input--invalid': gaveError }" placeholder="Pokémon you gave" />
      <p v-if="gaveError" class="field-error">{{ gaveError }}</p>
    </div>
    <div class="field">
      <label class="field-label" for="ref-got">Got</label>
      <input id="ref-got" v-model="form.got" type="text" class="field-input" :class="{ 'field-input--invalid': gotError }" placeholder="Pokémon you received" />
      <p v-if="gotError" class="field-error">{{ gotError }}</p>
    </div>
  </div>

  <div v-if="showDesc" class="field">
    <label class="field-label" for="ref-description">Description</label>
    <input id="ref-description" v-model="form.description" type="text" class="field-input" :class="{ 'field-input--invalid': descError }" placeholder="Brief description…" />
    <p v-if="descError" class="field-error">{{ descError }}</p>
  </div>

  <div class="field">
    <label class="field-label" for="ref-notes">Notes</label>
    <textarea id="ref-notes" v-model="form.notes" class="field-textarea" rows="3" placeholder="Public notes visible to everyone…" />
  </div>

  <div class="field">
    <label class="field-label" for="ref-private-notes">
      Private Notes <span class="field-hint">(only visible to you)</span>
    </label>
    <textarea id="ref-private-notes" v-model="form.privateNotes" class="field-textarea" rows="2" placeholder="Private notes only you can see…" />
  </div>
</template>

<style scoped>
@media (max-width: 480px) {
  .field-row { grid-template-columns: 1fr; }
}
</style>
