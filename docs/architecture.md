# Architecture Documentation

## System Overview

Eventr is a full-stack event management platform built with modern architectural principles including SOLID design patterns, event-driven architecture, and microservice-ready patterns.

## High-Level Architecture

```mermaid
graph TB
    subgraph "Client Layer"
        WEB[Web Frontend<br/>React + TypeScript]
        MOB[Mobile App<br/>Future: React Native]
        API_CLIENTS[API Clients<br/>External Integrations]
    end
    
    subgraph "Gateway Layer"
        LB[Load Balancer<br/>nginx/ALB]
        API_GW[API Gateway<br/>Spring Boot]
    end
    
    subgraph "Application Layer"
        EVENT_SVC[Event Service]
        REG_SVC[Registration Service]
        CHECKIN_SVC[Check-in Service]
        WEBHOOK_SVC[Webhook Service]
        ANALYTICS_SVC[Analytics Service]
        FILE_SVC[File Service]
    end
    
    subgraph "Infrastructure Layer"
        DB[(PostgreSQL<br/>Primary Database)]
        REDIS[(Redis<br/>Caching)]
        S3[AWS S3<br/>File Storage]
        EMAIL[Email Service<br/>SMTP]
        QUEUE[Message Queue<br/>Future: RabbitMQ]
    end
    
    subgraph "External Systems"
        WEBHOOKS[Webhook<br/>Endpoints]
        INTEGRATIONS[Third-party<br/>Services]
    end
    
    WEB --> LB
    MOB --> LB
    API_CLIENTS --> LB
    
    LB --> API_GW
    
    API_GW --> EVENT_SVC
    API_GW --> REG_SVC
    API_GW --> CHECKIN_SVC
    API_GW --> WEBHOOK_SVC
    API_GW --> ANALYTICS_SVC
    API_GW --> FILE_SVC
    
    EVENT_SVC --> DB
    REG_SVC --> DB
    CHECKIN_SVC --> DB
    WEBHOOK_SVC --> DB
    ANALYTICS_SVC --> DB
    FILE_SVC --> S3
    
    EVENT_SVC --> REDIS
    REG_SVC --> REDIS
    
    API_GW --> EMAIL
    WEBHOOK_SVC --> WEBHOOKS
    API_GW --> INTEGRATIONS
    
    classDef client fill:#e3f2fd
    classDef gateway fill:#f3e5f5
    classDef service fill:#e8f5e8
    classDef infra fill:#fff3e0
    classDef external fill:#fce4ec
    
    class WEB,MOB,API_CLIENTS client
    class LB,API_GW gateway
    class EVENT_SVC,REG_SVC,CHECKIN_SVC,WEBHOOK_SVC,ANALYTICS_SVC,FILE_SVC service
    class DB,REDIS,S3,EMAIL,QUEUE infra
    class WEBHOOKS,INTEGRATIONS external
```

## Domain Model

```mermaid
erDiagram
    Event ||--o{ EventInstance : has
    Event {
        uuid id PK
        string name
        text description
        enum type
        enum category
        datetime start_date
        datetime end_date
        string location
        int max_attendees
        enum status
    }
    
    EventInstance ||--o{ Registration : "receives"
    EventInstance ||--o{ Session : contains
    EventInstance {
        uuid id PK
        uuid event_id FK
        datetime instance_date
        enum status
    }
    
    Registration ||--o{ CheckIn : "enables"
    Registration {
        uuid id PK
        uuid event_instance_id FK
        string user_email
        string user_name
        enum status
        datetime registered_at
        json additional_info
    }
    
    Session ||--o{ CheckIn : "tracks"
    Session {
        uuid id PK
        uuid event_id FK
        string title
        text description
        datetime start_time
        datetime end_time
        string location
        int max_attendees
    }
    
    CheckIn {
        uuid id PK
        uuid registration_id FK
        uuid session_id FK
        enum type
        enum method
        datetime checked_in_at
        string checked_in_by
        string location
    }
    
    Webhook ||--o{ WebhookDelivery : generates
    Webhook {
        uuid id PK
        string url
        string[] event_types
        string secret
        boolean active
        enum status
    }
    
    WebhookDelivery {
        uuid id PK
        uuid webhook_id FK
        string event_type
        json payload
        enum status
        int attempt_count
        datetime next_retry_at
    }
```

