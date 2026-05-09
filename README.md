# Pastebin Clone

[![CI](https://github.com/medeirosjoaquim/pastebinclone/actions/workflows/ci.yml/badge.svg)](https://github.com/medeirosjoaquim/pastebinclone/actions/workflows/ci.yml)

A small but production-shaped pastebin: create a paste, share a short URL, optionally protect it with a password, set an expiration date, or make it self-destruct on first read.

Built as a portfolio piece — the goal is for the code to look the way a mid-sized team would actually ship it: typed entities, validated DTOs, real migrations, layered tests, one-command Docker setup.

## Stack

- **Backend**: Spring Boot 3.1, Java 17, Spring Data JPA, Spring Security (BCrypt), Bean Validation, Flyway
- **Database**: PostgreSQL 16
- **Frontend**: Angular 17 + Angular Material (in `frontend/`)
- **Tests**: JUnit 5, Mockito, AssertJ, Testcontainers, `@WebMvcTest`, `@DataJpaTest`, `@SpringBootTest`
- **CI**: GitHub Actions (backend `mvn verify` + frontend `ng build`)

## Quickstart

The whole backend (app + Postgres) runs with one command:

```bash
docker compose up --build
```

Backend listens on `http://localhost:8080`, Postgres on `localhost:5433`.

For the Angular frontend:

```bash
cd frontend && npm install && npm start
```

UI is at `http://localhost:4200`, talks to the backend at `http://localhost:8080/pastes`.

## API

Base path: `/pastes`

- `POST /pastes` — create a paste. Body validated by `CreatePasteDTO`.
- `GET /pastes` — list public, non-expired, non-password-protected pastes. Paginated (`?page=0&size=20&sort=createdAt,desc`).
- `GET /pastes/{url}` — fetch one paste. Increments view count. Sends `X-Paste-Password: <pw>` if protected. Returns 410 if expired, 401 if password missing/wrong.
- `GET /pastes/{url}/raw` — same as above but `text/plain`.
- `PUT /pastes/{url}` — update a paste; body must include the current `password`.
- `DELETE /pastes` — delete a paste; body has `{url, password}`.

### `curl` examples

Create:

```bash
curl -X POST http://localhost:8080/pastes \
  -H 'Content-Type: application/json' \
  -d '{"title":"hello","content":"println(\"hi\")","language":"java","password":"pw"}'
```

Read with password:

```bash
curl http://localhost:8080/pastes/abc1234567 -H 'X-Paste-Password: pw'
```

Read raw:

```bash
curl http://localhost:8080/pastes/abc1234567/raw -H 'X-Paste-Password: pw'
```

Delete:

```bash
curl -X DELETE http://localhost:8080/pastes \
  -H 'Content-Type: application/json' \
  -d '{"url":"abc1234567","password":"pw"}'
```

## Design notes

- **Short URL**: 10 hex chars of MD5 over `title + content + now + 8 random bytes`. Salted so identical input doesn't collide; uniqueness is enforced at the DB layer (`UNIQUE` index on `url`) and the service retries up to 5 times on a `DataIntegrityViolationException`.
- **Passwords**: hashed with `BCryptPasswordEncoder` (default cost). Never returned in any DTO. Verified with the encoder's constant-time `matches()`. The same password gates *both* viewing and mutating (delete/update); password-protected pastes are also hidden from the public listing.
- **Expiration**: pastes with a past `expirationDate` return `410 Gone` on direct fetch and are filtered out of the listing. The listing query is one statement: `(expiration_date IS NULL OR expiration_date > now) AND exposure = 'PUBLIC' AND (password IS NULL OR password = '')`.
- **Burn-after-read**: a `burnAfterRead` flag deletes the row in the same transaction as the fetch that returns it. The view count is *not* incremented for burn pastes — they can only ever be read once.
- **View count**: incremented atomically with a JPQL `UPDATE shared_pastes SET views = views + 1 WHERE id = ?` rather than a read-modify-write, so concurrent reads don't lose increments.
- **Schema management**: Flyway owns the schema (`src/main/resources/db/migration/V1__init.sql`). Hibernate is set to `validate` so the entity and DB can never silently drift.
- **Errors**: a single `@RestControllerAdvice` maps `ResponseStatusException`, validation failures, and unexpected exceptions to a uniform `{timestamp, status, message}` payload.

## Tests

```bash
./mvnw test
```

Covers:

- `PasteRepositoryTest` (`@DataJpaTest` + Testcontainers Postgres) — listing filter, atomic view increment, unique URL constraint.
- `PasteServiceTest` (Mockito) — exposure default, password hashing, slug-collision retry, burn-after-read, expiration, partial update.
- `PasteControllerTest` (`@WebMvcTest`) — DTO validation 400s, status-code mapping (200/204/401/410), `X-Paste-Password` header plumbing, raw `text/plain`.
- `PasteEndToEndTest` (`@SpringBootTest` + Testcontainers) — full HTTP flow: create → 401 without password → 200 with password → views increment → delete → 404; burn-after-read; listing hides password-protected; expired returns 410; raw endpoint returns plain text.

## Repository layout

- `src/main/java/com/app/pastebinclone/` — backend
  - `controllers/` — `PasteController`, `GlobalExceptionHandler`
  - `services/PasteService` — business logic
  - `repository/PasteRepository` — JPA repo with custom JPQL
  - `models/` — `Paste` entity, `Exposure` enum, `ErrorResponse`
  - `DTOs/` — request/response DTOs with bean validation
  - `config/SecurityConfig` — BCrypt + permit-all (no user accounts yet)
- `src/main/resources/db/migration/` — Flyway migrations
- `src/test/java/...` — tests
- `frontend/` — Angular 17 SPA

## Future work

Tier 2 features that would round this out for a senior-level review:

- User accounts + JWT (Spring Security is wired but currently `permitAll`).
- OpenAPI / Swagger UI via `springdoc-openapi`.
- Rate limiting (Bucket4j) on `POST /pastes`.
- `@Scheduled` cleanup of expired pastes from disk.
- Spring Boot Actuator + Micrometer + Prometheus endpoint.
- Frontend polish: syntax highlighting (Prism.js), copy-to-clipboard, dark mode, raw view link.
- Live demo on Fly.io / Render.
