# HostelOS: Software Engineering Syllabus Mapping

Maps every syllabus requirement to the exact section of the deliverables and to the supporting evidence in the repository.

**Abbreviations:** **Report** = `HostelOS_SE_Project_Report.docx`; **SRS** = `HostelOS_SRS.docx`; **Arch** = `HostelOS_Architecture_Documentation.docx`; **Slides** = `HostelOS_SE_Evaluation.pptx`; **Tests** = `HostelOS_Testing_Documentation.xlsx`; **Risks** = `HostelOS_Risk_Register.xlsx`. Section numbers refer to the headings in those files. "Fig." names a file in `SE_Evaluation/diagrams/`.

**Honesty labels used throughout:** *Verified* (a test or measurement was run), *Recorded* (from a log, timestamp or Git history), *Estimate* (retrospective, labelled), *Recommendation* (not done yet). Where a topic is only partly covered by the project, the "Status" column says so.

---

## A. Software Development Life Cycle (SDLC)

| Syllabus requirement | Where addressed | Evidence / notes | Status |
|---|---|---|---|
| Explain the SDLC model selected | Report 4.1 | Iterative and incremental model; theory (meaning, why it matters, application to HostelOS) | Covered |
| Justify why it was appropriate | Report 4.2 (table of project characteristics, alternatives: Waterfall, Scrum, Spiral, Prototyping) | Priority-ordered brief, deadline, correctness-critical billing | Covered |
| Explain the development phases | Report 4.3 (table: theory, actual, evidence, recommendation) | Six phases from requirements to maintenance | Covered |
| Map the actual process to the phases | Report 4.3, 4.4 (increments, recorded timeline) | Git history (one commit, 14 Nov 2026 20:42), backend start log (17:10), Maven/Vitest output | Covered; only events with evidence are listed |
| Iterative implementation and testing | Report 4.5 | Build-run-test-fix loop; defects D-01..D-10 fed back (Report 11.7) | Covered |
| SDLC diagram | Fig. `01_SDLC_lifecycle.png`; Report 4.1 (Figure 1); Slides 6 | Generated for this package | Covered |
| Distinguish actual versus recommended process | Report 4.6 (table), 4.3 last column | No sprints, reviews, CI or effort tracking took place; stated plainly. AI-assisted development is disclosed (Report front matter, 4.6) | Covered |

## B. Software Requirements Analysis

| Syllabus requirement | Where addressed | Evidence / notes | Status |
|---|---|---|---|
| Problem identification and background | Report 1.1 to 1.2, 2.1 | Project brief (not stored in repo); limitation that no survey was done (Report 17, L-10) | Covered |
| Existing system and its limitations | Report 3.1 | Six limitations tabulated | Covered (from the brief, not a field survey) |
| Proposed system | Report 3.2, 3.3 (comparison) | README.md, ARCHITECTURE.md | Covered |
| Stakeholders and their requirements | Report 5.1 (table), 5.2 (by role) | Links to FR IDs | Covered |
| Functional requirements | Report 5.3; SRS 4 (70 FR with IDs, priority, status, tests) | `requirements.py` source of truth; Tests > Traceability | Covered |
| Non-functional requirements | Report 5.4; SRS 5 (22 NFR) | Measured values: 289 MiB memory, 1 to 28 ms latency | Covered |
| System constraints and assumptions | Report 5.5, 5.6; SRS 2.6, 2.7, 8, 9 | | Covered |
| Use cases and user interactions | Report 5.7; SRS Appendix A (13 specifications); Fig. `02_use_case_diagram.png`; Slides 7 | UC-01..UC-13 | Covered |
| All six roles covered (Student, Warden, Admin, Mess, Housekeeping, Maintenance) | Report 5.2 (role table); SRS 2.4 (user classes); use case diagram | Role matrix: Arch 7.2, Report 7.4 | Covered |

## C. Software Requirements Specification (SRS)

