# Eventr - Event Management System

A Java Spring Boot and React application for managing events and registrations.

## System Requirements

- Java 21 (LTS)
- Maven 3.9.6+
- Node.js 18+
- npm 10+
- Docker and Docker Compose

## Project Structure

- `/src/main/java` - Java backend code
- `/src/main/resources` - Configuration files
- `/src/test` - Test files
- `/frontend` - React frontend application

## Development Setup

### 1. Configure Environment

The project is configured to use:
- Java 21 (LTS)
- Spring Boot 3.3.2
- Lombok 1.18.34
- PostgreSQL database (via Testcontainers or Docker Compose)
- AWS S3 for file storage (via Testcontainers or LocalStack)

### 2. Development Options

#### Option 1: Using Docker Compose (Recommended)

This option starts PostgreSQL, LocalStack (for AWS services), and MailHog (for email testing) in Docker containers:

```bash
# Start all services
docker-compose up -d

# Make the initialization script executable
chmod +x localstack-init/init-aws.sh

# Start the backend
./mvnw spring-boot:run -Pbackend

# In another terminal, start the frontend
cd frontend
npm start
```

#### Option 2: Using Testcontainers with start-dev.sh Script (Recommended)

This option uses Testcontainers to automatically manage PostgreSQL and LocalStack services, providing a consistent environment between development and testing:

For Unix/Mac users:
```bash
# Make the script executable
chmod +x start-dev.sh

# Start the development environment with Testcontainers
./start-dev.sh
```

For Windows users:
```cmd
# Start the development environment with Testcontainers
start-dev.bat
```

Benefits of using Testcontainers in development:
- No need to manually configure Docker Compose services
- Consistent environment between development and testing
- Automatic cleanup of containers when the application stops
- Isolated database and AWS services for each development session

#### Option 3: Using Maven Profiles

The project includes several Maven profiles for different development tasks:

```bash
# Run tests with Testcontainers
./mvnw test -Ptest

# Start the backend server
./mvnw spring-boot:run -Pbackend

# Start the frontend server
./mvnw -Pfrontend

# Start both backend and frontend
./mvnw spring-boot:run -Pdev
```

### 3. Accessing Services

- Backend API: http://localhost:8080/api
- Frontend: http://localhost:3000
- H2 Console (dev mode): http://localhost:8080/h2-console
- MailHog UI: http://localhost:8025
- LocalStack (AWS): http://localhost:4566

## Testing

### Running Tests

```bash
# Run all tests
./mvnw test

# Run tests with Testcontainers
./mvnw test -Ptest
```

### Test Environment

Tests use:
- Testcontainers for PostgreSQL database
- Testcontainers for LocalStack (AWS services)
- JUnit 5 and Spring Boot Test

## Building for Production

```bash
# Build the backend
./mvnw clean package -DskipTests

# Build the frontend
cd frontend
npm run build
```

## Troubleshooting

### Common Issues

1. **AWS Configuration Error**: If you see errors related to AWS configuration, check:
   - The region is set correctly in application.properties
   - LocalStack is running (for development)
   - AWS credentials are configured properly

2. **Database Connection Issues**: Ensure:
   - PostgreSQL is running (or use H2 for development)
   - Database credentials are correct in application.properties

3. **Test Failures**: If tests fail:
   - Check that Docker is running (required for Testcontainers)
   - Ensure test configuration is correct
# eventr
