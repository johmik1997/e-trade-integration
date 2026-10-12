# e-Trade integration

A lookup service over the Ethiopian e-Trade portal's business registration API, plus a
small Vue front end for it.

Upstream, a TIN lookup is one call:

```
GET https://etrade.gov.et/api/Registration/GetRegistrationInfoByTin/{tin}/{lang}
```

Calling it directly is awkward. It rejects any request whose `Referer` is not on its own
origin, with `417 Invalid referer headerr` (their typo). Its TLS chain does not validate on
the JVM. Field names are PascalCase with a typo (`LegalCondtion`), `RegDate` is a
US-formatted string in English but an *Ethiopian-calendar* one in Amharic, an unregistered
TIN comes back as `204 No Content`, and every associate carries a scanned photo as a bare
base64 blob, which makes one response ~70KB. This service deals with all of that behind a
stable contract — the same lookup comes back as ~700 bytes of clean JSON.

## Layout

| Path        | What it is                                                     |
|-------------|----------------------------------------------------------------|
| `backend/`  | Spring Boot 3.5 / Java 21, Maven. Proxies, caches, reshapes.    |
| `frontend/` | Vue 3 + Vite + TypeScript. TIN lookup form and a result card.   |

## Running it

Both halves at once, from the repo root:

```bash
./dev.sh
```

That starts the backend on `:8080` and the Vue dev server on `:5173` (installing npm
dependencies first if needed), and Ctrl-C stops both. Point it at a different portal with
`ETRADE_BASE_URL=http://localhost:9999 ./dev.sh`.

Or separately, in two terminals:

```bash
cd backend  && mvn spring-boot:run          # :8080
cd frontend && npm install && npm run dev   # :5173, proxies /api to :8080
```

The Vite dev server proxies `/api` to the backend, so the relative photo URLs the API
returns work in `<img src>` with no base-URL handling in the client.

```bash
cd backend && mvn test        # 33 tests, no network needed
cd frontend && npm run build  # type-checks and bundles
```

## API

### `GET /api/v1/registrations/{tin}?lang=en`

`tin` is ten digits; spaces and dashes are stripped before validation. `lang` defaults to
the first entry in `etrade.supported-languages`.

```json
{
  "tin": "0061688042",
  "registrationNumber": "BL/AA/2/0018364/2011",
  "registrationDate": { "iso": "2019-03-28", "raw": "3/28/2019" },
  "businessName": {
    "en": "MEDCO TECHNOLOGY SOLUTIONS PLC",
    "am": "ሜድኮ ቴክኖሎጂ ሶሉሽንስ ኃላ የተ የግ ማህበር"
  },
  "paidUpCapital": 5000000.00,
  "legalCondition": { "code": "2" },
  "associates": [
    {
      "name": { "en": "SEMERE TSADIK", "am": "ሰመረ ጻድቅ" },
      "photoUrl": "/api/v1/photos/b000e98d…"
    }
  ],
  "licences": [
    {
      "id": "7e7e0c7b-f9ed-4b34-a506-80ac4c6562eb",
      "licenceNumber": "BL/AA/14/673/2546312/2011",
      "tradeName": { "en": "MEDCO TECHNOLOGY SOLUTIONS PLC", "am": "ሜድኮ …" },
      "registeredOn": { "iso": "2019-03-28", "raw": "3/28/2019" },
      "renewalDate": { "iso": "2026-04-24", "raw": "4/24/2026" },
      "renewedFrom": { "iso": "2025-07-08", "raw": "7/8/2025" },
      "renewedTo": { "iso": "2026-07-07", "raw": "7/7/2026" },
      "activities": [
        { "code": 39141, "description": "Software development (including design, enrichment and implementation)" }
      ]
    }
  ],
  "retrievedAt": "2026-10-12T14:23:50.975734519Z"
}
```

Null fields are omitted. Every date is an object: `iso` is ISO-8601 and absent when the
portal sent something that is not a date, while `raw` always carries the portal's own
string. That matters because the registry writes the literal `"Data not available"` into
date fields on a licence that has never been renewed — `iso` is then absent and `raw` says
what the registry says, instead of a blank or a guessed date.

`licences` holds one entry per trade licence; the sample company has three (software
development, medical-supply import, diagnostic lab). It is empty for a taxpayer with none.

### `GET /api/v1/photos/{id}`

The associate photo, served with its detected content type. Ids are the SHA-256 of the
image bytes, so responses are immutable and cacheable for a month.

### `GET /api/v1/registrations/{tin}/contributions`

Every trade licence on the record, each priced. The fee band comes from the registration's
`LegalCondtion`; the licence says what is being paid for.

```json
[
  {
    "tin": "0061688042",
    "businessName": "MEDCO TECHNOLOGY SOLUTIONS PLC",
    "licenceId": "7e7e0c7b-f9ed-4b34-a506-80ac4c6562eb",
    "licenceNumber": "BL/AA/14/673/2546312/2011",
    "category": {
      "legalConditionCode": "2",
      "label": "Private Limited Company",
      "fee": 5000.00,
      "currency": "ETB",
      "inferred": false
    },
    "quotedAt": "2026-10-12T14:56:31.463919695Z"
  }
]
```