| Syllabus requirement | Where addressed (SRS unless stated) | Evidence / notes | Status |
|---|---|---|---|
| Introduction and purpose | 1.1 | | Covered |
| Scope | 1.2 | In scope and out of scope | Covered |
| Overall description | 2 (2.1 perspective, 2.3 functions, 2.5 environment) | | Covered |
| User classes and characteristics | 2.4 (table) | | Covered |
| Functional requirements with unique IDs | 4 (FR-AUTH, FR-MESS, FR-FIN, ...) | 70 IDs; status Implemented 82 / Partial 6 / Not implemented 4 across all 92 requirements | Covered |
| Non-functional requirements with unique IDs | 5 (NFR-SEC, NFR-REL, NFR-PERF, ...) | 22 IDs | Covered |
| External interface requirements | 3 (user, hardware, software, communication; endpoints by role) | | Covered |
| Hardware and software requirements | 7 | | Covered |
| Database requirements | 6 (integrity rules, ER overview, tables) | Fig. `05_ER_overview.png` | Covered |
| System constraints | 8 (with design consequences), 2.6 | | Covered |
| Assumptions and dependencies | 9, 2.7 | | Covered |
| Acceptance criteria | 10 (20 criteria with verdicts, workflows A to E) | All 20 met; two met by design or manual observation (stated) | Covered |
| Professional structure | Follows ISO/IEC/IEEE 29148 (successor of IEEE 830); gaps listed in SRS 11; traceability in SRS 12 | | Covered |
| Report coverage of the SRS | Report 6 (summary of the SRS) | | Covered |

## D. Software Architecture and Design

| Syllabus requirement | Where addressed | Evidence / notes | Status |
|---|---|---|---|
| Overall system architecture | Arch 1, 2; Report 7; Fig. `03_system_architecture.png`; Slides 8 | Three tiers, one deployable unit | Covered |
| Frontend architecture | Arch 3; Report 7.2, 10.4 | `frontend/src` (16 pages, auth, api client, TanStack Query) | Covered |
| Backend architecture | Arch 4; Report 7.3, 8.1 to 8.3 | 12 packages, layering, cross-cutting concerns | Covered |
| Database architecture | Arch 6; Report 9; Figs. `05_ER_overview.png`, `05a..05d` | `V1__schema.sql`; constraints, trigger, indexes | Covered |
| REST API communication | Arch 7 (conventions, endpoint catalogue of 60 endpoints) | Derived from controllers | Covered |
| Authentication and authorization | Arch 2.1, 8; Report 7.4, 10.3, 12.5 | JWT filter, 56 `@PreAuthorize`, tests IT-SEC-01..06, ST-10 | Covered |
| Role-based access control | Arch 7.2 (role matrix); Report 7.4; SRS 3.4 | Matrix derived from the code | Covered |
| Modular design and separation of concerns | Arch 4, 5; Report 8.1, 8.4, 8.7 | | Covered |
| Database relationships | Arch 6.1; ER figures | | Covered |
| Important design decisions and trade-offs | Arch 11 (16 decisions D1..D16); Report 8.8 | | Covered |
| Architecture diagrams | Fig. `03`; Arch 2; Report 7 | | Covered |
| Component diagrams | Fig. `04_component_diagram.png`; Arch 5; Report 8.2 | Derived from Java imports (not hand drawn) | Covered |
| Database ER diagrams | Figs. `05_ER_overview.png`, `05a..05d`; Arch 6; Report 9 | Derived from the migration | Covered |
| Design principles: separation of concerns | Report 8.7 (meaning, why, in HostelOS, example); Arch 12 | `MessController` delegates; rule in `MessOperationsService` | Covered |
| Design principles: encapsulation | Report 8.7 | `MealBooking.markConsumed`, `LedgerEntry` without setters | Covered |
| Design principles: modularity | Report 8.4, 8.7 | | Covered |
| Design principles: low coupling and high cohesion | Report 8.4 (coupling metrics Ce, Ca, instability from imports) | One cycle user to security acknowledged (R20, L-09) | Covered |
| Design principles: single responsibility | Report 8.7; Arch 12 | `MealChargeCalculator`, `SiteGateFilter` | Covered |
| Design principles: maintainability | Report 8.7 (Maintainability), 16; Arch 12 | | Covered |
| Additional diagrams requested | Sequence (Fig. `06`), activity (Fig. `07`), deployment (Fig. `08`), state machines (Figs. `09a`, `09b`) | Arch 9, 10; Report 7.5, 8.5, 8.6 | Covered |

