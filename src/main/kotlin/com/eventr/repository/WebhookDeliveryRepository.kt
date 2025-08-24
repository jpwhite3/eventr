package com.eventr.repository

import com.eventr.model.webhook.DeliveryStatus
import com.eventr.model.webhook.WebhookDelivery
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface WebhookDeliveryRepository : JpaRepository<WebhookDelivery, UUID> {
    
    /**
     * Find deliveries that are ready for retry
     */
    @Query("""
        SELECT wd FROM WebhookDelivery wd 
        WHERE wd.status IN (:statuses) 
        AND wd.attemptCount < wd.maxAttempts 
        AND wd.nextRetryAt IS NOT NULL 
        AND wd.nextRetryAt <= :currentTime
        ORDER BY wd.nextRetryAt ASC
    """)
    fun findDeliveriesReadyForRetry(
        @Param("statuses") statuses: List<DeliveryStatus> = listOf(
            DeliveryStatus.PENDING, 
            DeliveryStatus.FAILED, 
            DeliveryStatus.RETRYING
        ),
        @Param("currentTime") currentTime: LocalDateTime = LocalDateTime.now()
    ): List<WebhookDelivery>
    
    /**
     * Find deliveries by webhook ID
     */
    @Query("SELECT wd FROM WebhookDelivery wd WHERE wd.webhook.id = :webhookId ORDER BY wd.createdAt DESC")
    fun findByWebhookId(@Param("webhookId") webhookId: UUID): List<WebhookDelivery>
    
    /**
     * Find deliveries by status
     */
    fun findByStatusOrderByCreatedAtDesc(status: DeliveryStatus): List<WebhookDelivery>
    
    /**
     * Count deliveries by webhook and status
     */
    @Query("SELECT COUNT(wd) FROM WebhookDelivery wd WHERE wd.webhook.id = :webhookId AND wd.status = :status")
    fun countByWebhookIdAndStatus(
        @Param("webhookId") webhookId: UUID,
        @Param("status") status: DeliveryStatus
    ): Long
    
    /**
     * Find recent deliveries for a webhook
     */
    @Query("""
        SELECT wd FROM WebhookDelivery wd 
        WHERE wd.webhook.id = :webhookId 
        AND wd.createdAt >= :since 
        ORDER BY wd.createdAt DESC
    """)
    fun findRecentDeliveriesForWebhook(
        @Param("webhookId") webhookId: UUID,
        @Param("since") since: LocalDateTime
    ): List<WebhookDelivery>
    
    /**
     * Delete old successful deliveries to keep the table manageable
     */
    @Query("""
        DELETE FROM WebhookDelivery wd 
        WHERE wd.status = :status 
        AND wd.deliveredAt < :before
    """)
    fun deleteOldDeliveries(
        @Param("status") status: DeliveryStatus = DeliveryStatus.SUCCESS,
        @Param("before") before: LocalDateTime
    ): Int
}