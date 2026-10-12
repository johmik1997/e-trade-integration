<script setup lang="ts">
import { computed, ref } from 'vue'
import { Phone } from 'lucide-vue-next'
import type { Associate } from '../types/registration'
import { initials } from '../format'

const props = defineProps<{ associate: Associate }>()

/** The photo can 404 once the backend's photo cache has expired; degrade to initials. */
const photoFailed = ref(false)
const showPhoto = computed(() => Boolean(props.associate.photoUrl) && !photoFailed.value)
const primaryName = computed(() => props.associate.name.en ?? props.associate.name.am ?? 'Unnamed')
const secondaryName = computed(() =>
  props.associate.name.en && props.associate.name.am ? props.associate.name.am : null,
)
/** Both numbers were null in every record sampled, so this usually renders nothing. */
const phone = computed(
  () => [props.associate.mobilePhone, props.associate.regularPhone].filter(Boolean).join(' · ') || null,
)
</script>

<template>
  <li class="flex items-center gap-3 rounded-lg border border-base-clr3 bg-base-clr p-3 shadow-card">
    <img
      v-if="showPhoto"
      :src="associate.photoUrl!"
      :alt="`Photograph of ${primaryName}`"
      loading="lazy"
      decoding="async"
      class="size-14 shrink-0 rounded-full border-2 border-white bg-base-clr4 object-cover shadow-sm"
      @error="photoFailed = true"
    />
    <div
      v-else
      aria-hidden="true"
      class="grid size-14 shrink-0 place-items-center rounded-full bg-primary-light font-semibold text-primary"
    >
      {{ initials(primaryName) }}
    </div>

    <div class="min-w-0">
      <p class="truncate font-semibold">{{ primaryName }}</p>
      <p v-if="secondaryName" class="amharic truncate text-xs text-text-secondary">{{ secondaryName }}</p>
      <p class="mt-0.5 text-xs text-text-secondary">{{ associate.position ?? 'Position not stated' }}</p>
      <p v-if="phone" class="mt-0.5 flex items-center gap-1 text-xs tabular-nums text-text-secondary">
        <Phone class="size-3" />{{ phone }}
      </p>
    </div>
  </li>
</template>
