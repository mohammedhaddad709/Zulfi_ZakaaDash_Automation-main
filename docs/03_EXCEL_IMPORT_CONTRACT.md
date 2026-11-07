# Excel-driven future test onboarding (NOT implemented today)

When the user uploads Excel test cases, first inspect its *actual* sheets and columns. Do NOT assume a schema and do NOT immediately generate new `@Test` methods.

Normalize each approved test into:

- Unique Test Case ID; stable user-readable Title; Feature/Epic; Priority and Severity.
- Linked requirement/user story; preconditions; dependency/fixtures; test data references.
- Numbered steps with action and expected result per step.
- Actual observed result (ONLY from runtime execution, never prefilled as PASS).
- Positive/Negative/Security type and happy-path ordering.
- Platform/build environment; screenshots/video redaction; failure issue evidence.

## Required review gates

1. Compare Excel cases against already implemented methods, flag duplicates and outdated tests.
2. Ask for clarification for ambiguous business rules/expected outcomes.
3. Start with happy path and validate actual product outcome.
4. Implement remaining approved test cases incrementally.
5. Only then build feature TestNG suite and perform full sequential run.

`ExcelReader` from source exists, but before reading user XLSX enhance it with safe workbook closure, cell-type handling, null checking and deterministic data provider mapping.
