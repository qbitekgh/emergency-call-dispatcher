# Emergency Call Dispatcher

[![CI](https://github.com/qbitekgh/emergency-call-dispatcher/actions/workflows/ci.yml/badge.svg)](https://github.com/qbitekgh/emergency-call-dispatcher/actions/workflows/ci.yml)

A simplified emergency call queue built with Java 21 and Spring Boot.
Incoming calls get a priority and wait in a queue; available dispatchers get calls assigned automatically;
calls end up closed by the dispatcher or abandoned by the caller.

## Features

- **Automatic dispatching**: a new call goes straight to a free dispatcher. A dispatcher who closes a call
  or comes online immediately gets the next call from the queue.
- **Configurable queue order**: `priority` (most urgent first, then oldest) or `fifo` (oldest first),
  selected with the `dispatch.strategy` property.
- **Fair dispatcher choice**: the dispatcher who has been available the longest gets the next call.
- **Status rules enforced by the domain model**: an illegal transition (e.g. closing a call that is still queued)
  returns `409 Conflict`.
- **Statistics**: number of calls per status and priority, and average waiting time, computed in SQL.
- **Consistent errors**: every error is an RFC 9457 `ProblemDetail` JSON.

## Tech stack

| Area | Technologies |
|---|---|
| Language | Java 21 (records, sealed interfaces, pattern matching for `switch`, virtual threads) |
| Framework | Spring Boot 4: Spring Web MVC, Spring Data JPA (Hibernate), Spring JDBC (`JdbcTemplate`), Bean Validation, Actuator (health only) |
| Database | PostgreSQL 17, schema managed by Flyway |
| Build | Maven (wrapper included), Lombok |
| Tests | JUnit 5, AssertJ, Mockito, `@WebMvcTest`, `@SpringBootTest` with Testcontainers |
| Delivery | Docker (multi-stage build), Docker Compose, GitHub Actions |

## Running

Requirements: JDK 21 and Docker. Maven does not need to be installed; use the wrapper (`./mvnw` or `.\mvnw.cmd` on Windows).

**Everything in containers** (application + PostgreSQL):

```bash
docker compose up --build
```

**Application from the IDE or the command line**, database in a container:

```bash
docker compose up -d postgres
./mvnw spring-boot:run
```

The API is available at `http://localhost:8080`; `GET /actuator/health` reports whether the application is up.

**Tests** (unit and integration; Docker must be running for Testcontainers):

```bash
./mvnw verify
```

## API

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/calls` | Receive a call → `201 Created` with a `Location` header |
| `GET` | `/api/calls?status=&page=&size=` | Paged list, newest first, optional status filter |
| `GET` | `/api/calls/{id}` | Call details |
| `POST` | `/api/calls/{id}/close` | Close an assigned call |
| `POST` | `/api/calls/{id}/abandon` | Mark a queued call as abandoned |
| `GET` | `/api/queue` | Queued calls in dispatch order, grouped by priority |
| `POST` | `/api/dispatchers` | Add a dispatcher (starts `OFFLINE`) |
| `GET` | `/api/dispatchers` | List dispatchers |
| `PUT` | `/api/dispatchers/{id}/status` | Set status to `AVAILABLE` or `OFFLINE` |
| `GET` | `/api/stats` | Counts per status and priority, average waiting time |

Call statuses: `QUEUED → ASSIGNED → CLOSED` and `QUEUED → ABANDONED`.
Dispatcher statuses: `OFFLINE → AVAILABLE ⇄ BUSY`, `AVAILABLE → OFFLINE`; `BUSY` is set only by assigning a call.

### Example session

> On Windows PowerShell use `curl.exe` instead of `curl` (or run the commands in Git Bash).

Add a dispatcher and make them available:

```bash
curl -X POST localhost:8080/api/dispatchers -H "Content-Type: application/json" -d '{"name": "Anna"}'
curl -X PUT localhost:8080/api/dispatchers/1/status -H "Content-Type: application/json" -d '{"status": "AVAILABLE"}'
```

Receive a call: it is assigned to Anna at once.

```bash
curl -X POST localhost:8080/api/calls -H "Content-Type: application/json" \
  -d '{"callerNumber": "+48123456789", "type": "FIRE", "priority": "CRITICAL", "location": "Main St 1", "description": "Kitchen on fire"}'
```

```json
{
  "id": 1,
  "callerNumber": "+48123456789",
  "type": "FIRE",
  "priority": "CRITICAL",
  "location": "Main St 1",
  "description": "Kitchen on fire",
  "status": "ASSIGNED",
  "receivedAt": "2026-01-01T10:00:00Z",
  "assignedAt": "2026-01-01T10:00:00Z",
  "closedAt": null,
  "assignedDispatcherId": 1
}
```

A second call waits in the queue, because Anna is busy:

```bash
curl -X POST localhost:8080/api/calls -H "Content-Type: application/json" \
  -d '{"callerNumber": "48987654321", "type": "MEDICAL", "priority": "HIGH", "location": "Oak Ave 5"}'
curl localhost:8080/api/queue
```

```json
{
  "size": 1,
  "byPriority": {
    "HIGH": [
      {
        "id": 2,
        "callerNumber": "48987654321",
        "type": "MEDICAL",
        "priority": "HIGH",
        "location": "Oak Ave 5",
        "description": null,
        "status": "QUEUED",
        "receivedAt": "2026-01-01T10:00:20Z",
        "assignedAt": null,
        "closedAt": null,
        "assignedDispatcherId": null
      }
    ]
  }
}
```

Closing the first call frees Anna, who gets call 2 automatically:

```bash
curl -X POST localhost:8080/api/calls/1/close
curl localhost:8080/api/stats
```

An invalid request returns a `ProblemDetail`:

```bash
curl -X POST localhost:8080/api/calls -H "Content-Type: application/json" -d '{"callerNumber": "12ab"}'
```

```json
{
  "title": "Validation failed",
  "status": 400,
  "detail": "Request body contains invalid fields",
  "errors": {
    "callerNumber": "must be 9-15 digits, optionally starting with +",
    "location": "must not be blank",
    "priority": "must not be null",
    "type": "must not be null"
  }
}
```

## Design decisions

- **Rich domain model.** Status rules live in the entities and enums (`EmergencyCall.assignTo/close/abandon`,
  `CallStatus.canTransitionTo`), not in services. Services only load data, call domain methods and coordinate.
  Entities have Lombok `@Getter` only: no setters, so state can change only through methods that check the rules.
- **Strategy pattern for the queue.** `QueueOrderingStrategy` returns a `Comparator`; `PriorityThenFifoStrategy`
  and `FifoStrategy` implement it, and `DispatchConfig` picks one based on `dispatch.strategy`.
  Adding a new ordering does not change the dispatching code.
- **Observer pattern with Spring events.** Services publish `CallEvent`s (`CallReceived`, `CallAssigned`,
  `CallClosed`, `CallAbandoned`), and listeners react without the services knowing about them.
  `CallEvent` is a sealed interface of records, so the logger's pattern-matching `switch` needs no `default`
  and a new event type that is not handled is a compile error. The listener runs only after the transaction commits.
- **Concurrency.** Both entities have `@Version` (optimistic locking), so two requests can never overwrite each
  other's changes. The available dispatcher is selected with `SELECT ... FOR UPDATE` (pessimistic lock): when many
  calls arrive at once, transactions wait for each other instead of failing. An integration test sends 20 calls in
  parallel to 3 dispatchers and checks that every call is accepted and each dispatcher has exactly one.
- **JPA for entities, `JdbcTemplate` for reports.** Statistics are `GROUP BY` / `AVG` queries in plain SQL,
  so the database does the counting instead of loading every call into memory.
- **Stable API.** Controllers return records (`CallResponse`, `PageResponse<T>`, ...), never JPA entities.
- **Testability.** Time comes from an injected `java.time.Clock`; tests replace it to get exact timestamps.
- **Virtual threads** (`spring.threads.virtual.enabled=true`): each request runs on a lightweight Java 21 thread,
  which is released while it waits for the database.

## Tests

- **Unit tests** for the domain model, strategies, services (Mockito) and the event logger.
- **Web layer tests** (`@WebMvcTest`): HTTP status codes, JSON format, validation and error responses.
- **Integration tests** (`@SpringBootTest` + Testcontainers PostgreSQL): the full dispatching cycle through the API,
  queue order, statistics SQL and the concurrency scenario.

## Project structure

```
src/main/java/pl/kubit/emergency_call_dispatcher
├── call         calls: entity, statuses, service, controller, DTOs, events
├── dispatcher   dispatchers: entity, statuses, service, controller, DTOs
├── dispatch     pairing calls with dispatchers, queue strategies, /api/queue
├── stats        statistics with JdbcTemplate, /api/stats
└── common       error handling, shared DTOs, configuration
```