## Service Architecture (SOLID Principles)

### Single Responsibility Principle

Each service has a single, well-defined responsibility:

```mermaid
graph TB
    subgraph "Event Management Domain"
        EVENT_SVC[Event Service<br/>- Event CRUD<br/>- Event validation<br/>- Event lifecycle]
        SESSION_SVC[Session Service<br/>- Session management<br/>- Prerequisites<br/>- Capacity management]
    end
    
    subgraph "Registration Domain"
        REG_SVC[Registration Service<br/>- User registration<br/>- Waitlist management<br/>- Registration validation]
    end
    
    subgraph "Attendance Domain"
        CHECKIN_SVC[Check-in Service<br/>- Manual check-in<br/>- QR code check-in<br/>- Bulk operations]
        QR_SVC[QR Code Service<br/>- QR generation<br/>- QR validation<br/>- Signature verification]
    end
    
    subgraph "Integration Domain"
        WEBHOOK_SVC[Webhook Service<br/>- Webhook management<br/>- Event subscription]
        DELIVERY_SVC[Delivery Service<br/>- HTTP delivery<br/>- Retry logic<br/>- Failure handling]
        SIG_SVC[Signature Service<br/>- HMAC generation<br/>- Signature validation]
    end
    
    subgraph "Supporting Services"
        FILE_SVC[File Service<br/>- Upload handling<br/>- Storage management]
        ANALYTICS_SVC[Analytics Service<br/>- Statistics<br/>- Reporting]
        EMAIL_SVC[Email Service<br/>- Notifications<br/>- Templates]
    end
```

### Dependency Inversion Principle

Services depend on abstractions, not concretions:

```mermaid
graph TB
    subgraph "Interfaces"
        CHECKIN_INT[CheckInServiceInterface]
        WEBHOOK_INT[WebhookServiceInterface]
        HTTP_INT[HttpClientInterface]
        EMAIL_INT[EmailServiceInterface]
    end
    
    subgraph "Implementations"
        CHECKIN_IMPL[EventDrivenCheckInService]
        WEBHOOK_IMPL[WebhookServiceImpl]
        HTTP_IMPL[SpringHttpClient]
        EMAIL_IMPL[SmtpEmailService]
    end
    
    subgraph "Controllers"
        CHECKIN_CTRL[CheckInController]
        WEBHOOK_CTRL[WebhookController]
    end
    
    CHECKIN_CTRL --> CHECKIN_INT
    WEBHOOK_CTRL --> WEBHOOK_INT
    
    CHECKIN_INT <|.. CHECKIN_IMPL
    WEBHOOK_INT <|.. WEBHOOK_IMPL
    HTTP_INT <|.. HTTP_IMPL
    EMAIL_INT <|.. EMAIL_IMPL
    
    CHECKIN_IMPL --> HTTP_INT
    WEBHOOK_IMPL --> HTTP_INT
    CHECKIN_IMPL --> EMAIL_INT
```

## Event-Driven Architecture

### Domain Events Flow

```mermaid
sequenceDiagram
    participant User
    participant Controller
    participant Service
    participant EventPublisher
    participant WebhookService
    participant External
    
    User->>Controller: Register for Event
    Controller->>Service: processRegistration()
    Service->>Service: Business Logic
    Service->>Service: Save Registration
    Service->>EventPublisher: publish(UserRegisteredEvent)
    Service-->>Controller: RegistrationDto
    Controller-->>User: 201 Created
    
    EventPublisher->>WebhookService: handle(UserRegisteredEvent)
    WebhookService->>WebhookService: Find Active Webhooks
    WebhookService->>External: POST webhook payload
    
    alt Success
        External-->>WebhookService: 200 OK
        WebhookService->>WebhookService: Mark Success
    else Failure
        External-->>WebhookService: 5xx Error
        WebhookService->>WebhookService: Schedule Retry
    end
```

