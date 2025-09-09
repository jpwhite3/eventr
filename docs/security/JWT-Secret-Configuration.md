# JWT Secret Configuration Guide

**CRITICAL SECURITY**: This guide covers the secure configuration of JWT secrets for the Eventr application.

## Overview

JWT secrets are used to sign and verify authentication tokens. **Hardcoding JWT secrets in source code is a critical security vulnerability** that can lead to complete authentication bypass and system compromise.

## Security Requirements

1. **NEVER hardcode JWT secrets** in source code
2. **Use environment variables** or secure secret management systems
3. **Generate cryptographically secure secrets** (minimum 32 characters)
4. **Use different secrets** for different environments
5. **Rotate secrets regularly** in production

## Configuration Methods

### Development Environment

For development, the JWT secret is configured in `application-dev.properties`:

```properties
# Development-only JWT secret (base64 encoded)
app.jwt.secret=ZGV2ZWxvcG1lbnRTZWN1cmVKV1RTZWNyZXRLZXlGb3JFdmVudFJBcHBsaWNhdGlvbjIwMjQ=
app.jwt.expiration=86400000
```

**Important**: This development secret is only for local development and should NEVER be used in production.

### Production Environment

Production JWT secrets MUST be provided via environment variables:

```bash
# Set JWT secret via environment variable
export JWT_SECRET="$(openssl rand -base64 32)"
export JWT_EXPIRATION=86400000
```

### Environment Variable Configuration

#### Option 1: Environment Variables
```bash
# Generate secure secret
export JWT_SECRET="$(openssl rand -base64 32)"

# Start application
java -jar eventr.jar
```

#### Option 2: System Properties
```bash
# Via system properties
java -Dapp.jwt.secret="$(openssl rand -base64 32)" -jar eventr.jar
```

#### Option 3: External Configuration File
```yaml
# application-prod.yml (external configuration)
app:
  jwt:
    secret: ${JWT_SECRET}
    expiration: 86400000
```

## Generating Secure JWT Secrets

### Using OpenSSL (Recommended)
```bash
# Generate 32-byte base64 encoded secret
openssl rand -base64 32

# Example output:
# K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv72ol/pe/Unols=
```

### Using Node.js
```javascript
// Generate secure secret
const crypto = require('crypto');
console.log(crypto.randomBytes(32).toString('base64'));
```

### Using Python
```python
import secrets
import base64

# Generate secure secret
secret_bytes = secrets.token_bytes(32)
secret_base64 = base64.b64encode(secret_bytes).decode('utf-8')
print(secret_base64)
```

## Security Validation

The application validates JWT secret configuration at startup:

1. **Presence Check**: JWT secret must be configured
2. **Length Check**: Minimum 32 characters required
3. **Format Check**: Supports both base64 and plain string formats
4. **Startup Failure**: Application fails to start if JWT secret is invalid

### Error Messages

```
IllegalStateException: JWT secret must be configured via app.jwt.secret property or JWT_SECRET environment variable
IllegalStateException: JWT secret must be at least 32 characters long for security
```

## Production Deployment

### AWS Deployment
```bash
# Using AWS Secrets Manager
export JWT_SECRET=$(aws secretsmanager get-secret-value --secret-id prod/eventr/jwt-secret --query SecretString --output text)
```

### Docker Deployment
```bash
# Docker run with environment variable
docker run -e JWT_SECRET="$(openssl rand -base64 32)" eventr:latest

# Docker Compose
version: '3.8'
services:
  eventr:
    image: eventr:latest
    environment:
      - JWT_SECRET=${JWT_SECRET}
```

### Kubernetes Deployment
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: eventr
spec:
  template:
    spec:
      containers:
      - name: eventr
        image: eventr:latest
        env:
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: eventr-secrets
              key: jwt-secret
```

## Secret Rotation

### Production Secret Rotation Process

1. **Generate New Secret**:
   ```bash
   NEW_SECRET=$(openssl rand -base64 32)
   ```

2. **Update Secret Management System**:
   - AWS Secrets Manager
   - Azure Key Vault  
   - Kubernetes Secrets

3. **Rolling Deployment**:
   - Update environment variable
   - Restart application instances
   - Monitor for authentication failures

4. **Verification**:
   - Test authentication endpoints
   - Verify token generation/validation
   - Monitor application logs

### Development Secret Rotation

Development secrets can be rotated by:

1. Generating new secret: `openssl rand -base64 32`
2. Updating `application-dev.properties`
3. Restarting development environment

## Troubleshooting

### Common Issues

1. **Application won't start**: Check JWT secret is configured
2. **Authentication failures**: Verify secret matches between instances
3. **Token validation errors**: Ensure secret hasn't changed mid-session

### Verification Commands

```bash
# Check if JWT secret is configured
echo $JWT_SECRET | wc -c  # Should be > 32 characters

# Test application startup
curl -f http://localhost:8080/actuator/health

# Test authentication endpoint
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}'
```

## Security Best Practices

1. **Never commit secrets to version control**
2. **Use different secrets for each environment**
3. **Rotate secrets regularly (every 90 days)**
4. **Monitor for secret exposure in logs**
5. **Use secure secret management systems in production**
6. **Implement secret rotation automation**

## References

- [JWT Security Best Practices](https://tools.ietf.org/html/rfc8725)
- [OWASP Cryptographic Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html)
- [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-external-config)

---

**Last Updated**: September 8, 2025  
**Security Review**: Required for all JWT secret changes