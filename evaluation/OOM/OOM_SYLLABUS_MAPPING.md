# OOM Syllabus Mapping: HostelOS

This file maps every requirement of the OOM evaluation brief to the report section that explains it, the diagram that shows it, the real source location that implements it, and the viva questions that cover it. Source locations are `file:line` values resolved from the repository when this file was generated (16 November 2026). Status values are honest: a concept that the Java code does not contain is marked **Not explicitly implemented**; nothing was added to the code to make it appear.

Report = `HostelOS_OOM_Project_Report.docx`; UML = `HostelOS_UML_Documentation.docx`; Viva = `HostelOS_OOM_Viva_Preparation.docx`; Walkthrough = `HostelOS_Code_Walkthrough.docx`; Concept map = `OOM_Concept_Mapping.xlsx`.

## 1. Summary of coverage

| Status | Concepts |
|---|---|
| Demonstrated | 47 |
| Partially demonstrated | 11 |
| Not explicitly implemented | 2 |
| Recommended improvement | 1 |
| **Total** | **61** |

**Concepts that are not fully demonstrated** (stated plainly):

- **Encapsulation** (Partially demonstrated): Strong in the entities. Weak spots: `CleaningRequest.moveTo` and `setAssignee` are public; `Meal.update` does not validate its input; `@NoArgsConstructor` is public.
- **Abstract classes** (Not explicitly implemented): We did not create an abstract class because there was no shared partial implementation to factor out. `OncePerRequestFilter` (Spring) is an abstract class that two of our classes extend.
- **Inheritance (class)** (Partially demonstrated): No hierarchy among domain classes: entities extend nothing. Composition is preferred. Test code reuses `IntegrationTestBase` through inheritance.
- **Polymorphism through ChargeCalculator** (Recommended improvement): The design allows polymorphism; the wiring does not use it yet. Fix: inject `ChargeCalculator<MealCharge>`.
- **Aggregation** (Partially demonstrated): One clear example; stated as arguable because UML gives aggregation little precise meaning.
- **Composition** (Partially demonstrated): Hostel, Room, Bed is a domain-level composition but implemented as plain associations (no cascade, no collection in the parent).
- **Single Responsibility** (Partially demonstrated): Strong in entities and handlers; weak in the largest service.
- **Open/Closed** (Partially demonstrated): Partly met; the explanation text should move into the strategies.
- **Interface Segregation** (Partially demonstrated): Tension comes from the framework's wide base interface.
- **Dependency Inversion** (Partially demonstrated): Mockito tests of `LedgerService` show the benefit; the concrete calculators show the gap.
- **Factory (static factory methods)** (Partially demonstrated): This is the static factory idiom, not GoF Factory Method or Abstract Factory.
- **Singleton** (Partially demonstrated): Singleton scope is a container feature; `CurrentUser` is a static utility class.
- **Builder** (Not explicitly implemented): No Builder class exists in HostelOS; nothing was added to create one.
- **Coupling and cohesion** (Partially demonstrated): Cohesion is high inside packages; coupling is low but not zero.

## 2. Requirement-by-requirement mapping

### A. Classes, objects, attributes, methods and constructors

| ID | Concept | Status | Report | Diagram | Source location (file:line) | Viva questions |
|---|---|---|---|---|---|---|
| A1 | Class | Demonstrated | §6 | Fig. C1, C2 | `…/mess/MealBooking.java:22`<br>`…/finance/LedgerService.java:21` | A1, A3 |
| A2 | Object | Demonstrated | §6.1 | Fig. S1 | `…/mess/MealBooking.java:51`<br>`…/mess/MealScheduleService.java:42` | A3 |
| A3 | Attributes (fields) and data types | Demonstrated | §6.2 | Fig. C1 | `…/mess/MealBooking.java:42` | B2 |
| A4 | Methods | Demonstrated | §6.3 | Fig. C1 | `…/mess/MealBooking.java:68`<br>`…/finance/LedgerService.java:63` | A4 |
| A5 | Constructors (default, parameterised, private, overloaded, enum) | Demonstrated | §6.1 | Fig. C1 | `…/mess/MealBooking.java:44`<br>`…/finance/LedgerService.java:29`<br>`…/mess/MealCategory.java:15` | B3, B4, B5, B6 |
| A6 | Entity classes | Demonstrated | §6.2 | Fig. C1, C3b | `…/mess/Meal.java:12` | B1 |
| A7 | Service classes | Demonstrated | §6.3 | Fig. C4, C7 | `…/finance/LedgerService.java:19` | B5 |
| A8 | Controller classes | Demonstrated | §6.4 | Fig. C4 | `…/mess/MessController.java:19` | H7, J5 |
| A9 | Repository interfaces | Demonstrated | §6.5, §8.2 | Fig. C4 | `…/mess/MealBookingRepository.java:14` | C5 |
| A10 | DTOs (records) | Demonstrated | §6.6, §14.4 | Fig. C8 | `…/mess/MealDto.java:10` | A5, H6 |
| A11 | Security and configuration classes | Demonstrated | §6.7 | Fig. C5b | `…/security/SecurityConfig.java:27`<br>`…/security/JwtService.java:17` | J5 |
| A12 | Exception classes | Demonstrated | §6.8, §15 | Fig. C5a | `…/common/BusinessException.java:6` | I1 |