`inferred: true` means the record's legal condition was not in the configured schedule and
the fallback band was applied — the UI surfaces that as a warning rather than presenting a
guessed fee as authoritative.

### `POST /api/v1/payments`

```json
{ "tin": "0061688042", "licenceIds": ["7e7e0c7b-…", "122664f7-…"], "method": "TELEBIRR" }
```

Methods: `TELEBIRR`, `CBE_BIRR`, `BANK_TRANSFER`, `CASH`. Returns `201` with a `PENDING`
payment carrying a reference like `CNT-20261012-RBLB7B` (the suffix alphabet omits I, O, 0
and 1, since these get read aloud and typed in from receipts).

One payment covers **several licences** under a single reference, with a `lines` array (one
per licence) and a `total` — the payer selects what they are paying for and gets back one
receipt to quote.

**Amounts are not accepted from the client.** They are re-derived server-side from the
registration's category, so a payer cannot choose their own fee; an `amount` in the request
body is ignored.

An empty `licenceIds` is a `400`. If any id is not on the record the whole selection is
rejected with `404`, rather than quietly pricing the valid subset and under-charging against
what the payer chose.

### `POST /api/v1/payments/{reference}/confirm`

Marks the payment `PAID`. Idempotent — confirming twice returns the first settlement
unchanged, so a double-tap cannot rewrite history. This stands in for a provider callback:
**settlement is simulated. Nothing is sent to Telebirr, CBE or any other provider.**

Also: `GET /api/v1/payments/{reference}` and `GET /api/v1/registrations/{tin}/payments`.

### Errors

RFC 9457 problem documents. Upstream status codes do not leak through — a portal `500`
is a `503` from us, not a `500` of ours.

| Status | When                                                            |
|--------|-----------------------------------------------------------------|
| `400`  | TIN is not ten digits, or the language is not supported         |
| `404`  | No registration for that TIN (including the 200-with-nulls case) |
| `404`  | Licence id not on that registration, or unknown payment reference |
| `502`  | Portal answered 4xx — our request was malformed for it          |
| `503`  | Portal timed out, refused the connection, or 5xx'd on every try |

## Design notes

**Photos are lifted out of the JSON.** They are decoded once and held in a size-bounded
Caffeine store, and the registration response links to them. A real response drops from
hundreds of kilobytes to under a kilobyte, and browsers then cache each image separately.

**Photo ids are content hashes, and that is load-bearing.** The registration cache and the
photo store expire on independent schedules, so a cached registration can outlive its
photos. Because the id is derived from the bytes, re-fetching produces the *same* ids, so
previously issued URLs start working again. The service also notices the case directly: if
a cached registration references photos that are gone, it evicts and re-fetches rather than
handing out URLs that 404.

**`LegalCondtion` is not guessed at.** The portal publishes no code list (the sample TIN
returns `"2"`). The code is passed through verbatim, and a `label` appears only for codes
you have confirmed and added under `etrade.legal-conditions` in `application.yml`. An
unmapped code renders in the UI as `Code 2`, not as an invented description.

**`RegDate` is Gregorian, not Ethiopian.** `BL/AA/2/0018364/2011` carries Ethiopian year
2011 while `RegDate` is `3/28/2019`, and EC 2011 does span March 2019 — so the date needs
no calendar conversion, just `M/d/yyyy` parsing. Registration numbers are passed through
as opaque strings; their segments are not parsed.

## Configuration

Everything lives under `etrade.*` in `backend/src/main/resources/application.yml`:

| Key                             | Default                   | Notes                                     |
|---------------------------------|---------------------------|-------------------------------------------|
| `base-url`                      | `https://etrade.gov.et`   | Point at a stub for local work            |
| `connect-timeout` / `read-timeout` | `5s` / `30s`           | The portal is routinely slow              |
| `retries`                       | `1`                       | Extra attempts, for 5xx and transport errors only |
| `supported-languages`           | `[en, am]`                | Anything else is rejected with a `400`    |
| `legal-conditions`              | `{}`                      | Code → label, as you confirm them         |
| `photo-cache.max-size` / `.ttl` | `64MB` / `6h`             | Evicts by total bytes                     |
| `registration-cache.max-entries` / `.ttl` | `2000` / `1h`   |                                           |
| `cors-allowed-origins`          | `http://localhost:5173`   | Set this for real deployments             |
| `ssl-bundle`                    | unset                     | Name of a `spring.ssl.bundle.*` trust store; see below |

## Contribution fees

The schedule lives under `contribution.*` in `application.yml`, keyed by the portal's
`LegalCondtion` code:

```yaml
contribution:
  currency: ETB
  categories:
    "1": { label: Sole proprietor, fee: 1000.00 }
    "2": { label: Private Limited Company, fee: 5000.00 }
  fallback:
    label: Standard contribution
    fee: 2500.00
```

