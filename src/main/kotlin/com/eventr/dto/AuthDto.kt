package com.eventr.dto

import java.util.*

data class LoginRequestDto(
    val email: String,
    val password: String,
    val rememberMe: Boolean = false
)

data class RegisterRequestDto(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val phone: String? = null,
    val company: String? = null,
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
    val email: String
)

data class PasswordResetDto(
    val token: String,
    val newPassword: String
)

data class ChangePasswordDto(
    val currentPassword: String,
    val newPassword: String
)

data class UpdateProfileDto(
    val firstName: String?,
    val lastName: String?,
    val phone: String?,
    val company: String?,
    val jobTitle: String?,
    val bio: String?,
    val timezone: String?,
    val language: String?,
    val marketingEmails: Boolean?,
    val eventReminders: Boolean?,
    val weeklyDigest: Boolean?
)

data class EmailVerificationDto(
    val token: String
)