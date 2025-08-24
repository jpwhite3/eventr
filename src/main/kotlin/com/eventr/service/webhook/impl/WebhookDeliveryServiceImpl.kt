package com.eventr.service.webhook.impl

import com.eventr.events.DomainEvent
import com.eventr.model.webhook.DeliveryStatus
import com.eventr.model.webhook.WebhookDelivery
import com.eventr.repository.WebhookDeliveryRepository
import com.eventr.service.webhook.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional
class WebhookDeliveryServiceImpl(
    private val webhookService: WebhookService,
    private val deliveryRepository: WebhookDeliveryRepository,
    private val httpClient: HttpClient,
    private val signatureService: WebhookSignatureService,
    private val objectMapper: ObjectMapper
) : WebhookDeliveryService {
    
    private val logger = LoggerFactory.getLogger(WebhookDeliveryServiceImpl::class.java)
    
    override fun deliverEvent(event: DomainEvent) {
        logger.info("Processing event delivery: {} with ID: {}", event.eventType, event.eventId)
        
        val webhooks = webhookService.getActiveWebhooksForEventType(event.eventType)
        if (webhooks.isEmpty()) {
            logger.debug("No active webhooks found for event type: {}", event.eventType)
            return
        }
        
        logger.info("Found {} webhook(s) for event type: {}", webhooks.size, event.eventType)
        
        webhooks.forEach { webhook ->
            try {
                val payload = createWebhookPayload(event)
                val delivery = createWebhookDelivery(webhook, event, payload)
                
                deliverWebhook(delivery)
                
            } catch (e: Exception) {
                logger.error("Failed to create delivery for webhook: {} and event: {}", 
                    webhook.id, event.eventId, e)
            }
        }
    }
    
    override fun retryFailedDeliveries() {
        logger.info("Processing webhook delivery retries")
        
        val failedDeliveries = deliveryRepository.findDeliveriesReadyForRetry()
        logger.info("Found {} deliveries ready for retry", failedDeliveries.size)
        
        failedDeliveries.forEach { delivery ->
            try {
                deliverWebhook(delivery)
            } catch (e: Exception) {
                logger.error("Failed to retry delivery: {}", delivery.id, e)
            }
        }
    }
    
    @Transactional(readOnly = true)
    override fun getDeliveryHistory(webhookId: UUID): List<WebhookDelivery> {
        return deliveryRepository.findByWebhookId(webhookId)
    }
    
    @Transactional(readOnly = true)
    override fun getDeliveryStatistics(webhookId: UUID): DeliveryStatistics {
        val totalDeliveries = deliveryRepository.countByWebhookIdAndStatus(webhookId, DeliveryStatus.SUCCESS) +
                deliveryRepository.countByWebhookIdAndStatus(webhookId, DeliveryStatus.FAILED) +
                deliveryRepository.countByWebhookIdAndStatus(webhookId, DeliveryStatus.EXHAUSTED)
        
        val successfulDeliveries = deliveryRepository.countByWebhookIdAndStatus(webhookId, DeliveryStatus.SUCCESS)
        val failedDeliveries = deliveryRepository.countByWebhookIdAndStatus(webhookId, DeliveryStatus.FAILED) +
                deliveryRepository.countByWebhookIdAndStatus(webhookId, DeliveryStatus.EXHAUSTED)
        val pendingDeliveries = deliveryRepository.countByWebhookIdAndStatus(webhookId, DeliveryStatus.PENDING) +
                deliveryRepository.countByWebhookIdAndStatus(webhookId, DeliveryStatus.RETRYING)
        
        val successRate = if (totalDeliveries > 0) {
            successfulDeliveries.toDouble() / totalDeliveries.toDouble()
        } else {
            0.0
        }
        
        return DeliveryStatistics(
            totalDeliveries = totalDeliveries,
            successfulDeliveries = successfulDeliveries,
            failedDeliveries = failedDeliveries,
            pendingDeliveries = pendingDeliveries,
            successRate = successRate
        )
    }
    
    override fun retryDelivery(deliveryId: UUID): WebhookDelivery {
        logger.info("Manually retrying delivery: {}", deliveryId)
        
        val delivery = deliveryRepository.findById(deliveryId)
            .orElseThrow { IllegalArgumentException("Delivery not found: $deliveryId") }
        
        if (!delivery.canRetry()) {
            throw IllegalStateException("Delivery cannot be retried: $deliveryId")
        }
        
        return deliverWebhook(delivery)
    }
    
    private fun createWebhookPayload(event: DomainEvent): String {
        val payload = mapOf(
            "eventId" to event.eventId.toString(),
            "eventType" to event.eventType.toString(),
            "occurredAt" to event.occurredAt.toString(),
            "aggregateId" to event.aggregateId.toString(),
            "data" to event.payload
        )
        
        return objectMapper.writeValueAsString(payload)
    }
    
    private fun createWebhookDelivery(
        webhook: com.eventr.model.webhook.Webhook,
        event: DomainEvent,
        payload: String
    ): WebhookDelivery {
        return WebhookDelivery(
            webhook = webhook,
            eventType = event.eventType,
            payload = payload,
            maxAttempts = webhook.maxRetries,
            scheduledAt = LocalDateTime.now()
        ).also { delivery ->
            deliveryRepository.save(delivery)
        }
    }
    
    private fun deliverWebhook(delivery: WebhookDelivery): WebhookDelivery {
        logger.debug("Delivering webhook: {} to URL: {}", delivery.id, delivery.webhook.url)
        
        try {
            val signature = signatureService.generateSignature(delivery.payload, delivery.webhook.secret)
            val headers = mapOf(
                "X-Webhook-Signature" to signature,
                "X-Webhook-Event-Type" to delivery.eventType.toString(),
                "X-Webhook-Delivery-ID" to delivery.id.toString(),
                "User-Agent" to "Eventr-Webhooks/1.0"
            )
            
            val response = httpClient.post(
                url = delivery.webhook.url,
                payload = delivery.payload,
                headers = headers,
                timeoutSeconds = delivery.webhook.timeoutSeconds
            )
            
            if (response.isSuccess) {
                delivery.markAsDelivered(response.statusCode, response.body)
                delivery.webhook.updateStatistics(success = true)
                logger.info("Successfully delivered webhook: {} to URL: {}", 
                    delivery.id, delivery.webhook.url)
            } else {
                delivery.markAsFailed(
                    responseStatus = response.statusCode,
                    responseBody = response.body,
                    errorMessage = "HTTP ${response.statusCode}: ${response.body}"
                )
                delivery.webhook.updateStatistics(success = false)
                logger.warn("Webhook delivery failed: {} to URL: {} with status: {}", 
                    delivery.id, delivery.webhook.url, response.statusCode)
            }
            
        } catch (e: Exception) {
            delivery.markAsFailed(
                responseStatus = null,
                responseBody = null,
                errorMessage = e.message ?: "Unknown error"
            )
            delivery.webhook.updateStatistics(success = false)
            logger.error("Webhook delivery failed: {} to URL: {}", 
                delivery.id, delivery.webhook.url, e)
        }
        
        return deliveryRepository.save(delivery)
    }
}