package com.eventr.service.impl

import com.eventr.dto.*
import com.eventr.model.*
import com.eventr.repository.UserRepository
import com.eventr.service.UserRegistrationService
import com.eventr.service.UserAuthenticationService
import com.eventr.util.SecureLogger
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Implementation of UserRegistrationService focused on registration operations.
 * 
 * Responsibilities:
 * - User account creation and validation
 * - Email verification workflow
 * - Registration business logic
 */
@Service
class UserRegistrationServiceImpl(
    private val userRepository: UserRepository,
    private val passwordEncoder: BCryptPasswordEncoder,
    private val authenticationService: UserAuthenticationService
) : UserRegistrationService {
    
    private val secureLogger = SecureLogger(UserRegistrationServiceImpl::class.java)
    
    override fun register(registerDto: RegisterRequestDto): AuthResponseDto {
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
        
        // Send verification email (would integrate with EmailNotificationService)
        try {
            sendVerificationEmail(savedUser)
            secureLogger.logEmailEvent(savedUser.id!!, "REGISTRATION_VERIFICATION", true)
        } catch (e: Exception) {
            // Log error but don't fail registration
            secureLogger.logErrorEvent("EMAIL_SEND_FAILED", savedUser.id, e, "Failed to send verification email")
        }
        
        // Generate JWT token through authentication service
        val token = authenticationService.generateJwtToken(mapToUserDto(savedUser))
        
        return AuthResponseDto(
            token = token,
            user = mapToUserDto(savedUser),
            expiresIn = 86400 // 24 hours
        )
    }
    
    override fun verifyEmail(token: String): UserDto {
        val user = userRepository.findByEmailVerificationToken(token)
            ?: throw IllegalArgumentException("Invalid verification token")
        
        user.emailVerified = true
        user.emailVerificationToken = null
        user.status = UserStatus.ACTIVE
        
        val savedUser = userRepository.save(user)
        return mapToUserDto(savedUser)
    }
    
    override fun isEmailRegistered(email: String): Boolean {
        return userRepository.existsByEmail(email.lowercase().trim())
    }
    
    override fun resendEmailVerification(email: String) {
        val user = userRepository.findByEmail(email.lowercase().trim())
            ?: throw IllegalArgumentException("User not found")
        
        if (user.emailVerified) {
            throw IllegalArgumentException("Email already verified")
        }
        
        // Generate new verification token if needed
        if (user.emailVerificationToken == null) {
            user.emailVerificationToken = generateToken()
            userRepository.save(user)
        }
        
        try {
            sendVerificationEmail(user)
            secureLogger.logEmailEvent(user.id!!, "VERIFICATION_RESENT", true)
        } catch (e: Exception) {
            secureLogger.logErrorEvent("VERIFICATION_RESEND_FAILED", user.id, e, "Failed to resend verification email")
            throw IllegalStateException("Failed to send verification email")
        }
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
    
    private fun sendVerificationEmail(user: User) {
        // This would integrate with EmailNotificationService in production
        // For development, log the verification event securely
        secureLogger.logEmailEvent(user.id!!, "EMAIL_VERIFICATION_INITIATED", true, "Verification email queued")
        secureLogger.logDebugSafe("Email verification link generated for user: {}", user.id)
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