**The amounts shipped are demo placeholders, not the published tariff**, and the categories
themselves are inferred — e-Trade documents no code list, so `1` and `2` are read off
sampled records (sole proprietors and PLCs respectively). Confirm both the codes and the
fees before any real use. A code absent from the schedule is priced from `fallback` and
flagged `inferred`, so nothing silently prices itself.

Payments are held in an in-process map, so they are lost on restart. That is adequate for
the demo and nothing else; `PaymentService` is the single seam to replace with a database
and a real provider callback.

## Troubleshooting: "unable to find valid certification path"

A `503` with that detail means the service reached the portal but the JDK refused its TLS
certificate. Your browser loads the same URL because browsers fetch missing intermediate
CA certificates on the fly (via the certificate's AIA extension); the JDK does not. The
usual cause is that `etrade.gov.et` serves only its leaf certificate.

Confirm it:

```bash
./diagnose-tls.sh
```

As observed on 2026-10-12, the portal sends two certificates, but the intermediate is the
**wrong one**: it presents `GlobalSign Extended Validation CA - SHA256 - G3` while its leaf
is issued by `GlobalSign GCC R3 EV TLS CA 2025` — a stale intermediate left behind by a CA
migration, so openssl reports "unable to verify the first certificate". Two ways to fix it:

**Let the JDK fetch the correct intermediate itself** — one flag, nothing to maintain, but it
needs outbound access to the CA's AIA endpoint (plain HTTP) at startup. Note it must go to
the *forked* JVM: `MAVEN_OPTS` reaches only Maven's own JVM and has no effect here.

```bash
cd backend && mvn spring-boot:run \
  -Dspring-boot.run.jvmArguments=-Dcom.sun.security.enableAIAcaIssuers=true
```

**Or build a trust store** — works offline afterwards, and is the better choice for a
deployed service. This is what the repo is configured for:

```bash
cd backend && ./setup-truststore.sh
```

That copies the JDK's own `cacerts` (so every other host keeps validating normally), adds
the correct intermediate recovered from the leaf's AIA extension, and prints the
`application.yml` block — already applied in this repo, pointing at
`classpath:etrade-truststore.p12`. Verified: with it in place, live lookups succeed.

The store is gitignored because it is machine-generated, not because it is secret (it holds
only public CA certificates). Regenerate it with the same script when the portal's
certificate is next rotated.

If `diagnose-tls.sh` shows a *complete* chain, the cause is different: the portal is signed
by a CA the JDK does not ship at all. You then need that root certificate from its issuer
and should import it the same way. Do not reach for a trust-all `SSLContext` — it would
make every lookup silently interceptable.

## Status

Verified against the live portal on 2026-10-12: a lookup of TIN `0061688042` returns the
real registration through this service, the associate photo is served as a 286x365 JPEG
(52KB), and an unregistered TIN yields a `404`. 36 tests cover the mapping, the client's
status handling, caching, TLS bundle wiring and the HTTP surface.

Behaviour of the upstream API, as observed rather than assumed:

| Request | Portal's answer |
|---|---|
| No `Referer`, or one off its origin | `417` + `Invalid referer headerr` |
| Any `Referer` on `https://etrade.gov.et` | `200` — the path within the origin does not matter |
| `Origin` header instead of `Referer` | `417` — it specifically wants `Referer` |
| Registered TIN | `200` with the registration |
| Well-formed but unregistered TIN (e.g. `5555555555`) | `204 No Content` |
| Malformed TIN (`123`, `abcdefghij`) | `204 No Content` |
| `lang=en` | Gregorian dates (`3/28/2019`) |
| `lang=am` or `lang=or` | **Ethiopian-calendar** dates (`19/7/2011`), plus Amharic activity descriptions |
| Unknown `lang` (e.g. `xx`) | Falls back to English |
| Empty `lang` | `404` |

Note the `204`: the earlier guess that unknown TINs return `200` with an all-null body was
wrong. Both cases end up as our `404` regardless, since an empty body is treated as
not-found.

## Known gaps

**No scraping is involved, and none is needed.** Everything the portal holds for a TIN comes
back from its own JSON endpoint — including the trade licences, which were missed at first
only because the sample this was modelled on had been truncated before the `Businesses`
array. Every field observed across six live records is now mapped. Jackson still ignores
unknown fields, so if the portal adds more they are dropped rather than breaking the call;
re-run the field survey if you suspect new ones.

**Amharic is turned off rather than mishandled.** `supported-languages` is `[en]`, because
`am` and `or` return Ethiopian-calendar dates that nothing here converts; left enabled, they
would surface as unparseable. Enabling them properly means an EC-to-Gregorian conversion —
`19/7/2011` → `2019-03-28`. The live diff between `en` and `am` gives a dozen verified date
pairs to test such a conversion against.

**`LegalCondtion` has no published code list.** `"2"` is a PLC here and `"1"` appears on
sole proprietors, but that is inference from two samples, so `etrade.legal-conditions` ships
empty and unmapped codes render as `Code 2`.

**Operational.** No authentication or rate limiting on our own endpoints, and the caches are
in-process, so photo URLs are not shared across instances behind a load balancer. Both
matter before this is exposed beyond an internal network.
