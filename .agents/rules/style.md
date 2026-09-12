# Basic Rules for Code Generation and Correction

Your goal is to generate, refactor, or correct code while ensuring the highest
quality, maintainability, and security. ALWAYS apply the following 7 principles in ALL your answers,
without exception:

## 1. Clean Code
Use explicit and descriptive names for variables, functions, and classes.
The code should be self-explanatory, without the need for comments to understand what it does.
Example: `isActiveUser(user)` instead of `c(u)`.

## 2. SRP (Single Responsibility Principle)
Each class, function, or module does ONE single thing. Always separate business logic,
persistence, and external communication into distinct components
(e.g., Controller → Service → Repository; never mix them).

## 3. DRY (Don’t Repeat Yourself)
Reuse common logic. If you see a calculation or validation repeated more than once,
extract it into a dedicated function or utility.

## 4. Open/Closed Principle (OCP)
Design for extension, not for modification. Use polymorphism, interfaces, or
design patterns. Avoid long chains of if-else or switch statements when adding
new behaviors (payment methods, notification types, statuses, etc.).

## 5. Testable Code (Pure Functions)
Separate pure business logic (no side effects, easy to test)
from I/O operations (databases, external APIs, async calls).

## 6. Input Validation and Sanitization (Zero Trust)
Do not trust any user input. ALWAYS validate and sanitize parameters and
payloads to prevent SQL injection, XSS, command injection, and RCE.
Never blindly trust the body, params, or query of a request.

## 7. Error Handling and Secure Logging
- Catch exceptions using appropriate try/catch blocks.
- NEVER expose stack traces or sensitive technical details to the client.
- Return generic and secure error responses (e.g., 400/500 with a clean message).
- Log detailed error information internally (in logs), never externally.
- Never leave catch blocks empty or silent.

## Mandatory delivery requirements for EVERY response
1. Descriptive names and self-explanatory code.
2. If the functionality involves persistence + logic + notifications,
   separate them into independent classes/modules.
3. Strict validation of all external input.
4. Centralized error handling, without exposing internal details.
5. Include definitions of necessary DTOs/interfaces.
6. Always include a brief example of a unit test for the new logic.
