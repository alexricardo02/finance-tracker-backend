---
name: audit
description: Skill de audit para este proyecto
---

# Security Audit (OWASP)

I’m going to ask you to audit a code snippet before it goes into production.

Specifically, check for:
1. Injection (SQL, Command, NoSQL, XSS).
2. Are external inputs (body/params/query) blindly trusted without validation?
3. Are stack traces or raw 500 errors exposed to the client?
4. Are there empty or silent catch blocks that hide errors?
5. Are there side effects or unnecessary mutable global state?

Required response format:
- Vulnerability report classified by risk (High/Medium/Low).
- Fixed and secure version of the code.
