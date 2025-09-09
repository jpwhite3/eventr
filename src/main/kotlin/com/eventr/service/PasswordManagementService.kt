package com.eventr.service

import com.eventr.dto.*
import java.util.*

/**
 * Service interface for password management operations.
 * 
 * Focuses exclusively on password-related concerns:
 * - Password reset workflow
 * - Password change operations
 * - Password validation
 * 
 * Follows Single Responsibility Principle by handling only password management logic.
 */
interface PasswordManagementService {

    /**
     * Initiate password reset workflow by sending reset email.
     * 
     * @param email User email address
     * @throws IllegalStateException if email sending fails
     */
    fun requestPasswordReset(email: String)

    /**
     * Reset password using reset token.
     * 
     * @param resetDto Password reset data including token and new password
     * @return Authentication result with new JWT token
     * @throws IllegalArgumentException if token is invalid or expired
     */
    fun resetPassword(resetDto: PasswordResetDto): AuthResponseDto

    /**
     * Change password for authenticated user.
     * 
     * @param userId User ID
     * @param changePasswordDto Password change data
     * @return Updated user information
     * @throws IllegalArgumentException if current password is incorrect
     */
    fun changePassword(userId: UUID, changePasswordDto: ChangePasswordDto): UserDto

    /**
     * Validate password strength according to security requirements.
     * 
     * @param password Password to validate
     * @throws IllegalArgumentException if password doesn't meet requirements
     */
    fun validatePasswordStrength(password: String)
}