### Event Types and Handlers

```mermaid
graph TB
    subgraph "Domain Events"
        USER_REG[UserRegisteredEvent]
        USER_CANCEL[UserCancelledEvent]
        USER_CHECKIN[UserCheckedInEvent]
        EVENT_CREATE[EventCreatedEvent]
        EVENT_UPDATE[EventUpdatedEvent]
        SESSION_CREATE[SessionCreatedEvent]
    end
    
    subgraph "Event Handlers"
        WEBHOOK_HANDLER[WebhookEventHandler]
        EMAIL_HANDLER[EmailNotificationHandler]
        ANALYTICS_HANDLER[AnalyticsEventHandler]
        AUDIT_HANDLER[AuditLogHandler]
    end
    
    USER_REG --> WEBHOOK_HANDLER
    USER_REG --> EMAIL_HANDLER
    USER_REG --> ANALYTICS_HANDLER
    USER_REG --> AUDIT_HANDLER
    
    USER_CHECKIN --> WEBHOOK_HANDLER
    USER_CHECKIN --> ANALYTICS_HANDLER
    
    EVENT_CREATE --> WEBHOOK_HANDLER
    EVENT_CREATE --> EMAIL_HANDLER
    
    SESSION_CREATE --> WEBHOOK_HANDLER
```

## Data Layer Architecture

### Repository Pattern

```mermaid
graph TB
    subgraph "Repository Interfaces"
        EVENT_REPO_INT[EventRepository]
        REG_REPO_INT[RegistrationRepository]
        CHECKIN_REPO_INT[CheckInRepository]
        WEBHOOK_REPO_INT[WebhookRepository]
    end
    
    subgraph "JPA Implementations"
        EVENT_REPO[EventRepository<br/>extends JpaRepository]
        REG_REPO[RegistrationRepository<br/>extends JpaRepository]
        CHECKIN_REPO[CheckInRepository<br/>extends JpaRepository]
        WEBHOOK_REPO[WebhookRepository<br/>extends JpaRepository]
    end
    
    subgraph "Custom Queries"
        EVENT_CUSTOM[Custom Event Queries<br/>- findByCategory<br/>- findUpcoming<br/>- searchByName]
        CHECKIN_CUSTOM[Custom CheckIn Queries<br/>- statisticsByEvent<br/>- findRecentCheckIns<br/>- countByMethod]
    end
    
    EVENT_REPO_INT <|.. EVENT_REPO
    REG_REPO_INT <|.. REG_REPO
    CHECKIN_REPO_INT <|.. CHECKIN_REPO
    WEBHOOK_REPO_INT <|.. WEBHOOK_REPO
    
    EVENT_REPO --> EVENT_CUSTOM
    CHECKIN_REPO --> CHECKIN_CUSTOM
```

### Database Schema Design

```mermaid
graph TB
    subgraph "Core Entities"
        EVENTS[(events)]
        EVENT_INST[(event_instances)]
        SESSIONS[(sessions)]
        REGISTRATIONS[(registrations)]
        CHECKINS[(check_ins)]
    end
    
    subgraph "Integration Entities"
        WEBHOOKS[(webhooks)]
        WEBHOOK_DEL[(webhook_deliveries)]
        AUDIT_LOG[(audit_logs)]
    end
    
    subgraph "Configuration"
        SYSTEM_CONFIG[(system_config)]
        EMAIL_TEMPLATES[(email_templates)]
    end
    
    EVENTS --> EVENT_INST
    EVENT_INST --> REGISTRATIONS
    EVENT_INST --> SESSIONS
    REGISTRATIONS --> CHECKINS
    SESSIONS --> CHECKINS
    
    WEBHOOKS --> WEBHOOK_DEL
    
    EVENTS --> AUDIT_LOG
    REGISTRATIONS --> AUDIT_LOG
    CHECKINS --> AUDIT_LOG
```

