package com.eventr.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

enum class ConflictType {
    TIME_OVERLAP, // Sessions overlap in time
    RESOURCE_CONFLICT, // Same resource double-booked
    CAPACITY_EXCEEDED, // Session over capacity
    PREREQUISITE_VIOLATION, // Prerequisites not met
    DEPENDENCY_VIOLATION, // Session dependencies broken
    STAFF_CONFLICT, // Staff member double-booked
    LOCATION_CONFLICT, // Same location double-booked
    USER_CONFLICT // User registered for conflicting sessions
}

enum class ConflictSeverity {
    INFO, WARNING, ERROR, CRITICAL
}

enum class ConflictResolutionStatus {
    UNRESOLVED, ACKNOWLEDGED, RESOLVED, IGNORED, AUTO_RESOLVED
}

@Entity
data class ScheduleConflict(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,
    
    @Enumerated(EnumType.STRING)
    var type: ConflictType = ConflictType.TIME_OVERLAP,
    
    @Enumerated(EnumType.STRING)
    var severity: ConflictSeverity = ConflictSeverity.WARNING,
    
    var title: String,
    var description: String,
    
    // Related entities
    @ManyToOne
    @JoinColumn(name = "primary_session_id", nullable = true)
    var primarySession: Session? = null,
    
    @ManyToOne
    @JoinColumn(name = "secondary_session_id", nullable = true) 
    var secondarySession: Session? = null,
    
    @ManyToOne
    @JoinColumn(name = "resource_id", nullable = true)
    var resource: Resource? = null,
    
    @ManyToOne
    @JoinColumn(name = "registration_id", nullable = true)
    var registration: Registration? = null,
    
    // Conflict details
    var conflictStart: LocalDateTime? = null,
    var conflictEnd: LocalDateTime? = null,
    var affectedCount: Int = 0, // Number of people/resources affected
    
    // Resolution
    @Enumerated(EnumType.STRING)
    var resolutionStatus: ConflictResolutionStatus = ConflictResolutionStatus.UNRESOLVED,
    
    var resolutionNotes: String? = null,
    var resolvedBy: String? = null,
    var resolvedAt: LocalDateTime? = null,
    
    // Auto-resolution
    var canAutoResolve: Boolean = false,
    var autoResolutionStrategy: String? = null,
    
    // Tracking
    var detectedAt: LocalDateTime = LocalDateTime.now(),
    var lastCheckedAt: LocalDateTime = LocalDateTime.now(),
    
    // Notifications
    var notificationsSent: Boolean = false,
    var notificationRecipients: String? = null, // Comma-separated emails
    
    var isActive: Boolean = true,
    var createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

@Entity
data class ConflictResolution(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,
    
    @ManyToOne
    @JoinColumn(name = "conflict_id", nullable = false)
    var conflict: ScheduleConflict? = null,
    
    var resolutionType: String, // e.g., "TIME_CHANGE", "RESOURCE_SWAP", "CAPACITY_INCREASE"
    var description: String,
    
    // Changes made
    var changesSummary: String? = null, // JSON describing changes
    var beforeState: String? = null, // JSON snapshot before resolution
    var afterState: String? = null, // JSON snapshot after resolution
    
    var implementedBy: String,
    var implementedAt: LocalDateTime = LocalDateTime.now(),
    
    // Impact assessment
    var affectedSessions: Int = 0,
    var affectedRegistrations: Int = 0,
    var affectedResources: Int = 0,
    
    var notes: String? = null,
    
    var createdAt: LocalDateTime = LocalDateTime.now()
)