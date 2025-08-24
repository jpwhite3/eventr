# Test Coverage Management

This project is configured with JaCoCo for test coverage reporting and enforcement with a **75% minimum threshold**.

## Current Coverage Status

- **Current Coverage**: ~6% (well below the 75% target)
- **Coverage Goal**: 75% instruction and branch coverage
- **Coverage Enforcement**: Configurable (disabled by default until coverage improves)

## Coverage Configuration

### Maven Properties
- `jacoco.check.skip`: Controls whether coverage thresholds are enforced
  - `true`: Coverage reports generated but no enforcement (default)
  - `false`: Build fails if coverage below 75%

### Exclusions
The following packages are excluded from coverage calculations:
- `**/dto/**` - Data Transfer Objects
- `**/model/**` - Entity/Model classes  
- `**/config/**` - Configuration classes
- `EventrApplication.class` - Main application class

## Running Coverage

### Generate Coverage Report Only
```bash
./mvnw clean test jacoco:report -Dspring.profiles.active=test
```

### Generate Coverage + Enforce Thresholds
```bash
./mvnw clean test -Djacoco.check.skip=false -Dspring.profiles.active=test
```

### View Coverage Report
After running tests, open: `target/site/jacoco/index.html`

## CI/CD Integration

### GitHub Actions
- Coverage reports are generated on every PR/push
- Coverage reports uploaded to Codecov
- **Coverage enforcement currently disabled** in CI until coverage improves
- To enable: Set `-Djacoco.check.skip=false` in CI workflow

### Enabling Coverage Enforcement

1. **Locally**: Add more tests until coverage reaches 75%
2. **Verify**: Run with `./mvnw clean test -Djacoco.check.skip=false`
3. **Enable in CI**: Change CI workflow to use `-Djacoco.check.skip=false`
4. **Update Default**: Change `jacoco.check.skip` to `false` in `pom.xml`

## Improving Coverage

### Priority Areas for Testing
1. **Service Layer** (currently 3% coverage)
   - `com.eventr.service` package
   - Business logic and core functionality
   
2. **Controller Layer** (currently 20% coverage)
   - `com.eventr.controller` package
   - API endpoints and request handling

### Recommended Test Types
- Unit tests for service methods
- Integration tests for API endpoints
- Repository tests for data access
- Mock-based tests for external dependencies

## Coverage Targets by Package
- **Controllers**: 80%+ (user-facing APIs)
- **Services**: 85%+ (business logic)
- **Repositories**: 70%+ (data access, some auto-generated queries)

## Files Modified
- `pom.xml`: Added JaCoCo plugin with 75% threshold
- `.github/workflows/ci-cd.yml`: Updated to generate coverage reports
- `application-test.properties`: Configured for H2 testing
- Various repositories: Fixed H2-compatible SQL queries