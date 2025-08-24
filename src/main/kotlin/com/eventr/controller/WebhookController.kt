package com.eventr.controller

import com.eventr.dto.webhook.*
import com.eventr.events.*
import com.eventr.model.webhook.WebhookStatus
import com.eventr.service.webhook.WebhookService
import com.eventr.service.webhook.WebhookDeliveryService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/webhooks")
class WebhookController(
    private val webhookService: WebhookService,
    private val deliveryService: WebhookDeliveryService,
    private val eventPublisher: EventPublisher
) {
    
    private val logger = LoggerFactory.getLogger(WebhookController::class.java)
    
    @PostMapping
    fun createWebhook(
        @RequestBody request: CreateWebhookRequest,
        @RequestHeader("X-User-ID", required = false) userId: String?
    ): ResponseEntity<WebhookResponse> {
        logger.info("Creating webhook: {} for user: {}", request.name, userId)
        
        val webhook = webhookService.createWebhook(
            name = request.name,
            url = request.url,
            eventTypes = request.eventTypes,
            createdBy = userId,
            maxRetries = request.maxRetries,
            timeoutSeconds = request.timeoutSeconds
        )
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(webhook.toResponse())
    }
    
    @GetMapping
    fun getAllWebhooks(
        @RequestHeader("X-User-ID", required = false) userId: String?
    ): ResponseEntity<List<WebhookResponse>> {
        val webhooks = if (userId != null) {
            webhookService.getWebhooksByCreatedBy(userId)
        } else {
            webhookService.getAllWebhooks()
        }
        
        return ResponseEntity.ok(webhooks.map { it.toResponse() })
    }
    
    @GetMapping("/{webhookId}")
    fun getWebhook(@PathVariable webhookId: UUID): ResponseEntity<WebhookResponse> {
        val webhook = webhookService.getWebhookById(webhookId)
            ?: return ResponseEntity.notFound().build()
        
        return ResponseEntity.ok(webhook.toResponse())
    }
    
    @PutMapping("/{webhookId}")
    fun updateWebhook(
        @PathVariable webhookId: UUID,
        @RequestBody request: UpdateWebhookRequest
    ): ResponseEntity<WebhookResponse> {
        logger.info("Updating webhook: {}", webhookId)
        
        val webhook = webhookService.updateWebhook(
            webhookId = webhookId,
            name = request.name,
            url = request.url,
            eventTypes = request.eventTypes,
            status = request.status,
            maxRetries = request.maxRetries,
            timeoutSeconds = request.timeoutSeconds
        )
        
        return ResponseEntity.ok(webhook.toResponse())
    }
    
    @DeleteMapping("/{webhookId}")
    fun deleteWebhook(@PathVariable webhookId: UUID): ResponseEntity<Void> {
        logger.info("Deleting webhook: {}", webhookId)
        webhookService.deleteWebhook(webhookId)
        return ResponseEntity.noContent().build()
    }
    
    @PostMapping("/{webhookId}/activate")
    fun activateWebhook(@PathVariable webhookId: UUID): ResponseEntity<WebhookResponse> {
        val webhook = webhookService.setWebhookStatus(webhookId, WebhookStatus.ACTIVE)
        return ResponseEntity.ok(webhook.toResponse())
    }
    
    @PostMapping("/{webhookId}/deactivate")
    fun deactivateWebhook(@PathVariable webhookId: UUID): ResponseEntity<WebhookResponse> {
        val webhook = webhookService.setWebhookStatus(webhookId, WebhookStatus.INACTIVE)
        return ResponseEntity.ok(webhook.toResponse())
    }
    
    @PostMapping("/{webhookId}/regenerate-secret")
    fun regenerateSecret(@PathVariable webhookId: UUID): ResponseEntity<Map<String, String>> {
        logger.info("Regenerating secret for webhook: {}", webhookId)
        val webhook = webhookService.regenerateSecret(webhookId)
        return ResponseEntity.ok(mapOf("secret" to webhook.secret))
    }
    
    @GetMapping("/{webhookId}/deliveries")
    fun getDeliveries(@PathVariable webhookId: UUID): ResponseEntity<List<WebhookDeliveryResponse>> {
        val deliveries = deliveryService.getDeliveryHistory(webhookId)
        return ResponseEntity.ok(deliveries.map { it.toResponse() })
    }
    
    @PostMapping("/{webhookId}/deliveries/{deliveryId}/retry")
    fun retryDelivery(
        @PathVariable webhookId: UUID,
        @PathVariable deliveryId: UUID
    ): ResponseEntity<WebhookDeliveryResponse> {
        logger.info("Manually retrying delivery: {} for webhook: {}", deliveryId, webhookId)
        val delivery = deliveryService.retryDelivery(deliveryId)
        return ResponseEntity.ok(delivery.toResponse())
    }
    
    @PostMapping("/{webhookId}/test")
    fun testWebhook(
        @PathVariable webhookId: UUID,
        @RequestBody request: TestWebhookRequest
    ): ResponseEntity<Map<String, String>> {
        logger.info("Testing webhook: {} with event type: {}", webhookId, request.eventType)
        
        // Create a test event
        val testEvent = object : BaseDomainEvent(
            eventType = request.eventType,
            aggregateId = UUID.randomUUID(),
            payload = request.testData.ifEmpty { 
                mapOf("test" to true, "message" to "This is a test webhook delivery") 
            }
        ) {}
        
        // Publish the test event
        eventPublisher.publish(testEvent)
        
        return ResponseEntity.ok(mapOf(
            "message" to "Test webhook event published",
            "eventId" to testEvent.eventId.toString()
        ))
    }
    
    @PostMapping("/retry-failed")
    fun retryFailedDeliveries(): ResponseEntity<Map<String, String>> {
        logger.info("Manually triggering retry of failed deliveries")
        deliveryService.retryFailedDeliveries()
        return ResponseEntity.ok(mapOf("message" to "Failed deliveries retry initiated"))
    }
}

// Extension functions for mapping
private fun com.eventr.model.webhook.Webhook.toResponse() = WebhookResponse(
    id = this.id!!,
    name = this.name,
    url = this.url,
    status = this.status,
    eventTypes = this.eventTypes,
    maxRetries = this.maxRetries,
    timeoutSeconds = this.timeoutSeconds,
    totalDeliveries = this.totalDeliveries,
    successfulDeliveries = this.successfulDeliveries,
    failedDeliveries = this.failedDeliveries,
    successRate = this.getSuccessRate(),
    createdAt = this.createdAt.toString(),
    updatedAt = this.updatedAt.toString(),
    lastDeliveryAt = this.lastDeliveryAt?.toString(),
    lastSuccessAt = this.lastSuccessAt?.toString()
)

private fun com.eventr.model.webhook.WebhookDelivery.toResponse() = WebhookDeliveryResponse(
    id = this.id!!,
    webhookId = this.webhook.id!!,
    eventType = this.eventType,
    status = this.status.toString(),
    attemptCount = this.attemptCount,
    maxAttempts = this.maxAttempts,
    responseStatus = this.responseStatus,
    errorMessage = this.errorMessage,
    createdAt = this.createdAt.toString(),
    deliveredAt = this.deliveredAt?.toString(),
    nextRetryAt = this.nextRetryAt?.toString()
)