package com.eventr.repository

import com.eventr.model.webhook.Webhook
import com.eventr.model.webhook.WebhookEventType
import com.eventr.model.webhook.WebhookStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface WebhookRepository : JpaRepository<Webhook, UUID> {
    
    /**
     * Find all active webhooks that support a specific event type
     */
    @Query("SELECT w FROM Webhook w JOIN w.eventTypes et WHERE w.status = :status AND et = :eventType")
    fun findActiveWebhooksForEventType(
        @Param("status") status: WebhookStatus = WebhookStatus.ACTIVE,
        @Param("eventType") eventType: WebhookEventType
    ): List<Webhook>
    
    /**
     * Find webhooks by status
     */
    fun findByStatus(status: WebhookStatus): List<Webhook>
    
    /**
     * Find webhooks created by a specific user
     */
    fun findByCreatedBy(createdBy: String): List<Webhook>
    
    /**
     * Find webhooks by name (case-insensitive)
     */
    fun findByNameContainingIgnoreCase(name: String): List<Webhook>
    
    /**
     * Count active webhooks
     */
    fun countByStatus(status: WebhookStatus): Long
}