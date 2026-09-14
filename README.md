# Project Orbit — Backend Microservices

Seven modules: the six services from the API catalogue, plus an **API
Gateway**. JWT auth is now fully wired up.

## Services & ports

| Module                | Port | Role                                                        |
|------------------------|------|---------------------------------------------------------------|
| **api-gateway**        | 8080 | Entry point. Validates JWTs, coarse ADMIN-route enforcement.  |
| auth-service           | 8081 | Issues JWTs (register/login/me).                              |
| product-service        | 8082 | Product catalogue CRUD.                                       |
| order-service           | 8083 | Order creation/lookup/status. Calls product & inventory via Feign. |
| inventory-service       | 8084 | Stock levels. ADMIN + internal-key only.                       |
| payment-service         | 8085 | Mock payment processing/refunds.                                |
| notification-service     | 8086 | Mock notification dispatch. POST is internal/ADMIN only.        |

**Talk to the gateway (`http://localhost:8080`) from a client/frontend.**
Each service is still independently runnable on its own port too (useful
for testing one in isolation with curl/Postman).

## How auth works end-to-end

1. `POST /api/auth/register` and `POST /api/auth/login` are public (no
   token needed). Login returns a real signed JWT now — `LoginResponse.token`
   — containing `sub` (userId), `email`, and `role` claims, valid 1 hour.
2. Every other request must send `Authorization: Bearer <token>`.
3. **The gateway** validates the token's signature/expiry first, and does
   coarse route-level checks (`/api/admin/**` and `/api/inventory/**`
   require `role=ADMIN`). It forwards the original `Authorization` header
   downstream unchanged, plus adds `X-User-Id` / `X-User-Role` convenience
   headers.
4. **Each service** *also* validates the JWT itself (`JwtAuthenticationFilter`
   + `SecurityConfig` in its own `security` package) and does its own
   fine-grained checks — e.g. order-service reads `CurrentUser.id()` to
   scope "my orders" to the caller, payment-service requires ADMIN only on
   `/refund`. **This is intentional, not redundant**: authentication (is
   this token valid?) can live at the edge, but authorization (is *this*
   user allowed to do *this* specific thing to *this* specific resource)
   needs the business context that only lives inside each service. If a
   service were ever reachable directly (bypassing the gateway), it's still
   fully protected on its own.

All six services and the gateway share one JWT signing secret
(`jwt.secret` in every `application.yml`) — in a real deployment this
would come from a secrets manager / config server, not be copied by hand
into six files like it is here.

## Inter-service communication: OpenFeign, not RestTemplate

`order-service` needs to call `product-service` (to price/validate items)
and `inventory-service` (to decrement/restore stock) when placing or
cancelling an order. It now does this with **Spring Cloud OpenFeign**
(`@FeignClient` interfaces) instead of a manually-wired `RestTemplate`:

- `client/ProductFeignClient.java` — plain `@FeignClient`, no special
  headers, since `GET /api/products/{id}` is public.
- `client/InventoryFeignClient.java` — uses a per-client
  `InventoryFeignClientConfig` that attaches an `X-Internal-Api-Key` header
  to every request, because inventory's increase/decrease endpoints are
  `hasAnyRole("ADMIN","INTERNAL")`.

**Why Feign over RestTemplate:** it's declarative (an interface + annotations,
no manual `RestTemplate.exchange(...)` boilerplate), it's the standard
Spring Cloud pairing with service discovery (once Eureka/Consul is added
later, swapping the hardcoded `url = "${...}"` for a load-balanced
`name = "product-service"` client is a one-line change), and it composes
cleanly with retry/circuit-breaker libraries (Resilience4j) if you add
resilience later. RestTemplate still works fine for simple cases — Feign
just scales better as the number of inter-service calls grows.

### The internal-API-key pattern

This is the piece that doesn't come for free with JWT: **order-service
isn't an ADMIN, but it needs to call ADMIN-only inventory endpoints.**
Forwarding the customer's JWT would either fail (they're not ADMIN) or
require weakening inventory's role check — both wrong. Instead:

- `inventory-service` and `notification-service` each have an
  `InternalApiKeyFilter` that checks for a header `X-Internal-Api-Key`
  matching their configured `internal.api-key`. If it matches, the request
  is authenticated as a synthetic `ROLE_INTERNAL` principal.
- `order-service`'s `internal.api-key` config value must match
  `inventory-service`'s — that's the shared secret between just those two
  services (not shared with product/payment/auth, since they're never
  called this way).
- Their `SecurityConfig`s allow `hasAnyRole("ADMIN", "INTERNAL")` on the
  relevant endpoints, so either a real admin's JWT *or* a valid internal
  key works.

## Running locally

Postgres per service, same as before:

```bash
docker run --name orbit-postgres -e POSTGRES_USER=orbit -e POSTGRES_PASSWORD=orbit -p 5432:5432 -d postgres:16
for db in orbit_auth orbit_product orbit_order orbit_inventory orbit_payment orbit_notification; do
  docker exec orbit-postgres psql -U orbit -c "CREATE DATABASE $db;"
done
```

Start order: **auth-service and the other five services first, gateway last**
(the gateway doesn't do service discovery yet, so it just needs the static
`localhost` ports to be listening):

```bash
# from each service's own folder
mvn spring-boot:run
```

Then hit everything through `http://localhost:8080` instead of the
individual ports.

## Structure (per service, unchanged)

```
src/main/java/com/orbit/ecommerce/<service>/
├── controller   → REST endpoints
├── service      → business logic
├── repository   → Spring Data JPA repositories
├── model        → JPA entities (plain Java: explicit getters/setters/constructors + a hand-written builder)
├── dto          → request/response DTOs (same pattern - no Lombok)
├── security     → JwtUtil (validate-only) + JwtAuthenticationFilter + SecurityConfig
│                  (+ InternalApiKeyFilter, inventory/notification only)
├── exception    → custom exceptions + @RestControllerAdvice global handler
├── config       → misc beans
└── client       → (order-service only) Feign clients to product/inventory
```

No Lombok anywhere in this project - every entity/DTO that previously used
`@Data`/`@Builder`/`@Getter`/`@Setter` now has hand-written getters, setters,
constructors, and a manual static `Builder` class with the same fluent API
(`Product.builder().name(...).build()` still works exactly as before) so no
service/controller call sites needed to change. `@RequiredArgsConstructor` on
services/controllers/filters was replaced with an explicit constructor.

## Known simplifications (call these out if this goes further)

- `order-service`'s `/cancel` endpoint doesn't re-check that the caller
  owns the order being cancelled — any authenticated user can cancel any
  order id right now. Add an ownership check (`order.getUserId().equals(
  CurrentUser.id())` or allow ADMIN override) before this goes anywhere
  real.
- The JWT secret and internal API key are plaintext in `application.yml`
  for six independent files — fine for learning/local dev, not fine for
  production. Centralize via environment variables or a config server.
- No token refresh/blacklist/logout mechanism yet — tokens are valid for a
  flat 1 hour and there's no way to revoke one early.

## Next steps

1. Spin everything up, log in via the gateway, and confirm a full
   order-placement flow works with a real JWT end to end.
2. Add service discovery (Eureka) so the gateway and Feign clients stop
   using hardcoded `localhost` ports.
3. Build the React + Vite frontend (Section 13–19) against the gateway.
