package com.eventr.service

import com.eventr.dto.*
import java.util.*

/**
 * Service interface for user authentication operations.
 * 
 * Focuses exclusively on authentication concerns:
 * - Login/logout operations
 * - JWT token validation and generation
 * - Session management
 * 
 * Follows Single Responsibility Principle by handling only authentication logic.
 */
interface UserAuthenticationService {

    /**
     * Authenticate user credentials and generate JWT token.
     * 
     * @param loginDto User credentials
     * @return Authentication result with JWT token and user information
     * @throws IllegalArgumentException if credentials are invalid
     */
    fun login(loginDto: LoginRequestDto): AuthResponseDto

    /**
     * Validate JWT token for authentication.
     * 
     * @param token JWT token to validate
     * @return true if token is valid, false otherwise
     */
    fun validateJwtToken(token: String): Boolean

    /**
     * Extract user ID from valid JWT token.
     * 
     * @param token Valid JWT token
     * @return User ID from token claims
     * @throws IllegalArgumentException if token is invalid
     */
    fun getUserIdFromJwtToken(token: String): UUID

    /**
     * Generate JWT token for authenticated user.
     * 
     * @param user User to generate token for
     * @param rememberMe Whether to use extended expiration
     * @return JWT token string
     */
    fun generateJwtToken(user: UserDto, rememberMe: Boolean = false): String

    /**
     * Logout user by invalidating token (if token blacklisting is implemented).
     * 
     * @param token JWT token to invalidate
     */
    fun logout(token: String)
}