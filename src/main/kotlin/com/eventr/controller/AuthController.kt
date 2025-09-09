package com.eventr.controller

import com.eventr.dto.*
import com.eventr.service.UserAuthenticationService
import com.eventr.service.UserRegistrationService
import com.eventr.service.PasswordManagementService
import com.eventr.service.UserProfileService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authenticationService: UserAuthenticationService,
    private val registrationService: UserRegistrationService,
    private val passwordService: PasswordManagementService,
    private val profileService: UserProfileService
) {
    
    @PostMapping("/register")
    fun register(@RequestBody registerDto: RegisterRequestDto): ResponseEntity<AuthResponseDto> {
        return try {
            val response = registrationService.register(registerDto)
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }
    
    @PostMapping("/login")
    fun login(@RequestBody loginDto: LoginRequestDto): ResponseEntity<AuthResponseDto> {
        return try {
            val response = authenticationService.login(loginDto)
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(401).build()
        }
    }
    
    @PostMapping("/verify-email")
    fun verifyEmail(@RequestBody verificationDto: EmailVerificationDto): ResponseEntity<UserDto> {
        return try {
            val user = registrationService.verifyEmail(verificationDto.token)
            ResponseEntity.ok(user)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }
    
    @PostMapping("/forgot-password")
    fun forgotPassword(@RequestBody resetRequestDto: PasswordResetRequestDto): ResponseEntity<Map<String, String>> {
        return try {
            passwordService.requestPasswordReset(resetRequestDto.email)
            ResponseEntity.ok(mapOf("message" to "Password reset email sent if account exists"))
        } catch (e: Exception) {
            ResponseEntity.status(500).body(mapOf("error" to "Failed to send password reset email"))
        }
    }
    
    @PostMapping("/reset-password")
    fun resetPassword(@RequestBody resetDto: PasswordResetDto): ResponseEntity<AuthResponseDto> {
        return try {
            val response = passwordService.resetPassword(resetDto)
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }
    
    @PutMapping("/change-password")
    fun changePassword(
        @RequestHeader("Authorization") authToken: String,
        @RequestBody changePasswordDto: ChangePasswordDto
    ): ResponseEntity<UserDto> {
        return try {
            val userId = extractUserIdFromToken(authToken)
            val user = passwordService.changePassword(userId, changePasswordDto)
            ResponseEntity.ok(user)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            ResponseEntity.status(401).build()
        }
    }
    
    @PutMapping("/profile")
    fun updateProfile(
        @RequestHeader("Authorization") authToken: String,
        @RequestBody updateDto: UpdateProfileDto
    ): ResponseEntity<UserDto> {
        return try {
            val userId = extractUserIdFromToken(authToken)
            val user = profileService.updateProfile(userId, updateDto)
            ResponseEntity.ok(user)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            ResponseEntity.status(401).build()
        }
    }
    
    @GetMapping("/profile")
    fun getProfile(@RequestHeader("Authorization") authToken: String): ResponseEntity<UserDto> {
        return try {
            val userId = extractUserIdFromToken(authToken)
            val user = profileService.getUserById(userId)
            if (user != null) {
                ResponseEntity.ok(user)
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            ResponseEntity.status(401).build()
        }
    }
    
    @PostMapping("/logout")
    fun logout(): ResponseEntity<Map<String, String>> {
        // In a production app, you would invalidate the token on the server side
        // For this simplified JWT implementation, logout is handled client-side
        return ResponseEntity.ok(mapOf("message" to "Logged out successfully"))
    }
    
    private fun extractUserIdFromToken(authToken: String): UUID {
        try {
            // Remove "Bearer " prefix if present
            val token = authToken.removePrefix("Bearer ").trim()
            
            // Validate and extract user ID from JWT token
            if (!authenticationService.validateJwtToken(token)) {
                throw IllegalArgumentException("Invalid JWT token")
            }
            
            return authenticationService.getUserIdFromJwtToken(token)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid authorization token: ${e.message}")
        }
    }
}