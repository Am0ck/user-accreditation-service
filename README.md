# Accreditation Service

A REST API for managing the lifecycle of investor accreditation verification. Users submit accreditation requests with supporting documents, and accreditations can later be confirmed, failed, or expired.

## Technology Stack

- **Java 17**
- **Spring Boot 3.5.x**
- **Maven** (Maven Wrapper included)
- **Lombok**
- **Spring Validation**
- **JUnit 5 + MockMvc** (testing)

---
## How to Run
### Prerequisites

- Java 17 installed and available on `PATH`
- A Unix-like shell (`bash`/`sh`)
- Internet access on first run so the Maven Wrapper can download dependencies
### Quick start (Linux)

```bash
chmod +x mvnw run.sh
./run.sh
```
This builds the JAR (skipping tests) and starts the service.

### Manual
```bash
./mvnw clean package -DskipTests
java -jar target/accreditation-service-0.0.1-SNAPSHOT.jar
```

### Run tests
```bash
./mvnw test
```

The service starts on **port 9999**: `http://localhost:9999`

---

## API Reference

### 1. Submit Accreditation Request

**POST** `/user/accreditation`

Creates a new accreditation request with `PENDING` status. Returns 409 if the user already has a `PENDING` request.

**Request:**
```json
{
  "user_id": "user-123",
  "accreditation_type": "BY_INCOME",
  "document": {
    "name": "tax_return_2024.pdf",
    "mime_type": "application/pdf",
    "content": "<base64-encoded-content>"
  }
}
```

**Response 201 Created:**
```json
{
  "accreditation_id": "87bb6030-458e-11ed-b023-039b275a916a"
}
```

**Error responses:**
- `400 Bad Request` — missing or invalid fields, or invalid `accreditation_type` value
- `409 Conflict` — user already has a PENDING accreditation

---

### 2. Update Accreditation Outcome (Admin)

**PUT** `/user/accreditation/{accreditationId}`

**Request:**
```json
{
  "outcome": "CONFIRMED"
}
```

**Response 200 OK:**
```json
{
  "accreditation_id": "87bb6030-458e-11ed-b023-039b275a916a"
}
```

**Valid `outcome` values:** `CONFIRMED`, `FAILED`, `EXPIRED`

**Valid status transitions:**

| From      | To        |
|-----------|-----------|
| PENDING   | CONFIRMED |
| PENDING   | FAILED    |
| CONFIRMED | EXPIRED   |

EXPIRED can be reached either through the update API or through the scheduled background expiry process.

Any other transition returns `400 Bad Request`. A `FAILED` accreditation cannot be updated further.

**Error responses:**
- `400 Bad Request` — invalid transition, missing field, or invalid `outcome` value
- `404 Not Found` — accreditation ID does not exist

---

### 3. Get Accreditations for a User

**GET** `/user/{userId}/accreditation`

**Response 200 OK:**
```json
{
  "user_id": "user-123",
  "accreditation_statuses": {
    "87bb6030-458e-11ed-b023-039b275a916a": {
      "accreditation_type": "BY_INCOME",
      "status": "CONFIRMED"
    }
  }
}
```

Returns an empty `accreditation_statuses` map if the user has no accreditations.

---

### Accreditation Types

`BY_INCOME` · `BY_NET_WORTH`

---

### Error Response Format

All error responses share a consistent structure with `status`, `error`, and `message` fields:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Accreditation not found with id: <uuid>"
}
```

Validation errors include a `fieldErrors` map:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "fieldErrors": {
    "userId": "must not be blank"
  }
}
```

Invalid enum values return the accepted options:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid value for field...."
}
```

Other malformed JSON returns a generic 400:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Malformed JSON request"
}
```

Unknown routes return 404, and using the wrong HTTP method returns 405:
```json
{
  "status": 405,
  "error": "Method Not Allowed",
  "message": "Request method 'GET' is not supported"
}
```

---

## Architecture Overview

The project uses a **layered architecture**:

- **Controller** — handles HTTP, validates input, delegates to the service layer. No business logic.
- **Service** — all business rules live here: status transitions, duplicate-pending enforcement, expiry logic, concurrency guard.
- **Repository** — data access abstracted behind an interface. `InMemoryAccreditationRepository` uses a `ConcurrentHashMap<UUID, Accreditation>` (primary store) and a `ConcurrentHashMap<String, Set<UUID>>` (secondary index by `userId`) for O(1) lookups. This abstraction reduces the impact of replacing the in-memory store with a database-backed repository.
- **Scheduler** — `AccreditationExpiryScheduler` fires hourly and calls the service to expire `CONFIRMED` accreditations whose `updatedAt` is older than 30 days.
- **Exception / GlobalExceptionHandler** — maps domain exceptions and Spring MVC errors to appropriate HTTP status codes (400, 404, 405, 409).
- **Model / DTOs** — entities are internal. DTOs with `@JsonProperty` annotations own the external JSON contract. No domain objects leak to the API layer.

**Key patterns applied:**
- Repository pattern (interface + in-memory impl)
- Fail-fast Bean Validation (`@NotBlank`, `@NotNull`, `@Valid`) at the controller boundary
- Per-user `synchronized` block in `AccreditationService.create()` to prevent duplicate PENDING records under concurrent requests
- `@Scheduled` job for background expiry (scheduler triggers only, logic stays in the service layer)

---

## Challenge Questions

### 1. How would you add an audit log of historical accreditation status updates?

By adding an `AuditEntry` entity (fields: `accreditationId`, `userId`, `fromStatus`, `toStatus`, `changedAt`, `changedBy (for example: admin,user,or system)`) and an `AuditRepository` backed by the same in-memory store (or a database table in production).

In `AccreditationService.updateStatus()` (and `expireStaleAccreditations()`), after saving the updated accreditation, write an `AuditEntry` to the audit repository. For production, this write should be transactional with the status update so the two are always consistent.

This would be a high-level extension to the current implementation rather than part of the existing solution.

### 2. How would you scale the GET endpoint if traffic grew significantly?

The first step would be to move persistence to a database and index user_id so user-based lookups remain efficient beyond the memory limits of one application instance. Since the service layer is already separated from the repository layer, this change would be mostly concentrated in the persistence implementation.

The next step would be horizontal scaling at the application layer. Since the API is stateless, multiple service instances could run behind a load balancer once the data is externalized from memory.

Because this endpoint is read-heavy and keyed by user, it would also be a strong candidate for caching. A cache entry could be stored per userId and invalidated whenever an accreditation is created or updated for that user.

If traffic grew beyond what indexed database reads plus caching could comfortably support, a dedicated read model could be introduced specifically for user accreditation lookups. That would keep the GET path fast and predictable without adding that complexity to the current take-home solution.

---

## Submission Contents

The intended submission includes:

```
accreditation-service/
├── src/                   # application and test source
├── .mvn/                  # Maven wrapper
├── mvnw / mvnw.cmd        # Maven wrapper scripts
├── pom.xml
├── run.sh                 # Linux build + run script
└── README.md
```

Build artifacts (`target/`), IDE files (`.idea/`), and JARs are excluded via `.gitignore`.

