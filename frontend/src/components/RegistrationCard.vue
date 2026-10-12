<script setup lang="ts">
import { computed } from 'vue'
import { Building2, Clock3 } from 'lucide-vue-next'
import type { Registration } from '../types/registration'
import { formatCapital, formatDate, formatTimestamp } from '../format'
import AssociateCard from './AssociateCard.vue'
import LicenceCard from './LicenceCard.vue'

const props = defineProps<{ registration: Registration; selected: string[] }>()
const emit = defineEmits<{ 'update:selected': [value: string[]] }>()

const selectableLicences = computed(() => props.registration.licences.filter((l) => l.id))
const allSelected = computed(
  () => selectableLicences.value.length > 0 && props.selected.length === selectableLicences.value.length,
)

function toggle(id: string | null | undefined, on: boolean) {
  if (!id) {
    return
  }
  emit('update:selected', on ? [...props.selected, id] : props.selected.filter((s) => s !== id))
}

function toggleAll() {
  emit('update:selected', allSelected.value ? [] : selectableLicences.value.map((l) => l.id as string))
}

const legalCondition = computed(() => {
  const condition = props.registration.legalCondition
  if (!condition) {
    return '—'
  }
  // The portal publishes no code list, so an unlabelled code is shown as a code.
  return condition.label ?? `Code ${condition.code}`
})

const facts = computed(() => [
  { label: 'TIN', value: props.registration.tin, mono: true },
  { label: 'Registration number', value: props.registration.registrationNumber ?? '—', mono: true },
  { label: 'Registration date', value: formatDate(props.registration.registrationDate) },
  { label: 'Paid-up capital', value: formatCapital(props.registration.paidUpCapital) },
  { label: 'Legal condition', value: legalCondition.value },
])
</script>

<template>
  <article class="animate-slide-up overflow-hidden rounded-lg border border-base-clr3 bg-base-clr shadow-card">
    <header class="flex items-start gap-3 border-b border-base-clr3 bg-primary-light/40 px-5 py-4">
      <span class="grid size-10 shrink-0 place-items-center rounded-lg bg-primary text-white shadow-sm">
        <Building2 class="size-5" />
      </span>
      <div class="min-w-0">
        <h2 class="truncate text-md font-semibold leading-tight">
          {{ registration.businessName.en ?? registration.businessName.am }}
        </h2>
        <p v-if="registration.businessName.am" class="amharic mt-0.5 truncate text-sm text-text-secondary">
          {{ registration.businessName.am }}
        </p>
      </div>
    </header>

    <dl class="grid grid-cols-2 gap-4 px-5 py-4 sm:grid-cols-3 lg:grid-cols-5">
      <div v-for="fact in facts" :key="fact.label" class="min-w-0">
        <dt class="text-xxs uppercase tracking-wide text-text-secondary">{{ fact.label }}</dt>
        <dd class="mt-1 truncate font-semibold" :class="fact.mono && 'font-mono text-sm font-medium'">
          {{ fact.value }}
        </dd>
      </div>
    </dl>

    <section v-if="registration.licences.length" class="border-t border-base-clr3 bg-base-clr2 px-5 py-4">
      <div class="mb-3 flex flex-wrap items-center justify-between gap-2">
        <h3 class="text-xs font-semibold uppercase tracking-wide text-text-secondary">
          Trade licences ({{ registration.licences.length }})
          <span class="ml-1 font-normal normal-case tracking-normal">
            — select the ones to contribute for
          </span>
        </h3>
        <button
          v-if="selectableLicences.length > 1"
          type="button"
          class="rounded-lg border border-primary/25 bg-white px-2.5 py-1 text-xs font-semibold text-primary shadow-xs transition-colors hover:bg-primary-light/40"
          @click="toggleAll"
        >
          {{ allSelected ? 'Clear all' : 'Select all' }}
        </button>
      </div>
      <ul class="grid gap-3 xl:grid-cols-2">
        <LicenceCard
          v-for="(licence, index) in registration.licences"
          :key="licence.id ?? licence.licenceNumber ?? index"
          :licence="licence"
          :selectable="Boolean(licence.id)"
          :selected="Boolean(licence.id) && selected.includes(licence.id!)"
          @update:selected="(on) => toggle(licence.id, on)"
        />
      </ul>
    </section>

    <section v-if="registration.associates.length" class="border-t border-base-clr3 px-5 py-4">
      <h3 class="mb-3 text-xs font-semibold uppercase tracking-wide text-text-secondary">
        Managers and owners
      </h3>
      <ul class="grid gap-3 sm:grid-cols-2">
        <AssociateCard
          v-for="(associate, index) in registration.associates"
          :key="associate.photoUrl ?? `${associate.name.en ?? associate.name.am}-${index}`"
          :associate="associate"
        />
      </ul>
    </section>

    <footer
      class="flex items-center gap-1.5 border-t border-base-clr3 bg-base-clr2 px-5 py-3 text-xs text-text-secondary"
    >
      <Clock3 class="size-3.5" />
      Retrieved from e-Trade at {{ formatTimestamp(registration.retrievedAt) }}
    </footer>
  </article>
</template>
