package com.eventr.service.impl

import com.eventr.dto.*
import com.eventr.model.*
import com.eventr.repository.UserRepository
import com.eventr.service.UserAuthenticationService
import com.eventr.util.SecureLogger
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.crypto.SecretKey

/**
 * Implementation of UserAuthenticationService focused on authentication operations.
 * 
 * Responsibilities:
 * - User login/logout operations
 * - JWT token generation and validation
 * - Authentication security checks
 */
@Service
class UserAuthenticationServiceImpl(
    private val userRepository: UserRepository,
    private val passwordEncoder: BCryptPasswordEncoder
) : UserAuthenticationService {
    
    private val secureLogger = SecureLogger(UserAuthenticationServiceImpl::class.java)
    
    @Value("\${app.jwt.secret:#{environment.JWT_SECRET}}")
    private lateinit var jwtSecretString: String
    
    @Value("\${app.jwt.expiration:86400000}")
    private var jwtExpiration: Long = 86400000L // 24 hours in milliseconds
    
    private lateinit var jwtSecret: SecretKey
    
    @PostConstruct
    private fun initJwtSecret() {
        // Validate JWT secret configuration
        if (jwtSecretString.isBlank()) {
            throw IllegalStateException(
                "JWT secret must be configured via app.jwt.secret property or JWT_SECRET environment variable. " +
                "For security, the JWT secret cannot be hardcoded and must be provided externally."
            )
        }
        
        // Ensure minimum security requirements
        if (jwtSecretString.length < 32) {
            throw IllegalStateException(
                "JWT secret must be at least 32 characters long for security. " +
                "Current length: ${jwtSecretString.length}. Please generate a secure secret using: openssl rand -base64 32"
            )
        }
        
        try {
            // Try to decode as base64 first, fallback to plain string
            val secretBytes = try {
                Base64.getDecoder().decode(jwtSecretString)
            } catch (e: IllegalArgumentException) {
                // If not valid base64, use string directly (for development)
                jwtSecretString.toByteArray()
            }
            
            jwtSecret = Keys.hmacShaKeyFor(secretBytes)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to initialize JWT secret: ${e.message}", e)
        }
    }
    
    override fun login(loginDto: LoginRequestDto): AuthResponseDto {
        val user = userRepository.findByEmail(loginDto.email.lowercase().trim())
            ?: throw IllegalArgumentException("Invalid email or password")
        
        if (!passwordEncoder.matches(loginDto.password, user.passwordHash)) {
            throw IllegalArgumentException("Invalid email or password")
        }
        
        if (user.status == UserStatus.SUSPENDED) {
            throw IllegalArgumentException("Account suspended. Please contact support.")
        }
        
        if (user.status == UserStatus.INACTIVE) {
            throw IllegalArgumentException("Account inactive. Please contact support.")
        }
        
        // Update last login
        user.lastLoginAt = LocalDateTime.now()
        userRepository.save(user)
        
        // Generate JWT token
        val token = generateJwtToken(mapToUserDto(user), loginDto.rememberMe)
        val expiresIn = if (loginDto.rememberMe) 2592000 else 86400 // 30 days or 24 hours
        
        return AuthResponseDto(
            token = token,
            user = mapToUserDto(user),
            expiresIn = expiresIn.toLong()
        )
    }
    
    override fun validateJwtToken(token: String): Boolean {
        return try {
            Jwts.parser()
                .verifyWith(jwtSecret)
                .build()
                .parseSignedClaims(token)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override fun getUserIdFromJwtToken(token: String): UUID {
        val claims = Jwts.parser()
            .verifyWith(jwtSecret)
            .build()
            .parseSignedClaims(token)
        return UUID.fromString(claims.payload.subject)
    }
    
    override fun generateJwtToken(user: UserDto, rememberMe: Boolean): String {
        val now = Date()
        val expiration = if (rememberMe) 2592000000L else jwtExpiration // 30 days or 24 hours
        val expiryDate = Date(now.time + expiration)
        
        return Jwts.builder()
            .subject(user.id.toString())
            .claim("email", user.email)
            .claim("role", user.role)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(jwtSecret)
            .compact()
    }
    
    override fun logout(token: String) {
        // Token blacklisting could be implemented here in the future
        // For now, client-side token removal is sufficient
        secureLogger.logSecurityEvent("USER_LOGOUT", null, true, "User logged out")
    }
    
    private fun mapToUserDto(user: User): UserDto {
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        return UserDto(
            id = user.id!!,
            email = user.email!!,
            firstName = user.firstName!!,
            lastName = user.lastName!!,
            phone = user.phone,
            company = user.company,
            jobTitle = user.jobTitle,
            bio = user.bio,
            profileImageUrl = user.profileImageUrl,
            role = user.role.name,
            status = user.status.name,
            emailVerified = user.emailVerified,
            timezone = user.timezone,
            language = user.language,
            marketingEmails = user.marketingEmails,
            eventReminders = user.eventReminders,
            weeklyDigest = user.weeklyDigest,
            createdAt = user.createdAt.format(formatter),
            lastLoginAt = user.lastLoginAt?.format(formatter)
        )
    }
}