# ADR 0003: Frontend architecture

- Status: accepted
- Date: 2026-10-06

## Context

The frontend is an Angular application (`frontend/`) that talks to the Spring Boot API and signs users in through
Keycloak. It should keep the spirit of the backend (clear boundaries, rules written down and checked by tests) without
copying hexagonal architecture, which would be ceremony here.

## Decision

**Structure: by feature, with checked boundaries.**

```
src/app/
├── core/        singletons: auth, config, API plumbing (types, errors), i18n, toasts, layout, generic pages
├── shared/      presentational pieces and helpers with no feature knowledge (money formatting)
└── features/<feature>/
    ├── pages/        routed components; they use state, ui and data-access
    ├── ui/           presentation only: inputs and outputs, no HTTP, no state
    ├── state/        signals of the feature; they use data-access
    └── data-access/  the only place of a feature that knows about HTTP
```

Rules, enforced by ESLint (`eslint-plugin-boundaries`, see `frontend/eslint.config.mjs`): core and shared never
import a feature; a feature never imports another feature; ui imports neither state, data-access nor pages;
data-access imports neither ui, state nor pages; state imports neither ui nor pages; `HttpClient` is reachable only from
data-access and core.

**Other decisions**

- Standalone components, signals for state, no NgRx: the state is local to each feature. NgRx can come later if state
  ever has to be shared widely.
- **The API contract is generated.** The backend versions `docs/api/openapi.json` (a backend test fails when it drifts);
  the frontend generates its TypeScript types from it (`npm run api:generate`), and CI fails when they are stale.
- **Sign-in**: Authorization Code with PKCE through `angular-oauth2-oidc`, client `ledgerai-frontend`. The access token
  is sent only to the API's base URL. Roles are read from the token to adapt the UI; the backend is the one that
  enforces them.
- **Runtime configuration**: `public/config.json` (API URL, Keycloak issuer and client) is read at startup, so the same
  build runs in any environment.
- **Money is a string.** The API sends amounts as strings with the exact decimals of the currency, because JavaScript
  numbers are binary floating point. The frontend formats them without converting to a number. The one place that has
  to compare amounts as the person types (is the new entry balanced?) uses exact decimal arithmetic on `BigInt`
  (`shared/money/decimal.ts`) and sends the amounts back as strings. The backend remains the authority: the screen
  only drives the interface.
- **A feature owns its screens and its state, not the other features'.** Screens that need the same fact (the trial
  balance on the dashboard and on its own page) each call the API through their own data-access, rather than share a
  service across features. What is truly common (the status pill of an entry, the confirmation dialog, the icons of
  account types, the money and date pipes) lives in `shared/`.
- **Names, not identifiers.** Screens show account names; an unknown account reads "Unknown account", never an id.
- **Failures are local.** Sections of a screen load and fail independently (`Panel`), each with its own retry; the error
  itself is a translated toast raised once by the HTTP interceptor.
- **Errors are toasts.** One interceptor turns every failed API call into a translated toast (the problem kind picks
  the title; the backend message is added only where it is meant for users, never for server errors). A caller that
  shows an error itself (a form field) opts out per request.
- **Languages**: English and French (Transloco, dictionaries in `public/i18n`), chosen from the user's saved choice or
  the browser. A test fails if the two dictionaries do not define the same keys and placeholders.
- **Styling**: Tailwind CSS.

## Consequences

- A new screen follows a known layout, and breaking the layering fails the lint instead of waiting for a review.
- Backend messages are in English: they appear untranslated in toasts. Error codes in the API would allow translating
  them; this is a known limitation.
- The generated `schema.ts` is committed, so it can be reviewed and builds do not need the backend.
