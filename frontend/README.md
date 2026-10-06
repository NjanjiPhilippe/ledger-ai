# Frontend

Reserved for the Angular application (not started yet).

Planned first slice: sign-in with Keycloak (Authorization Code + PKCE, client `ledgerai-frontend`), the current
user, accounts, journal entries (record, post, reverse) and the trial balance, against the API on
http://localhost:8085.

The backend allows CORS from http://localhost:4200 and its OpenAPI spec is served at
http://localhost:8085/v3/api-docs once the API is running.
