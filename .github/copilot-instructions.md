# EventR - AI Development Guidelines

## Project Overview
EventR is a full-stack enterprise event management platform built with **Spring Boot 3 + Kotlin** backend and **React + TypeScript** frontend. The system implements SOLID principles, layered architecture, and event-driven patterns for corporate event lifecycle management (creation → registration → check-in → analytics).

## Essential Architecture Patterns

### Backend Structure (Kotlin + Spring Boot)
```
com.eventr/
├── controller/     # 19 REST controllers (@RestController)
├── service/        # 20 business services (some need interface extraction)  
├── repository/     # 14 JPA repositories (extends JpaRepository)
├── model/          # 14 JPA entities (@Entity with relationships)
├── dto/            # 16 data transfer objects (request/response)
├── config/         # Spring configuration (@Configuration)
└── events/         # Domain events & event handlers
```

**Critical Pattern**: Services should follow interface-implementation pattern but many don't yet. When modifying services, extract interfaces first (`ResourceManagementService` → `ResourceService` interface + `ResourceServiceImpl`).

### Service Layer Anti-Patterns to Avoid
- **Large services**: `ResourceManagementService` (608 lines) needs splitting into `ResourceService`, `ResourceBookingService`, `ResourceAnalyticsService`
- **Missing interfaces**: Most services are concrete classes - always create interface when adding new services
- **Cross-domain dependencies**: Services should depend on their own domain repos, not cross-cutting

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
// Domain events trigger webhooks, emails, analytics
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

### Webhook System Architecture
EventR has sophisticated webhook delivery system with retry logic:
```kotlin
// Webhook events automatically fire on domain events
@EventListener
fun handleUserRegistered(event: UserRegisteredEvent) {
    webhookDeliveryService.deliverWebhooksForEvent("USER_REGISTERED", event.payload)
}
```

**Webhook Development**: Use `webhook-client/` test client (port 3002) to test webhook deliveries locally.

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
- `webhook-client/server.js` - Webhook test client for local development

When modifying this codebase, prioritize maintaining the layered architecture, creating service interfaces, and following the established event-driven patterns for cross-cutting concerns.