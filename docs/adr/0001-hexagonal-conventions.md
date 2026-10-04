# ADR 0001 — Hexagonal conventions: where interfaces live

- Status: accepted
- Date: 2026-10-04

## Context

The backend follows Ports & Adapters. Early on, the code mixed two styles: most use cases were
concrete classes, while the advisor use cases were an interface plus a `...Service` implementation.
Two classes also crossed a boundary (a domain port imported a JPA entity, and a use case returned
one). Without a written rule, each new class was a coin flip.

## Decision

1. **Interfaces are outbound ports, and live in `domain.port`.**
   Persistence, the AI advisor, the tenant and the current user are reached through interfaces
   that the domain owns and the infrastructure implements.
2. **A use case is a concrete class, and is itself the inbound port.**
   Commands are `...UseCase`, reads are `...Query`. Controllers depend on them directly.
   No interface-per-use-case.
3. **Dependencies point inward**: `web` and `infrastructure` may use `application`; `application`
   may use `domain`; nothing depends on `web` or `infrastructure`.
4. **The domain is plain Java**: no Spring, JPA, Jackson or Lombok.
5. **The application layer does not know the persistence technology** (no JPA, no Spring Data).
   Spring is accepted there for `@Service`, `@Transactional`, `@PreAuthorize` and event publishing:
   a deliberate trade-off to avoid duplicating cross-cutting concerns in the web layer.

These rules are enforced by `HexagonalArchitectureTest` (ArchUnit), so they fail the build instead
of living only in this document.

## Consequences

- Less ceremony: no second file to keep in sync for each use case.
- Use cases are tested by mocking their outbound ports (or by faking them, see
  `PostThenReverseScenarioTest`).
- Trade-off: a controller depends on a class rather than an abstraction. If the same use case ever
  needs two entry mechanisms with different behaviour, an inbound interface can be introduced for
  that case only; the structure does not prevent it.
- Trade-off: Spring annotations in `application` mean that layer is not framework-free. The ArchUnit
  rules still keep it free of persistence details.