### B. Four pillars of OOP

| ID | Concept | Status | Report | Diagram | Source location (file:line) | Viva questions |
|---|---|---|---|---|---|---|
| B1 | Encapsulation | Partially demonstrated | §7 | Fig. C1, ST1 | `…/mess/MealBooking.java:62`<br>`…/finance/LedgerEntry.java:17` | A2, A4, B6, C1, C2, C3, J3, J6 |
| B2 | Abstraction (interfaces) | Demonstrated | §8 | Fig. C3a, C4 | `…/accommodation/AllocationStrategy.java:9`<br>`…/finance/ChargeCalculator.java:11` | A2, C3, C4, C5 |
| B2b | Abstract classes | Not explicitly implemented | §8.3 | Fig. C5b | `(not applicable)` | C4 |
| B3 | Inheritance (class) | Partially demonstrated | §9 | Fig. C5a, C5b | `…/common/BusinessException.java:6`<br>`…/security/JwtAuthFilter.java:14`<br>`…/config/SiteGateFilter.java:30` | A2, D1, D2 |
| B3b | Inheritance (interface) | Demonstrated | §9.2 | Fig. C3a, C4 | `…/mess/MealRepository.java:8`<br>`…/accommodation/PackRoomsStrategy.java:9` | D1 |
| B3c | Method overriding | Demonstrated | §9.3 | Fig. C5b | `…/security/JwtAuthFilter.java:20`<br>`…/finance/MealChargeCalculator.java:13` | D3 |
| B4 | Runtime polymorphism (interface-based) | Demonstrated | §10.1 | Fig. C3a | `…/accommodation/AccommodationService.java:97`<br>`…/accommodation/AccommodationService.java:99` | A2, D4, D7 |
| B4b | Polymorphism through framework base types | Demonstrated | §10.2 | Fig. C5b | `…/config/SpaWebConfig.java:21` | D3 |
| B4c | Compile-time polymorphism (overloading) | Demonstrated | §10.3 | Fig. C1 | `…/mess/MessOperationsService.java:87`<br>`…/finance/LedgerService.java:73` | D5 |
| B4d | Parametric polymorphism (generics) | Demonstrated | §10.3 | Fig. C2 | `…/finance/ChargeCalculator.java:11` | D5 |
| B4e | Polymorphism through ChargeCalculator | Recommended improvement | §10.4, §13.5 | Fig. C2 | `…/mess/MessOperationsService.java:41`<br>`…/finance/LedgerService.java:46` | D6 |

### C. Relationships between classes

| ID | Concept | Status | Report | Diagram | Source location (file:line) | Viva questions |
|---|---|---|---|---|---|---|
| C1 | Association | Demonstrated | §11.1 | Fig. C8, C7 | `…/mess/MealBooking.java:27`<br>`…/accommodation/Allocation.java:20` | D2, E1, E2, E5 |
| C2 | Aggregation | Partially demonstrated | §11.2 | Fig. C3a, C8 | `…/accommodation/AccommodationService.java:30` | E1, E3 |
| C3 | Composition | Partially demonstrated | §11.3 | Fig. C8 | `…/mess/MessOperationsService.java:30`<br>`…/security/JwtService.java:23` | E1, E4 |

### D. UML diagrams

| ID | Concept | Status | Report | Diagram | Source location (file:line) | Viva questions |
|---|---|---|---|---|---|---|
| D1 | Use case diagram | Demonstrated | §12.1 | Fig. U1 to U3 | `…/mess/MessController.java:35` | F1, F4 |
| D2 | Class diagram | Demonstrated | §12.2 | Fig. C1 to C8 | `(see diagrams/)` | F1, F2, F5 |
| D3 | Sequence diagrams | Demonstrated | §12.3 | Fig. S1 to S5c | `…/mess/MessOperationsService.java:93` | F1, F3 |
| D4 | Activity diagrams | Demonstrated | §12.4 | Fig. A1 to A4, ST1 to ST3 | `…/housekeeping/CleaningStatus.java:10` | F1, F3 |

### E. SOLID principles

