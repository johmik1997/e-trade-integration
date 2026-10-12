<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import {
  ArrowLeft,
  ArrowRight,
  BadgeCheck,
  CircleAlert,
  Loader2,
  Receipt,
  TriangleAlert,
  Wallet,
} from 'lucide-vue-next'
import type { FeeQuote, Payment, PaymentMethod, Registration } from '../types/registration'
import { confirmPayment, fetchQuotes, initiatePayment } from '../api/etrade'
import { formatMoney, formatTimestamp } from '../format'

const props = defineProps<{ registration: Registration; selected: string[] }>()

const METHODS: { value: PaymentMethod; label: string }[] = [
  { value: 'TELEBIRR', label: 'Telebirr' },
  { value: 'CBE_BIRR', label: 'CBE Birr' },
  { value: 'BANK_TRANSFER', label: 'Bank transfer' },
  { value: 'CASH', label: 'Cash' },
]

/** select → review the amount → pay. */
const step = ref<'select' | 'review' | 'paid'>('select')
const quotes = ref<FeeQuote[]>([])
const method = ref<PaymentMethod>('TELEBIRR')
const payment = ref<Payment | null>(null)
const busy = ref(false)
const error = ref<string | null>(null)

const chosen = computed(() => quotes.value.filter((q) => q.licenceId && props.selected.includes(q.licenceId)))
const total = computed(() => chosen.value.reduce((sum, q) => sum + q.category.fee, 0))
const currency = computed(() => chosen.value[0]?.category.currency ?? 'ETB')
const anyInferred = computed(() => chosen.value.some((q) => q.category.inferred))

/** Reload prices whenever a different registration is looked up. */
watch(
  () => props.registration.tin,
  async () => {
    quotes.value = []
    payment.value = null
    error.value = null
    step.value = 'select'
    if (!props.registration.licences.length) {
      return
    }
    busy.value = true
    try {
      quotes.value = await fetchQuotes(props.registration.tin)
    } catch (e) {
      error.value = e instanceof Error ? e.message : String(e)
    } finally {
      busy.value = false
    }
  },
  { immediate: true },
)

// Changing the selection after reaching the review step invalidates the amount shown.
watch(
  () => props.selected,
  () => {
    if (step.value === 'review') {
      step.value = 'select'
    }
  },
  { deep: true },
)

async function pay() {
  if (!chosen.value.length || busy.value) {
    return
  }
  busy.value = true
  error.value = null
  try {
    const created = await initiatePayment(
      props.registration.tin,
      chosen.value.map((q) => q.licenceId as string),
      method.value,
    )
    payment.value = await confirmPayment(created.reference)
    step.value = 'paid'
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    busy.value = false
  }
}

function startOver() {
  payment.value = null
  error.value = null
  step.value = 'select'
}
</script>

