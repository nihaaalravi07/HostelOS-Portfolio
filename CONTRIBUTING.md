# Contributing to HostelOS

Thanks for your interest. HostelOS is an academic project, but issues and pull requests are welcome.

## Development setup

Prerequisites: JDK 21, Maven 3.9+, Node.js 22+, and PostgreSQL 16 or Docker.

```bash
# database
docker compose up -d

# backend (http://localhost:8080)
cd backend
export JWT_SECRET="$(openssl rand -base64 48)"   # required
export DEMO_MODE=true                            # demo data for local work
mvn spring-boot:run

# frontend (http://localhost:5173)
cd frontend && npm ci && npm run dev
```

Run the checks before pushing:

```bash
cd backend  && mvn test
cd frontend && npm test && npm run build
```

Integration tests start their own embedded PostgreSQL and set their own test-only JWT secret, so they need no environment variables.

## Branch naming

`type/short-description`, for example `feat/scheduled-rent`, `fix/cutoff-timezone`, `docs/api-examples`, `test/allocation-strategies`, `refactor/split-mess-service`.

## Commit messages

Use [Conventional Commits](https://www.conventionalcommits.org): `type(scope): summary` in the imperative, for example `fix(mess): reject confirmation of cancelled bookings`. Types: `feat`, `fix`, `docs`, `test`, `refactor`, `chore`, `ci`. Keep commits focused.

## Pull requests

- Describe what changed and why, and link the issue if there is one.
- Include tests for behaviour changes; the existing suite must pass (CI runs it).
- Update the docs (`docs/`, README) when behaviour, endpoints or configuration change.
- Keep PRs small. Do not mix refactoring and feature work.
- Never commit secrets, `.env` files or generated output (`node_modules`, `target`, `dist`).

## Code quality expectations

- **Rules live in the domain.** Business rules belong in entities and services, not controllers. Controllers validate, authorise with `@PreAuthorize`, and delegate.
- **Schema changes** go in a new Flyway migration (`V2__...sql`); never edit an applied migration.
- **Money** is `NUMERIC(12,2)` / `BigDecimal`. Ledger entries are append-only; correct mistakes with reversals.
- **Cross-module access** goes through the other module's service and ids, not its repositories or entities.
- **DTOs** are Java records; never return entities from controllers.
- Frontend code is TypeScript; keep `npm run build` and `npx oxlint .` free of errors.
- Prefer small, readable methods and comments that explain *why*.