## E. Project Planning, Scheduling and Effort Estimation

| Syllabus requirement | Where addressed | Evidence / notes | Status |
|---|---|---|---|
| Project scope and deliverables | Report 14.1 | | Covered |
| Work Breakdown Structure | Report 14.2 (diagram + 24 work packages with hours); Fig. `10_work_breakdown_structure.png` | Hours are **retrospective estimates** (labelled) | Covered, estimate |
| Development phases | Report 4.3, 14.3 | | Covered |
| Project schedule and milestones | Report 14.3 (proposed 8-week plan versus recorded actuals); 4.4 timeline | Only recorded events carry dates; no dates invented | Covered, proposed vs recorded |
| Task dependencies | Report 14.4 (table and critical path) | | Covered |
| Resource requirements | Report 14.5 | | Covered |
| Effort estimation | Report 14.6: bottom-up 356 person-hours; Basic COCOMO from measured 5.8 KLOC gives 15.2 person-months; comparison and caveats | No formal estimation technique was used during development (stated) | Covered, estimate |
| Team responsibilities | Report 14.7 (proposed RACI template with placeholders) | Individual contributions are **not asserted**; one Git commit exists | Template only |
| Distinguish recorded versus estimated/proposed | Report 14 (opening callout), 1.5 (kinds of statements) | | Covered |

## F. Software Testing

| Syllabus requirement | Where addressed | Evidence / notes | Status |
|---|---|---|---|
| Testing strategy | Report 11.2 (pyramid, levels table); Slides 13 | | Covered |
| Unit testing | Report 11.2, App. A; Tests > Test Register (25 unit tests) | `MealBookingLifecycleTest`, `StatusTransitionTest`, `LedgerServiceTest`, `DatabaseUrlPostProcessorTest` | Verified |
| Integration testing | Report 11.2; Tests (26 integration tests on embedded PostgreSQL) | `integration/*IntegrationTest` | Verified |
| System testing | Report 11.2, 11.5, App. C; Tests > Test Register (ST-01..ST-12), System Test Evidence | `run_system_tests.py` output; ST-02/03/05..09 observed 14 Nov 2026 and labelled as such | Verified / observed |
| API testing | Tests > Categories; Report 11.2 | MockMvc HTTP tests, live HTTP script | Verified |
| Frontend testing | Report 11.2; Tests (11 Vitest tests) | `MealCard.test.tsx`, `Login.test.tsx`, `format.test.ts` | Verified |
| Database testing | Tests > Categories (13 cases); Report 11.6 | IT-BIL-05, IT-E2E-02, IT-SVC-04 | Verified |
| Authentication and authorization testing | Tests > Categories (17 cases) | IT-SEC-01..06, IT-GATE-01..04, ST-10 | Verified |
| Error handling and validation testing | Tests > Categories; Report 12.3 | UT-MB-03, IT-BIL-04 (rating 9 gives 400) | Verified |
| Concurrency testing | Report 11.6 ("simultaneous confirmation test"); IT-BIL-03, ST-04 | Six threads: one HTTP 200, five 409, one ledger entry | Verified |
| End-to-end workflow testing | Report 11.6; IT-BIL-01, IT-E2E-01, IT-SVC-01, IT-SVC-03, IT-CHK-01 | No browser-automation suite exists (L-02) | Verified (API level) |
| Testing objectives | Report 11.1 | | Covered |
| Test environment | Report 11.3; Tests > Summary | | Covered |
| Test cases with unique IDs, inputs, expected and actual results, status | Tests > Test Register (74 rows); Report App. A | Status taken from Maven/Vitest output; no result typed by hand | Verified |
| Test coverage information | Report 11.5; Tests > Summary, Test Results; Arch App. A | Backend: lines 86.6 %, branches 67.6 %, instructions 85.0 % (JaCoCo). Frontend: **21.8 %** of lines over all sources (81.95 % over only the files tests import) | Measured; frontend gap stated |
| Meal workflow end to end (book, confirm, one charge, dashboards) | Report 11.6; Arch 9.1, 9.2; Figs. `06`, `07`; Slides 12 | IT-BIL-01, IT-E2E-01 (exact deltas on student, mess and admin views) | Verified |
| How duplicate billing is prevented | Report 11.6 (five layers), Arch 9.1, Slides 12 | UT-MB-03, UT-LG-01, IT-BIL-03, IT-E2E-02, ST-04 | Verified |
| Requirement-to-test traceability | Tests > Traceability and RTM Matrix; SRS 12; Report App. B | 92 requirements: 67 automated, 7 system-test only, 14 manual only, 4 not implemented | Covered |
| Defects and limitations | Tests > Defects & Limitations; Report 11.7, 11.8, 17 | Ten defects, all fixed, each with verification | Covered |