| ID | Concept | Status | Report | Diagram | Source location (file:line) | Viva questions |
|---|---|---|---|---|---|---|
| E1 | Single Responsibility | Partially demonstrated | §13.1 | Fig. C7 | `…/finance/LedgerService.java:16`<br>`…/mess/MessOperationsService.java:21` | G1, G2, J7 |
| E2 | Open/Closed | Partially demonstrated | §13.2 | Fig. C3a | `…/accommodation/AccommodationService.java:101`<br>`…/accommodation/SpreadRoomsStrategy.java:9` | G1, G3, J7 |
| E3 | Liskov Substitution | Demonstrated | §13.3 | Fig. C3a | `…/accommodation/PackRoomsStrategy.java:12`<br>`…/accommodation/SpreadRoomsStrategy.java:12` | G1, G4 |
| E4 | Interface Segregation | Partially demonstrated | §13.4 | Fig. C4 | `…/finance/ChargeCalculator.java:12`<br>`…/mess/MealRepository.java:8` | G1, G5 |
| E5 | Dependency Inversion | Partially demonstrated | §13.5 | Fig. C2, C7 | `…/user/UserService.java:15`<br>`…/mess/MessOperationsService.java:40` | D6, G1, G6, J6, J7 |

### F. Design patterns

| ID | Concept | Status | Report | Diagram | Source location (file:line) | Viva questions |
|---|---|---|---|---|---|---|
| F1 | Classification of patterns | Demonstrated | §14 | Table 21 | `(see report Chapter 14)` | H1, H2 |
| F2 | Dependency Injection (IoC) | Demonstrated | §14.2 | Fig. C7 | `…/finance/LedgerService.java:48` | B5, D7, G6, H5 |
| F3 | Strategy (GoF) | Demonstrated | §14.3 | Fig. C3a | `…/accommodation/AllocationStrategy.java:6` | H3 |
| F4 | DTO pattern | Demonstrated | §14.4 | Fig. C8 | `…/mess/MealDto.java:10` | H6 |
| F5 | Service Layer | Demonstrated | §14.6 | Fig. C4 | `…/mess/MessOperationsService.java:20` | H7 |
| F6 | Factory (static factory methods) | Partially demonstrated | §14.5 | Fig. C1 | `…/mess/MealBooking.java:51`<br>`…/common/BusinessException.java:16` | B3, H4 |
| F7 | Singleton | Partially demonstrated | §14.5 | - | `(framework behaviour)` | H4 |
| F8 | Builder | Not explicitly implemented | §14.7 | - | `…/security/JwtService.java:29` | H3 |
| F9 | Repository pattern | Demonstrated | §14.1 | Fig. C4 | `…/accommodation/BedRepository.java:11` | H2 |
| F10 | Template Method (framework) | Demonstrated | §14.3 | Fig. C5b | `…/security/JwtAuthFilter.java:20` | H3 |
| F11 | Chain of Responsibility / Proxy (framework) | Demonstrated | §14.3 | Fig. C5b | `…/security/SecurityConfig.java:50` | H3 |

### G. Exception handling and validation

| ID | Concept | Status | Report | Diagram | Source location (file:line) | Viva questions |
|---|---|---|---|---|---|---|
| G1 | Custom exception | Demonstrated | §15.1 | Fig. C5a | `…/common/BusinessException.java:7` | I1 |
| G2 | Global exception handler | Demonstrated | §15.2 | Fig. C5a | `…/common/GlobalExceptionHandler.java:14` | I2, J3, J4 |
| G3 | try-catch blocks | Demonstrated | §15.3 | Fig. S3 | `…/accommodation/AccommodationService.java:163` | I3 |
| G4 | Validation annotations | Demonstrated | §15.4 | - | `…/maintenance/ComplaintController.java:17` | C2 |
| G5 | Business rule validation | Demonstrated | §15.5 | Fig. ST1 | `…/mess/MealBooking.java:55` | C2 |
| G6 | HTTP error responses | Demonstrated | §15.6 | Table 24 | `…/security/SecurityConfig.java:43` | I2, I4 |
| G7 | Database constraint violations | Demonstrated | §15.6 | Fig. S3 | `…/common/GlobalExceptionHandler.java:4` | I2, J3 |
| G8 | Authentication and authorization failures | Demonstrated | §15.7 | Fig. S4 | `…/user/AuthController.java:39` | I4 |

### H. Modularity and maintainability

