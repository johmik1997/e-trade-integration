import type {
  FeeQuote,
  Payment,
  PaymentMethod,
  Problem,
  Registration,
} from '../types/registration'

/** Thrown for every non-2xx answer, carrying the problem document when there is one. */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly problem?: Problem,
  ) {
    super(problem?.detail ?? problem?.title ?? `Request failed with HTTP ${status}`)
    this.name = 'ApiError'
  }

  /** True when the TIN simply has no registration, which is a normal answer, not a fault. */
  get isNotFound(): boolean {
    return this.status === 404
  }
}

export async function lookupRegistration(
  tin: string,
  lang: string,
  signal?: AbortSignal,
): Promise<Registration> {
  const query = new URLSearchParams({ lang })
  const response = await fetch(
    `/api/v1/registrations/${encodeURIComponent(tin)}?${query}`,
    { headers: { Accept: 'application/json' }, signal },
  )

  if (!response.ok) {
    throw new ApiError(response.status, await readProblem(response))
  }
  return (await response.json()) as Registration
}

async function readProblem(response: Response): Promise<Problem | undefined> {
  // A gateway or proxy in front of the service may answer with HTML rather than a
  // problem document, so never assume the body is JSON.
  try {
    const body = await response.json()
    return typeof body === 'object' && body !== null ? (body as Problem) : undefined
  } catch {
    return undefined
  }
}

export async function fetchQuotes(tin: string, signal?: AbortSignal): Promise<FeeQuote[]> {
  return request<FeeQuote[]>(`/api/v1/registrations/${encodeURIComponent(tin)}/contributions`, { signal })
}

/**
 * Starts a payment. The amount is not sent: the backend re-derives it from the
 * registration's category, so the payer cannot choose their own fee.
 */
export async function initiatePayment(
  tin: string,
  licenceIds: string[],
  method: PaymentMethod,
  signal?: AbortSignal,
): Promise<Payment> {
  return request<Payment>('/api/v1/payments', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ tin, licenceIds, method }),
    signal,
  })
}

/** Stands in for a provider callback; settlement is simulated in this demo. */
export async function confirmPayment(reference: string, signal?: AbortSignal): Promise<Payment> {
  return request<Payment>(`/api/v1/payments/${encodeURIComponent(reference)}/confirm`, {
    method: 'POST',
    signal,
  })
}

async function request<T>(url: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(url, {
    ...init,
    headers: { Accept: 'application/json', ...(init.headers ?? {}) },
  })
  if (!response.ok) {
    throw new ApiError(response.status, await readProblem(response))
  }
  return (await response.json()) as T
}
