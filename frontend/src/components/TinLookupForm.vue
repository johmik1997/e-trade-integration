<script setup lang="ts">
import { computed, ref } from 'vue'
import { Search, Loader2 } from 'lucide-vue-next'

const props = defineProps<{ busy: boolean }>()
const emit = defineEmits<{ submit: [tin: string, lang: string] }>()

const tin = ref('')

/** Mirrors the backend rule: ten digits after spaces and dashes are stripped. */
const normalized = computed(() => tin.value.replace(/[\s-]/g, ''))
const valid = computed(() => /^\d{10}$/.test(normalized.value))
const showHint = computed(() => normalized.value.length > 0 && !valid.value)

function submit() {
  if (valid.value && !props.busy) {
    // The portal returns Ethiopian-calendar dates for am/or, so the backend accepts en only.
    emit('submit', normalized.value, 'en')
  }
}
</script>

<template>
  <form class="flex flex-col gap-4 sm:flex-row sm:items-start" @submit.prevent="submit">
    <div class="min-w-0 flex-1">
      <label for="tin" class="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-text-secondary">
        Taxpayer identification number
      </label>
      <input
        id="tin"
        v-model="tin"
        inputmode="numeric"
        autocomplete="off"
        spellcheck="false"
        placeholder="0061688042"
        :aria-invalid="showHint"
        aria-describedby="tin-hint"
        class="w-full rounded-lg border-text-border-clr text-sm tabular-nums tracking-[0.08em] shadow-xs transition-colors placeholder:tracking-normal focus:border-primary focus:ring-1 focus:ring-primary"
        :class="showHint && 'border-error focus:border-error focus:ring-error'"
      />
      <p id="tin-hint" class="mt-1.5 text-xs" :class="showHint ? 'text-error' : 'text-text-secondary'">
        {{ showHint ? 'A TIN is exactly 10 digits.' : 'Spaces and dashes are fine.' }}
      </p>
    </div>

    <button
      type="submit"
      :disabled="!valid || props.busy"
      class="flex min-h-[2.4rem] min-w-[7rem] items-center justify-center gap-2 self-start rounded-lg border border-primary/95 bg-primary px-4 py-1.5 font-semibold capitalize text-white shadow-[0_4px_10px_rgba(18,60,119,0.22)] transition-all duration-200 hover:bg-primary-dark hover:shadow-[0_6px_14px_rgba(18,60,119,0.28)] disabled:cursor-not-allowed disabled:opacity-60 sm:mt-[1.6rem]"
    >
      <Loader2 v-if="props.busy" class="size-4 animate-spin" />
      <Search v-else class="size-4" />
      {{ props.busy ? 'Looking up…' : 'Look up' }}
    </button>
  </form>
</template>
