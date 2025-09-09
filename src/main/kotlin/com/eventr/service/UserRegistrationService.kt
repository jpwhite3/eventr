package com.eventr.service

import com.eventr.dto.*
import java.util.*

/**
 * Service interface for user registration operations.
 * 
 * Focuses exclusively on user registration concerns:
 * - User account creation
 * - Email verification workflow
 * - Registration validation
 * 
 * Follows Single Responsibility Principle by handling only registration logic.
 */
interface UserRegistrationService {

    /**
     * Register new user account with validation.
     * 
     * @param registerDto User registration data
     * @return Registration result with JWT token and user information
     * @throws IllegalArgumentException if registration data is invalid or email exists
     */
    fun register(registerDto: RegisterRequestDto): AuthResponseDto

    /**
     * Verify user email address using verification token.
     * 
     * @param token Email verification token
     * @return Updated user information
     * @throws IllegalArgumentException if token is invalid or expired
     */
    fun verifyEmail(token: String): UserDto

    /**
     * Check if email address is already registered.
     * 
     * @param email Email address to check
     * @return true if email exists, false otherwise
     */
    fun isEmailRegistered(email: String): Boolean

    /**
     * Resend email verification for pending users.
     * 
     * @param email User email address
     * @throws IllegalArgumentException if user not found or already verified
     */
    fun resendEmailVerification(email: String)
}