## Security Architecture

### Authentication & Authorization Flow

```mermaid
sequenceDiagram
    participant Client
    participant API_Gateway
    participant Auth_Service
    participant Business_Service
    participant Database
    
    Client->>API_Gateway: Request with JWT
    API_Gateway->>Auth_Service: Validate Token
    
    alt Valid Token
        Auth_Service-->>API_Gateway: User Context
        API_Gateway->>Business_Service: Process Request
        Business_Service->>Database: Data Operation
        Database-->>Business_Service: Result
        Business_Service-->>API_Gateway: Response
        API_Gateway-->>Client: Success Response
    else Invalid Token
        Auth_Service-->>API_Gateway: Unauthorized
        API_Gateway-->>Client: 401 Unauthorized
    end
```

### Role-Based Access Control

```mermaid
graph TB
    subgraph "Roles"
        ADMIN[System Admin]
        EVENT_MANAGER[Event Manager]
        STAFF[Event Staff]
        USER[Regular User]
    end
    
    subgraph "Permissions"
        CREATE_EVENT[Create Events]
        MANAGE_EVENT[Manage Events]
        CHECKIN_USERS[Check-in Users]
        VIEW_ANALYTICS[View Analytics]
        MANAGE_WEBHOOKS[Manage Webhooks]
        REGISTER[Register for Events]
        VIEW_OWN[View Own Data]
    end
    
    ADMIN --> CREATE_EVENT
    ADMIN --> MANAGE_EVENT
    ADMIN --> CHECKIN_USERS
    ADMIN --> VIEW_ANALYTICS
    ADMIN --> MANAGE_WEBHOOKS
    
    EVENT_MANAGER --> CREATE_EVENT
    EVENT_MANAGER --> MANAGE_EVENT
    EVENT_MANAGER --> VIEW_ANALYTICS
    EVENT_MANAGER --> MANAGE_WEBHOOKS
    
    STAFF --> CHECKIN_USERS
    STAFF --> VIEW_ANALYTICS
    
    USER --> REGISTER
    USER --> VIEW_OWN
```

## API Design Patterns

### RESTful Resource Design

```mermaid
graph TB
    subgraph "Resource Hierarchy"
        EVENTS[/api/events]
        EVENT_ID[/api/events/{id}]
        EVENT_SESSIONS[/api/events/{id}/sessions]
        EVENT_REGISTRATIONS[/api/events/{id}/registrations]
        EVENT_ANALYTICS[/api/events/{id}/analytics]
        
        REGISTRATIONS[/api/registrations]
        REG_ID[/api/registrations/{id}]
        
        CHECKINS[/api/check-ins]
        CHECKIN_ID[/api/check-ins/{id}]
        
        WEBHOOKS[/api/webhooks]
        WEBHOOK_ID[/api/webhooks/{id}]
        WEBHOOK_TEST[/api/webhooks/{id}/test]
    end
    
    EVENTS --> EVENT_ID
    EVENT_ID --> EVENT_SESSIONS
    EVENT_ID --> EVENT_REGISTRATIONS
    EVENT_ID --> EVENT_ANALYTICS
    
    REGISTRATIONS --> REG_ID
    CHECKINS --> CHECKIN_ID
    WEBHOOKS --> WEBHOOK_ID
    WEBHOOK_ID --> WEBHOOK_TEST
```

### Error Handling Strategy

```mermaid
flowchart TD
    A[API Request] --> B{Validation}
    B -->|Pass| C[Business Logic]
    B -->|Fail| D[400 Bad Request]
    
    C --> E{Business Rules}
    E -->|Pass| F[Data Operation]
    E -->|Fail| G[409 Conflict]
    
    F --> H{Database Operation}
    H -->|Success| I[200/201 Response]
    H -->|Not Found| J[404 Not Found]
    H -->|Error| K[500 Internal Error]
    
    D --> L[Error Response JSON]
    G --> L
    J --> L
    K --> L
    
    L --> M[Structured Error Format:<br/>{timestamp, status, error,<br/> message, path, details}]
```

