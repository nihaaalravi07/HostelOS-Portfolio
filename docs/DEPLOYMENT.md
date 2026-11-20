# Deployment

HostelOS ships as **one container, one port**: a multi-stage `Dockerfile` builds the React app, embeds it in the Spring Boot jar, and runs that jar on a small JRE image. The only external dependency is a PostgreSQL 16 database.

> **Status.** The Docker image and the Compose set-up were built and run locally, and the repository contains a Render Blueprint (`render.yaml`) for Render + Neon. This repository does **not** claim a live deployment, and the Render and Neon steps below were not re-run for the portfolio release. Free-tier limits change often; check each provider's current terms.

## Environment variables

| Variable | Required | Default | Meaning |
|---|---|---|---|
| `JWT_SECRET` | **yes** | none (the app refuses to start) | Signs login tokens. At least 32 characters. Generate: `openssl rand -base64 48`. Changing it logs everybody out |
| `DATABASE_URL` | one of the two styles | none | `postgresql://user:pass@host/db?sslmode=require`; translated to JDBC by `DatabaseUrlPostProcessor` |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | alternative | `jdbc:postgresql://localhost:5432/hostelos`, `hostelos`, `hostelos` | JDBC-style settings used when `DATABASE_URL` is not set |
| `DEMO_MODE` | no | `false` | `true` seeds demo data and lists demo accounts on the login screen. **Never use on a real deployment** |
| `DEMO_PASSWORD` | no | `hostelos123` | Password of the seeded demo accounts (only meaningful with `DEMO_MODE=true`) |
| `SITE_PASSCODE` | recommended for a public demo URL | empty (no gate) | Shared passcode asked from every visitor before anything else |
| `PORT` | set by the host | `8080` | HTTP port |
| `DB_POOL_SIZE` | no | `5` | Maximum database connections |

`.env.example` lists these with placeholders. Real values go in your host's secret store or a local `.env`, which is git-ignored. The backend's connection pool keeps no idle connections so a scale-to-zero database can sleep, and the container runs with the Asia/Kolkata time zone so "today's meals", cutoffs and the midnight roll-over match campus time.

## Local setup

See the README ([Getting started](../README.md#getting-started)). In short:

```bash
cp .env.example .env                     # set JWT_SECRET
docker compose --profile full up --build # PostgreSQL + the production image on http://localhost:8080
```

`docker compose up -d` (without the profile) starts only PostgreSQL for development. Use `POSTGRES_PORT=5433` and `APP_PORT=8081` if 5432 or 8080 are busy.

To build the single jar without Docker (needs JDK 21, Maven, Node 22+): `scripts/build-single-jar.sh` produces `backend/target/hostelos-backend-1.0.0.jar`; run it with the environment variables above.

## Docker image

```bash
docker build -t hostelos .
docker run -p 8080:8080 \
  -e DATABASE_URL=postgresql://user:pass@host/db \
  -e JWT_SECRET="$(openssl rand -base64 48)" \
  -e SITE_PASSCODE=choose-a-passcode \
  hostelos
```

The image runs as a non-root user, caps the heap at 65% of the container's RAM and uses the serial GC, which is what a ~512 MB free instance needs. Cold start is slow on a very small CPU allocation (measured earlier at about 145 s with 0.1 CPU versus about 9 s with 0.5 CPU), so a free instance that has gone to sleep should be warmed up before a demo.

## Render + Neon (configured, not claimed live)

`render.yaml` defines one free Docker web service (health check `/healthz`, auto-deploy off) that asks for `DATABASE_URL` and `SITE_PASSCODE`, generates `JWT_SECRET`, and sets `DEMO_MODE=true`. The database is not created by the Blueprint: create a PostgreSQL database (for example on Neon, direct connection, not pooled), copy its connection string, and paste it when Render prompts.

1. Push this repository to your own GitHub account (keep it private while testing).
2. Create the database and copy the connection string; do not put it in the repository.
3. In Render choose **New → Blueprint**, select the repository, and provide `DATABASE_URL` and `SITE_PASSCODE`.
4. Wait for the build and the first start (the first start migrates the schema and, in demo mode, seeds the demo data).
5. Verify with the bundled smoke test, which checks the health endpoint, the UI, logins, a few dashboards and a role refusal:

```bash
./scripts/smoke-test.sh https://YOUR-SERVICE.onrender.com YOUR_PASSCODE   # needs demo mode, uses the demo accounts
```

For a deployment with real users, set `DEMO_MODE=false` in `render.yaml` (or the dashboard); you then have to create users yourself, because there is no user-registration screen yet.

## Security considerations

- **Secrets** are read from the environment only. The application has no default JWT secret. Never commit `.env` files; `.gitignore` excludes them.
- **Demo mode** exposes demo account emails and the shared password on the login screen. Use it for local demos and short-lived evaluation deployments only.
- **Site passcode** keeps casual visitors and crawlers away from a public demo URL, but it is a single shared secret without rate limiting. Do not put real personal data behind it.
- **Missing hardening** (see [PROJECT_STATUS.md](PROJECT_STATUS.md)): no refresh tokens, login rate limiting or account lockout. Add these before any real use.
- **HTTPS** is expected to be terminated by the platform; the app trusts `X-Forwarded-*` headers (`forward-headers-strategy: framework`).
- **Transport and CORS:** CORS only allows `localhost`/`127.0.0.1` origins, which is fine because the production UI is served from the same origin.
- **Database:** the app user needs DDL rights on first start (Flyway creates the schema). The ledger trigger and constraints are part of the schema, so do not restore data by bypassing them.

## CI

`.github/workflows/ci.yml` runs on every push to `main` and every pull request: the backend build and tests (Java 21), the frontend tests and production build (Node 22, `npm ci`), and a Docker image build. No secrets are used or needed.
