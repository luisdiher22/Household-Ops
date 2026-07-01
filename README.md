# Household Ops

A household operations assistant for the UHNW family-office stack — fills the gap between arrival-readiness checklists and the ongoing, day-to-day churn of running a household: what's low on stock, who's doing what, and what needs the owner's sign-off before it happens.

## Why this project

This was built as a portfolio project while applying to a company that builds family-office software (Eleven Family Office, Eleven Property, Eleven Messages, and similar). Their public product materials cover two things well: checklists/maintenance tracking for getting a property guest-ready, and a family of conversational assistants that answer natural-language questions over structured data ("what capital calls are due this week?").

What's missing from that picture is the boring, constant stuff: the pantry running low, an errand that needs assigning to a specific staff member, a repair that costs more than the household's casual-spend threshold and needs the owner to actually say yes. This project is that fifth assistant — modeled deliberately on the shape of the other two patterns already in place there:

- **Approval flow** mirrors a "threshold spend needs the principal's sign-off" pattern: tasks and shopping-list items over a household's configured threshold automatically raise an approval request against that household's specific principal, and can't be marked complete until it's decided.
- **The assistant** answers natural-language questions the same way a "capital calls due this week" query would need to work — grounded in live structured data via tool-calling, not a document search.

## Architecture

```mermaid
flowchart TB
    subgraph Client
        UI["React SPA<br/>(Vite, TanStack Query)"]
    end

    subgraph Backend["Spring Boot"]
        Controller["REST Controllers"]
        Security["JWT Auth + Household-Scoping Guard"]
        Service["Domain Services<br/>(Household, Task, Inventory, ShoppingList, Approval)"]
        XML["ReorderRulesEngine<br/>(classic Spring XML bean)"]
        Assistant["Assistant Orchestration<br/>(Spring AI ChatClient)"]
        Cache["Redis Cache-Aside<br/>(inventory status)"]
    end

    subgraph External
        Claude["Anthropic Claude<br/>(tool-calling)"]
    end

    DB[(Postgres)]
    Redis[(Redis)]

    UI -->|JWT bearer| Controller
    Controller --> Security
    Security --> Service
    Service --> DB
    Service <--> Cache
    Cache <--> Redis
    Service --> XML
    Controller --> Assistant
    Assistant -->|4 read-only tools,<br/>household injected server-side| Service
    Assistant <--> Claude
```

## Things worth noticing

