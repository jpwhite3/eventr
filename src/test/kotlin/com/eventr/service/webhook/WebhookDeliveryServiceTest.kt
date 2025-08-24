package com.eventr.service.webhook

import com.eventr.events.UserRegisteredEvent
import com.eventr.model.webhook.*
import com.eventr.repository.WebhookDeliveryRepository
import com.eventr.service.webhook.impl.WebhookDeliveryServiceImpl
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import java.util.*
import org.junit.jupiter.api.Assertions.*

class WebhookDeliveryServiceTest {

    @Mock
    private lateinit var webhookService: WebhookService

    @Mock
    private lateinit var deliveryRepository: WebhookDeliveryRepository

    @Mock
    private lateinit var httpClient: HttpClient

    @Mock
    private lateinit var signatureService: WebhookSignatureService

    private lateinit var objectMapper: ObjectMapper

    private lateinit var deliveryService: WebhookDeliveryService

    private val testWebhookId = UUID.randomUUID()
    private val testDeliveryId = UUID.randomUUID()
    private val testEventId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        objectMapper = ObjectMapper()
        deliveryService = WebhookDeliveryServiceImpl(
            webhookService, deliveryRepository, httpClient, signatureService, objectMapper
        )
    }

    @Test
    fun `deliverEvent should create and deliver webhooks for matching event type`() {
        // Arrange
        val event = createTestUserRegisteredEvent()
        val webhook = createTestWebhook()
        val signature = "sha256=test-signature"

        whenever(webhookService.getActiveWebhooksForEventType(WebhookEventType.USER_REGISTERED))
            .thenReturn(listOf(webhook))
        whenever(signatureService.generateSignature(any(), eq(webhook.secret)))
            .thenReturn(signature)
        whenever(httpClient.post(any(), any(), any(), any()))
            .thenReturn(HttpResponse(200, "OK", responseTimeMs = 100))
        whenever(deliveryRepository.save(any<WebhookDelivery>())).thenAnswer { invocation ->
            val delivery = invocation.arguments[0] as WebhookDelivery
            delivery.copy(id = testDeliveryId)
        }

        // Act
        deliveryService.deliverEvent(event)

        // Assert
        verify(webhookService).getActiveWebhooksForEventType(WebhookEventType.USER_REGISTERED)
        verify(signatureService).generateSignature(any(), eq(webhook.secret))
        verify(httpClient).post(
            eq(webhook.url),
            any(),
            argThat { headers ->
                headers["X-Webhook-Signature"] == signature &&
                headers["X-Webhook-Event-Type"] == "USER_REGISTERED"
            },
            eq(webhook.timeoutSeconds)
        )
        verify(deliveryRepository, times(2)).save(any<WebhookDelivery>()) // Create + update
    }

    @Test
    fun `deliverEvent should handle no webhooks gracefully`() {
        // Arrange
        val event = createTestUserRegisteredEvent()

        whenever(webhookService.getActiveWebhooksForEventType(WebhookEventType.USER_REGISTERED))
            .thenReturn(emptyList())

        // Act
        deliveryService.deliverEvent(event)

        // Assert
        verify(webhookService).getActiveWebhooksForEventType(WebhookEventType.USER_REGISTERED)
        verify(httpClient, never()).post(any(), any(), any(), any())
        verify(deliveryRepository, never()).save(any<WebhookDelivery>())
    }

    @Test
    fun `deliverEvent should handle HTTP failures and mark delivery as failed`() {
        // Arrange
        val event = createTestUserRegisteredEvent()
        val webhook = createTestWebhook()
        val signature = "sha256=test-signature"

        whenever(webhookService.getActiveWebhooksForEventType(WebhookEventType.USER_REGISTERED))
            .thenReturn(listOf(webhook))
        whenever(signatureService.generateSignature(any(), eq(webhook.secret)))
            .thenReturn(signature)
        whenever(httpClient.post(any(), any(), any(), any()))
            .thenReturn(HttpResponse(500, "Internal Server Error", responseTimeMs = 100))
        whenever(deliveryRepository.save(any<WebhookDelivery>())).thenAnswer { invocation ->
            val delivery = invocation.arguments[0] as WebhookDelivery
            delivery.copy(id = testDeliveryId)
        }

        // Act
        deliveryService.deliverEvent(event)

        // Assert
        verify(deliveryRepository, times(2)).save(argThat<WebhookDelivery> { delivery ->
            // Check that the final saved delivery is marked as failed
            delivery.status == DeliveryStatus.RETRYING || delivery.status == DeliveryStatus.FAILED
        })
    }

    @Test
    fun `retryFailedDeliveries should process ready retries`() {
        // Arrange
        val webhook = createTestWebhook()
        val failedDelivery = createTestDelivery(webhook, DeliveryStatus.RETRYING)
        
        whenever(deliveryRepository.findDeliveriesReadyForRetry(any(), any()))
            .thenReturn(listOf(failedDelivery))
        whenever(signatureService.generateSignature(any(), any()))
            .thenReturn("sha256=retry-signature")
        whenever(httpClient.post(any(), any(), any(), any()))
            .thenReturn(HttpResponse(200, "OK", responseTimeMs = 50))
        whenever(deliveryRepository.save(any<WebhookDelivery>())).thenAnswer { invocation ->
            invocation.arguments[0] as WebhookDelivery
        }

        // Act
        deliveryService.retryFailedDeliveries()

        // Assert
        verify(deliveryRepository).findDeliveriesReadyForRetry(any(), any())
        verify(httpClient).post(any(), any(), any(), any())
        verify(deliveryRepository).save(any<WebhookDelivery>())
    }

    @Test
    fun `getDeliveryStatistics should return correct statistics`() {
        // Arrange
        whenever(deliveryRepository.countByWebhookIdAndStatus(testWebhookId, DeliveryStatus.SUCCESS))
            .thenReturn(90L)
        whenever(deliveryRepository.countByWebhookIdAndStatus(testWebhookId, DeliveryStatus.FAILED))
            .thenReturn(8L)
        whenever(deliveryRepository.countByWebhookIdAndStatus(testWebhookId, DeliveryStatus.EXHAUSTED))
            .thenReturn(2L)
        whenever(deliveryRepository.countByWebhookIdAndStatus(testWebhookId, DeliveryStatus.PENDING))
            .thenReturn(5L)
        whenever(deliveryRepository.countByWebhookIdAndStatus(testWebhookId, DeliveryStatus.RETRYING))
            .thenReturn(3L)

        // Act
        val statistics = deliveryService.getDeliveryStatistics(testWebhookId)

        // Assert
        assertEquals(100L, statistics.totalDeliveries) // 90 + 8 + 2
        assertEquals(90L, statistics.successfulDeliveries)
        assertEquals(10L, statistics.failedDeliveries) // 8 + 2
        assertEquals(8L, statistics.pendingDeliveries) // 5 + 3
        assertEquals(0.9, statistics.successRate)
    }

    @Test
    fun `retryDelivery should retry specific delivery`() {
        // Arrange
        val webhook = createTestWebhook()
        val delivery = createTestDelivery(webhook, DeliveryStatus.FAILED)
        
        whenever(deliveryRepository.findById(testDeliveryId))
            .thenReturn(Optional.of(delivery))
        whenever(signatureService.generateSignature(any(), any()))
            .thenReturn("sha256=manual-retry-signature")
        whenever(httpClient.post(any(), any(), any(), any()))
            .thenReturn(HttpResponse(200, "OK", responseTimeMs = 75))
        whenever(deliveryRepository.save(any<WebhookDelivery>())).thenAnswer { invocation ->
            invocation.arguments[0] as WebhookDelivery
        }

        // Act
        val result = deliveryService.retryDelivery(testDeliveryId)

        // Assert
        assertNotNull(result)
        verify(deliveryRepository).findById(testDeliveryId)
        verify(httpClient).post(any(), any(), any(), any())
        verify(deliveryRepository).save(any<WebhookDelivery>())
    }

    private fun createTestUserRegisteredEvent(): UserRegisteredEvent {
        return UserRegisteredEvent(
            aggregateId = UUID.randomUUID(),
            relatedEventId = testEventId,
            userEmail = "test@example.com",
            userName = "Test User",
            registrationStatus = "REGISTERED"
        )
    }

    private fun createTestWebhook(): Webhook {
        return Webhook(
            id = testWebhookId,
            name = "Test Webhook",
            url = "https://example.com/webhook",
            secret = "test-secret",
            status = WebhookStatus.ACTIVE,
            eventTypes = mutableSetOf(WebhookEventType.USER_REGISTERED),
            maxRetries = 3,
            timeoutSeconds = 30
        )
    }

    private fun createTestDelivery(
        webhook: Webhook,
        status: DeliveryStatus = DeliveryStatus.PENDING
    ): WebhookDelivery {
        return WebhookDelivery(
            id = testDeliveryId,
            webhook = webhook,
            eventType = WebhookEventType.USER_REGISTERED,
            payload = """{"test": "payload"}""",
            status = status,
            attemptCount = if (status == DeliveryStatus.FAILED || status == DeliveryStatus.RETRYING) 1 else 0,
            maxAttempts = 3
        )
    }
}