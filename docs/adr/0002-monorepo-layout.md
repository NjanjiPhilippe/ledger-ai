# ADR 0002: Monorepo layout

- Status: accepted
- Date: 2026-10-06

## Context

The project now has two applications (a Spring Boot API and an Angular frontend) and shared infrastructure
(PostgreSQL and Keycloak through docker compose, a Keycloak realm, CI, documentation). Everything lived at the
repository root, which only worked while there was a single application.

## Decision

One repository, split by deliverable:

- `backend/` holds the Spring Boot project: `pom.xml`, the Maven wrapper and `src/`.
- `frontend/` holds the Angular project.
- The infrastructure stays **at the root**, outside both applications: `docker-compose.yml`, `keycloak/`,
  `.github/workflows/`, `docs/` and the README. It serves both applications and belongs to neither.

## Consequences

- One link, one history and one CI for the whole project; a change that spans the API contract and the UI can be
  one pull request.
- Commands run from the folder of the application (`cd backend`), and tooling is configured with that folder
  (the CI uses `working-directory: backend`, IDEs open `backend/pom.xml`).
- Files that reach across folders must find the repository root instead of assuming the working directory
  (`KeycloakRealmFileTest` walks up to `docker-compose.yml`).
- Trade-off: path-anchored rules (`.gitignore`, `.gitattributes`) must not be anchored to the old root.
