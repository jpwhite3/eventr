package com.eventr.model.webhook

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

enum class DeliveryStatus {
    PENDING,
    SUCCESS,
    FAILED,
    RETRYING,
    EXHAUSTED
}

@Entity
@Table(name = "webhook_deliveries")
data class WebhookDelivery(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "webhook_id", nullable = false)
    var webhook: Webhook,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var eventType: WebhookEventType,
    
    @Column(nullable = false, columnDefinition = "TEXT")
    var payload: String,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: DeliveryStatus = DeliveryStatus.PENDING,
    
    @Column(name = "attempt_count")
    var attemptCount: Int = 0,
    
    @Column(name = "max_attempts")
    var maxAttempts: Int = 3,
    
    @Column(name = "response_status")
    var responseStatus: Int? = null,
    
    @Column(name = "response_body", columnDefinition = "TEXT")
    var responseBody: String? = null,
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    var errorMessage: String? = null,
    
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "scheduled_at")
    var scheduledAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "delivered_at")
    var deliveredAt: LocalDateTime? = null,
    
    @Column(name = "next_retry_at")
    var nextRetryAt: LocalDateTime? = null
) {
    fun canRetry(): Boolean {
        return status in listOf(DeliveryStatus.PENDING, DeliveryStatus.FAILED, DeliveryStatus.RETRYING) &&
                attemptCount < maxAttempts
    }
    
    fun markAsDelivered(responseStatus: Int, responseBody: String) {
        this.status = DeliveryStatus.SUCCESS
        this.responseStatus = responseStatus
        this.responseBody = responseBody
        this.deliveredAt = LocalDateTime.now()
        this.nextRetryAt = null
    }
    
    fun markAsFailed(responseStatus: Int?, responseBody: String?, errorMessage: String) {
        this.attemptCount++
        this.responseStatus = responseStatus
        this.responseBody = responseBody
        this.errorMessage = errorMessage
        
        if (canRetry()) {
            this.status = DeliveryStatus.RETRYING
            // Exponential backoff: 1min, 5min, 15min
            val delayMinutes = when (attemptCount) {
                1 -> 1L
                2 -> 5L
                3 -> 15L
                else -> 60L
            }
            this.nextRetryAt = LocalDateTime.now().plusMinutes(delayMinutes)
        } else {
            this.status = DeliveryStatus.EXHAUSTED
            this.nextRetryAt = null
        }
    }
    
    fun isReadyForRetry(): Boolean {
        return canRetry() && 
                nextRetryAt != null && 
                nextRetryAt!!.isBefore(LocalDateTime.now())
    }
}