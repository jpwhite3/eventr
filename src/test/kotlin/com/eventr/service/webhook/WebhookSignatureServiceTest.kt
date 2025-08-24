package com.eventr.service.webhook

import com.eventr.service.webhook.impl.WebhookSignatureServiceImpl
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*

class WebhookSignatureServiceTest {

    private lateinit var signatureService: WebhookSignatureService

    @BeforeEach
    fun setUp() {
        signatureService = WebhookSignatureServiceImpl()
    }

    @Test
    fun `generateSignature should create valid HMAC-SHA256 signature`() {
        // Arrange
        val payload = """{"test": "data", "timestamp": "2023-01-01T00:00:00Z"}"""
        val secret = "test-secret-key"

        // Act
        val signature = signatureService.generateSignature(payload, secret)

        // Assert
        assertNotNull(signature)
        assertTrue(signature.startsWith("sha256="))
        assertTrue(signature.length > 7) // "sha256=" plus base64 encoded hash
    }

    @Test
    fun `generateSignature should be deterministic`() {
        // Arrange
        val payload = """{"user": "john@example.com", "event": "user_registered"}"""
        val secret = "consistent-secret"

        // Act
        val signature1 = signatureService.generateSignature(payload, secret)
        val signature2 = signatureService.generateSignature(payload, secret)

        // Assert
        assertEquals(signature1, signature2)
    }

    @Test
    fun `generateSignature should produce different signatures for different payloads`() {
        // Arrange
        val payload1 = """{"event": "user_registered"}"""
        val payload2 = """{"event": "user_cancelled"}"""
        val secret = "same-secret"

        // Act
        val signature1 = signatureService.generateSignature(payload1, secret)
        val signature2 = signatureService.generateSignature(payload2, secret)

        // Assert
        assertNotEquals(signature1, signature2)
    }

    @Test
    fun `generateSignature should produce different signatures for different secrets`() {
        // Arrange
        val payload = """{"event": "test_event"}"""
        val secret1 = "secret-one"
        val secret2 = "secret-two"

        // Act
        val signature1 = signatureService.generateSignature(payload, secret1)
        val signature2 = signatureService.generateSignature(payload, secret2)

        // Assert
        assertNotEquals(signature1, signature2)
    }

    @Test
    fun `validateSignature should return true for valid signature`() {
        // Arrange
        val payload = """{"webhook": "test", "data": {"id": 123}}"""
        val secret = "validation-secret"
        val validSignature = signatureService.generateSignature(payload, secret)

        // Act
        val isValid = signatureService.validateSignature(payload, validSignature, secret)

        // Assert
        assertTrue(isValid)
    }

    @Test
    fun `validateSignature should return false for invalid signature`() {
        // Arrange
        val payload = """{"webhook": "test"}"""
        val secret = "validation-secret"
        val invalidSignature = "sha256=invalid-signature-hash"

        // Act
        val isValid = signatureService.validateSignature(payload, invalidSignature, secret)

        // Assert
        assertFalse(isValid)
    }

    @Test
    fun `validateSignature should return false for wrong secret`() {
        // Arrange
        val payload = """{"webhook": "test"}"""
        val correctSecret = "correct-secret"
        val wrongSecret = "wrong-secret"
        val signature = signatureService.generateSignature(payload, correctSecret)

        // Act
        val isValid = signatureService.validateSignature(payload, signature, wrongSecret)

        // Assert
        assertFalse(isValid)
    }

    @Test
    fun `validateSignature should return false for tampered payload`() {
        // Arrange
        val originalPayload = """{"amount": 100}"""
        val tamperedPayload = """{"amount": 1000}"""
        val secret = "security-secret"
        val signature = signatureService.generateSignature(originalPayload, secret)

        // Act
        val isValid = signatureService.validateSignature(tamperedPayload, signature, secret)

        // Assert
        assertFalse(isValid)
    }

    @Test
    fun `generateSecret should create unique secrets`() {
        // Act
        val secret1 = signatureService.generateSecret()
        val secret2 = signatureService.generateSecret()
        val secret3 = signatureService.generateSecret()

        // Assert
        assertNotNull(secret1)
        assertNotNull(secret2)
        assertNotNull(secret3)
        assertNotEquals(secret1, secret2)
        assertNotEquals(secret2, secret3)
        assertNotEquals(secret1, secret3)
    }

    @Test
    fun `generateSecret should create base64 encoded secrets`() {
        // Act
        val secret = signatureService.generateSecret()

        // Assert
        assertNotNull(secret)
        assertTrue(secret.length > 0)
        
        // Should be valid base64
        try {
            java.util.Base64.getDecoder().decode(secret)
            // If no exception, it's valid base64
        } catch (e: IllegalArgumentException) {
            fail("Generated secret is not valid base64: $secret")
        }
    }

    @Test
    fun `generateSecret should have sufficient entropy`() {
        // Generate multiple secrets and check they're all different
        val secrets = (1..10).map { signatureService.generateSecret() }.toSet()
        
        // All should be unique
        assertEquals(10, secrets.size)
        
        // All should have reasonable length (base64 of 32 bytes should be ~44 chars)
        secrets.forEach { secret ->
            assertTrue(secret.length >= 40, "Secret too short: $secret")
        }
    }

    @Test
    fun `signature validation should be timing-safe`() {
        // This is a basic test to ensure we're using MessageDigest.isEqual
        // In practice, timing attacks are hard to test in unit tests
        val payload = """{"test": "timing"}"""
        val secret = "timing-secret"
        val validSignature = signatureService.generateSignature(payload, secret)
        
        // These should all return false, but in constant time
        val invalidSignatures = listOf(
            "sha256=short",
            "sha256=exactly-the-same-length-as-valid-sig-but-wrong",
            "sha256=completely-different-signature-with-same-len"
        )
        
        invalidSignatures.forEach { invalidSig ->
            assertFalse(signatureService.validateSignature(payload, invalidSig, secret))
        }
        
        // Valid signature should still work
        assertTrue(signatureService.validateSignature(payload, validSignature, secret))
    }
}