<script setup lang="ts">
import { computed } from 'vue'
import { CircleAlert, SearchX, ServerCrash } from 'lucide-vue-next'
import { ApiError } from '../api/etrade'

const props = defineProps<{ error: unknown }>()

const message = computed(() => {
  const error = props.error
  if (error instanceof ApiError) {
    if (error.isNotFound) {
      return {
        title: 'No registration found',
        detail: 'e-Trade holds no business registration for that TIN.',
        icon: SearchX,
        tone: 'bg-gray-2 text-text-secondary ring-gray-3',
        bar: 'bg-gray-4',
      }
    }
    if (error.status === 503) {
      return {
        title: 'e-Trade is not responding',
        detail: 'The portal timed out or refused the connection. Try again shortly.',
        icon: ServerCrash,
        tone: 'bg-warning/10 text-warning ring-warning/20',
        bar: 'bg-warning',
      }
    }
    return {
      title: error.problem?.title ?? 'Lookup failed',
      detail: error.problem?.detail ?? error.message,
      icon: CircleAlert,
      tone: 'bg-error/10 text-error ring-error/20',
      bar: 'bg-error',
    }
  }
  return {
    title: 'Could not reach the lookup service',
    detail: error instanceof Error ? error.message : String(error),
    icon: ServerCrash,
    tone: 'bg-error/10 text-error ring-error/20',
    bar: 'bg-error',
  }
})
</script>

<template>
  <div
    class="animate-fade-in flex items-start gap-3 overflow-hidden rounded-lg border border-base-clr3 bg-base-clr p-4 shadow-card"
    role="status"
  >
    <span class="h-full w-1 shrink-0 self-stretch rounded-full" :class="message.bar" aria-hidden="true" />
    <span class="grid size-9 shrink-0 place-items-center rounded-lg ring-1" :class="message.tone">
      <component :is="message.icon" class="size-4" />
    </span>
    <div class="min-w-0">
      <p class="font-semibold">{{ message.title }}</p>
      <p class="mt-0.5 text-xs text-text-secondary">{{ message.detail }}</p>
    </div>
  </div>
</template>
