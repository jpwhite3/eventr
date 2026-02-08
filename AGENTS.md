# AGENTS.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

EventR is a full-stack corporate event management platform with:
- **Backend**: Spring Boot 3.4 + Kotlin (port 8080)
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
./mvnw test -Dtest=WebhookServiceTest # Single test class
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

### Backend Package Structure

```
com.eventr/
├── controller/     # 19 REST controllers
├── service/        # 20+ business services (prefer interface + impl pattern)
├── repository/     # 14 JPA repositories (extends JpaRepository)
├── model/          # 14 JPA entities with UUID primary keys
├── dto/            # 16 data transfer objects
├── config/         # Spring configuration
├── events/         # Domain events and handlers
├── exception/      # Custom exceptions
└── util/           # Helper utilities
```

### Frontend Structure

```
frontend/src/
├── components/     # 35 reusable UI components
├── pages/          # 28 route-level pages
├── api/            # API client (proxies to localhost:8080)
├── hooks/          # Custom React hooks
├── services/       # Frontend services
└── utils/          # Helper functions
```

### Key Domain Model

```
Event (1) → (many) Session → (many) CheckIn
Event (1) → (many) Registration → (many) CheckIn
Webhook (1) → (many) WebhookDelivery
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

Domain events trigger webhooks, emails, and analytics. Use Spring's event publisher:

```kotlin
applicationEventPublisher.publishEvent(UserRegisteredEvent(registration))
applicationEventPublisher.publishEvent(UserCheckedInEvent(checkIn))
```

### Testing Pattern

```kotlin
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
class ServiceNameTest {
    // TestConfig provides mocked AWS/external dependencies
    // Tests use H2 in-memory database automatically
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

## Known Anti-Patterns to Avoid

- **Large services**: `ResourceManagementService` (608 lines) should be split into smaller services
- **Missing interfaces**: Many services are concrete classes - always create interface for new services
- **Cross-domain dependencies**: Services should depend on their own domain repositories

## Key Files

- `src/main/kotlin/com/eventr/EventrApplication.kt` - Spring Boot entry point
- `frontend/src/App.tsx` - React application root
- `webhook-client/server.js` - Webhook test client (port 3002)
- `docs/api.md` - Complete API endpoint reference
- `docs/architecture.md` - System design diagrams

## Port Configuration

- Backend API: **8080**
- Frontend: **3002** (NOT 3001, configured in package.json)
- Webhook test client: **3002**
- Frontend proxies `/api/*` to `localhost:8080`
