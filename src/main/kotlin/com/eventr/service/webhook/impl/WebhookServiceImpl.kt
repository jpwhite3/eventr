package com.eventr.service.webhook.impl

import com.eventr.model.webhook.Webhook
import com.eventr.model.webhook.WebhookEventType
import com.eventr.model.webhook.WebhookStatus
import com.eventr.repository.WebhookRepository
import com.eventr.service.webhook.WebhookService
import com.eventr.service.webhook.WebhookSignatureService
import com.eventr.service.webhook.WebhookStatistics
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional
class WebhookServiceImpl(
    private val webhookRepository: WebhookRepository,
    private val signatureService: WebhookSignatureService
) : WebhookService {
    
    private val logger = LoggerFactory.getLogger(WebhookServiceImpl::class.java)
    
    override fun createWebhook(
        name: String,
        url: String,
        eventTypes: Set<WebhookEventType>,
        createdBy: String?,
        maxRetries: Int,
        timeoutSeconds: Int
    ): Webhook {
        logger.info("Creating webhook: {} for URL: {}", name, url)
        
        val webhook = Webhook(
            name = name,
            url = url,
            secret = signatureService.generateSecret(),
            status = WebhookStatus.ACTIVE,
            eventTypes = eventTypes.toMutableSet(),
            createdBy = createdBy,
            maxRetries = maxRetries,
            timeoutSeconds = timeoutSeconds,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        val saved = webhookRepository.save(webhook)
        logger.info("Successfully created webhook with ID: {}", saved.id)
        return saved
    }
    
    override fun updateWebhook(
        webhookId: UUID,
        name: String?,
        url: String?,
        eventTypes: Set<WebhookEventType>?,
        status: WebhookStatus?,
        maxRetries: Int?,
        timeoutSeconds: Int?
    ): Webhook {
        logger.info("Updating webhook: {}", webhookId)
        
        val webhook = getWebhookById(webhookId) 
            ?: throw IllegalArgumentException("Webhook not found: $webhookId")
        
        name?.let { webhook.name = it }
        url?.let { webhook.url = it }
        eventTypes?.let { webhook.eventTypes = it.toMutableSet() }
        status?.let { webhook.status = it }
        maxRetries?.let { webhook.maxRetries = it }
        timeoutSeconds?.let { webhook.timeoutSeconds = it }
        webhook.updatedAt = LocalDateTime.now()
        
        val updated = webhookRepository.save(webhook)
        logger.info("Successfully updated webhook: {}", webhookId)
        return updated
    }
    
    override fun deleteWebhook(webhookId: UUID) {
        logger.info("Deleting webhook: {}", webhookId)
        
        if (!webhookRepository.existsById(webhookId)) {
            throw IllegalArgumentException("Webhook not found: $webhookId")
        }
        
        webhookRepository.deleteById(webhookId)
        logger.info("Successfully deleted webhook: {}", webhookId)
    }
    
    @Transactional(readOnly = true)
    override fun getWebhookById(webhookId: UUID): Webhook? {
        return webhookRepository.findById(webhookId).orElse(null)
    }
    
    @Transactional(readOnly = true)
    override fun getAllWebhooks(): List<Webhook> {
        return webhookRepository.findAll()
    }
    
    @Transactional(readOnly = true)
    override fun getWebhooksByStatus(status: WebhookStatus): List<Webhook> {
        return webhookRepository.findByStatus(status)
    }
    
    @Transactional(readOnly = true)
    override fun getWebhooksByCreatedBy(createdBy: String): List<Webhook> {
        return webhookRepository.findByCreatedBy(createdBy)
    }
    
    @Transactional(readOnly = true)
    override fun getActiveWebhooksForEventType(eventType: WebhookEventType): List<Webhook> {
        return webhookRepository.findActiveWebhooksForEventType(WebhookStatus.ACTIVE, eventType)
    }
    
    override fun setWebhookStatus(webhookId: UUID, status: WebhookStatus): Webhook {
        logger.info("Setting webhook {} status to: {}", webhookId, status)
        
        val webhook = getWebhookById(webhookId) 
            ?: throw IllegalArgumentException("Webhook not found: $webhookId")
        
        webhook.status = status
        webhook.updatedAt = LocalDateTime.now()
        
        val updated = webhookRepository.save(webhook)
        logger.info("Successfully updated webhook {} status to: {}", webhookId, status)
        return updated
    }
    
    @Transactional(readOnly = true)
    override fun getWebhookStatistics(webhookId: UUID): WebhookStatistics {
        val webhook = getWebhookById(webhookId) 
            ?: throw IllegalArgumentException("Webhook not found: $webhookId")
        
        return WebhookStatistics(
            totalDeliveries = webhook.totalDeliveries,
            successfulDeliveries = webhook.successfulDeliveries,
            failedDeliveries = webhook.failedDeliveries,
            successRate = webhook.getSuccessRate(),
            lastDeliveryAt = webhook.lastDeliveryAt?.toString(),
            lastSuccessAt = webhook.lastSuccessAt?.toString()
        )
    }
    
    override fun regenerateSecret(webhookId: UUID): Webhook {
        logger.info("Regenerating secret for webhook: {}", webhookId)
        
        val webhook = getWebhookById(webhookId) 
            ?: throw IllegalArgumentException("Webhook not found: $webhookId")
        
        webhook.secret = signatureService.generateSecret()
        webhook.updatedAt = LocalDateTime.now()
        
        val updated = webhookRepository.save(webhook)
        logger.info("Successfully regenerated secret for webhook: {}", webhookId)
        return updated
    }
}