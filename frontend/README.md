# Frontend

Angular 22 application for LedgerAI: sign-in with Keycloak (Authorization Code + PKCE), the current user, the
accounts list. English and French, Tailwind CSS, errors shown as toasts.

The architecture and its rules are in [ADR 0003](../docs/adr/0003-frontend-architecture.md).

## Requirements

Node.js 22.22.3+ or 24.15+, and the API (http://localhost:8085) and Keycloak (http://localhost:8180) running: see
the [root README](../README.md).

## Commands

| Command | What it does |
|---|---|
| `npm ci` | Install the dependencies |
| `npm start` | Dev server on http://localhost:4200 |
| `npm test` | Unit tests (Vitest), watch mode |
| `npm run test:ci` | Unit tests, single run |
| `npm run lint` | ESLint, including the architecture boundaries |
| `npm run format` / `format:check` | Prettier |
| `npm run build` | Production build in `dist/` |
| `npm run api:generate` | Regenerate `src/app/core/api/schema.ts` from `../docs/api/openapi.json` |

## Configuration

`public/config.json` is read when the application starts:

```json
{
  "apiBaseUrl": "http://localhost:8085",
  "keycloak": { "issuer": "http://localhost:8180/realms/ledgerai", "clientId": "ledgerai-frontend" }
}
```

## Structure

```
src/app/
├── core/        auth, config, api (generated types, error handling), i18n, toasts, layout
├── shared/      presentational pieces and helpers (money formatting)
└── features/    home, accounts: pages / ui / state / data-access
```

Translations live in `public/i18n/{en,fr}.json`; a test fails if they diverge.
Monetary amounts are strings (never `number`): see `src/app/shared/money`.
