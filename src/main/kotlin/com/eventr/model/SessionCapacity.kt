package com.eventr.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

enum class CapacityType {
    FIXED, DYNAMIC, UNLIMITED
}

enum class WaitlistStrategy {
    NONE, FIFO, PRIORITY_BASED, REGISTRATION_TIME
}

@Entity
data class SessionCapacity(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,
    
    @OneToOne
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    var session: Session? = null,
    
    @Enumerated(EnumType.STRING)
    var capacityType: CapacityType = CapacityType.FIXED,
    
    var maximumCapacity: Int = 0,
    var minimumCapacity: Int = 0, // For session viability
    var currentRegistrations: Int = 0,
    var availableSpots: Int = 0,
    
    // Waitlist management
    var enableWaitlist: Boolean = true,
    var waitlistCapacity: Int? = null, // null means unlimited waitlist
    var currentWaitlistCount: Int = 0,
    
    @Enumerated(EnumType.STRING)
    var waitlistStrategy: WaitlistStrategy = WaitlistStrategy.FIFO,
    
    // Dynamic capacity settings
    var allowOverbooking: Boolean = false,
    var overbookingPercentage: Double = 0.0, // e.g., 110% = 10% overbooking
    var autoPromoteFromWaitlist: Boolean = true,
    
    // Capacity thresholds for notifications
    var lowCapacityThreshold: Int = 5, // Alert when few spots remain
    var highDemandThreshold: Double = 0.8, // Alert when 80% full
    
    // Capacity history tracking
    var lastCapacityUpdate: LocalDateTime = LocalDateTime.now(),
    var capacityHistory: String? = null, // JSON array of capacity changes
    
    var createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now()
)