| ID | Concept | Status | Report | Diagram | Source location (file:line) | Viva questions |
|---|---|---|---|---|---|---|
| H1 | Package structure | Demonstrated | §16.1 | Fig. M1 | `…/HostelOsApplication.java:10` | J1, J8 |
| H2 | Separation of responsibilities | Demonstrated | §16.2 | Fig. C4 | `…/mess/MessController.java:43` | J8 |
| H3 | Controller-service-repository organisation | Demonstrated | §16.3 | Fig. C4 | `…/mess/MealController.java:22` | H7, J1, J2 |
| H4 | Coupling and cohesion | Partially demonstrated | §16.4 | Fig. M1, C7 | `…/resident/ResidentSummary.java:4` | E5, J8 |
| H5 | Reusability | Demonstrated | §16.5 | Fig. C2 | `…/finance/LedgerService.java:113` | J9 |
| H6 | Dependency management | Demonstrated | §16.6 | Fig. C7 | `…/accommodation/AccommodationService.java:32` | J9 |
| H7 | Maintainability and extensibility | Demonstrated | §16.7, §18 | Fig. ST2 | `…/housekeeping/CleaningStatus.java:12` | J9 |

## 3. Required UML diagrams

| Required diagram | Delivered as | Where |
|---|---|---|
| Use case diagram (six roles) | 3 diagrams | Report §12.1; UML §2 |
| Class diagram | 10 figures (8 numbered diagrams) | Report §5-§11 and §12.2; UML §3 |
| Sequence: student meal booking | s1_book_meal | Report §12.3; UML §4.1 |
| Sequence: mess staff consumption confirmation | s2_confirm_consumption | UML §4.2 |
| Sequence: ledger charge generation | s3_ledger_charge | UML §4.3 |
| Sequence: student authentication | s4_authentication | UML §4.4 |
| Sequence: maintenance request lifecycle | s5a, s5b, s5c (three parts) | UML §4.5 to §4.7 |
| Activity: meal booking and consumption | a1 (booking) and a2 (consumption and billing) | UML §5.1, §5.2 |
| Activity: cleaning request workflow | a3_activity_cleaning | UML §5.3 |
| Activity: maintenance complaint workflow | a4_activity_maintenance | UML §5.4 |
| (extra) State machines | st1, st2, st3 | UML §6 |

## 4. Required report sections (brief items 1 to 23)

| # | Required section | Where in the report |
|---|---|---|
| 1 | Cover page | Cover |
| 2 | Abstract | Abstract |
| 3 | Introduction to HostelOS | Chapter 1 |
| 4 | Problem statement | Chapter 2 |
| 5 | Objectives | Chapter 3 |
| 6 | Technology stack | Chapter 4 |
| 7 | Overview of the object-oriented architecture | Chapter 5 |
| 8 | Classes, objects, attributes, methods and constructors | Chapter 6 |
| 9 | Encapsulation | Chapter 7 |
| 10 | Abstraction | Chapter 8 |
| 11 | Inheritance | Chapter 9 |
| 12 | Polymorphism | Chapter 10 |
| 13 | Relationships between classes | Chapter 11 |
| 14 | UML diagrams | Chapter 12 |
| 15 | SOLID principles | Chapter 13 |
| 16 | Design patterns | Chapter 14 |
| 17 | Exception handling | Chapter 15 |
| 18 | Modularity and maintainability | Chapter 16 |
| 19 | Case study: meal booking and billing workflow | Chapter 17 |
| 20 | Limitations and possible improvements | Chapter 18 |
| 21 | Conclusion | Chapter 19 |
| 22 | References | Chapter 20 |
| 23 | Appendices containing relevant code snippets | Appendices A to D |

## 5. Viva coverage

| Section | Topic | Questions |
|---|---|---|
| A | Basic OOP concepts | A1, A2, A3, A4, A5 |
| B | Classes, objects, attributes and constructors | B1, B2, B3, B4, B5, B6 |
| C | Encapsulation and abstraction | C1, C2, C3, C4, C5 |
| D | Inheritance and polymorphism | D1, D2, D3, D4, D5, D6, D7 |
| E | Association, aggregation and composition | E1, E2, E3, E4, E5 |
| F | UML diagrams | F1, F2, F3, F4, F5 |
| G | SOLID principles | G1, G2, G3, G4, G5, G6 |
| H | Design patterns | H1, H2, H3, H4, H5, H6, H7 |
| I | Exception handling | I1, I2, I3, I4 |
| J | HostelOS-specific implementation questions | J1, J2, J3, J4, J5, J6, J7, J8, J9 |

59 questions plus 30 follow-up questions (three per section) and a code navigation sheet. Every concept row above lists the viva questions that cover it (no concept is left without a question). The two concepts marked "Not explicitly implemented" (abstract classes, Builder) are covered by questions C4 and H3, which answer what HostelOS does instead.

## 6. Consistency rules applied

- Counts (classes, entities, services, repositories, annotations) come from `extract_oom.py`; the report, presentation, viva document and this file read the same data.
- Code excerpts are read from the repository when the documents are generated and are quoted with `file:line`.
- Class diagrams are generated from the same extracted facts and fail if a member does not exist.
- No Java file was modified for these deliverables.