## Performance Considerations

### Caching Strategy

```mermaid
graph TB
    subgraph "Cache Layers"
        BROWSER[Browser Cache<br/>Static Assets]
        CDN[CDN Cache<br/>Images, Files]
        APP_CACHE[Application Cache<br/>Redis]
        DB_CACHE[Database Cache<br/>Query Cache]
    end
    
    subgraph "Cache Patterns"
        READ_THROUGH[Read-Through<br/>Event Details]
        WRITE_BEHIND[Write-Behind<br/>Analytics Data]
        CACHE_ASIDE[Cache-Aside<br/>User Sessions]
    end
    
    CLIENT --> BROWSER
    CLIENT --> CDN
    API --> APP_CACHE
    APP_CACHE --> DB_CACHE
    
    APP_CACHE --> READ_THROUGH
    APP_CACHE --> WRITE_BEHIND
    APP_CACHE --> CACHE_ASIDE
```

### Scalability Patterns

```mermaid
graph TB
    subgraph "Horizontal Scaling"
        LB[Load Balancer]
        API1[API Instance 1]
        API2[API Instance 2]
        API3[API Instance N]
    end
    
    subgraph "Database Scaling"
        DB_PRIMARY[(Primary DB<br/>Writes)]
        DB_REPLICA1[(Read Replica 1)]
        DB_REPLICA2[(Read Replica 2)]
    end
    
    subgraph "Service Separation"
        READ_SERVICE[Read Service<br/>Optimized for Queries]
        WRITE_SERVICE[Write Service<br/>Optimized for Writes]
        WEBHOOK_SERVICE[Webhook Service<br/>Async Processing]
    end
    
    LB --> API1
    LB --> API2
    LB --> API3
    
    API1 --> READ_SERVICE
    API1 --> WRITE_SERVICE
    API2 --> READ_SERVICE
    API2 --> WRITE_SERVICE
    
    READ_SERVICE --> DB_REPLICA1
    READ_SERVICE --> DB_REPLICA2
    WRITE_SERVICE --> DB_PRIMARY
    
    DB_PRIMARY --> DB_REPLICA1
    DB_PRIMARY --> DB_REPLICA2
    
    WRITE_SERVICE --> WEBHOOK_SERVICE
```

## Deployment Architecture

### Container Architecture

```mermaid
graph TB
    subgraph "Container Orchestration"
        K8S[Kubernetes Cluster]
        
        subgraph "Application Pods"
            API_POD[API Service Pod<br/>Spring Boot]
            FRONTEND_POD[Frontend Pod<br/>nginx + React]
            WEBHOOK_POD[Webhook Client Pod<br/>Node.js]
        end
        
        subgraph "Data Pods"
            DB_POD[PostgreSQL Pod<br/>with Persistence]
            REDIS_POD[Redis Pod<br/>Session Cache]
        end
        
        subgraph "Gateway"
            INGRESS[Ingress Controller<br/>nginx]
        end
    end
    
    INTERNET --> INGRESS
    INGRESS --> API_POD
    INGRESS --> FRONTEND_POD
    INGRESS --> WEBHOOK_POD
    
    API_POD --> DB_POD
    API_POD --> REDIS_POD
```

### CI/CD Pipeline

```mermaid
graph LR
    A[Developer<br/>Push] --> B[GitHub Actions]
    B --> C[Build & Test]
    C --> D{Tests Pass?}
    D -->|Yes| E[Build Docker<br/>Images]
    D -->|No| F[Notify Developer]
    E --> G[Push to Registry]
    G --> H{Branch?}
    H -->|main| I[Deploy to Prod]
    H -->|develop| J[Deploy to Staging]
    I --> K[Health Checks]
    J --> L[Integration Tests]
    K --> M[Success]
    L --> N[Ready for Prod]
```