## G. Software Quality Assurance, Verification and Validation

| Syllabus requirement | Where addressed | Evidence / notes | Status |
|---|---|---|---|
| Quality assurance approach | Report 12.1 | Prevention, detection, correction | Covered |
| Verification versus validation | Report 12.2 (concept table and activities done / not done) | Validation with real users not yet done (stated) | Covered |
| Code quality practices | Report 12.1 (table QA_EXAMPLES) | Records for DTOs, small modules, TypeScript checks; no static analysis or peer review (stated) | Covered |
| Testing and defect prevention | Report 12.1, 11.7 | | Covered |
| Input validation | Report 12.3 | 38 Bean Validation declarations | Covered |
| Exception handling | Report 12.3 | 69 throw sites, `GlobalExceptionHandler` | Covered |
| Database integrity | Report 9, 12.4; Arch 6.2 | | Covered |
| Security controls | Report 12.5, 10.3; Arch 8 (threat table) | Gaps: no rate limiting (R14) | Covered |
| Consistency of financial records | Report 12.4, 9.2 | Computed balance, idempotent posting, reversal, trigger | Covered |
| Acceptance testing | Report 12.6; SRS 10 | | Covered |
| Examples from the implementation | Throughout; code excerpts in Report 10.2 (copied from source files by the generator) | | Covered |

## H. Version Control and Configuration Management

| Syllabus requirement | Where addressed | Evidence / notes | Status |
|---|---|---|---|
| Git usage | Report 13 (table) | One commit on `main` (14 Nov 2026) with a GitHub remote | Implemented (basic) |
| Repository structure | Report 13 | | Implemented |
| Branching and commit practices | Report 13 | None used; **recommendation**: GitHub Flow, conventional commits, PR review | Recommendation |
| .gitignore | Report 13 | | Implemented |
| Dependency management | Report 13 | Maven parent 3.5.0 + pinned libraries; `package-lock.json` | Implemented |
| Environment configuration | Report 13 (environment variable table) | | Implemented |
| Database migrations | Report 13, 9.3 | Flyway `V1__schema.sql`; `ddl-auto: validate` | Implemented (single migration) |
| Build configuration | Report 13, 10.5 | Maven, npm/Vite, Dockerfile, scripts | Implemented |
| Application configuration | Report 13; Arch 10.2 | `application.yml` | Implemented |
| Version and release management | Report 13 | pom 1.0.0, package.json 0.0.0 (inconsistency, L-11); no tags or changelog; **recommendation** given | Recommendation |

