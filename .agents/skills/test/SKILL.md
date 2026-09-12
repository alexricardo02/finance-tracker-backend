---
name: test
description: Skill de test para este proyecto
---

# Generate a test suite

Generate a complete set of unit tests for the specified code.

1. If the code is not testable (it mixes logic with I/O), first refactor it
   to separate the pure logic, and indicate that you did so.
2. Cover: success cases, boundary cases (nulls, empty values, out-of-range values),
   and expected error/exception cases.
3. Use mocks/stubs for repositories or external services (Mockito).
4. Name the tests descriptively
   (e.g., shouldThrowValidationException_whenUserAgeIsNegative).
