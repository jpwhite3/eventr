package com.eventr.dto

import com.eventr.model.DependencyType
import com.eventr.model.PrerequisiteOperator
import com.eventr.model.PrerequisiteType
import java.time.LocalDateTime
import java.util.*

data class SessionPrerequisiteDto(
    var id: UUID? = null,
    var sessionId: UUID? = null,
    var sessionTitle: String? = null,
    var type: PrerequisiteType = PrerequisiteType.SESSION_ATTENDANCE,
    var prerequisiteSessionId: UUID? = null,
    var prerequisiteSessionTitle: String? = null,
    var groupId: String? = null,
    var operator: PrerequisiteOperator = PrerequisiteOperator.AND,
    var priority: Int = 0,
    var isRequired: Boolean = true,
    var ruleDefinition: String? = null,
    var errorMessage: String? = null,
    var allowGracePeriod: Boolean = false,
    var gracePeriodHours: Int = 0,
    var allowAdminOverride: Boolean = true,
    var isActive: Boolean = true
)

data class PrerequisiteCreateDto(
    var sessionId: UUID,
    var type: PrerequisiteType,
    var prerequisiteSessionId: UUID? = null,
    var groupId: String? = null,
    var operator: PrerequisiteOperator = PrerequisiteOperator.AND,
    var priority: Int = 0,
    var isRequired: Boolean = true,
    var ruleDefinition: String? = null,
    var errorMessage: String? = null,
    var allowGracePeriod: Boolean = false,
    var gracePeriodHours: Int = 0,
    var allowAdminOverride: Boolean = true
)

data class SessionDependencyDto(
    var id: UUID? = null,
    var parentSessionId: UUID,
    var parentSessionTitle: String? = null,
    var dependentSessionId: UUID,
    var dependentSessionTitle: String? = null,
    var dependencyType: DependencyType = DependencyType.SEQUENCE,
    var isStrict: Boolean = true,
    var timingGapMinutes: Int = 0,
    var description: String? = null
)

data class DependencyCreateDto(
    var parentSessionId: UUID,
    var dependentSessionId: UUID,
    var dependencyType: DependencyType = DependencyType.SEQUENCE,
    var isStrict: Boolean = true,
    var timingGapMinutes: Int = 0,
    var description: String? = null
)

data class PrerequisiteValidationDto(
    var sessionId: UUID,
    var registrationId: UUID,
    var validationResults: List<PrerequisiteValidationResultDto> = emptyList(),
    var canRegister: Boolean = false,
    var overallStatus: String = "PENDING", // PASSED, FAILED, PENDING
    var failureReasons: List<String> = emptyList(),
    var canAdminOverride: Boolean = false
)

data class PrerequisiteValidationResultDto(
    var prerequisiteId: UUID,
    var type: PrerequisiteType,
    var isPassed: Boolean = false,
    var isRequired: Boolean = true,
    var message: String? = null,
    var canBeOverridden: Boolean = false,
    var prerequisiteSessionTitle: String? = null
)

data class SessionPathDto(
    var startSessionId: UUID,
    var endSessionId: UUID,
    var pathSessions: List<SessionPathNodeDto> = emptyList(),
    var totalPathLength: Int = 0,
    var isValid: Boolean = true,
    var hasCircularDependency: Boolean = false
)

data class SessionPathNodeDto(
    var sessionId: UUID,
    var sessionTitle: String,
    var position: Int,
    var dependencyType: DependencyType,
    var requiredGapMinutes: Int = 0
)

data class DependencyAnalysisDto(
    var eventId: UUID,
    var totalSessions: Int = 0,
    var sessionsWithPrerequisites: Int = 0,
    var sessionsWithDependencies: Int = 0,
    var circularDependencies: List<CircularDependencyDto> = emptyList(),
    var longestDependencyChain: Int = 0,
    var dependencyMap: Map<UUID, List<UUID>> = emptyMap(), // session -> list of dependent sessions
    var prerequisiteMap: Map<UUID, List<UUID>> = emptyMap(), // session -> list of prerequisite sessions
    var isolatedSessions: List<UUID> = emptyList() // Sessions with no dependencies or prerequisites
)

data class CircularDependencyDto(
    var affectedSessions: List<UUID> = emptyList(),
    var sessionTitles: List<String> = emptyList(),
    var dependencyChain: String? = null,
    var severity: String = "ERROR" // WARNING, ERROR, CRITICAL
)