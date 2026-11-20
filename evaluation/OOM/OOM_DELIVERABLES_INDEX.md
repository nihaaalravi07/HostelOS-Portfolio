# OOM Deliverables Index: HostelOS

Object-Oriented Methodologies (OOM) academic evaluation package for **HostelOS: A Better Way to Live on Campus**. Generated 16 November 2026 from the repository in `HostelOS/`. This package is separate from the Software Engineering package in `SE_Evaluation/`.

## Primary deliverables

| File | Purpose | Size |
|---|---|---|
| `HostelOS_OOM_Project_Report.docx` | Full academic report: cover, abstract, 20 chapters (classes, four pillars, relationships, UML, SOLID, patterns, exceptions, modularity, case study, limitations, conclusion, references) and four appendices | 83 pages |
| `HostelOS_OOM_Evaluation.pptx` | Short academic presentation with speaker notes | 15 slides |
| `HostelOS_UML_Documentation.docx` | Every UML diagram with title, purpose, explanation, class and relationship descriptions and implementation references | 58 pages |
| `OOM_Concept_Mapping.xlsx` | Concept to class to implementation to code location to explanation, with status; summary sheet (formulas) and viva index | 61 concepts, 3 sheets |
| `HostelOS_OOM_Viva_Preparation.docx` | 59 questions with answers in ten sections (A to J), 30 follow-up questions and a code navigation sheet | 28 pages |
| `HostelOS_Code_Walkthrough.docx` | Twelve important Java classes: purpose, attributes, methods, relationships, OOP concepts, code, workflow role, what to say in the viva | 25 pages |
| `OOM_SYLLABUS_MAPPING.md` | Each syllabus requirement to report section, diagram, source location and viva questions; list of concepts that are only partly demonstrated | - |
| `OOM_DELIVERABLES_INDEX.md` | This file | - |

## Diagrams (`diagrams/`, 28 PNG files at 200 dpi)

| File | Diagram |
|---|---|
| `01_u1_use_case_residents_staff.png` | Use case diagram 1: student and service-staff use cases |
| `02_u2_use_case_management.png` | Use case diagram 2: warden and administrator use cases |
| `03_u3_use_case_authentication.png` | Use case diagram 3: authentication (all six roles) |
| `04_c1_booking.png` | Class diagram 1: meal booking domain model |
| `05_c2_ledger.png` | Class diagram 2: ledger and charge calculators |
| `06_c3a_strategy.png` | Class diagram 3a: allocation strategies (Strategy pattern) |
| `07_c3b_accommodation.png` | Class diagram 3b: accommodation entities |
| `08_c4_layers.png` | Class diagram 4: layered structure of the mess module |
| `09_c5a_exceptions.png` | Class diagram 5a: exception handling classes |
| `10_c5b_security.png` | Class diagram 5b: filters and security classes |
| `11_c6_workflow.png` | Class diagram 6: workflow entities and their state enums |
| `12_c7_services.png` | Class diagram 7: service collaboration (constructor injection) |
| `13_c8_relationships.png` | Class diagram 8: association, aggregation and composition in HostelOS |
| `14_s1_book_meal.png` | Sequence diagram 1: student books a meal |
| `15_s2_confirm_consumption.png` | Sequence diagram 2: mess staff confirm consumption |
| `16_s3_ledger_charge.png` | Sequence diagram 3: ledger charge generation |
| `17_s4_authentication.png` | Sequence diagram 4: student authentication |
| `18_s5a_maintenance_submit_assign.png` | Sequence diagram 5a: maintenance request lifecycle, submit and claim |
| `19_s5b_maintenance_resolve_close.png` | Sequence diagram 5b: maintenance request lifecycle, start and resolve |
| `20_s5c_maintenance_close.png` | Sequence diagram 5c: maintenance request lifecycle, close |
| `21_a1_activity_booking.png` | Activity diagram 1: meal booking |
| `22_a2_activity_consumption.png` | Activity diagram 2: meal consumption and billing |
| `23_a3_activity_cleaning.png` | Activity diagram 3: cleaning request workflow |
| `24_a4_activity_maintenance.png` | Activity diagram 4: maintenance complaint workflow |
| `25_st1_meal_booking.png` | State machine diagram 1: MealBooking |
| `26_st2_cleaning.png` | State machine diagram 2: CleaningRequest |
| `27_st3_complaint.png` | State machine diagram 3: Complaint |
| `28_m1_package_dependencies.png` | Package dependency diagram (derived from import statements) |

## What was verified in this session

| Check | Result |
|---|---|
| Backend tests re-run with `mvn test` | 51 tests, 0 failures, 0 errors |
| Java facts re-extracted | 90 files, 4134 lines, 58 classes, 16 interfaces, 11 enums, 63 records |
| Class diagram members | each attribute and method checked against the extracted source by script |
| Code excerpts | read from the repository at generation time, with `file:line` |
| Word documents | rendered through LibreOffice to PDF; page counts above |
| Concept map | spreadsheet formulas recalculated, 0 errors |

## What was not verified

- The documents were not opened in Microsoft Word, PowerPoint or Excel (LibreOffice rendering only). After opening in Word, right-click the table of contents and choose *Update Field* if page numbers look off by one.
- UML sequence and activity diagrams were drawn by reading the cited methods; they are not generated from executable traces.
- No Java source was changed. Findings that suggest code changes (Chapter 18 of the report) are recommendations only.
- The frontend is out of scope.

## Findings the documents state openly

- No abstract classes and no class hierarchy among domain classes (composition is used).
- `ChargeCalculator<S>` has two implementations but both call sites inject the concrete class, so there is no dynamic dispatch on the interface today.
- `MessOperationsService` has about five responsibilities (SRP); `AccommodationService.recommend` tests the strategy name (OCP).
- One package cycle between `user` and `security`; no catch-all exception handler.
- No automated test covers the two allocation strategies.
- Builder, Abstract Factory, State and Observer are not implemented; Singleton is only Spring's bean scope.

## Placeholders for the team to complete

- Student names, enrolment numbers and faculty guide on the cover pages and title slide.
- The exact titles of the course lecture and laboratory notes in the report's reference list.

## Rebuilding

Generators and facts are in `_source/`. Run `_source/build_all.sh` (requires Python 3 with matplotlib, Pillow, openpyxl and PyMuPDF; Graphviz; Node.js packages from `package.json`; LibreOffice; Poppler). `_source/node_modules` is a symbolic link to `SE_Evaluation/_source/node_modules`; if that folder is deleted run `npm install` in `_source/` and remove the link first.

## Folder layout

```
OOM_Evaluation/
  HostelOS_OOM_Project_Report.docx
  HostelOS_OOM_Evaluation.pptx
  HostelOS_UML_Documentation.docx
  OOM_Concept_Mapping.xlsx
  HostelOS_OOM_Viva_Preparation.docx
  HostelOS_Code_Walkthrough.docx
  OOM_SYLLABUS_MAPPING.md
  OOM_DELIVERABLES_INDEX.md
  diagrams/         28 PNG files
  _source/          generators, extractor, data and evidence
```
