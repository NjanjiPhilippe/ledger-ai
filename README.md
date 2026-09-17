# LedgerAI

LedgerAI is a personal fintech project built as both a portfolio piece and a deep technical exploration of clean architecture principles applied to a real-world accounting domain. It combines a Spring Boot backend with an Angular frontend to deliver core double-entry accounting and financial reporting capabilities, augmented with an AI-powered financial advisor layer.

## Overview

The project models the core mechanics of a general ledger system: accounts, journal entries, and the financial reports derived from them. It is designed single-tenant today, but architected with a clear path toward multi-tenancy.

The backend is being built as a hands-on exercise to internalize every architectural decision from domain modeling to persistence with each deviation from a "textbook" implementation documented and justified.

## Architecture

LedgerAI backend follows **Hexagonal Architecture (Ports & Adapters)** combined with **CQRS** and **event-driven balance projections**:

- **Domain layer** : value objects, aggregates, and business rules for accounts and journal entries, independent of any framework or infrastructure concern.
- **Application layer** : use cases and ports (interfaces) that define what the system does, without knowing how.
- **Infrastructure layer** :JPA-backed adapters implementing the domain ports, tenant-scoped repositories, and database migrations.
- **CQRS** : command and query responsibilities are separated, with balance projections updated via domain events rather than computed on read.

### Data model highlights

- `Account` and `JournalEntry` are modeled as hexagonal, JPA-backed, tenant-scoped aggregates.
- `JournalEntry` lines are mapped as an `@ElementCollection` / `@Embeddable`, keeping the entry and its lines as a single consistency boundary.
- A database-level foreign key from `journal_entry_line.account_id` to `account.id` enforces referential integrity at the persistence layer, on top of domain-level invariants.
- Schema migrations are managed with **Liquibase**.

## Features

- **Double-entry accounting core** : accounts and journal entries with enforced balancing rules.
- **Financial reporting**
  - Trial balance
  - Balance sheet
  - Income statement
  - Financial snapshot composer for point-in-time reporting
- **AI Advisor layer** : a pluggable advisory module with swappable AI providers (OpenAI and Anthropic) selected via `@ConditionalOnProperty`, allowing the backend to generate financial insights on top of the ledger data.

## Tech Stack

**Backend**
- Java 21 / Spring Boot
- Hexagonal architecture, CQRS, event-driven projections
- Liquibase (schema migrations)
- PostgreSQL

**Frontend** (planned)
- Angular 21
- Keycloak authentication via Authorization Code + PKCE flow (planned, based on prior experience with `angular-oauth2-oidc`)

## Authentication

The frontend will integrate with **Keycloak** using the OAuth2 Authorization Code flow with PKCE. This approach is informed by prior work on an earlier iteration of the project, where `angular-oauth2-oidc`'s default `sessionStorage`-backed implementation resolved an `invalid_nonce_in_state` issue seen with a custom in-memory storage strategy.

## Roadmap

- [ ] Finish the from-scratch backend build (domain → application → infrastructure)
- [ ] Expand financial reporting use cases
- [ ] Harden multi-tenancy support
- [ ] Continue integrating and refining the AI Advisor layer
- [ ] Build the frontend (Angular 21) and integrate it with the backend

## Project Status

Early stage / in progress. The backend rebuild is not yet finished, and the frontend has not been started yet. Progress is being documented and shared as part of a "build in public" series.

## License

TBD