## I. Risk Analysis

| Syllabus requirement | Where addressed | Evidence / notes | Status |
|---|---|---|---|
| Risk register (ID, description, probability, impact, severity, mitigation, contingency) | Risks > Risk Register (21 risks); Report 15 | Severity = P x I with editable thresholds (Risks > Scale) | Covered |
| Duplicate meal billing | R01 | Mitigated: 5 layers, tests, ST-04 | Covered |
| Unauthorized access | R02, R03, R14 | | Covered |
| Database failure | R04 | | Covered |
| Data inconsistency | R05 | | Covered |
| Concurrent requests | R06 | | Covered |
| Incorrect room allocation | R08 | | Covered |
| Service downtime | R09 | Cold start measured: 145 s at 0.1 CPU | Covered |
| Data loss | R07 | Open: no backups | Covered |
| Dependency vulnerabilities | R10 | | Covered |
| Incomplete user adoption | R11 | | Covered |
| Mitigated versus residual | Risks > Control Status, Summary, Risk Matrix; Report 15.2, 15.3 | 9 mitigated, 7 partial, 1 accepted, 4 open | Covered |

## J. Software Maintenance

| Syllabus requirement | Where addressed | Evidence / notes | Status |
|---|---|---|---|
| Corrective, adaptive, perfective, preventive maintenance | Report 16.1 (table with real examples) | Defects D-02..D-04, D-08, checkout, tests | Covered |
| Database backup and recovery | Report 16.3 | **Not implemented** (R07, NFR-REL-04); plan is proposed | Proposal |
| Dependency updates | Report 16.4 | | Proposal |
| Bug fixing | Report 16.2 (process), 11.7 (defect log) | | Covered |
| Scalability | Report 16.5; Arch 13.3 | | Covered |
| Future enhancements | Report 18 (prioritised table) | | Proposal |
| Realistic maintenance strategy | Report 16.6 (calendar), 16.1 to 16.5 | | Proposal |

## Primary deliverables checklist

| Required deliverable | File | Notes |
|---|---|---|
| Complete Project Report: cover, TOC, abstract, introduction, problem, objectives, existing/proposed systems, SDLC, requirements, SRS, architecture, design, database, implementation, testing, QA, configuration management, planning, risk, maintenance, limitations, future scope, conclusion, references, appendices | `HostelOS_SE_Project_Report.docx` | All sections present (Report sections 1 to 20 plus Appendices A to D, preceded by declaration, abstract, TOC, list of figures) |
| PowerPoint presentation (15 to 18 slides) | `HostelOS_SE_Evaluation.pptx` | 18 slides, speaker notes included |
| Standalone SRS | `HostelOS_SRS.docx` | |
| Testing documentation workbook: test case register, results, categories, traceability matrix, defects and limitations, summary | `HostelOS_Testing_Documentation.xlsx` | Also RTM grid and system test evidence |
| Risk register workbook | `HostelOS_Risk_Register.xlsx` | Also scale, summary, matrices, control status |
| Architecture documentation | `HostelOS_Architecture_Documentation.docx` | |
| Diagrams (SDLC, use case, architecture, component, ER, sequence, activity, deployment) | `diagrams/` (15 PNG files) | Plus state machines and WBS |
| `SE_SYLLABUS_MAPPING.md` and `DELIVERABLES_INDEX.md` | This folder | |

## Things the team must still do or confirm

1. Fill in the placeholders for names, enrolment numbers, faculty guide, signatures and the acknowledgement (cover pages, declaration, RACI table).
2. Confirm the AI-assistance statement in the declaration matches the institution's expectations and be ready to explain every module (risk R17).
3. Confirm whether the hosted deployment exists; the documents describe it as *prepared, not verified in production*.
4. Re-run `SE_Evaluation/_source/run_system_tests.py` after any code change and regenerate the package with `_source/build_all.sh`.
