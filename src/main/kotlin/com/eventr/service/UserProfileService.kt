package com.eventr.service

import com.eventr.dto.*
import java.util.*

/**
 * Service interface for user profile management operations.
 * 
 * Focuses exclusively on user profile concerns:
 * - Profile information updates
 * - User data retrieval
 * - Profile preferences management
 * 
 * Follows Single Responsibility Principle by handling only profile management logic.
 */
interface UserProfileService {

    /**
     * Update user profile information.
     * 
     * @param userId User ID
     * @param updateDto Profile update data
     * @return Updated user information
     * @throws IllegalArgumentException if user not found
     */
    fun updateProfile(userId: UUID, updateDto: UpdateProfileDto): UserDto

    /**
     * Get user profile by ID.
     * 
     * @param userId User ID
     * @return User information or null if not found
     */
    fun getUserById(userId: UUID): UserDto?

    /**
     * Get user profile by email address.
     * 
     * @param email User email address
     * @return User information or null if not found
     */
    fun getUserByEmail(email: String): UserDto?

    /**
     * Update user preferences (notifications, timezone, language).
     * 
     * @param userId User ID
     * @param preferences User preference settings
     * @return Updated user information
     * @throws IllegalArgumentException if user not found
     */
    fun updateUserPreferences(userId: UUID, preferences: Map<String, Any>): UserDto

    /**
     * Get user's notification preferences.
     * 
     * @param userId User ID
     * @return Notification preference settings
     */
    fun getNotificationPreferences(userId: UUID): Map<String, Boolean>
}