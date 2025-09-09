package com.eventr.service.impl

import com.eventr.dto.*
import com.eventr.model.*
import com.eventr.repository.UserRepository
import com.eventr.service.PasswordManagementService
import com.eventr.service.UserAuthenticationService
import com.eventr.util.SecureLogger
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Implementation of PasswordManagementService focused on password operations.
 * 
 * Responsibilities:
 * - Password reset workflow management
 * - Password change validation and processing
 * - Password strength validation
 * - Security token management for password operations
 */
@Service
class PasswordManagementServiceImpl(
    private val userRepository: UserRepository,
    private val passwordEncoder: BCryptPasswordEncoder,
    private val authenticationService: UserAuthenticationService
) : PasswordManagementService {
    
    private val secureLogger = SecureLogger(PasswordManagementServiceImpl::class.java)
    
    override fun requestPasswordReset(email: String) {
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
    
    override fun resetPassword(resetDto: PasswordResetDto): AuthResponseDto {
        val user = userRepository.findByPasswordResetToken(resetDto.token)
            ?: throw IllegalArgumentException("Invalid or expired reset token")
        
        if (user.passwordResetExpires?.isBefore(LocalDateTime.now()) == true) {
            throw IllegalArgumentException("Reset token has expired")
        }
        
        validatePasswordStrength(resetDto.newPassword)
        
        user.passwordHash = passwordEncoder.encode(resetDto.newPassword)
        user.passwordResetToken = null
        user.passwordResetExpires = null
        
        val savedUser = userRepository.save(user)
        
        // Generate new token through authentication service
        val token = authenticationService.generateJwtToken(mapToUserDto(savedUser))
        
        return AuthResponseDto(
            token = token,
            user = mapToUserDto(savedUser),
            expiresIn = 86400
        )
    }
    
    override fun changePassword(userId: UUID, changePasswordDto: ChangePasswordDto): UserDto {
        val user = userRepository.findById(userId).orElseThrow { 
            IllegalArgumentException("User not found") 
        }
        
        if (!passwordEncoder.matches(changePasswordDto.currentPassword, user.passwordHash)) {
            throw IllegalArgumentException("Current password is incorrect")
        }
        
        validatePasswordStrength(changePasswordDto.newPassword)
        
        user.passwordHash = passwordEncoder.encode(changePasswordDto.newPassword)
        val savedUser = userRepository.save(user)
        
        secureLogger.logSecurityEvent("PASSWORD_CHANGED", userId, true, "User password changed successfully")
        
        return mapToUserDto(savedUser)
    }
    
    override fun validatePasswordStrength(password: String) {
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
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*".toRegex())) {
            throw IllegalArgumentException("Password must contain at least one special character")
        }
    }
    
    private fun generateToken(): String {
        return UUID.randomUUID().toString().replace("-", "")
    }
    
    private fun sendPasswordResetEmail(user: User) {
        // This would integrate with EmailNotificationService in production
        // For development, log the password reset event securely
        secureLogger.logSecurityEvent("PASSWORD_RESET_EMAIL_INITIATED", user.id, true, "Password reset email queued")
        secureLogger.logDebugSafe("Password reset link generated for user: {}", user.id)
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