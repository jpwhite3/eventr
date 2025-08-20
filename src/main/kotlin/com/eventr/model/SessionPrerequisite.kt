package com.eventr.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

enum class PrerequisiteType {
    SESSION_ATTENDANCE, // Must have attended another session
    SESSION_REGISTRATION, // Must be registered for another session
    PROFILE_REQUIREMENT, // Must have certain profile attributes
    CUSTOM_RULE // Custom business logic
}

enum class PrerequisiteOperator {
    AND, OR, NOT
}

@Entity
data class SessionPrerequisite(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,
    
    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    var session: Session? = null,
    
    @Enumerated(EnumType.STRING)
    var type: PrerequisiteType = PrerequisiteType.SESSION_ATTENDANCE,
    
    // For session prerequisites
    @ManyToOne
    @JoinColumn(name = "prerequisite_session_id", nullable = true)
    var prerequisiteSession: Session? = null,
    
    // Logical grouping and operators
    var groupId: String? = null, // Group related prerequisites
    
    @Enumerated(EnumType.STRING)
    var operator: PrerequisiteOperator = PrerequisiteOperator.AND,
    
    var priority: Int = 0, // Order of evaluation
    var isRequired: Boolean = true,
    
    // Flexible rule definition
    var ruleDefinition: String? = null, // JSON describing the rule
    var errorMessage: String? = null, // Custom error message
    
    // Grace periods and flexibility
    var allowGracePeriod: Boolean = false,
    var gracePeriodHours: Int = 0,
    
    // Bypass options for administrators
    var allowAdminOverride: Boolean = true,
    
    var isActive: Boolean = true,
    var createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

@Entity  
data class SessionDependency(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,
    
    @ManyToOne
    @JoinColumn(name = "parent_session_id", nullable = false)
    var parentSession: Session? = null,
    
    @ManyToOne
    @JoinColumn(name = "dependent_session_id", nullable = false) 
    var dependentSession: Session? = null,
    
    @Enumerated(EnumType.STRING)
    var dependencyType: DependencyType = DependencyType.SEQUENCE,
    
    var isStrict: Boolean = true, // Strict enforcement vs recommendation
    var timingGapMinutes: Int = 0, // Minimum gap between sessions
    
    var description: String? = null,
    
    var createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class DependencyType {
    SEQUENCE, // Sessions must be taken in order
    PARALLEL, // Sessions should be taken together  
    EXCLUSIVE, // Cannot register for both sessions
    PREREQUISITE // One session enables the other
}