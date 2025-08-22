# Contributing to Eventr

🎉 Thank you for considering contributing to Eventr! We welcome contributions from everyone and are grateful for every pull request! 

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [How Can I Contribute?](#how-can-i-contribute)
- [Development Setup](#development-setup)
- [Pull Request Process](#pull-request-process)
- [Style Guides](#style-guides)
- [Testing](#testing)
- [Community](#community)

## Code of Conduct

This project and everyone participating in it is governed by the [Eventr Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code. Please report unacceptable behavior to [your-email@example.com].

## Getting Started

### 🐛 Reporting Bugs

Before creating bug reports, please check [existing issues](https://github.com/YOUR_USERNAME/eventr/issues) as you might find out that you don't need to create one. When you are creating a bug report, please include as many details as possible:

- **Use a clear and descriptive title**
- **Describe the exact steps to reproduce the problem**
- **Provide specific examples to demonstrate the steps**
- **Describe the behavior you observed and what behavior you expected to see**
- **Include screenshots if applicable**
- **Include environment details** (OS, browser, Java version, Node version)

### 💡 Suggesting Enhancements

Enhancement suggestions are tracked as GitHub issues. When creating an enhancement suggestion, please include:

- **Use a clear and descriptive title**
- **Provide a detailed description of the suggested enhancement**
- **Provide specific examples to demonstrate how the enhancement would be used**
- **Explain why this enhancement would be useful**
- **List some other applications where this enhancement exists, if applicable**

### 🚀 Your First Code Contribution

Unsure where to begin? You can start by looking through these issue labels:

- `good-first-issue` - Issues that should only require a few lines of code
- `help-wanted` - Issues that are a bit more involved than beginner issues
- `documentation` - Improvements or additions to documentation

## How Can I Contribute?

### 🔧 Areas We Need Help

- **Frontend Development** (React, TypeScript)
- **Backend Development** (Spring Boot, Kotlin)
- **UI/UX Design** (Bootstrap, CSS)
- **Documentation** (README, API docs, tutorials)
- **Testing** (Unit tests, integration tests)
- **DevOps** (CI/CD, Docker, deployment)
- **Accessibility** (WCAG compliance, screen readers)
- **Internationalization** (i18n support)

### 🎯 Current Priority Areas

1. **Mobile Responsiveness** - Improving mobile experience
2. **Performance Optimization** - Database queries and frontend bundling
3. **Accessibility** - Making the app accessible to all users
4. **API Documentation** - OpenAPI/Swagger improvements
5. **Test Coverage** - Increasing test coverage

## Development Setup

### Prerequisites

- Java 21 (LTS)
- Maven 3.9.6+
- Node.js 18+
- npm 10+
- Docker & Docker Compose

### 🛠️ Local Setup

```bash
# 1. Fork and clone the repository
git clone https://github.com/YOUR_USERNAME/eventr.git
cd eventr

# 2. Set up your development environment
cp .env.example .env  # Configure environment variables

# 3. Start development services
docker-compose up -d

# 4. Start the application
./start-dev.sh  # Unix/Mac
# OR
start-dev.bat   # Windows

# 5. Access the application
# Frontend: http://localhost:3001
# Backend: http://localhost:8080
```

### 🏗️ Project Structure

```
eventr/
├── src/main/kotlin/com/eventr/     # Backend code
│   ├── controller/                 # REST controllers
│   ├── service/                   # Business logic
│   ├── model/                     # Data models
│   ├── repository/                # Data access
│   └── config/                    # Configuration
├── frontend/src/                  # Frontend code
│   ├── components/               # React components
│   ├── pages/                   # Page components
│   ├── api/                     # API client
│   └── __tests__/              # Frontend tests
├── src/test/                     # Backend tests
└── docs/                        # Documentation
```

## Pull Request Process

### 1. 🌿 Create a Branch

```bash
# Create and switch to a new branch
git checkout -b feature/your-feature-name
# OR
git checkout -b fix/your-bug-fix
# OR
git checkout -b docs/your-documentation-update
```

### 2. 🔨 Make Your Changes

- Make your code changes
- Follow the style guides (see below)
- Add or update tests as necessary
- Update documentation if needed

### 3. ✅ Test Your Changes

```bash
# Backend tests
./mvnw test

# Frontend tests
cd frontend
npm test

# Integration tests
./mvnw test -Ptest

# Code coverage
cd frontend
npm test -- --coverage
```

### 4. 📝 Commit Your Changes

Follow the conventional commit format:

```bash
git commit -m "feat: add markdown support for event descriptions"
git commit -m "fix: resolve QR code scanning issue on mobile"
git commit -m "docs: update API documentation"
```

**Commit Types:**
- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation changes
- `style:` Code style changes (formatting, etc.)
- `refactor:` Code refactoring
- `test:` Adding or updating tests
- `chore:` Maintenance tasks

### 5. 🚀 Push and Create PR

```bash
# Push your branch
git push origin feature/your-feature-name

# Create a pull request on GitHub
# Use the PR template and fill out all sections
```

### 6. 🔄 PR Review Process

1. **Automated Checks**: CI/CD pipeline runs tests
2. **Code Review**: Maintainers review your code
3. **Address Feedback**: Make requested changes
4. **Final Approval**: PR gets approved and merged

## Style Guides

### 📝 Git Commit Messages

- Use the present tense ("Add feature" not "Added feature")
- Use the imperative mood ("Move cursor to..." not "Moves cursor to...")
- Limit the first line to 72 characters or less
- Reference issues and pull requests liberally after the first line

### 🎨 Frontend Code Style

```typescript
// Use TypeScript for new components
interface Props {
    title: string;
    onSubmit: (data: FormData) => void;
}

const EventCard: React.FC<Props> = ({ title, onSubmit }) => {
    // Use functional components with hooks
    const [loading, setLoading] = useState(false);
    
    // Use meaningful variable names
    const handleFormSubmit = useCallback((data: FormData) => {
        setLoading(true);
        onSubmit(data);
    }, [onSubmit]);

    return (
        <div className="card">
            <h3>{title}</h3>
            {/* Use semantic HTML */}
        </div>
    );
};
```

### ☕ Backend Code Style

```kotlin
// Use Kotlin idioms
@RestController
@RequestMapping("/api/events")
class EventController(
    private val eventService: EventService
) {
    
    @GetMapping
    fun getAllEvents(
        @RequestParam(required = false) city: String?,
        @RequestParam(required = false) category: String?
    ): ResponseEntity<List<EventDto>> {
        val events = eventService.findEvents(city, category)
        return ResponseEntity.ok(events)
    }
    
    // Use meaningful function names
    @PostMapping
    fun createEvent(@RequestBody @Valid eventDto: EventCreateDto): ResponseEntity<EventDto> {
        val createdEvent = eventService.createEvent(eventDto)
        return ResponseEntity.status(HttpStatus.CREATED).body(createdEvent)
    }
}
```

### 🏗️ General Guidelines

- **DRY**: Don't Repeat Yourself - extract common functionality
- **SOLID**: Follow SOLID principles
- **Clean Code**: Write self-documenting code with clear names
- **Error Handling**: Provide meaningful error messages
- **Security**: Follow security best practices
- **Performance**: Consider performance implications
- **Accessibility**: Make UI accessible (WCAG 2.1 AA)

## Testing

### 🧪 Testing Strategy

- **Unit Tests**: Test individual components/functions
- **Integration Tests**: Test component interactions
- **E2E Tests**: Test complete user workflows
- **Accessibility Tests**: Test with screen readers and keyboard navigation

### 📊 Coverage Requirements

- **Backend**: Maintain >80% code coverage
- **Frontend**: Maintain >70% code coverage
- **Critical Paths**: 100% coverage for payment, security, and data integrity paths

### 🔍 Testing Examples

```typescript
// Frontend test example
describe('EventCard Component', () => {
    it('should render event title correctly', () => {
        render(<EventCard title="Test Event" onSubmit={jest.fn()} />);
        expect(screen.getByText('Test Event')).toBeInTheDocument();
    });
    
    it('should handle form submission', async () => {
        const mockSubmit = jest.fn();
        render(<EventCard title="Test Event" onSubmit={mockSubmit} />);
        
        fireEvent.click(screen.getByRole('button', { name: /submit/i }));
        
        await waitFor(() => {
            expect(mockSubmit).toHaveBeenCalledTimes(1);
        });
    });
});
```

```kotlin
// Backend test example
@SpringBootTest
@Testcontainers
class EventServiceTest {
    
    @Test
    fun `should create event successfully`() {
        // Given
        val eventDto = EventCreateDto(
            name = "Test Event",
            description = "Test Description"
        )
        
        // When
        val result = eventService.createEvent(eventDto)
        
        // Then
        assertThat(result.name).isEqualTo("Test Event")
        assertThat(result.id).isNotNull()
    }
}
```

## Community

### 💬 Communication Channels

- **GitHub Issues**: For bugs and feature requests
- **GitHub Discussions**: For general questions and discussions
- **Discord**: [Join our Discord server](https://discord.gg/YOUR_INVITE) (coming soon)

### 🤝 Getting Help

- Check the [documentation](docs/)
- Search [existing issues](https://github.com/YOUR_USERNAME/eventr/issues)
- Ask in [GitHub Discussions](https://github.com/YOUR_USERNAME/eventr/discussions)
- Join our community chat

### 🏆 Recognition

Contributors who make significant contributions may be:

- Added to the `CONTRIBUTORS.md` file
- Mentioned in release notes
- Invited to join the core team
- Given maintainer privileges

---

## 📋 Checklist

Before submitting your PR, make sure:

- [ ] You've read the contributing guidelines
- [ ] Your code follows the style guides
- [ ] You've added/updated tests for your changes
- [ ] All tests pass locally
- [ ] You've updated documentation if necessary
- [ ] Your commit messages follow the conventional format
- [ ] You've filled out the PR template completely

Thank you for contributing to Eventr! 🎉