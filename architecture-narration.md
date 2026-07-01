# Architecture diagram — talking points

Notes for narrating the Mermaid diagram in the README. Walk it in request order: a click in the browser flows left to right through these boxes. Not meant to be read verbatim — pick the sentences that fit your pacing.

## Client

**React SPA (Vite, TanStack Query)**
The frontend. A single-page React app — login, dashboard, tasks, shopping list, approvals, and the assistant chat all live here. TanStack Query handles fetching and caching data from the API so the UI doesn't have to hand-roll loading/error state everywhere. It talks to the backend over plain REST, attaching a JWT to every request.

*Talking point: "This is the only part of the stack that's not Java — everything behind it is Spring."*laLasslCoo

## Backend (Spring Boot)

**REST Controllers**
The entry point for every API call — `/api/households/...`, `/api/tasks/...`, and so on. Thin by design: they parse the request, delegate to a service, and shape the response. No business logic lives here.

**JWT Auth + Household-Scoping Guard**
Every request (except login) carries a bearer token, validated here. This is also where the app enforces that a staff member can only ever touch their *own* household's data — even with a valid token, a cross-household request gets rejected. Certain actions (deciding an approval, changing someone's role) are further gated to specific roles.

*Talking point: "This is deliberately two layers, not one — 'are you logged in' and 'is this actually your household' are separate checks."*

**Domain Services (Household, Task, Inventory, ShoppingList, Approval)**
Where the actual business rules live. The one worth calling out: when a task or shopping-list item costs more than the household's configured threshold, the Approval service automatically raises a request against that household's principal — and the item can't be marked done or purchased until it's decided.

**ReorderRulesEngine (classic Spring XML bean)**
Decides whether an inventory item needs restocking, and how much to order. The twist: this one bean is wired the old way, with a literal Spring XML file (`<bean class="...">`), not an `@Component` annotation — matching how a real long-lived enterprise codebase often keeps one stable, rarely-touched rules engine frozen in legacy config while everything around it modernizes.

*Talking point: "This box is here on purpose — it's the one piece of the stack that's deliberately old-school, to show I can work in that kind of codebase, not just greenfield Spring Boot."*

**Assistant Orchestration (Spring AI ChatClient)**
The natural-language layer. Takes a question like "what does the house need before Friday," sends it to Claude along with four tools it's allowed to call, and lets Spring AI's own tool-calling loop handle the back-and-forth (ask the model → it requests a tool → run it → hand back the result → repeat until it has an answer).

**Redis Cache-Aside (inventory status)**
The one place in the app that's actually cached. Computing "what's low on stock" gets hit constantly — by the dashboard and by the assistant — so the result is cached in Redis and explicitly invalidated whenever inventory changes, rather than left to guesswork or a blanket cache-everything approach.

*Talking point: "I picked one real, defensible thing to cache instead of sprinkling `@Cacheable` everywhere to check a box."*

## External

**Anthropic Claude (tool-calling)**
The actual model. It never touches the database directly and it never decides which household it's allowed to look at — it only ever calls the four tools it's given, and those tools are hard-scoped server-side to whoever is logged in.

*Talking point: "The model can't be tricked into leaking another household's data, because it never gets to choose the household — that's injected before the request ever reaches it."*

## Data stores

**Postgres**
The system of record — households, staff, tasks, inventory, shopping lists, approvals. Schema is managed with Flyway migrations, not Hibernate auto-DDL.

**Redis**
Backs the one cache described above. Nothing else in the app depends on it being up (if Redis were down, the inventory-status call would just be a bit slower, not broken).

## The arrows worth explaining out loud

- **UI → Controller, labeled "JWT bearer"**: every authenticated request carries the token; this is the one line that represents the entire frontend/backend contract.
- **Assistant → Service, labeled "4 read-only tools, household injected server-side"**: this is the security property described above — worth pointing at directly since it's easy to miss in a quick read of the diagram.
- **Service ↔ Cache ↔ Redis**: a double-headed arrow on purpose — reads check the cache first, writes evict it.
