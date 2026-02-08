# AGENTS.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

EventR is a full-stack corporate event management platform with a modular architecture:
- **Backend**: Spring Boot 3.5.9 + Kotlin 2.3.0 (port 8080)
- **Frontend**: React 18 + TypeScript (port 3002)
- **Infrastructure**: PostgreSQL, Redis, LocalStack (S3), MailHog

## Essential Commands

### Development Environment

```bash
# Start infrastructure (required first)
docker-compose up -d

# Start all services (recommended)
./start-dev.sh

# Or start individually:
./mvnw spring-boot:run -Pbackend     # Backend only
cd frontend && npm start              # Frontend only (runs on port 3002)
```

### Testing

```bash
# Backend tests
./mvnw test                           # All tests
./mvnw test -Dtest=EventServiceTest   # Single test class
./mvnw test -Ptest                    # With test profile (Testcontainers)
./mvnw test jacoco:report             # With coverage report

# Frontend tests
cd frontend
npm test                              # Interactive mode
npm test -- --coverage                # With coverage
npm test -- ExampleComponent.test.tsx # Single test file
npm test -- --ci --watchAll=false     # CI mode
```

### Build

```bash
./mvnw clean package -DskipTests      # Backend JAR
cd frontend && npm run build          # Frontend production build
docker build -t eventr:latest .       # Docker image
```

## Architecture

### Backend Package Structure (Modular Architecture)

```
com.eventr/
├── controller/         # 8 REST controllers
├── service/
│   ├── interfaces/     # Service interfaces
│   └── impl/           # 8 service implementations
├── repository/         # 7 JPA repositories (extends JpaRepository)
├── model/              # 9 JPA entities with UUID primary keys
├── dto/                # 14 data transfer objects
├── modules/            # Domain modules (DDD-style)
│   ├── checkin/        # Check-in bounded context
│   ├── event/          # Event management bounded context
│   │   ├── api/        # Public module API
│   │   ├── internal/   # Internal implementation
│   │   └── events/     # Domain events
│   ├── identity/       # User identity bounded context
│   ├── notification/   # Notification bounded context
│   └── registration/   # Registration bounded context
├── infrastructure/     # Infrastructure layer
│   ├── config/         # Configuration classes
│   ├── persistence/    # Database utilities
│   └── storage/        # File storage (S3)
├── shared/             # Shared kernel
│   ├── event/          # Domain event infrastructure
│   ├── exception/      # Custom exceptions
│   └── types/          # Shared value types
├── config/             # Spring configuration
└── util/               # Helper utilities
```

### Frontend Structure

```
frontend/src/
├── components/     # 10 reusable UI components
├── pages/          # 14 route-level pages
├── api/            # API client (proxies to localhost:8080)
├── hooks/          # Custom React hooks
├── services/       # Frontend services
└── utils/          # Helper functions
```

### Key Domain Model

```
Event (1) → (many) Session → (many) CheckIn
Event (1) → (many) Registration → (many) CheckIn
```

All entities use **UUID primary keys** and have `createdAt`/`updatedAt` audit fields.

## Critical Patterns

### Service Layer Pattern

Services should follow interface-implementation pattern. When modifying services:

```kotlin
// Create interface first
interface EventService {
    fun createEvent(dto: CreateEventDto): EventDto
}

// Then implementation
@Service
class EventServiceImpl(
    private val eventRepository: EventRepository,
    private val applicationEventPublisher: ApplicationEventPublisher
) : EventService {
    override fun createEvent(dto: CreateEventDto): EventDto {
        // Business logic
        applicationEventPublisher.publishEvent(EventCreatedEvent(event))
        return event.toDto()
    }
}
```

### Event-Driven Architecture

Domain events trigger emails and analytics. Use Spring's event publisher:

```kotlin
applicationEventPublisher.publishEvent(UserRegisteredEvent(registration))
applicationEventPublisher.publishEvent(UserCheckedInEvent(checkIn))
```

### Testing Pattern

```kotlin
@SpringBootTest
@ActiveProfiles("test")
class ServiceNameTest {
    // Tests use H2 in-memory database automatically
    // Use @MockkBean for external dependencies
}
```

## Infrastructure Dependencies

Services expect these ports from `docker-compose up -d`:
- **5432** - PostgreSQL
- **6379** - Redis  
- **4566** - LocalStack (S3 simulation)
- **1025** - MailHog SMTP
- **8025** - MailHog UI

## Maven Profiles

- `backend` - Backend only with dev profile
- `dev` - Backend + frontend concurrently  
- `test` - Testing with Testcontainers
- `frontend` - Frontend via Maven frontend plugin

## Architectural Patterns

- **Modular Architecture**: Domain logic organized into bounded contexts under `modules/`
- **Interface Segregation**: Services follow interface + impl pattern in `service/interfaces/` and `service/impl/`
- **Domain Events**: Cross-module communication via Spring events in `shared/event/`
- **Infrastructure Separation**: External concerns isolated in `infrastructure/`

## Key Files

- `src/main/kotlin/com/eventr/EventrApplication.kt` - Spring Boot entry point
- `frontend/src/App.tsx` - React application root
- `docs/api.md` - Complete API endpoint reference
- `docs/architecture.md` - System design diagrams

## Port Configuration

- Backend API: **8080**
- Frontend: **3002** (NOT 3001, configured in package.json)
- Frontend proxies `/api/*` to `localhost:8080`
