package com.eventr.service

import com.eventr.dto.*
import com.eventr.model.User
import com.eventr.model.UserRole
import com.eventr.model.UserStatus
import com.eventr.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.crypto.SecretKey
import java.util.Base64
import jakarta.annotation.PostConstruct
import com.eventr.util.SecureLogger

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: BCryptPasswordEncoder = BCryptPasswordEncoder(),
    private val emailService: EmailService
) {
    
    private val secureLogger = SecureLogger(AuthService::class.java)
    
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
    
    fun register(registerDto: RegisterRequestDto): AuthResponseDto {
        // Check if email already exists
        if (userRepository.existsByEmail(registerDto.email)) {
            throw IllegalArgumentException("Email already registered")
        }
        
        // Validate password strength
        validatePassword(registerDto.password)
        
        // Create new user
        val user = User().apply {
            email = registerDto.email.lowercase().trim()
            firstName = registerDto.firstName.trim()
            lastName = registerDto.lastName.trim()
            phone = registerDto.phone?.trim()
            company = registerDto.company?.trim()
            jobTitle = registerDto.jobTitle?.trim()
            passwordHash = passwordEncoder.encode(registerDto.password)
            role = UserRole.ATTENDEE
            status = UserStatus.PENDING_VERIFICATION
            emailVerified = false
            emailVerificationToken = generateToken()
            marketingEmails = registerDto.marketingEmails
            createdAt = LocalDateTime.now()
        }
        
        val savedUser = userRepository.save(user)
        
        // Send verification email
        try {
            sendVerificationEmail(savedUser)
            secureLogger.logEmailEvent(savedUser.id!!, "REGISTRATION_VERIFICATION", true)
        } catch (e: Exception) {
            // Log error but don't fail registration
            secureLogger.logErrorEvent("EMAIL_SEND_FAILED", savedUser.id, e, "Failed to send verification email")
        }
        
        // Generate JWT token (simplified - in production use proper JWT library)
        val token = generateJwtToken(savedUser)
        
        return AuthResponseDto(
            token = token,
            user = mapToUserDto(savedUser),
            expiresIn = 86400 // 24 hours
        )
    }
    
    fun login(loginDto: LoginRequestDto): AuthResponseDto {
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
        val token = generateJwtToken(user)
        val expiresIn = if (loginDto.rememberMe) 2592000 else 86400 // 30 days or 24 hours
        
        return AuthResponseDto(
            token = token,
            user = mapToUserDto(user),
            expiresIn = expiresIn.toLong()
        )
    }
    
    fun verifyEmail(token: String): UserDto {
        val user = userRepository.findByEmailVerificationToken(token)
            ?: throw IllegalArgumentException("Invalid verification token")
        
        user.emailVerified = true
        user.emailVerificationToken = null
        user.status = UserStatus.ACTIVE
        
        val savedUser = userRepository.save(user)
        return mapToUserDto(savedUser)
    }
    
    fun requestPasswordReset(email: String) {
        val user = userRepository.findByEmail(email.lowercase().trim())
            ?: return // Don't reveal if email exists or not
        
        user.passwordResetToken = generateToken()
        user.passwordResetExpires = LocalDateTime.now().plusHours(1) // 1 hour expiry
        
        userRepository.save(user)
        
        try {
            sendPasswordResetEmail(user)
            secureLogger.logSecurityEvent("PASSWORD_RESET_EMAIL_SENT", user.id, true)
        } catch (e: Exception) {
            secureLogger.logErrorEvent("PASSWORD_RESET_EMAIL_FAILED", user.id, e, "Failed to send password reset email")
            throw IllegalStateException("Failed to send password reset email")
        }
    }
    
    fun resetPassword(resetDto: PasswordResetDto): AuthResponseDto {
        val user = userRepository.findByPasswordResetToken(resetDto.token)
            ?: throw IllegalArgumentException("Invalid or expired reset token")
        
        if (user.passwordResetExpires?.isBefore(LocalDateTime.now()) == true) {
            throw IllegalArgumentException("Reset token has expired")
        }
        
        validatePassword(resetDto.newPassword)
        
        user.passwordHash = passwordEncoder.encode(resetDto.newPassword)
        user.passwordResetToken = null
        user.passwordResetExpires = null
        
        val savedUser = userRepository.save(user)
        
        // Generate new token
        val token = generateJwtToken(savedUser)
        
        return AuthResponseDto(
            token = token,
            user = mapToUserDto(savedUser),
            expiresIn = 86400
        )
    }
    
    fun changePassword(userId: UUID, changePasswordDto: ChangePasswordDto): UserDto {
        val user = userRepository.findById(userId).orElseThrow { 
            IllegalArgumentException("User not found") 
        }
        
        if (!passwordEncoder.matches(changePasswordDto.currentPassword, user.passwordHash)) {
            throw IllegalArgumentException("Current password is incorrect")
        }
        
        validatePassword(changePasswordDto.newPassword)
        
        user.passwordHash = passwordEncoder.encode(changePasswordDto.newPassword)
        val savedUser = userRepository.save(user)
        
        return mapToUserDto(savedUser)
    }
    
    fun updateProfile(userId: UUID, updateDto: UpdateProfileDto): UserDto {
        val user = userRepository.findById(userId).orElseThrow { 
            IllegalArgumentException("User not found") 
        }
        
        updateDto.firstName?.let { user.firstName = it.trim() }
        updateDto.lastName?.let { user.lastName = it.trim() }
        updateDto.phone?.let { user.phone = it.trim() }
        updateDto.company?.let { user.company = it.trim() }
        updateDto.jobTitle?.let { user.jobTitle = it.trim() }
        updateDto.bio?.let { user.bio = it.trim() }
        updateDto.timezone?.let { user.timezone = it }
        updateDto.language?.let { user.language = it }
        updateDto.marketingEmails?.let { user.marketingEmails = it }
        updateDto.eventReminders?.let { user.eventReminders = it }
        updateDto.weeklyDigest?.let { user.weeklyDigest = it }
        
        val savedUser = userRepository.save(user)
        return mapToUserDto(savedUser)
    }
    
    fun getUserById(userId: UUID): UserDto? {
        return userRepository.findById(userId).map { mapToUserDto(it) }.orElse(null)
    }
    
    fun getUserByEmail(email: String): UserDto? {
        return userRepository.findByEmail(email.lowercase().trim())?.let { mapToUserDto(it) }
    }
    
    private fun validatePassword(password: String) {
        if (password.length < 8) {
            throw IllegalArgumentException("Password must be at least 8 characters long")
        }
        if (!password.matches(".*[A-Z].*".toRegex())) {
            throw IllegalArgumentException("Password must contain at least one uppercase letter")
        }
        if (!password.matches(".*[a-z].*".toRegex())) {
            throw IllegalArgumentException("Password must contain at least one lowercase letter")
        }
        if (!password.matches(".*\\d.*".toRegex())) {
            throw IllegalArgumentException("Password must contain at least one number")
        }
    }
    
    private fun generateToken(): String {
        return UUID.randomUUID().toString().replace("-", "")
    }
    
    private fun generateJwtToken(user: User): String {
        val now = Date()
        val expiryDate = Date(now.time + jwtExpiration)
        
        return Jwts.builder()
            .subject(user.id.toString())
            .claim("email", user.email)
            .claim("role", user.role.name)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(jwtSecret)
            .compact()
    }
    
    fun validateJwtToken(token: String): Boolean {
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
    
    fun getUserIdFromJwtToken(token: String): UUID {
        val claims = Jwts.parser()
            .verifyWith(jwtSecret)
            .build()
            .parseSignedClaims(token)
        return UUID.fromString(claims.payload.subject)
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
    
    private fun sendVerificationEmail(user: User) {
        // This would integrate with the existing EmailService
        // For development, log the verification event securely
        secureLogger.logEmailEvent(user.id!!, "EMAIL_VERIFICATION_INITIATED", true, "Verification email queued")
        secureLogger.logDebugSafe("Email verification link generated for user: {}", user.id)
    }
    
    private fun sendPasswordResetEmail(user: User) {
        // This would integrate with the existing EmailService
        // For development, log the password reset event securely
        secureLogger.logSecurityEvent("PASSWORD_RESET_EMAIL_INITIATED", user.id, true, "Password reset email queued")
        secureLogger.logDebugSafe("Password reset link generated for user: {}", user.id)
    }
}