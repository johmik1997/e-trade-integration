<script setup lang="ts">
import { computed } from 'vue'
import { BadgeCheck, CalendarClock, CircleAlert, CircleHelp } from 'lucide-vue-next'
import type { BusinessLicence } from '../types/registration'
import { formatDate, licenceStanding } from '../format'

const props = defineProps<{
  licence: BusinessLicence
  selectable?: boolean
  selected?: boolean
}>()
const emit = defineEmits<{ 'update:selected': [value: boolean] }>()

/** Clicking anywhere on a selectable card toggles it, not just the checkbox itself. */
function toggle() {
  if (props.selectable) {
    emit('update:selected', !props.selected)
  }
}

const standing = computed(() => licenceStanding(props.licence.renewedTo))
const tradeName = computed(
  () => props.licence.tradeName.en ?? props.licence.tradeName.am ?? 'Unnamed trade',
)

const TONES = {
  valid: { pill: 'bg-success/10 text-success ring-success/20', icon: BadgeCheck },
  due: { pill: 'bg-warning/10 text-warning ring-warning/20', icon: CalendarClock },
  expired: { pill: 'bg-error/10 text-error ring-error/20', icon: CircleAlert },
  unknown: { pill: 'bg-gray-2 text-text-secondary ring-gray-3', icon: CircleHelp },
} as const

const tone = computed(() => TONES[standing.value.tone])
</script>

<template>
  <li
    class="animate-fade-in rounded-lg border bg-base-clr p-4 shadow-card transition-all"
    :class="[
      selectable && 'cursor-pointer hover:shadow-sm',
      selected ? 'border-primary ring-1 ring-primary/30' : 'border-base-clr3 hover:shadow-sm',
    ]"
    @click="toggle"
  >
    <header class="flex flex-wrap items-start justify-between gap-2">
      <div class="flex min-w-0 gap-3">
        <input
          v-if="selectable"
          type="checkbox"
          :checked="selected"
          :aria-label="`Select licence ${licence.licenceNumber ?? ''}`"
          class="mt-0.5 size-4 shrink-0 rounded border-text-border-clr text-primary focus:ring-primary"
          @click.stop
          @change="emit('update:selected', ($event.target as HTMLInputElement).checked)"
        />
        <div class="min-w-0">
        <p class="font-mono text-xs text-text-secondary">
          {{ licence.licenceNumber ?? 'No licence number' }}
        </p>
          <p class="mt-0.5 font-semibold leading-snug">{{ tradeName }}</p>
        </div>
      </div>
      <span
        class="flex shrink-0 items-center gap-1 rounded-full px-2.5 py-1 text-xxs font-semibold ring-1"
        :class="tone.pill"
      >
        <component :is="tone.icon" class="size-3" />
        {{ standing.label }}
      </span>
    </header>

    <ul v-if="licence.activities.length" class="mt-3 flex flex-col gap-1.5">
      <li
        v-for="(activity, index) in licence.activities"
        :key="activity.code ?? index"
        class="flex gap-2 text-sm"
      >
        <span class="shrink-0 rounded bg-base-clr4 px-1.5 py-0.5 font-mono text-xxs text-text-secondary">
          {{ activity.code ?? '—' }}
        </span>
        <span class="min-w-0">{{ activity.description ?? 'No description' }}</span>
      </li>
    </ul>

    <dl class="mt-3 grid grid-cols-2 gap-3 border-t border-base-clr3 pt-3 sm:grid-cols-4">
      <div>
        <dt class="text-xxs uppercase tracking-wide text-text-secondary">Registered</dt>
        <dd class="mt-0.5 font-semibold">{{ formatDate(licence.registeredOn) }}</dd>
      </div>
      <div>
        <dt class="text-xxs uppercase tracking-wide text-text-secondary">Renewed from</dt>
        <dd class="mt-0.5 font-semibold">{{ formatDate(licence.renewedFrom) }}</dd>
      </div>
      <div>
        <dt class="text-xxs uppercase tracking-wide text-text-secondary">Renewed to</dt>
        <dd class="mt-0.5 font-semibold">{{ formatDate(licence.renewedTo) }}</dd>
      </div>
      <div>
        <dt class="text-xxs uppercase tracking-wide text-text-secondary">Last renewal</dt>
        <dd class="mt-0.5 font-semibold">{{ formatDate(licence.renewalDate) }}</dd>
      </div>
    </dl>
  </li>
</template>