## Testing Architecture

### Testing Pyramid

```mermaid
graph TB
    subgraph "Testing Levels"
        E2E[End-to-End Tests<br/>Cypress/Playwright<br/>🔺]
        INTEGRATION[Integration Tests<br/>Testcontainers<br/>🔶]
        UNIT[Unit Tests<br/>JUnit + Mockito<br/>🟦]
    end
    
    subgraph "Test Coverage"
        UNIT_COV[Unit: >90%<br/>Fast execution<br/>Isolated components]
        INT_COV[Integration: >80%<br/>Real dependencies<br/>API contracts]
        E2E_COV[E2E: Key flows<br/>User scenarios<br/>Cross-browser]
    end
    
    UNIT --> UNIT_COV
    INTEGRATION --> INT_COV
    E2E --> E2E_COV
```

### Test Data Strategy

```mermaid
graph TB
    subgraph "Test Environments"
        UNIT_ENV[Unit Test Environment<br/>H2 In-Memory DB<br/>Mock Services]
        INT_ENV[Integration Environment<br/>Testcontainers<br/>Real Services]
        E2E_ENV[E2E Environment<br/>Full Stack<br/>Test Data Sets]
    end
    
    subgraph "Data Management"
        FIXTURES[Test Fixtures<br/>Builder Pattern<br/>Factory Classes]
        CLEANUP[Test Cleanup<br/>@Transactional<br/>@DirtiesContext]
        ISOLATION[Test Isolation<br/>Independent Tests<br/>Random UUIDs]
    end
    
    UNIT_ENV --> FIXTURES
    INT_ENV --> CLEANUP
    E2E_ENV --> ISOLATION
```

## Monitoring and Observability

### Application Monitoring

```mermaid
graph TB
    subgraph "Metrics Collection"
        SPRING_METRICS[Spring Actuator<br/>Health, Metrics]
        CUSTOM_METRICS[Custom Metrics<br/>Business KPIs]
        INFRA_METRICS[Infrastructure<br/>CPU, Memory, Disk]
    end
    
    subgraph "Monitoring Stack"
        PROMETHEUS[Prometheus<br/>Metrics Storage]
        GRAFANA[Grafana<br/>Visualization]
        ALERTS[AlertManager<br/>Notifications]
    end
    
    subgraph "Logging"
        APP_LOGS[Application Logs<br/>Structured JSON]
        ACCESS_LOGS[Access Logs<br/>Request Tracking]
        ERROR_LOGS[Error Logs<br/>Exception Details]
    end
    
    SPRING_METRICS --> PROMETHEUS
    CUSTOM_METRICS --> PROMETHEUS
    INFRA_METRICS --> PROMETHEUS
    
    PROMETHEUS --> GRAFANA
    PROMETHEUS --> ALERTS
    
    APP_LOGS --> ELK_STACK
    ACCESS_LOGS --> ELK_STACK
    ERROR_LOGS --> ELK_STACK
```

### Health Check Strategy

```mermaid
graph TB
    subgraph "Health Checks"
        LIVENESS[Liveness Probe<br/>Application Running]
        READINESS[Readiness Probe<br/>Ready for Traffic]
        STARTUP[Startup Probe<br/>Initialization Complete]
    end
    
    subgraph "Dependencies"
        DB_HEALTH[Database Health<br/>Connection Pool]
        EXTERNAL_HEALTH[External Services<br/>S3, Email SMTP]
        CACHE_HEALTH[Cache Health<br/>Redis Connection]
    end
    
    LIVENESS --> DB_HEALTH
    READINESS --> DB_HEALTH
    READINESS --> EXTERNAL_HEALTH
    READINESS --> CACHE_HEALTH
    STARTUP --> DB_HEALTH
```

This architecture documentation provides a comprehensive view of Eventr's system design, following modern software engineering practices and architectural patterns. The system is designed for scalability, maintainability, and testability while providing robust event management capabilities with integrated webhook support.