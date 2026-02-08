# EventR - AI Development Guidelines

## Project Overview
EventR is a full-stack enterprise event management platform built with **Spring Boot 3 + Kotlin** backend and **React + TypeScript** frontend. The system implements SOLID principles, layered architecture, and event-driven patterns for corporate event lifecycle management (creation → registration → check-in → analytics).

## Essential Architecture Patterns

### Backend Structure (Kotlin + Spring Boot 3.4) - Modular Architecture
```
com.eventr/
├── controller/         # 8 REST controllers (@RestController)
├── service/
│   ├── interfaces/     # Service interfaces
│   └── impl/           # 8 service implementations
├── repository/         # 7 JPA repositories (extends JpaRepository)
├── model/              # 9 JPA entities (@Entity with relationships)
├── dto/                # 14 data transfer objects (request/response)
├── modules/            # Domain modules (DDD-style)
│   ├── checkin/        # Check-in bounded context
│   ├── event/          # Event management (api/, internal/, events/)
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
├── config/             # Spring configuration (@Configuration)
└── util/               # Helper utilities
```

**Critical Pattern**: Services follow interface-implementation pattern in `service/interfaces/` and `service/impl/`. Domain logic is organized into bounded contexts under `modules/`.

### Architectural Patterns
- **Modular Architecture**: Domain logic organized into bounded contexts under `modules/`
- **Interface Segregation**: Services follow interface + impl pattern
- **Domain Events**: Cross-module communication via Spring events in `shared/event/`
- **Infrastructure Separation**: External concerns isolated in `infrastructure/`

### Database & Testing Architecture
- **Production**: PostgreSQL with JPA/Hibernate
- **Development**: PostgreSQL via Docker Compose
- **Testing**: H2 in-memory with `@SpringBootTest` + `@ActiveProfiles("test")`
- **Integration**: Testcontainers for real database testing

**Testing Pattern**:
```kotlin
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig::class)
class ServiceNameTest {
    // Use TestConfig for mocked AWS/external dependencies
}
```

## Development Workflow Commands

### Essential Development Scripts
```bash
# Full development environment (backend + frontend + infrastructure)
./start-dev.sh                    # Recommended - starts all services

# Individual services
./mvnw spring-boot:run -Pbackend   # Backend only (port 8080)
./mvnw spring-boot:run -Pdev       # Backend + frontend via Maven
cd frontend && npm start           # Frontend only (port 3002, NOT 3001)

# Testing
./mvnw test                        # Backend tests
./mvnw test -Ptest                 # With test profile
cd frontend && npm test            # Frontend tests
```

### Critical Infrastructure Dependencies
```bash
# Required before backend startup
docker-compose up -d               # PostgreSQL, Redis, LocalStack (AWS), MailHog

# Services expect these ports:
# 5432  - PostgreSQL
# 6379  - Redis  
# 4566  - LocalStack (S3 simulation)
# 1025  - MailHog SMTP
# 8025  - MailHog UI
```

### Maven Profile System
- `backend` - Backend only with dev profile
- `dev` - Backend + frontend concurrently  
- `test` - Testing with Testcontainers
- `frontend` - Frontend via Maven frontend plugin

## Project-Specific Conventions

### Kotlin Backend Patterns
```kotlin
// Service pattern (create interfaces!)
interface EventService {
    fun createEvent(dto: CreateEventDto): EventDto
}

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

// Repository pattern
interface EventRepository : JpaRepository<Event, UUID> {
    fun findByTypeAndStatus(type: EventType, status: EventStatus): List<Event>
    
    @Query("SELECT e FROM Event e WHERE e.startDate BETWEEN :start AND :end")
    fun findEventsInDateRange(start: LocalDateTime, end: LocalDateTime): List<Event>
}
```

### Event-Driven Architecture
The system uses domain events for cross-cutting concerns:
```kotlin
// Domain events trigger emails, analytics
applicationEventPublisher.publishEvent(UserRegisteredEvent(registration))
applicationEventPublisher.publishEvent(UserCheckedInEvent(checkIn))
```

### Frontend TypeScript Patterns
```typescript
// API client pattern (in src/api/)
export const eventAPI = {
    createEvent: (data: CreateEventDto): Promise<EventDto> => 
        apiClient.post('/api/events', data),
    getEvents: (): Promise<EventDto[]> => 
        apiClient.get('/api/events')
};

// Component structure
src/
├── components/     # Reusable UI components
├── pages/         # Route-level components  
├── api/           # API client functions
├── hooks/         # Custom React hooks
└── utils/         # Helper functions
```

## Critical Integration Points

### File Upload (S3 Integration)
- **Development**: LocalStack simulates S3 (port 4566)
- **Service**: `FileService` handles uploads with UUID naming
- **Frontend**: File upload components use presigned URLs

### Real-time Features (WebSocket)
- Check-in updates broadcast via WebSocket
- Analytics dashboards receive real-time updates
- Connection handling in `WebSocketConfig`

## Database Design Patterns

### Core Domain Model
```
Event (1) → (many) Registration → (many) CheckIn
Event (1) → (many) Session → (many) CheckIn
Resource (many) ← → (many) Session (booking system)
```

**UUID Primary Keys**: All entities use UUID, not auto-increment IDs.

**Audit Fields**: Most entities have `createdAt`, `updatedAt` fields.

### JPA Relationship Patterns
```kotlin
@Entity
class Event(
    @Id
    val id: UUID = UUID.randomUUID(),
    
    @OneToMany(mappedBy = "event", cascade = [CascadeType.ALL])
    val sessions: List<Session> = mutableListOf(),
    
    @OneToMany(mappedBy = "event")
    val registrations: List<Registration> = mutableListOf()
)
```

## Development Environment Debugging

### Backend Debugging Issues
- **Port conflicts**: Backend defaults to 8080, frontend to 3002 (NOT 3001)
- **Database connection**: Ensure `docker-compose up -d` runs before backend
- **Profile confusion**: Use `-Dspring.profiles.active=dev` for development

### Frontend Common Issues  
- **Proxy setup**: Frontend automatically proxies `/api/*` to `localhost:8080`
- **Port configuration**: Frontend runs on 3002 via `PORT=3002` in package.json
- **Build issues**: Clear `node_modules` and reinstall if module errors

### Testing Environment
- Tests use H2 in-memory database automatically
- `TestConfig.kt` provides mocked AWS services  
- Use `@ActiveProfiles("test")` for all test classes

## Key Files to Understand

**Architecture**: `docs/architecture.md` - Complete system design  
**Development Setup**: `docs/local-development.md` - Detailed environment setup  
**Service Analysis**: `docs/architecture/Package-Structure-Analysis.md` - Service refactoring guidance  
**API Reference**: `docs/api.md` - Complete endpoint documentation  

**Configuration**: 
- `src/main/resources/application-dev.yml` - Development configuration  
- `docker-compose.dev.yml` - Full development environment  
- `frontend/package.json` - Frontend build configuration with port 3002

**Entry Points**:
- `src/main/kotlin/com/eventr/EventrApplication.kt` - Spring Boot main  
- `frontend/src/App.tsx` - React application root

When modifying this codebase, prioritize maintaining the layered architecture, creating service interfaces, and following the established event-driven patterns for cross-cutting concerns.
