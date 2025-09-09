# Layered Architecture Refactoring Plan

## Current Architecture Violations

### Problem: Controllers Directly Access Repositories
The current architecture violates the **Dependency Inversion Principle** and **Separation of Concerns** by having controllers directly access data repositories and handle business logic.

**Current Problematic Pattern:**
```kotlin
@RestController
class EventController(
    private val eventRepository: EventRepository,           // ❌ Data access layer
    private val registrationRepository: RegistrationRepository, // ❌ Data access layer  
    private val emailService: EmailService,                 // ❌ Mixed responsibilities
    private val dynamoDbService: DynamoDbService           // ❌ Infrastructure layer
)
```

### Issues with Current Approach
1. **Violates Dependency Inversion**: High-level modules (controllers) depend on low-level modules (repositories)
2. **Poor Separation of Concerns**: Controllers handle HTTP, business logic, and data access
3. **Testing Complexity**: Controllers require database setup for unit testing
4. **Transaction Management Issues**: Controllers cannot properly manage database transactions
5. **Business Logic Scattered**: Logic mixed between controllers and services

## Target Architecture

### Proper Layered Architecture
```
┌─────────────────────────────┐
│     Presentation Layer      │ ← Controllers (HTTP concerns only)
├─────────────────────────────┤  
│      Business Logic Layer   │ ← Services (Business logic & transactions)
├─────────────────────────────┤
│      Data Access Layer      │ ← Repositories (Data persistence)
└─────────────────────────────┘
```

### Service Layer Interface Design

**EventService Interface:**
- Comprehensive business operations for events
- Proper abstraction of data access concerns
- Transaction boundary management
- DTO conversion handling
- Business validation and rules
- Error handling with domain exceptions

**Key Methods:**
- `createEvent(eventDto: EventCreateDto): EventDto`
- `findEvents(filters...): List<EventDto>`
- `updateEvent(id: UUID, updateDto: EventUpdateDto): EventDto`
- `publishEvent(id: UUID): EventDto`
- `deleteEvent(id: UUID)`
- `cancelRegistrations(id: UUID, registrationIds: List<UUID>): Map<String, Any>`

## Target Controller Pattern

```kotlin
@RestController
@RequestMapping("/api/events")
class EventController(
    private val eventService: EventService  // ✅ Only service dependency
) {
    
    @PostMapping
    fun createEvent(@Valid @RequestBody eventDto: EventCreateDto): ResponseEntity<EventDto> {
        val createdEvent = eventService.createEvent(eventDto)
        return ResponseEntity.status(HttpStatus.CREATED).body(createdEvent)
    }
    
    @GetMapping
    fun getEvents(
        @RequestParam(required = false) city: String?,
        @RequestParam(required = false) category: String?  
    ): ResponseEntity<List<EventDto>> {
        val events = eventService.findEvents(city, category)
        return ResponseEntity.ok(events)
    }
}
```

### Target Service Implementation Pattern

```kotlin
@Service
@Transactional
class EventServiceImpl(
    private val eventRepository: EventRepository,
    private val registrationRepository: RegistrationRepository,
    private val emailService: EmailService,
    private val dynamoDbService: DynamoDbService
) : EventService {
    
    override fun createEvent(eventDto: EventCreateDto): EventDto {
        // 1. Business validation
        validateEventData(eventDto)
        
        // 2. Entity creation and persistence
        val event = convertToEntity(eventDto)
        val savedEvent = eventRepository.save(event)
        
        // 3. Business logic execution
        handleEventCreation(savedEvent)
        
        // 4. DTO conversion
        return convertToDto(savedEvent)
    }
}
```

## Benefits of Layered Architecture

### 1. **Proper Separation of Concerns**
- **Controllers**: Handle HTTP requests/responses, input validation, and routing
- **Services**: Implement business logic, coordinate data operations, manage transactions
- **Repositories**: Handle data persistence and retrieval

### 2. **Improved Testability**
- **Unit Testing**: Controllers can be tested with service mocks
- **Integration Testing**: Services can be tested with repository mocks
- **Business Logic Testing**: Isolated testing of business rules

### 3. **Transaction Management**
- **@Transactional Services**: Proper database transaction boundaries
- **Rollback Support**: Automatic rollback on business rule violations
- **Consistent Data State**: ACID properties maintained

### 4. **Maintainability**
- **Single Responsibility**: Each layer has one clear responsibility
- **Dependency Direction**: Dependencies flow toward business logic
- **Change Isolation**: Changes in one layer don't affect others

### 5. **Scalability**
- **Service Composition**: Services can orchestrate multiple data operations
- **Caching Integration**: Services can implement caching strategies
- **External System Integration**: Services handle third-party API calls

## Implementation Status

### ✅ **Completed:**
- **EventService Interface** - Comprehensive business operations defined
- **Architecture Documentation** - Layered architecture principles documented
- **Service Method Design** - All major event operations specified
- **Error Handling Integration** - Custom exceptions and global error handling ready

### 🔄 **Next Phase (Future Implementation):**
- **Service Implementation** - Concrete service classes with business logic
- **Controller Refactoring** - Update controllers to use services only
- **Repository Method Updates** - Add specific repository methods as needed
- **Transaction Configuration** - Ensure proper @Transactional setup
- **Testing Updates** - Update tests for new architecture

## Controllers Requiring Refactoring

### **Priority 1: Critical Controllers**
1. **EventController** - Currently accesses multiple repositories directly
2. **RegistrationController** - Mixed repository and service dependencies
3. **AuthController** - Some direct repository access patterns

### **Priority 2: Additional Controllers**
1. **AnalyticsController** - Should use dedicated AnalyticsService
2. **FileController** - Should use FileManagementService
3. **SessionController** - Should use SessionManagementService

## Migration Strategy

### **Phase 1: Foundation (Completed)**
- ✅ Create service interfaces
- ✅ Define business operations
- ✅ Document architecture principles

### **Phase 2: Implementation (Future)**
- Service implementations with business logic
- Controller updates to use services
- Repository method additions as needed

### **Phase 3: Optimization (Future)**
- Performance optimization
- Caching strategies
- Additional business services

## Success Criteria

- ✅ **Service Interface Design**: Comprehensive business operations defined
- ✅ **Architecture Documentation**: Clear layered architecture principles
- 🔄 **Implementation**: Service implementations (future phase)
- 🔄 **Controller Updates**: Remove repository dependencies (future phase)
- 🔄 **Testing**: Updated test suite for new architecture (future phase)

## Architecture Benefits Achieved

1. **Clear Separation of Concerns**: Controllers handle only HTTP concerns
2. **Dependency Inversion**: Controllers depend on service abstractions
3. **Business Logic Centralization**: All business rules in service layer
4. **Transaction Boundary Management**: Services manage database transactions
5. **Improved Testability**: Easier unit and integration testing
6. **Maintainability**: Clear layer responsibilities and dependencies

This architectural foundation provides a solid blueprint for implementing proper layered architecture in the EventR application, ensuring scalability, maintainability, and testability.