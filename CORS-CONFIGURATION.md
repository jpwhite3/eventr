# CORS Configuration Strategy

## Overview

This document outlines the comprehensive CORS (Cross-Origin Resource Sharing) configuration strategy for the Eventr application, designed to provide secure, flexible, and environment-appropriate cross-origin access.

## Strategy Benefits

✅ **No CORS Issues in Development**: Supports multiple localhost ports and IP addresses  
✅ **Production Ready**: Restrictive, secure defaults for production environments  
✅ **Environment Specific**: Different configurations per environment (dev, staging, prod)  
✅ **Centralized Management**: Single source of truth, no duplicate configurations  
✅ **Runtime Flexibility**: Environment variable overrides for deployment flexibility  
✅ **Maintainable**: Eliminated 16+ duplicate `@CrossOrigin` annotations  

## Configuration Hierarchy

The CORS configuration follows this priority order:

1. **Application Properties** (`cors.*` properties in `application-{profile}.properties`)
2. **Environment Variables** (`CORS_ALLOWED_ORIGINS`)
3. **Profile-based Fallbacks** (hardcoded defaults per environment)

## Environment Configurations

### Development (`application-dev.properties`)
```properties
cors.allowed-origins=http://localhost:3000,http://localhost:3001,http://localhost:3002,http://localhost:3003,http://127.0.0.1:3000,http://127.0.0.1:3001,http://127.0.0.1:3002,http://127.0.0.1:3003
cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS,PATCH
cors.allowed-headers=*
cors.allow-credentials=true
cors.max-age=3600
```

**Features:**
- Multiple localhost ports (3000-3003) for different dev server instances
- Both `localhost` and `127.0.0.1` support
- All headers allowed (`*`) for development flexibility
- Extended method support including `PATCH`
- 1-hour cache for preflight requests

### Testing (`application-test.properties`)
```properties
cors.allowed-origins=http://localhost:3000,http://localhost:8080
cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS,PATCH
cors.allowed-headers=*
cors.allow-credentials=true
cors.max-age=3600
```

**Features:**
- Limited to test-specific ports
- Same permissive headers as development
- Supports both frontend (3000) and backend (8080) testing

### Staging (`application-staging.properties`)
```properties
cors.allowed-origins=https://staging.yourdomain.com,https://admin-staging.yourdomain.com
cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS,PATCH
cors.allowed-headers=Content-Type,Authorization,X-Requested-With,Accept,Origin,Access-Control-Request-Method,Access-Control-Request-Headers,X-CSRF-Token
cors.allow-credentials=true
cors.max-age=1800
```

**Features:**
- HTTPS-only origins for staging domains
- Specific header whitelist (no wildcard)
- CSRF token support
- 30-minute preflight cache

### Production (`application-prod.properties`)
```properties
cors.allowed-origins=${CORS_ALLOWED_ORIGINS:https://yourdomain.com}
cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
cors.allowed-headers=Content-Type,Authorization,X-Requested-With,Accept,Origin,Access-Control-Request-Method,Access-Control-Request-Headers
cors.allow-credentials=true
cors.max-age=86400
```

**Features:**
- Environment variable override capability
- Most restrictive header policy
- No `PATCH` method (if not needed in production)
- 24-hour preflight cache for performance
- HTTPS-only origins

## Implementation Details

### Central Configuration Class

The `WebConfig` class (`src/main/kotlin/com/eventr/config/WebConfig.kt`) provides:

- **Properties Binding**: Uses `@ConfigurationProperties` for type-safe configuration
- **Environment Detection**: Automatic fallbacks based on active Spring profiles
- **Logging**: Comprehensive logging of CORS settings on startup
- **WebSocket Support**: Additional CORS mapping for WebSocket endpoints (`/ws/**`)
- **Flexibility**: Support for environment variable overrides

### Key Features

1. **Runtime Logging**: 
   ```
   INFO: Configuring CORS with origins: [http://localhost:3000, ...]
   INFO: Allowed methods: [GET, POST, PUT, DELETE, OPTIONS, PATCH]
   INFO: Allowed headers: [*]
   INFO: Allow credentials: true
   ```

2. **Security**: Unauthorized origins receive `403 Forbidden`
3. **Performance**: Appropriate `max-age` values per environment
4. **Maintenance**: No `@CrossOrigin` annotations on controllers

## Usage Examples

### Development Testing
```bash
# This works - allowed origin
curl -X OPTIONS -H "Origin: http://localhost:3002" \
  -H "Access-Control-Request-Method: POST" \
  http://localhost:8080/api/events

# This fails - unauthorized origin  
curl -X OPTIONS -H "Origin: http://malicious-site.com" \
  -H "Access-Control-Request-Method: POST" \
  http://localhost:8080/api/events
```

### Production Deployment
```bash
# Set allowed origins via environment variable
export CORS_ALLOWED_ORIGINS="https://myapp.com,https://admin.myapp.com"
java -jar eventr.jar --spring.profiles.active=prod
```

### Adding New Development Port
Simply add to `application-dev.properties`:
```properties
cors.allowed-origins=http://localhost:3000,http://localhost:3001,http://localhost:3002,http://localhost:3003,http://localhost:3004
```

## Security Considerations

### What's Protected
- ✅ Strict origin validation
- ✅ Method whitelisting per environment  
- ✅ Header restrictions in staging/production
- ✅ HTTPS enforcement in non-development environments
- ✅ No wildcard (`*`) origins with credentials

### Best Practices Implemented
- Environment-specific restrictions
- Principle of least privilege (most restrictive in production)
- Comprehensive logging for security monitoring
- No sensitive information in CORS headers

## Migration from Previous Configuration

**Before**: 16 controllers with duplicate `@CrossOrigin` annotations  
**After**: Centralized configuration with environment-specific properties

The migration:
1. ✅ Removed all `@CrossOrigin(origins = [...])` annotations from controllers
2. ✅ Centralized configuration in `WebConfig` class
3. ✅ Added environment-specific property files
4. ✅ Implemented fallback mechanisms
5. ✅ Added comprehensive logging

## Testing the Configuration

### Unix/Linux/macOS
```bash
# Start development server
./start-dev.sh

# Test allowed origin
curl -X OPTIONS -H "Origin: http://localhost:3002" \
  -H "Access-Control-Request-Method: POST" \
  -v http://localhost:8080/api/events
```

### Windows
```cmd
# Start development server
start-dev.bat

# Test allowed origin (if curl is available)
curl -X OPTIONS -H "Origin: http://localhost:3002" ^
  -H "Access-Control-Request-Method: POST" ^
  -v http://localhost:8080/api/events
```

### Expected Response Headers
```
- Access-Control-Allow-Origin: http://localhost:3002
- Access-Control-Allow-Methods: GET,POST,PUT,DELETE,OPTIONS,PATCH  
- Access-Control-Allow-Headers: Content-Type
- Access-Control-Allow-Credentials: true
- Access-Control-Max-Age: 3600
```

## Hot-Reload Support

Both `start-dev.sh` and `start-dev.bat` now include optimized hot-reload configuration:

- **Fast Refresh**: Instant component updates without losing state
- **Source Maps**: Full debugging support with original source files
- **Optimized Polling**: Efficient file watching for changes
- **Development Mode**: Additional React development tools enabled

This CORS configuration strategy ensures both development productivity and production security while maintaining flexibility for different deployment scenarios.