# Project status

Last verified: **21 November 2026** on macOS (Apple silicon), JDK 21.0.13, Node 24, PostgreSQL 16.

## Verified for this release

| Check | Result |
|---|---|
| `mvn test` (backend) | **51 tests, 0 failures, 0 errors** (unit, Mockito and integration tests on an embedded PostgreSQL) |
| `npm test` (frontend, Vitest) | **11 tests, 0 failures** |
| `npm run build` (type-check + Vite production build) | succeeds; Vite warns that the main chunk is larger than 500 kB |
| `oxlint` | 0 errors; warnings only (React purity rule: `Date` called during render in a few pages) |
| Run end to end against PostgreSQL 16 | Backend jar with the React build embedded, demo mode, headless-browser screenshots of all main screens; booking a meal created no charge, confirming it as mess staff charged exactly the meal price once, and a second confirmation returned `409` |
| Start-up without `JWT_SECRET` | The application refuses to start |
| GitHub Actions workflow | Syntax checked locally; the first run on GitHub is recorded in the repository's Actions tab |

**Test coverage.** JaCoCo was run during the Software Engineering evaluation (before this release; coverage tooling is not part of the Maven build): about 87% of backend lines and 68% of branches. Frontend coverage measured with Vitest was 21.8% of lines overall, because only three test files exist (login, meal card, formatting). These numbers were not re-measured for this release.

## Completed functionality

| Area | Status |
|---|---|
| Authentication | JWT login for six roles; `@PreAuthorize` on every protected endpoint; verified by `SecurityIntegrationTest` |
| Student dashboard | Profile, room and roommates, month spend, balance, next cleaning, open complaints, today's menu, announcements |
| Pay-per-meal | Menus, prices, cutoffs, book / cancel / re-book, history, staff confirmation, no-show closing, automatic closing of stale bookings |
| Billing and ledger | Confirmation and charge in one transaction, idempotent and race-safe, append-only ledger with reversals, running-balance statements, simulated payments, monthly rent posting |
| Feedback and analytics | Taste and hygiene ratings for meals eaten, mess analytics, finance report |
| Accommodation | Hostels, rooms, beds, allocate / transfer / release, two recommendation strategies, database-enforced single occupancy |
| Housekeeping | Slot booking with conflict prevention, claim / assign, status workflow with notes |
| Maintenance | Submit, assign or claim, controlled status transitions, resolution notes, resident closes |
| Checkout | Request, live clearance checks, clear (releases the bed) or reject |
| Deployment | Dockerfile, Compose, Render Blueprint, optional passcode gate, health check |

## Known limitations

- **Payments are simulated.** There is no payment gateway.
- **No user, resident or role administration.** `GET /api/users` is read-only; accounts come from the demo seed. A real deployment needs a way to create users.
- **Announcements** can be created through the API but there is no form in the UI.
- **Monthly rent** is posted by an administrator action (`POST /api/finance/rent`), not on a schedule.
- **Complaints are not routed by hostel**; maintenance staff see the whole board.
- **Security hardening is basic:** no refresh tokens, login rate limiting or account lockout. The site passcode is a shared secret.
- **No browser end-to-end tests;** UI behaviour beyond three component test files was verified manually.
- **No real food photography:** meal cards use designed fallback tiles; images placed in `frontend/public/food/` are used automatically.
- **QR check-in** is not implemented; staff confirm manually.
- **Frontend bundle** is a single chunk (about 800 kB) without code splitting.
- **Demo data** is generated relative to the day it is first seeded, and demo mode keeps today's meals bookable until midnight (`hostelos.demo-open-today`).
- **Object-oriented design gaps** are recorded in the OOM evaluation: no abstract classes or domain class hierarchy, `ChargeCalculator<S>` is not used polymorphically, `MessOperationsService` has several responsibilities, no catch-all exception handler, and no automated test of the allocation strategies.
- **History:** the repository was prepared for publication in a single commit; earlier development history is not included.

## Remaining work

Ideas, not commitments: payment gateway integration, scheduled rent, user and resident administration screens, notifications, QR meal check-in, rate limiting and lockout, route-level code splitting, browser end-to-end tests, and splitting `MessOperationsService`.
