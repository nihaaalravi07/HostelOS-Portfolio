# HostelOS SE Evaluation Package: Deliverables Index

Generated 2026-11-16 from the repository, its test output and its documentation. Project: **HostelOS — A Better Way to Live on Campus**.

## Primary deliverables

| # | File | Purpose | Size | Content |
|---|---|---|---|---|
| 1 | `HostelOS_SE_Project_Report.docx` | The complete project report for the SE evaluation | 3.2 MB | About 68 pages (A4): declaration, abstract, TOC, list of figures, sections 1 to 20, appendices A to D |
| 2 | `HostelOS_SE_Evaluation.pptx` | Presentation for the evaluation | 460 KB | 18 slides with speaker notes, native charts and editable diagrams |
| 3 | `HostelOS_SRS.docx` | Standalone Software Requirements Specification (ISO/IEC/IEEE 29148 structure) | 1.2 MB | About 31 pages: 70 functional and 22 non-functional requirements with IDs, acceptance criteria, 13 use case specifications |
| 4 | `HostelOS_Testing_Documentation.xlsx` | Test register, results, categories, traceability, defects | 71 KB | 8 sheets: Summary, Test Register, Test Results, Categories, Traceability, RTM Matrix, Defects & Limitations, System Test Evidence. 74 test cases, 92 requirements |
| 5 | `HostelOS_Risk_Register.xlsx` | Risk register with inherent and residual ratings | 25 KB | 5 sheets: Risk Register, Summary, Risk Matrix, Control Status, Scale. 21 risks |
| 6 | `HostelOS_Architecture_Documentation.docx` | System, component, database, API and deployment architecture | 3.0 MB | About 32 pages: 13 sections, 60-endpoint catalogue, 16 design decisions |
| 7 | `SE_SYLLABUS_MAPPING.md` | Maps every syllabus requirement to report sections and evidence | small | Sections A to J plus a deliverables checklist |
| 8 | `DELIVERABLES_INDEX.md` | This file | small | |

## Diagrams (`diagrams/`, PNG, 200 dpi)

| File | Diagram | How it was produced |
|---|---|---|
| `01_SDLC_lifecycle.png` | SDLC: iterative and incremental model with the six increments | Drawn (matplotlib) |
| `02_use_case_diagram.png` | Use cases for the six roles | Drawn (matplotlib) |
| `03_system_architecture.png` | Three-tier system architecture | Drawn (matplotlib) |
| `04_component_diagram.png` | Backend component diagram | **Derived from the Java imports** (`module_deps.py`), Graphviz |
| `05_ER_overview.png`, `05a..05d_ER_diagram_*.png` | ER overview and four domain diagrams with every column | **Derived from `V1__schema.sql`** (`extract_facts.py`), Graphviz |
| `06_sequence_booking_billing.png` | Sequence: booking, confirmation, exactly-once billing | Drawn from `MealService`, `MessOperationsService`, `LedgerService` |
| `07_activity_consumption.png` | Activity: consumption confirmation with every refusal path | Drawn from `confirmConsumption` |
| `08_deployment_diagram.png` | Deployment: local, one Docker image, prepared hosted setup | Drawn from Dockerfile, compose, render.yaml |
| `09a_*`, `09b_*` | State machines: booking, checkout, cleaning, complaint | Drawn from the status enums and entity methods |
| `10_work_breakdown_structure.png` | Work breakdown structure | From the WBS data (`misc.py`) |

## Evidence behind the numbers

| Item | Source |
|---|---|
| 51 backend test results | `backend/target/surefire-reports/*.xml` (Maven run, 16 Nov 2026) |
| Backend coverage (lines 86.6 %) | JaCoCo 0.8.12 report, `backend/target/site/jacoco` |
| 11 frontend test results and coverage | Vitest JSON and coverage summary in `_source/data/` |
| System tests ST-01, 04, 10, 11, 12 | `_source/evidence/system_tests_result.txt/.json`, produced by `_source/run_system_tests.py` |
| System tests ST-02, 03, 05 to 09 | Observed on 14 Nov 2026 during development; recorded in the test register as *observed*, not re-run |
| Code size, endpoints, schema, module dependencies | `_source/extract_facts.py`, `_source/module_deps.py` (read the repository) |

## Source of the package (`_source/`)

Everything is generated from data so that documents stay consistent: `content/` (requirements, test cases, risks, narrative), `data/` (extracted facts and merged JSON), generators (`make_srs.js`, `make_arch.js`, `make_report.js`, `make_pptx.js`, `make_xlsx.py`), `diagrams/` (diagram scripts and PNGs), `build_all.sh` (rebuild everything). Requires Node, Python (openpyxl, Pillow, matplotlib, pymupdf), Graphviz and LibreOffice. `node_modules/` can be deleted and recreated with `npm install`.

## Changes made to the repository for this package

* Added tests: `MealWorkflowEndToEndIntegrationTest` (student, mess and admin views change by exactly the meal price; database rejects a duplicate ledger source); renamed one test method to fix a typo; tightened one assertion (`SpaServingIntegrationTest`). Backend tests: 49 to 51.
* Nothing in the application source was changed. No commit or push was made.

## Verification performed on the files

* Word documents were converted to PDF with LibreOffice and inspected page by page as contact sheets and selected full pages; the table of contents is pre-filled with page numbers read from that rendering and is a real Word field that can be updated (right-click, Update field).
* The slide deck passed the Office schema validator and was rendered and inspected slide by slide.
* Both workbooks were recalculated with LibreOffice: 0 formula errors; key values were checked against the source data.
* Not verified: opening the files in Microsoft Word, PowerPoint and Excel themselves (only LibreOffice was available). Word may show slightly different pagination, so the TOC page numbers could differ by a page; update the field if so.

## To fill in before submission

Names and enrolment numbers, faculty guide, signatures, acknowledgement, and the RACI role table (cover page, declaration, report sections 14.7). The documents deliberately do not assert individual contributions.
