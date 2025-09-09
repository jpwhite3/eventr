package com.eventr.service.impl

import com.eventr.dto.*
import com.eventr.model.*
import com.eventr.repository.UserRepository
import com.eventr.service.UserProfileService
import com.eventr.util.SecureLogger
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Implementation of UserProfileService focused on profile management operations.
 * 
 * Responsibilities:
 * - User profile information updates
 * - User data retrieval and queries
 * - User preferences management
 * - Profile-related validation
 */
@Service
class UserProfileServiceImpl(
    private val userRepository: UserRepository
) : UserProfileService {
    
    private val secureLogger = SecureLogger(UserProfileServiceImpl::class.java)
    
    override fun updateProfile(userId: UUID, updateDto: UpdateProfileDto): UserDto {
        val user = userRepository.findById(userId).orElseThrow { 
            IllegalArgumentException("User not found") 
        }
        
        // Update profile fields if provided
        updateDto.firstName?.let { 
            validateName(it, "firstName")
            user.firstName = it.trim() 
        }
        updateDto.lastName?.let { 
            validateName(it, "lastName")
            user.lastName = it.trim() 
        }
        updateDto.phone?.let { 
            validatePhone(it)
            user.phone = it.trim() 
        }
        updateDto.company?.let { user.company = it.trim() }
        updateDto.jobTitle?.let { user.jobTitle = it.trim() }
        updateDto.bio?.let { 
            validateBio(it)
            user.bio = it.trim() 
        }
        updateDto.timezone?.let { user.timezone = it }
        updateDto.language?.let { user.language = it }
        
        // Update notification preferences
        updateDto.marketingEmails?.let { user.marketingEmails = it }
        updateDto.eventReminders?.let { user.eventReminders = it }
        updateDto.weeklyDigest?.let { user.weeklyDigest = it }
        
        val savedUser = userRepository.save(user)
        
        secureLogger.logDebugSafe("User profile updated for user: {}", userId)
        
        return mapToUserDto(savedUser)
    }
    
    override fun getUserById(userId: UUID): UserDto? {
        return userRepository.findById(userId).map { mapToUserDto(it) }.orElse(null)
    }
    
    override fun getUserByEmail(email: String): UserDto? {
        return userRepository.findByEmail(email.lowercase().trim())?.let { mapToUserDto(it) }
    }
    
    override fun updateUserPreferences(userId: UUID, preferences: Map<String, Any>): UserDto {
        val user = userRepository.findById(userId).orElseThrow { 
            IllegalArgumentException("User not found") 
        }
        
        // Update preferences based on provided map
        preferences["marketingEmails"]?.let { 
            user.marketingEmails = it as? Boolean ?: user.marketingEmails
        }
        preferences["eventReminders"]?.let { 
            user.eventReminders = it as? Boolean ?: user.eventReminders
        }
        preferences["weeklyDigest"]?.let { 
            user.weeklyDigest = it as? Boolean ?: user.weeklyDigest
        }
        preferences["timezone"]?.let { 
            user.timezone = it as? String ?: user.timezone
        }
        preferences["language"]?.let { 
            user.language = it as? String ?: user.language
        }
        
        val savedUser = userRepository.save(user)
        
        secureLogger.logDebugSafe("User preferences updated for user: {}", userId)
        
        return mapToUserDto(savedUser)
    }
    
    override fun getNotificationPreferences(userId: UUID): Map<String, Boolean> {
        val user = userRepository.findById(userId).orElseThrow { 
            IllegalArgumentException("User not found") 
        }
        
        return mapOf(
            "marketingEmails" to user.marketingEmails,
            "eventReminders" to user.eventReminders,
            "weeklyDigest" to user.weeklyDigest
        )
    }
    
    private fun validateName(name: String, fieldName: String) {
        if (name.trim().length < 2) {
            throw IllegalArgumentException("$fieldName must be at least 2 characters long")
        }
        if (name.trim().length > 50) {
            throw IllegalArgumentException("$fieldName must be less than 50 characters")
        }
        if (!name.matches("^[a-zA-Z\\s'-]+$".toRegex())) {
            throw IllegalArgumentException("$fieldName can only contain letters, spaces, hyphens, and apostrophes")
        }
    }
    
    private fun validatePhone(phone: String) {
        val cleanPhone = phone.replace("[^0-9+()-\\s]".toRegex(), "")
        if (cleanPhone.length < 10) {
            throw IllegalArgumentException("Phone number must be at least 10 digits")
        }
        if (cleanPhone.length > 20) {
            throw IllegalArgumentException("Phone number must be less than 20 characters")
        }
    }
    
    private fun validateBio(bio: String) {
        if (bio.trim().length > 500) {
            throw IllegalArgumentException("Bio must be less than 500 characters")
        }
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