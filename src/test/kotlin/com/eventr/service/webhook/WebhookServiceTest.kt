package com.eventr.service.webhook

import com.eventr.model.webhook.Webhook
import com.eventr.model.webhook.WebhookEventType
import com.eventr.model.webhook.WebhookStatus
import com.eventr.repository.WebhookRepository
import com.eventr.service.webhook.impl.WebhookServiceImpl
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import java.util.*
import org.junit.jupiter.api.Assertions.*

class WebhookServiceTest {

    @Mock
    private lateinit var webhookRepository: WebhookRepository

    @Mock
    private lateinit var signatureService: WebhookSignatureService

    private lateinit var webhookService: WebhookService

    private val testWebhookId = UUID.randomUUID()
    private val testSecret = "test-secret-123"

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        webhookService = WebhookServiceImpl(webhookRepository, signatureService)
    }

    @Test
    fun `createWebhook should create new webhook with generated secret`() {
        // Arrange
        val name = "Test Webhook"
        val url = "https://example.com/webhook"
        val eventTypes = setOf(WebhookEventType.USER_REGISTERED, WebhookEventType.USER_CANCELLED)
        val createdBy = "test-user"

        whenever(signatureService.generateSecret()).thenReturn(testSecret)
        whenever(webhookRepository.save(any<Webhook>())).thenAnswer { invocation ->
            val webhook = invocation.arguments[0] as Webhook
            webhook.copy(id = testWebhookId)
        }

        // Act
        val result = webhookService.createWebhook(
            name = name,
            url = url,
            eventTypes = eventTypes,
            createdBy = createdBy
        )

        // Assert
        assertNotNull(result)
        assertEquals(testWebhookId, result.id)
        assertEquals(name, result.name)
        assertEquals(url, result.url)
        assertEquals(testSecret, result.secret)
        assertEquals(WebhookStatus.ACTIVE, result.status)
        assertEquals(eventTypes, result.eventTypes)
        assertEquals(createdBy, result.createdBy)

        verify(signatureService).generateSecret()
        verify(webhookRepository).save(any<Webhook>())
    }

    @Test
    fun `updateWebhook should update existing webhook fields`() {
        // Arrange
        val existingWebhook = createTestWebhook()
        val newName = "Updated Webhook"
        val newUrl = "https://new-example.com/webhook"
        val newEventTypes = setOf(WebhookEventType.EVENT_CREATED)

        whenever(webhookRepository.findById(testWebhookId))
            .thenReturn(Optional.of(existingWebhook))
        whenever(webhookRepository.save(any<Webhook>())).thenAnswer { invocation ->
            invocation.arguments[0] as Webhook
        }

        // Act
        val result = webhookService.updateWebhook(
            webhookId = testWebhookId,
            name = newName,
            url = newUrl,
            eventTypes = newEventTypes
        )

        // Assert
        assertEquals(newName, result.name)
        assertEquals(newUrl, result.url)
        assertEquals(newEventTypes, result.eventTypes)

        verify(webhookRepository).findById(testWebhookId)
        verify(webhookRepository).save(any<Webhook>())
    }

    @Test
    fun `updateWebhook should throw exception when webhook not found`() {
        // Arrange
        whenever(webhookRepository.findById(testWebhookId))
            .thenReturn(Optional.empty())

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            webhookService.updateWebhook(testWebhookId, name = "New Name")
        }

        verify(webhookRepository).findById(testWebhookId)
        verify(webhookRepository, never()).save(any<Webhook>())
    }

    @Test
    fun `deleteWebhook should delete existing webhook`() {
        // Arrange
        whenever(webhookRepository.existsById(testWebhookId)).thenReturn(true)

        // Act
        webhookService.deleteWebhook(testWebhookId)

        // Assert
        verify(webhookRepository).existsById(testWebhookId)
        verify(webhookRepository).deleteById(testWebhookId)
    }

    @Test
    fun `deleteWebhook should throw exception when webhook not found`() {
        // Arrange
        whenever(webhookRepository.existsById(testWebhookId)).thenReturn(false)

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            webhookService.deleteWebhook(testWebhookId)
        }

        verify(webhookRepository).existsById(testWebhookId)
        verify(webhookRepository, never()).deleteById(any())
    }

    @Test
    fun `getActiveWebhooksForEventType should return matching webhooks`() {
        // Arrange
        val eventType = WebhookEventType.USER_REGISTERED
        val activeWebhooks = listOf(createTestWebhook())

        whenever(webhookRepository.findActiveWebhooksForEventType(WebhookStatus.ACTIVE, eventType))
            .thenReturn(activeWebhooks)

        // Act
        val result = webhookService.getActiveWebhooksForEventType(eventType)

        // Assert
        assertEquals(activeWebhooks, result)
        verify(webhookRepository).findActiveWebhooksForEventType(WebhookStatus.ACTIVE, eventType)
    }

    @Test
    fun `setWebhookStatus should update webhook status`() {
        // Arrange
        val existingWebhook = createTestWebhook()
        val newStatus = WebhookStatus.INACTIVE

        whenever(webhookRepository.findById(testWebhookId))
            .thenReturn(Optional.of(existingWebhook))
        whenever(webhookRepository.save(any<Webhook>())).thenAnswer { invocation ->
            invocation.arguments[0] as Webhook
        }

        // Act
        val result = webhookService.setWebhookStatus(testWebhookId, newStatus)

        // Assert
        assertEquals(newStatus, result.status)

        verify(webhookRepository).findById(testWebhookId)
        verify(webhookRepository).save(any<Webhook>())
    }

    @Test
    fun `regenerateSecret should generate new secret`() {
        // Arrange
        val existingWebhook = createTestWebhook()
        val newSecret = "new-secret-456"

        whenever(webhookRepository.findById(testWebhookId))
            .thenReturn(Optional.of(existingWebhook))
        whenever(signatureService.generateSecret()).thenReturn(newSecret)
        whenever(webhookRepository.save(any<Webhook>())).thenAnswer { invocation ->
            invocation.arguments[0] as Webhook
        }

        // Act
        val result = webhookService.regenerateSecret(testWebhookId)

        // Assert
        assertEquals(newSecret, result.secret)

        verify(webhookRepository).findById(testWebhookId)
        verify(signatureService).generateSecret()
        verify(webhookRepository).save(any<Webhook>())
    }

    @Test
    fun `getWebhookStatistics should return correct statistics`() {
        // Arrange
        val webhook = createTestWebhook().apply {
            totalDeliveries = 100
            successfulDeliveries = 90
            failedDeliveries = 10
        }

        whenever(webhookRepository.findById(testWebhookId))
            .thenReturn(Optional.of(webhook))

        // Act
        val result = webhookService.getWebhookStatistics(testWebhookId)

        // Assert
        assertEquals(100, result.totalDeliveries)
        assertEquals(90, result.successfulDeliveries)
        assertEquals(10, result.failedDeliveries)
        assertEquals(0.9, result.successRate)

        verify(webhookRepository).findById(testWebhookId)
    }

    private fun createTestWebhook(): Webhook {
        return Webhook(
            id = testWebhookId,
            name = "Test Webhook",
            url = "https://example.com/webhook",
            secret = testSecret,
            status = WebhookStatus.ACTIVE,
            eventTypes = mutableSetOf(WebhookEventType.USER_REGISTERED),
            createdBy = "test-user",
            maxRetries = 3,
            timeoutSeconds = 30
        )
    }
}