<template>
  <section class="animate-slide-up rounded-lg border border-base-clr3 bg-base-clr shadow-card">
    <header class="flex items-center gap-3 border-b border-base-clr3 px-5 py-4">
      <span class="grid size-9 shrink-0 place-items-center rounded-lg bg-primary text-white shadow-sm">
        <Wallet class="size-4" />
      </span>
      <div class="min-w-0 flex-1">
        <h2 class="font-semibold leading-tight">Contribution</h2>
        <p class="text-xs text-text-secondary">
          The fee follows the category of each selected licence
        </p>
      </div>
      <ol class="hidden items-center gap-2 text-xxs font-semibold uppercase tracking-wide sm:flex">
        <li v-for="(label, index) in ['Select', 'Review', 'Pay']" :key="label" class="flex items-center gap-2">
          <span
            class="rounded-full px-2 py-0.5"
            :class="
              ['select', 'review', 'paid'][index] === step
                ? 'bg-primary text-white'
                : 'bg-base-clr4 text-text-secondary'
            "
          >
            {{ label }}
          </span>
          <span v-if="index < 2" class="text-text-border-clr">›</span>
        </li>
      </ol>
    </header>

    <p v-if="!registration.licences.length" class="px-5 py-8 text-center text-text-secondary">
      This registration holds no trade licence, so there is nothing to contribute for.
    </p>

    <!-- Step 1: nothing to choose here — selection happens on the licence cards above -->
    <div v-else-if="step === 'select'" class="flex flex-wrap items-center justify-between gap-3 px-5 py-4">
      <p class="text-sm" :class="chosen.length ? 'text-text-clr' : 'text-text-secondary'">
        <template v-if="chosen.length">
          <strong>{{ chosen.length }}</strong> of {{ registration.licences.length }} licence{{
            registration.licences.length === 1 ? '' : 's'
          }}
          selected
        </template>
        <template v-else> Select one or more trade licences above to continue. </template>
      </p>
      <button
        type="button"
        :disabled="!chosen.length || busy"
        class="flex min-h-[2.4rem] min-w-[7rem] items-center justify-center gap-2 rounded-lg border border-primary/95 bg-primary px-4 py-1.5 font-semibold text-white shadow-[0_4px_10px_rgba(18,60,119,0.22)] transition-all hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-60"
        @click="step = 'review'"
      >
        <Loader2 v-if="busy" class="size-4 animate-spin" />
        Next
        <ArrowRight v-if="!busy" class="size-4" />
      </button>
    </div>

    <!-- Step 2: the amount, then pay -->
    <div v-else-if="step === 'review'" class="flex flex-col gap-4 px-5 py-4">
      <ul class="flex flex-col gap-2">
        <li
          v-for="quote in chosen"
          :key="quote.licenceId ?? ''"
          class="flex items-center justify-between gap-3 rounded-lg bg-base-clr2 px-3 py-2"
        >
          <span class="min-w-0">
            <span class="block truncate font-mono text-xs text-text-secondary">
              {{ quote.licenceNumber ?? 'No licence number' }}
            </span>
            <span class="text-sm font-semibold">{{ quote.category.label }}</span>
          </span>
          <span class="shrink-0 font-semibold tabular-nums">
            {{ formatMoney(quote.category.fee, quote.category.currency) }}
          </span>
        </li>
      </ul>

      <div
        v-if="anyInferred"
        class="flex items-start gap-2 rounded-lg bg-warning/10 p-3 text-xs text-warning ring-1 ring-warning/20"
      >
        <TriangleAlert class="mt-0.5 size-3.5 shrink-0" />
        <span>
          At least one licence has a legal condition that is not in the fee schedule, so the
          standard band was applied. Confirm before collecting.
        </span>
      </div>

      <div class="flex flex-col gap-3 border-t border-base-clr3 pt-4 sm:flex-row sm:items-end">
        <div class="flex-1">
          <label
            for="method"
            class="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-text-secondary"
          >
            Payment method
          </label>
          <select
            id="method"
            v-model="method"
            class="w-full rounded-lg border-text-border-clr text-sm shadow-xs focus:border-primary focus:ring-1 focus:ring-primary"
          >
            <option v-for="option in METHODS" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </div>
        <div class="sm:text-right">
          <p class="text-xxs uppercase tracking-wide text-text-secondary">Total due</p>
          <p class="text-lg font-bold tabular-nums">{{ formatMoney(total, currency) }}</p>
        </div>
        <div class="flex gap-2">
          <button
            type="button"
            class="flex min-h-[2.4rem] items-center gap-1.5 rounded-lg border border-primary/25 bg-white px-3 py-1.5 font-semibold text-primary shadow-xs transition-colors hover:bg-primary-light/40"
            @click="step = 'select'"
          >
            <ArrowLeft class="size-4" />
            Back
          </button>
          <button
            type="button"
            :disabled="busy"
            class="flex min-h-[2.4rem] min-w-[7rem] items-center justify-center gap-2 rounded-lg border border-primary/95 bg-primary px-4 py-1.5 font-semibold text-white shadow-[0_4px_10px_rgba(18,60,119,0.22)] transition-all hover:bg-primary-dark disabled:opacity-60"
            @click="pay"
          >
            <Loader2 v-if="busy" class="size-4 animate-spin" />
            <Receipt v-else class="size-4" />
            Pay
          </button>
        </div>
      </div>
    </div>

    <!-- Step 3: receipt -->
    <div v-else-if="payment" class="flex flex-col gap-4 px-5 py-4">
      <div class="flex items-start gap-3 rounded-lg bg-success/10 p-4 text-success ring-1 ring-success/20">
        <BadgeCheck class="mt-0.5 size-5 shrink-0" />
        <div class="min-w-0 text-text-clr">
          <p class="font-semibold">Contribution paid</p>
          <p class="mt-0.5 text-xs text-text-secondary">
            Reference <span class="font-mono font-semibold">{{ payment.reference }}</span>
            <template v-if="payment.settledAt"> · {{ formatTimestamp(payment.settledAt) }}</template>
          </p>
        </div>
        <p class="ml-auto shrink-0 text-lg font-bold tabular-nums text-text-clr">
          {{ formatMoney(payment.total, payment.currency) }}
        </p>
      </div>

      <ul class="flex flex-col gap-1.5">
        <li
          v-for="line in payment.lines"
          :key="line.licenceId ?? ''"
          class="flex items-center justify-between gap-3 text-sm"
        >
          <span class="min-w-0 truncate font-mono text-xs text-text-secondary">
            {{ line.licenceNumber }}
          </span>
          <span class="shrink-0 tabular-nums">{{ formatMoney(line.amount, payment.currency) }}</span>
        </li>
      </ul>

      <div class="flex flex-wrap items-center gap-3 border-t border-base-clr3 pt-4">
        <button
          type="button"
          class="min-h-[2.4rem] rounded-lg border border-primary/25 bg-white px-4 py-1.5 font-semibold text-primary shadow-xs transition-colors hover:bg-primary-light/40"
          @click="startOver"
        >
          New contribution
        </button>
        <p class="text-xs text-text-secondary">
          Settlement is simulated in this demo — no provider is contacted.
        </p>
      </div>
    </div>

    <p
      v-if="error"
      class="mx-5 mb-4 flex items-start gap-2 rounded-lg bg-error/10 p-3 text-xs text-error ring-1 ring-error/20"
    >
      <CircleAlert class="mt-0.5 size-3.5 shrink-0" />{{ error }}
    </p>
  </section>
</template>
