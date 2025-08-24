package com.eventr.model.webhook

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

enum class WebhookStatus {
    ACTIVE, INACTIVE, SUSPENDED
}

enum class WebhookEventType {
    USER_REGISTERED,
    USER_CANCELLED,
    USER_CHECKED_IN,
    USER_CHECKED_OUT,
    SESSION_CREATED,
    SESSION_UPDATED,
    SESSION_CANCELLED,
    EVENT_CREATED,
    EVENT_UPDATED,
    EVENT_CANCELLED,
    RESOURCE_BOOKED,
    RESOURCE_CANCELLED,
    CONFLICT_DETECTED,
    CONFLICT_RESOLVED
}

@Entity
@Table(name = "webhooks")
data class Webhook(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,
    
    @Column(nullable = false)
    var name: String,
    
    @Column(nullable = false)
    var url: String,
    
    @Column(nullable = false)
    var secret: String,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: WebhookStatus = WebhookStatus.ACTIVE,
    
    @ElementCollection(targetClass = WebhookEventType::class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "webhook_events", joinColumns = [JoinColumn(name = "webhook_id")])
    @Column(name = "event_type")
    var eventTypes: MutableSet<WebhookEventType> = mutableSetOf(),
    
    @Column(name = "created_by")
    var createdBy: String? = null,
    
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    
    // Retry configuration
    @Column(name = "max_retries")
    var maxRetries: Int = 3,
    
    @Column(name = "timeout_seconds")
    var timeoutSeconds: Int = 30,
    
    // Statistics
    @Column(name = "total_deliveries")
    var totalDeliveries: Long = 0,
    
    @Column(name = "successful_deliveries")
    var successfulDeliveries: Long = 0,
    
    @Column(name = "failed_deliveries")
    var failedDeliveries: Long = 0,
    
    @Column(name = "last_delivery_at")
    var lastDeliveryAt: LocalDateTime? = null,
    
    @Column(name = "last_success_at")
    var lastSuccessAt: LocalDateTime? = null
) {
    fun isActive(): Boolean = status == WebhookStatus.ACTIVE
    
    fun supportsEventType(eventType: WebhookEventType): Boolean = eventTypes.contains(eventType)
    
    fun updateStatistics(success: Boolean) {
        totalDeliveries++
        if (success) {
            successfulDeliveries++
            lastSuccessAt = LocalDateTime.now()
        } else {
            failedDeliveries++
        }
        lastDeliveryAt = LocalDateTime.now()
        updatedAt = LocalDateTime.now()
    }
    
    fun getSuccessRate(): Double {
        return if (totalDeliveries > 0) {
            successfulDeliveries.toDouble() / totalDeliveries.toDouble()
        } else {
            0.0
        }
    }
}