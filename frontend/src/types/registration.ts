/** Mirrors the backend contract in et.medco.etrade.domain. Nulls are omitted by Jackson. */

export interface LocalizedText {
  en?: string | null
  am?: string | null
}

/**
 * A date as the portal gave it, plus the backend's reading of it. `iso` is absent when the
 * portal sent something that is not a date — most often the literal "Data not available"
 * on a licence that has never been renewed.
 */
export interface PortalDate {
  iso?: string | null
  raw?: string | null
}

export interface LegalCondition {
  code: string
  /** Only present when the code appears in the backend's etrade.legal-conditions map. */
  label?: string | null
}

export interface Associate {
  position?: string | null
  name: LocalizedText
  mobilePhone?: string | null
  regularPhone?: string | null
  /** Relative URL served by the backend; null when the portal held no usable photo. */
  photoUrl?: string | null
}

export interface Activity {
  code?: number | null
  description?: string | null
}

export interface BusinessLicence {
  id?: string | null
  licenceNumber?: string | null
  tradeName: LocalizedText
  registeredOn?: PortalDate | null
  renewalDate?: PortalDate | null
  renewedFrom?: PortalDate | null
  renewedTo?: PortalDate | null
  licensingGroup?: string | null
  activities: Activity[]
}

export interface Registration {
  tin: string
  registrationNumber?: string | null
  registrationDate?: PortalDate | null
  businessName: LocalizedText
  paidUpCapital?: number | null
  legalCondition?: LegalCondition | null
  associates: Associate[]
  licences: BusinessLicence[]
  retrievedAt: string
}

/** RFC 9457 problem document, as produced by the backend's ApiExceptionHandler. */
export interface Problem {
  type?: string
  title: string
  status: number
  detail?: string
  tin?: string
  upstreamStatus?: number
}

/* ── Contribution and payment ───────────────────────────────────────────────── */

export interface ContributionCategory {
  /** The LegalCondtion code the band was derived from; absent if the record had none. */
  legalConditionCode?: string | null
  label: string
  fee: number
  currency: string
  /** True when the code was not in the configured schedule and the fallback band applied. */
  inferred: boolean
}

export interface FeeQuote {
  tin: string
  businessName?: string | null
  licenceId?: string | null
  licenceNumber?: string | null
  category: ContributionCategory
  quotedAt: string
}

export type PaymentMethod = 'TELEBIRR' | 'CBE_BIRR' | 'BANK_TRANSFER' | 'CASH'
export type PaymentStatus = 'PENDING' | 'PAID'

export interface PaymentLine {
  licenceId?: string | null
  licenceNumber?: string | null
  categoryLabel: string
  amount: number
}

export interface Payment {
  reference: string
  tin: string
  businessName?: string | null
  /** One line per licence the payment covers. */
  lines: PaymentLine[]
  total: number
  currency: string
  method: PaymentMethod
  status: PaymentStatus
  createdAt: string
  settledAt?: string | null
}
