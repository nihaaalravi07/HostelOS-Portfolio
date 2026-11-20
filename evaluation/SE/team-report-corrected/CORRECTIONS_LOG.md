# HostelOS SE report and slides: corrections log

Corrected edition of `HostelOS_Project_Report.pdf` (43 pages) and `HostelOS_Presentation.pptx` (29 slides).
Every technical statement was checked against the repository (source, schema, tests run on 16 Nov 2026).

## Files
- `HostelOS_Project_Report_Corrected.docx` and `.pdf` (53 pages, table of contents, same 15 chapters and appendices, plus Appendix C: corrections)
- `HostelOS_Presentation_Corrected.pptx` (20 slides with speaker notes; was 29)

## What was wrong and how it was fixed

| # | Earlier draft | Corrected | Evidence |
|---|---|---|---|
| 1 | Class diagram: Student, Staff, Warden extend User; methods authenticate(), hasPermission(), markEaten(), append(), escalate() | One `User` entity with a `Role` enum (six roles); real methods markConsumed, cancel, rebook, markMissed; diagram regenerated from the Java source | user/User.java, user/Role.java, mess/MealBooking.java |
| 2 | Running balance stored on each ledger line; app role has no update/delete permission | No balance column; balance computed when the statement is read; UPDATE/DELETE blocked by a database trigger | V1__schema.sql, LedgerService.statement |
| 3 | Rent charged automatically on the 1st of each month | Rent is posted by an administrator-run endpoint (idempotent); only the demo seeder posts it automatically. FR-19 marked Partial | FinanceController, LedgerService.postMonthlyRent |
| 4 | Complaints routed to the maintenance queue of the student's hostel | Shared queue; staff claim or warden/admin assigns. FR-25 marked Partial | ComplaintService |
| 5 | Technology "Node.js with Express (confirm)", "PostgreSQL (confirm)" left in the report | Java 21, Spring Boot 3.5.0, Spring Data JPA, PostgreSQL 16, Flyway; React 19 front end | backend/pom.xml |
| 6 | API table with guessed endpoints marked "Design" | Real endpoints and the roles allowed, from the controllers | controller classes |
| 7 | ER diagram with USER, ROOM(hostel_block), LEDGER_TXN(balance_after); four roles | 14 real tables; six roles; ER generated from the migration | V1__schema.sql |
| 8 | "Four sprints in 16 weeks" but six listed, overlapping weeks; rotating Scrum master, burn-down, retrospectives | Iterative increments reconstructed from history; 16-week table labelled a planning baseline; ceremonies marked not held; recorded timeline added | git history (one commit) |
| 9 | Peer review of all changes, pull requests, CI pipeline, staging, daily backups, pilot block, "100% changes peer-reviewed" | Shown as recommended practice, not as done; applied versus recommended listed side by side | git log, repository tree |
| 10 | Key billing tests "Planned"; 9 of 48 tests verified; coverage targets with no numbers | 74 recorded tests, all passing; billing, concurrency and immutability tests executed; measured coverage: backend 86.6%, frontend 21.8% (81.95% of imported files) | Maven, JaCoCo, Vitest |
| 11 | FP estimate 8.6 KLOC / 23 person-months presented as sizing the plan | Kept, and compared with the measured size 5.8 KLOC / 15.2 person-months; effort was not tracked | SE package size data |
| 12 | Layout: half-empty pages, no table of contents, 29 slides | Table of contents added; 20 slides; figures regenerated or reused where correct | - |

## Kept from the earlier draft (verified correct)
Meal cutoff times (6:30 am, 11:00 am, 3:30 pm, 6:30 pm), four cleaning slots, complaint categories and priorities, Rs 3,500 rent, the demonstration reconciliation (8,975 - 3,500 = 5,475; 1,815 mess spend), FP count 161 / 172, COCOMO arithmetic, the UI screenshots, data flow diagrams and checkout flow.

## Still for the team to do
1. Section 7.1: replace the proposed role table with who actually did what.
2. Section 1.1: confirm the AI-assistance statement is acceptable to the institution.
3. Open both files once in Word and PowerPoint (checked only through LibreOffice). In Word, right-click the table of contents and choose Update Field if page numbers differ.
4. Not covered by the evidence and stated as such: user acceptance tests, load and penetration tests, requirements validation with real users.
