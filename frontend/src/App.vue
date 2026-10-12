<script setup lang="ts">
import { ref, shallowRef } from 'vue'
import { FileSearch } from 'lucide-vue-next'
import type { Registration } from './types/registration'
import { lookupRegistration } from './api/etrade'
import TinLookupForm from './components/TinLookupForm.vue'
import RegistrationCard from './components/RegistrationCard.vue'
import ProblemMessage from './components/ProblemMessage.vue'
import ContributionPanel from './components/ContributionPanel.vue'

const registration = shallowRef<Registration | null>(null)
/** Which licences the contribution covers; chosen on the cards, spent in the panel. */
const selectedLicences = ref<string[]>([])
const error = shallowRef<unknown>(null)
const busy = ref(false)

/** Keeps a slow lookup from overwriting the result of a newer one. */
let inFlight: AbortController | null = null

async function lookup(tin: string, lang: string) {
  inFlight?.abort()
  const controller = new AbortController()
  inFlight = controller

  busy.value = true
  error.value = null
  try {
    const result = await lookupRegistration(tin, lang, controller.signal)
    registration.value = result
    // Default to every licence: paying for all of them is the common case.
    selectedLicences.value = result.licences.map((licence) => licence.id).filter((id): id is string => Boolean(id))
  } catch (e) {
    if (controller.signal.aborted) {
      return
    }
    registration.value = null
    selectedLicences.value = []
    error.value = e
  } finally {
    if (inFlight === controller) {
      inFlight = null
      busy.value = false
    }
  }
}
</script>

<template>
  <div class="min-h-full bg-base-clr2">
    <header class="border-b border-base-clr3 bg-base-clr">
      <div class="mx-auto flex max-w-6xl items-center gap-3 px-4 py-4">
        <span class="grid size-9 place-items-center rounded-lg bg-primary text-white shadow-sm">
          <FileSearch class="size-5" />
        </span>
        <div>
          <h1 class="text-md font-semibold leading-tight">e-Trade TIN lookup</h1>
          <p class="text-xs text-text-secondary">
            Business registration and trade licences from the e-Trade portal
          </p>
        </div>
      </div>
    </header>

    <main class="mx-auto flex max-w-6xl flex-col gap-4 px-4 py-6">
      <section class="rounded-lg border border-base-clr3 bg-base-clr p-5 shadow-card">
        <TinLookupForm :busy="busy" @submit="lookup" />
      </section>

      <ProblemMessage v-if="error" :error="error" />
      <template v-else-if="registration">
        <RegistrationCard v-model:selected="selectedLicences" :registration="registration" />
        <ContributionPanel :registration="registration" :selected="selectedLicences" />
      </template>
      <p
        v-else-if="!busy"
        class="rounded-lg border border-dashed border-base-clr3 bg-base-clr px-5 py-10 text-center text-text-secondary"
      >
        Enter a TIN to see its registration.
      </p>
    </main>
  </div>
</template>
