package com.eventr.dto

import jakarta.validation.constraints.*
import java.util.*

data class LoginRequestDto(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Please provide a valid email address")
    @field:Size(max = 254, message = "Email must not exceed 254 characters")
    val email: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 1, max = 128, message = "Password must be between 1 and 128 characters")
    val password: String,

    val rememberMe: Boolean = false
)

data class RegisterRequestDto(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Please provide a valid email address")
    @field:Size(max = 254, message = "Email must not exceed 254 characters")
    val email: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
        message = "Password must contain at least one lowercase letter, one uppercase letter, and one number"
    )
    val password: String,

    @field:NotBlank(message = "First name is required")
    @field:Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    @field:Pattern(
        regexp = "^[a-zA-Z\\s'-]+$",
        message = "First name can only contain letters, spaces, hyphens, and apostrophes"
    )
    val firstName: String,

    @field:NotBlank(message = "Last name is required")
    @field:Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    @field:Pattern(
        regexp = "^[a-zA-Z\\s'-]+$",
        message = "Last name can only contain letters, spaces, hyphens, and apostrophes"
    )
    val lastName: String,

    @field:Size(max = 20, message = "Phone number must not exceed 20 characters")
    @field:Pattern(
        regexp = "^[\\+]?[1-9][\\d\\s\\-\\(\\)]{7,18}$|^$",
        message = "Please provide a valid phone number"
    )
    val phone: String? = null,

    @field:Size(max = 100, message = "Company name must not exceed 100 characters")
    val company: String? = null,

    @field:Size(max = 100, message = "Job title must not exceed 100 characters")
    val jobTitle: String? = null,

    val marketingEmails: Boolean = true
)

data class AuthResponseDto(
    val token: String,
    val user: UserDto,
    val expiresIn: Long
)

data class UserDto(
    val id: UUID,
    val email: String,
    val firstName: String,
    val lastName: String,
    val phone: String? = null,
    val company: String? = null,
    val jobTitle: String? = null,
    val bio: String? = null,
    val profileImageUrl: String? = null,
    val role: String,
    val status: String,
    val emailVerified: Boolean,
    val timezone: String? = null,
    val language: String? = null,
    val marketingEmails: Boolean,
    val eventReminders: Boolean,
    val weeklyDigest: Boolean,
    val createdAt: String,
    val lastLoginAt: String? = null
)

data class PasswordResetRequestDto(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Please provide a valid email address")
    @field:Size(max = 254, message = "Email must not exceed 254 characters")
    val email: String
)

data class PasswordResetDto(
    @field:NotBlank(message = "Reset token is required")
    @field:Size(min = 32, max = 64, message = "Invalid reset token format")
    val token: String,

    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
        message = "Password must contain at least one lowercase letter, one uppercase letter, and one number"
    )
    val newPassword: String
)

data class ChangePasswordDto(
    @field:NotBlank(message = "Current password is required")
    @field:Size(min = 1, max = 128, message = "Current password must be between 1 and 128 characters")
    val currentPassword: String,

    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, max = 128, message = "New password must be between 8 and 128 characters")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
        message = "New password must contain at least one lowercase letter, one uppercase letter, and one number"
    )
    val newPassword: String
)

data class UpdateProfileDto(
    @field:Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    @field:Pattern(
        regexp = "^[a-zA-Z\\s'-]+$|^$",
        message = "First name can only contain letters, spaces, hyphens, and apostrophes"
    )
    val firstName: String?,

    @field:Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    @field:Pattern(
        regexp = "^[a-zA-Z\\s'-]+$|^$",
        message = "Last name can only contain letters, spaces, hyphens, and apostrophes"
    )
    val lastName: String?,

    @field:Size(max = 20, message = "Phone number must not exceed 20 characters")
    @field:Pattern(
        regexp = "^[\\+]?[1-9][\\d\\s\\-\\(\\)]{7,18}$|^$",
        message = "Please provide a valid phone number"
    )
    val phone: String?,

    @field:Size(max = 100, message = "Company name must not exceed 100 characters")
    val company: String?,

    @field:Size(max = 100, message = "Job title must not exceed 100 characters")
    val jobTitle: String?,

    @field:Size(max = 500, message = "Bio must not exceed 500 characters")
    val bio: String?,

    @field:Size(max = 50, message = "Timezone must not exceed 50 characters")
    val timezone: String?,

    @field:Size(max = 10, message = "Language code must not exceed 10 characters")
    @field:Pattern(
        regexp = "^[a-z]{2}(-[A-Z]{2})?$|^$",
        message = "Language must be in format 'en' or 'en-US'"
    )
    val language: String?,

    val marketingEmails: Boolean?,
    val eventReminders: Boolean?,
    val weeklyDigest: Boolean?
)

data class EmailVerificationDto(
    @field:NotBlank(message = "Verification token is required")
    @field:Size(min = 32, max = 64, message = "Invalid verification token format")
    val token: String
)