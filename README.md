# 🎯 Eventr - Corporate Event Management System

[![CI/CD Pipeline](https://github.com/YOUR_USERNAME/eventr/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/YOUR_USERNAME/eventr/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.2-green.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.2.0-blue.svg)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.9.2-blue.svg)](https://www.typescriptlang.org/)

A comprehensive, full-stack event management platform designed for corporate environments. Built with Spring Boot, React, and TypeScript, Eventr provides seamless event creation, registration, and management capabilities with modern features like markdown support, multi-session events, and advanced analytics.

## ✨ Features

### 🎪 Event Management
- **Event Creation**: Rich event builder with image uploads and markdown-enabled descriptions
- **Multi-Session Support**: Create complex events with multiple sessions and prerequisites
- **Event Types**: Support for in-person, virtual, and hybrid events
- **Advanced Analytics**: Real-time attendance tracking and comprehensive reporting
- **Capacity Management**: Waitlist functionality and session capacity controls

### 👥 User Experience
- **Easy Registration**: Streamlined registration process with custom forms
- **QR Code Check-in**: Mobile-friendly check-in process
- **Bulk Operations**: Efficient bulk check-in and user management
- **Offline Support**: Offline check-in capabilities for events
- **Markdown Support**: Rich text formatting for event descriptions

### 🏗️ Technical Features
- **Responsive Design**: Mobile-first UI with Bootstrap 5
- **RESTful API**: Well-documented Spring Boot backend
- **File Storage**: AWS S3 integration for image and document storage
- **Email Integration**: Automated email notifications
- **Security**: Role-based access control and secure authentication

## 🚀 Quick Start

### Prerequisites

- **Java 21** (LTS)
- **Maven 3.9.6+**
- **Node.js 18+**
- **npm 10+**
- **Docker & Docker Compose**

### 🐳 Docker Setup (Recommended)

```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/eventr.git
cd eventr

# Start all services with Docker Compose
docker-compose up -d

# Initialize AWS services (LocalStack)
chmod +x localstack-init/init-aws.sh

# Start the backend
./mvnw spring-boot:run -Pbackend

# In another terminal, start the frontend
cd frontend
npm install
npm start
```

### 🛠️ Local Development Setup

```bash
# Option 1: Use development script (Unix/Mac)
chmod +x start-dev.sh
./start-dev.sh

# Option 2: Use development script (Windows)
start-dev.bat

# Option 3: Manual setup with Maven profiles
./mvnw spring-boot:run -Pdev  # Starts both backend and frontend
```

### 📱 Access the Application

- **Frontend**: http://localhost:3001
- **Backend API**: http://localhost:8080/api
- **API Documentation**: http://localhost:8080/swagger-ui.html
- **H2 Console** (dev): http://localhost:8080/h2-console
- **MailHog UI**: http://localhost:8025
- **LocalStack Dashboard**: http://localhost:4566

## 📚 Documentation

### 🏗️ Architecture

```
eventr/
├── src/main/kotlin/com/eventr/     # Backend (Spring Boot + Kotlin)
│   ├── controller/                 # REST API controllers
│   ├── service/                   # Business logic layer
│   ├── model/                     # Data models
│   ├── repository/                # Data access layer
│   └── config/                    # Configuration classes
├── frontend/                      # Frontend (React + TypeScript)
│   ├── src/components/           # Reusable UI components
│   ├── src/pages/               # Page components
│   ├── src/api/                 # API client
│   └── public/                  # Static assets
├── src/test/                     # Backend tests
└── docker-compose.yml           # Local development services
```

### 🔧 Key Technologies

**Backend:**
- Spring Boot 3.3.2
- Kotlin
- JPA/Hibernate
- PostgreSQL
- AWS S3 (via LocalStack for dev)
- Maven

**Frontend:**
- React 18.2.0
- TypeScript 5.9.2
- Bootstrap 5
- React Router
- Axios
- React Markdown

**Development:**
- Docker & Docker Compose
- Testcontainers
- LocalStack (AWS simulation)
- MailHog (email testing)

### 🎨 UI Components

The application includes several reusable components:

- **Event Builder**: Rich form with markdown preview
- **Advanced Analytics Dashboard**: Real-time charts and metrics
- **QR Scanner**: Mobile-optimized check-in interface
- **Form Builder**: Dynamic form creation for registrations
- **Session Builder**: Multi-session event configuration
- **Bulk Operations**: Batch user management tools

### 📊 API Endpoints

Key API endpoints include:

```
GET    /api/events              # List events
POST   /api/events              # Create event
GET    /api/events/{id}         # Get event details
PUT    /api/events/{id}         # Update event
DELETE /api/events/{id}         # Delete event

GET    /api/registrations       # List registrations
POST   /api/registrations       # Register for event

POST   /api/check-in/{id}       # Check in attendee
GET    /api/attendance/{id}     # Get attendance data

GET    /api/sessions            # List sessions
POST   /api/sessions            # Create session
```

## 🧪 Testing

```bash
# Run all tests
./mvnw test

# Run tests with Testcontainers
./mvnw test -Ptest

# Frontend tests
cd frontend
npm test

# Run with coverage
npm test -- --coverage
```

### Test Strategy

- **Backend**: JUnit 5 + Spring Boot Test + Testcontainers
- **Frontend**: Jest + React Testing Library
- **Integration**: Testcontainers for database and AWS services
- **E2E**: Planned Cypress integration

## 🏗️ Building for Production

```bash
# Build backend
./mvnw clean package -DskipTests

# Build frontend
cd frontend
npm run build

# Build Docker image
docker build -t eventr:latest .
```

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

### Quick Contribution Steps

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Run tests (`./mvnw test && cd frontend && npm test`)
5. Commit your changes (`git commit -m 'Add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

### Development Guidelines

- Follow existing code style and conventions
- Write tests for new features
- Update documentation as needed
- Use meaningful commit messages
- Keep PRs focused and atomic

## 📈 Roadmap

### Near Term (v1.1)
- [ ] Mobile app (React Native)
- [ ] Advanced reporting dashboard
- [ ] Calendar integrations
- [ ] SSO/LDAP authentication

### Future (v2.0)
- [ ] Multi-tenant architecture
- [ ] API rate limiting
- [ ] Advanced notification system
- [ ] Integration marketplace

## 🔐 Security

Security is a top priority. Please review our [Security Policy](SECURITY.md) for reporting vulnerabilities.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

- **Documentation**: Check our [docs](docs/)
- **Issues**: [GitHub Issues](https://github.com/YOUR_USERNAME/eventr/issues)
- **Discussions**: [GitHub Discussions](https://github.com/YOUR_USERNAME/eventr/discussions)

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- React community for the amazing ecosystem
- All contributors who make this project possible

---

<div align="center">
  <b>Built with ❤️ for the open source community</b>
  <br>
  <sub>Made by <a href="https://github.com/YOUR_USERNAME">YOUR_NAME</a> and <a href="https://github.com/YOUR_USERNAME/eventr/contributors">contributors</a></sub>
</div>