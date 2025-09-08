package com.eventr.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "calendar_subscriptions")
data class CalendarSubscription(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    
    @Column(name = "user_id", nullable = false)
    val userId: UUID,
    
    @Column(name = "subscription_token", nullable = false, unique = true)
    val token: String,
    
    @Column(name = "subscription_url", nullable = false)
    val url: String,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "expires_at")
    val expiresAt: LocalDateTime? = null,
    
    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,
    
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)