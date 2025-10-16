package com.eventr.config

import com.eventr.model.User
import com.eventr.model.UserRole
import com.eventr.model.UserStatus
import com.eventr.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.core.io.ClassPathResource
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.*

/**
 * Test for user fixture loading to ensure development users are properly loaded.
 */
@ExtendWith(MockitoExtension::class)
class UserFixtureTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var passwordEncoder: BCryptPasswordEncoder

    @Test
    fun `should load user fixtures correctly`() {
        // Arrange
        val objectMapper: ObjectMapper = jacksonObjectMapper().apply {
            registerModule(JavaTimeModule())
        }

        // Act - Test that the users.json file can be read and parsed
        val resource = ClassPathResource("fixtures/users.json")
        val users = objectMapper.readTree(resource.inputStream)

        // Assert
        assert(users.isArray) { "Users fixture should be an array" }
        assert(users.size() >= 3) { "Should have at least 3 test users" }

        // Verify required users exist
        val userEmails = users.map { it["email"].asText() }.toSet()
        assert(userEmails.contains("admin@eventr.dev")) { "Should have admin user" }
        assert(userEmails.contains("organizer@eventr.dev")) { "Should have organizer user" }
        assert(userEmails.contains("user@eventr.dev")) { "Should have regular user" }

        // Verify structure of first user
        val firstUser = users[0]
        assert(firstUser.has("id")) { "User should have id" }
        assert(firstUser.has("email")) { "User should have email" }
        assert(firstUser.has("firstName")) { "User should have firstName" }
        assert(firstUser.has("lastName")) { "User should have lastName" }
        assert(firstUser.has("password")) { "User should have password" }
        assert(firstUser.has("role")) { "User should have role" }
        assert(firstUser.has("status")) { "User should have status" }

        // Verify password is the expected development password
        users.forEach { userNode ->
            assert(userNode["password"].asText() == "DevPassword123") { 
                "All development users should have the same password: DevPassword123" 
            }
        }

        println("✅ User fixture validation passed!")
        println("📧 Found ${users.size()} development users")
        users.forEach { user ->
            println("   - ${user["email"].asText()} (${user["role"].asText()})")
        }
    }
}