- **One bean is deliberately wired via classic Spring XML**, not `@Component`: `ReorderRulesEngine`'s implementation (`legacy/reorder-rules-context.xml` + `@ImportResource`) — modeling how a stable, rarely-touched business-rules component might persist in a long-lived enterprise codebase rather than being migrated to annotations for its own sake. It still participates fully in the Boot context (autowirable, runs on a `@Scheduled` job) once loaded.
- **Redis backs a real cache-aside**, not a decorative one: the inventory low-stock aggregation is the most-read, moderately expensive computed view in the system (hit by the dashboard and by the assistant's inventory tool on nearly every query), invalidated explicitly on writes via `@CacheEvict`. Getting this right in practice surfaced a genuine bug — see below.
- **The assistant uses tool-calling over four fixed, read-only queries, not RAG.** The household a query is scoped to always comes from the authenticated caller's JWT, server-side — the model never supplies or chooses it, so there's no path for a prompt to leak another household's data.
- **JWT auth with 4 roles** (Owner/Principal, House Manager, Staff, Vendor) mapped to real family-office personas, with household-scoping enforced as an explicit guard clause rather than left implicit, and approval decisions additionally require being the *specific* assigned principal — not just any Owner.

### A real bug worth mentioning

While verifying the frontend in an actual browser (not just via `curl`), the inventory-status page intermittently 500'd. The Week 2 fix for a different Redis issue — Jackson's default `ObjectMapper` doesn't know how to serialize `java.time.Instant` — had been made by swapping in Spring's shared `ObjectMapper`. That fixed serialization, but silently dropped Jackson's default-typing metadata, so a cache *hit* deserialized back as a raw `LinkedHashMap` instead of the actual DTO record (a `ClassCastException` on read, not write). Earlier manual testing hadn't caught it because every prior test happened to evict the cache before reading it back, so the actual hit path never ran. The fix uses a dedicated `ObjectMapper` for the Redis serializer with both `JavaTimeModule` and default typing active — worth mentioning because it's the kind of bug that only a real end-to-end check (not curl one-shots, not unit tests against a mocked cache) will surface.

## Tech stack


| Layer    | Choice                                                                    |
| ---------- | --------------------------------------------------------------------------- |
| Backend  | Java 17, Spring Boot 3.5, Spring Data JPA, Spring Security, Flyway        |
| Database | PostgreSQL 16                                                             |
| Cache    | Redis 7                                                                   |
| Auth     | JWT (jjwt), BCrypt                                                        |
| AI       | Spring AI 1.0 + Anthropic Claude (tool-calling)                           |
| Frontend | React 19, TypeScript, Vite, Tailwind CSS v4, TanStack Query, React Router |
| API docs | springdoc-openapi / Swagger UI                                            |

## Running it

### One command (Docker)

```bash
docker compose up --build
```

Brings up Postgres, Redis, and the app (backend + bundled frontend) on **http://localhost:8090**. Demo data seeds automatically on first boot.

To enable the assistant page, export `ANTHROPIC_API_KEY` before running — everything else works without it (the assistant endpoint returns a clean error if the key isn't set).

### Local development (faster iteration)

```bash
docker compose up postgres redis   # just the infra
cd backend && ./mvnw spring-boot:run   # API on :8090
cd frontend && npm install && npm run dev   # UI on :5173, proxies /api to :8090
```

### Demo credentials

Seeded automatically; password is `password123` for all:


| Role          | Email                    |
| --------------- | -------------------------- |
| Owner         | owner@householdops.dev   |
| House Manager | manager@householdops.dev |
| Staff         | staff@householdops.dev   |
| Vendor        | vendor@householdops.dev  |

The login page has one-click buttons for each.

### Demo script

1. Log in as the House Manager, create a task with an estimated cost above the household's threshold ($250 by default).
2. Log in as the Owner — see the pending approval, approve or reject it with a note.
3. On the Shopping List page, click "Check inventory for restocks" — the XML-wired `ReorderRulesEngine` runs and queues auto-generated items for anything low on stock.
4. On the Assistant page, ask "What does the house need before Friday?" — watch it call both the tasks and inventory tools and synthesize one answer, with the tool-call trace shown underneath.

## Testing

- **Unit tests**: the approval-threshold trigger and its guards (`ApprovalServiceTest`), the task DONE-while-pending guard (`TaskServiceTest`), the XML-wired reorder engine's math (`DefaultReorderRulesEngineTest`), and the inventory low-stock computation (`InventoryStatusServiceTest`).
- **Integration test**: `SecurityAuthorizationTests` exercises the real JWT filter chain end-to-end via `MockMvc` — no token, wrong password, wrong role, and cross-household access, all against the live Postgres-backed app context.
- Explicitly not covered: end-to-end browser tests (verified manually via Playwright during development instead — see the bug above), load testing, and mutation testing. Disproportionate for a project this size.

Run with `cd backend && ./mvnw test` (requires Postgres/Redis running, e.g. via `docker compose up postgres redis`).

## What I'd do with more time

- **RS256 over HS256** for the JWT signature — the production answer for key rotation and multi-service trust; HS256 with a shared secret was the simpler choice to ship.
- **A real polymorphic `ApprovalRequest` subject** instead of the soft `subjectType`/`subjectId` reference — simpler to build against a small, fixed set of subject types, at the cost of no DB-level FK integrity on the subject.
- **httpOnly cookie auth** instead of JWTs in `localStorage` on the frontend — avoids XSS token exposure; skipped for the simplicity of a plain REST API with no session/CSRF machinery.
- **Write-capable assistant tools**, gated behind an explicit confirmation step — today the assistant is deliberately read-only as a safety boundary.
- **Testcontainers** instead of pointing tests at the same long-lived dev Postgres instance — the more correct approach for test isolation, but the extra setup wasn't worth it for this timeline.
