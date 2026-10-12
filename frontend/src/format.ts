/** Display helpers shared by the components. */

import type { PortalDate } from './types/registration'

const BIRR = new Intl.NumberFormat('en-ET', {
  style: 'currency',
  currency: 'ETB',
  maximumFractionDigits: 2,
})

const DAY = new Intl.DateTimeFormat('en-GB', { dateStyle: 'medium' })

export function formatCapital(amount: number | null | undefined): string {
  return amount === null || amount === undefined ? '—' : BIRR.format(amount)
}

/** Like formatCapital, but honours the currency the backend quoted the fee in. */
export function formatMoney(amount: number | null | undefined, currency: string): string {
  if (amount === null || amount === undefined) {
    return '—'
  }
  return new Intl.NumberFormat('en-ET', {
    style: 'currency',
    currency,
    maximumFractionDigits: 2,
  }).format(amount)
}

/**
 * Formats a portal date. Falls back to the portal's own words when it sent something that
 * is not a date, so "Data not available" reads as the registry wrote it rather than as a
 * blank or a wrong date.
 */
export function formatDate(date: PortalDate | null | undefined): string {
  if (!date) {
    return '—'
  }
  if (!date.iso) {
    return date.raw ?? '—'
  }
  const [year, month, day] = date.iso.split('-').map(Number)
  return DAY.format(new Date(Date.UTC(year, month - 1, day)))
}

export function formatTimestamp(iso: string): string {
  return new Date(iso).toLocaleString()
}

/** Initials for the photo placeholder; works for Amharic names too. */
export function initials(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => [...part][0] ?? '')
    .join('')
    .toUpperCase()
}

/**
 * How a licence stands relative to its renewal window, for the status pill. Expiry is
 * judged on `renewedTo`; a licence with no parseable window is reported as unknown rather
 * than guessed at.
 */
export type LicenceStanding = { tone: 'valid' | 'due' | 'expired' | 'unknown'; label: string }

export function licenceStanding(renewedTo: PortalDate | null | undefined): LicenceStanding {
  if (!renewedTo?.iso) {
    return { tone: 'unknown', label: 'Renewal date unknown' }
  }
  const end = new Date(`${renewedTo.iso}T00:00:00Z`).getTime()
  const days = Math.round((end - Date.now()) / 86_400_000)

  if (days < 0) {
    return { tone: 'expired', label: `Expired ${Math.abs(days)} day${Math.abs(days) === 1 ? '' : 's'} ago` }
  }
  if (days <= 60) {
    return { tone: 'due', label: days === 0 ? 'Expires today' : `Expires in ${days} days` }
  }
  return { tone: 'valid', label: `Valid until ${formatDate(renewedTo)}` }
}
