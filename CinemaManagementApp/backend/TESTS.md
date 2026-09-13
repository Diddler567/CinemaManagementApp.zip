# Test Documentation (JUnit / Spring Boot)

## Environment
- Spring Boot integration tests with MockMvc
- In-memory H2 DB (Spring test context)
- Seeded admin user created by DataLoader:
  - username: admin
  - password: admin123

## How to run
mvn test

---

## Test Suites

### 1) RegistrationAndLoginTest
- Registration creates inactive account
- Inactive users cannot login
- Admin can activate user
- Three consecutive failed logins deactivate user

### 2) TokenValidationErrorsTest
- TOKEN_MISSING (no Authorization header)
- TOKEN_INVALID (unknown token)
- TOKEN_EXPIRED (expired token is invalidated)

### 3) TokenOwnershipSecurityTest
- Token used to access other user (non-admin) => both accounts deactivated
- (optional) asserts TOKEN_NOT_OWNER code in JSON

### 4) PasswordChangeSecurityTest
- Three consecutive failed password-change attempts deactivate user
- Token invalidated on every password-change attempt (success or failure)

### 5) ScreeningSearchSortTest
- Words matching: all words must appear in field (case-insensitive)
- Sorting: default genre->title, timetable view start_time
- Works for VISITOR on ANNOUNCED programs (public-ish GET)

### 6) ProgramStateTransitionTest
- Preconditions enforced on program transitions:
  - ASSIGNMENT->REVIEW requires handler assigned to SUBMITTED screenings
  - REVIEW->SCHEDULING requires no pending SUBMITTED screenings
  - SCHEDULING->FINAL_PUBLICATION requires no REVIEWED screenings (submitter must approve/reject first)
- Also verifies FINAL_SUBMISSION alias mapping works (FINAL_SUBMISSION -> FINAL_PUBLICATION)

### 7) AuditTrailTest
- Significant actions create audit_events rows
- Context (method/path) is present

### 8) RegistrationRateLimitTest
- /auth/register is rate limited (3/min); 4th returns 429 with Retry-After

---

## Notes
- Tests are integration-style (MockMvc + Spring context) because the project is REST + JPA.
- If you want more “pure unit tests” later, you can add service-level tests with mocked repositories,
  but these ITs are the best match for verifying the PDF requirements end-to-end.
