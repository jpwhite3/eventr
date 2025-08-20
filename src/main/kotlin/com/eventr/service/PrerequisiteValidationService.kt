package com.eventr.service

import com.eventr.dto.*
import com.eventr.model.*
import com.eventr.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional(readOnly = true)
class PrerequisiteValidationService(
    private val sessionPrerequisiteRepository: SessionPrerequisiteRepository,
    private val sessionDependencyRepository: SessionDependencyRepository,
    private val sessionRegistrationRepository: SessionRegistrationRepository,
    private val sessionRepository: SessionRepository,
    private val checkInRepository: CheckInRepository
) {

    fun validatePrerequisites(sessionId: UUID, registrationId: UUID): PrerequisiteValidationDto {
        val prerequisites = sessionPrerequisiteRepository.findBySessionIdAndIsActiveTrue(sessionId)
        val validationResults = mutableListOf<PrerequisiteValidationResultDto>()
        
        var canRegister = true
        val failureReasons = mutableListOf<String>()
        var canAdminOverride = false
        
        prerequisites.forEach { prerequisite ->
            val result = validateSinglePrerequisite(prerequisite, registrationId)
            validationResults.add(result)
            
            if (prerequisite.isRequired && !result.isPassed) {
                canRegister = false
                result.message?.let { failureReasons.add(it) }
            }
            
            if (prerequisite.allowAdminOverride) {
                canAdminOverride = true
            }
        }
        
        // Group validation by logical operators
        val groupedResults = validatePrerequisiteGroups(prerequisites, registrationId)
        canRegister = canRegister && groupedResults
        
        val overallStatus = when {
            canRegister -> "PASSED"
            canAdminOverride -> "FAILED_OVERRIDABLE"
            else -> "FAILED"
        }
        
        return PrerequisiteValidationDto(
            sessionId = sessionId,
            registrationId = registrationId,
            validationResults = validationResults,
            canRegister = canRegister,
            overallStatus = overallStatus,
            failureReasons = failureReasons,
            canAdminOverride = canAdminOverride
        )
    }

    fun validateSessionDependencies(sessionId: UUID, registrationId: UUID): List<String> {
        val dependencies = sessionDependencyRepository.findByDependentSessionId(sessionId)
        val violations = mutableListOf<String>()
        
        dependencies.forEach { dependency ->
            when (dependency.dependencyType) {
                DependencyType.SEQUENCE -> {
                    if (!hasAttendedSession(registrationId, dependency.parentSession!!.id!!)) {
                        violations.add("Must complete '${dependency.parentSession!!.title}' before registering for this session")
                    }
                }
                DependencyType.PREREQUISITE -> {
                    if (!isRegisteredForSession(registrationId, dependency.parentSession!!.id!!)) {
                        violations.add("Must be registered for '${dependency.parentSession!!.title}' to register for this session")
                    }
                }
                DependencyType.EXCLUSIVE -> {
                    if (isRegisteredForSession(registrationId, dependency.parentSession!!.id!!)) {
                        violations.add("Cannot register for this session if registered for '${dependency.parentSession!!.title}'")
                    }
                }
                DependencyType.PARALLEL -> {
                    // Recommendation rather than strict requirement
                    if (!isRegisteredForSession(registrationId, dependency.parentSession!!.id!!)) {
                        violations.add("Recommended to also register for '${dependency.parentSession!!.title}'")
                    }
                }
            }
        }
        
        return violations
    }

    fun detectCircularDependencies(eventId: UUID): List<CircularDependencyDto> {
        val sessions = sessionRepository.findByEventIdAndIsActiveTrue(eventId)
        val circularDependencies = mutableListOf<CircularDependencyDto>()
        
        sessions.forEach { session ->
            val cycles = sessionDependencyRepository.detectCircularDependencies(session.id!!)
            if (cycles.isNotEmpty()) {
                val sessionIds = cycles.map { UUID.fromString(it[0].toString()) }.distinct()
                val sessionTitles = sessionIds.mapNotNull { id ->
                    sessionRepository.findById(id).orElse(null)?.title
                }
                
                circularDependencies.add(CircularDependencyDto(
                    affectedSessions = sessionIds,
                    sessionTitles = sessionTitles,
                    dependencyChain = sessionTitles.joinToString(" -> "),
                    severity = "ERROR"
                ))
            }
        }
        
        return circularDependencies.distinctBy { it.affectedSessions.sorted() }
    }

    fun getSessionDependencyPath(fromSessionId: UUID, toSessionId: UUID): SessionPathDto {
        val pathResults = sessionDependencyRepository.findDependencyPath(fromSessionId, toSessionId)
        
        if (pathResults.isEmpty()) {
            return SessionPathDto(
                startSessionId = fromSessionId,
                endSessionId = toSessionId,
                isValid = false
            )
        }
        
        val pathNodes = mutableListOf<SessionPathNodeDto>()
        var position = 0
        
        pathResults.forEach { result ->
            val sessionId = UUID.fromString(result[1].toString())
            val session = sessionRepository.findById(sessionId).orElse(null)
            
            if (session != null) {
                pathNodes.add(SessionPathNodeDto(
                    sessionId = sessionId,
                    sessionTitle = session.title,
                    position = position++,
                    dependencyType = DependencyType.SEQUENCE // Default, would need more info from query
                ))
            }
        }
        
        return SessionPathDto(
            startSessionId = fromSessionId,
            endSessionId = toSessionId,
            pathSessions = pathNodes,
            totalPathLength = pathNodes.size,
            isValid = true,
            hasCircularDependency = false
        )
    }

    fun analyzeDependencyStructure(eventId: UUID): DependencyAnalysisDto {
        val sessions = sessionRepository.findByEventIdAndIsActiveTrue(eventId)
        val prerequisites = sessionPrerequisiteRepository.findByEventId(eventId)
        val dependencies = sessionDependencyRepository.findByEventId(eventId)
        
        val sessionsWithPrerequisites = prerequisites.map { it.session?.id }.distinct().size
        val sessionsWithDependencies = dependencies.flatMap { 
            listOf(it.parentSession?.id, it.dependentSession?.id) 
        }.distinct().size
        
        val dependencyMap = dependencies.groupBy { it.parentSession?.id }
            .mapValues { entry -> entry.value.mapNotNull { it.dependentSession?.id } }
            .filterKeys { it != null }
            .mapKeys { it.key!! }
        
        val prerequisiteMap = prerequisites.groupBy { it.session?.id }
            .mapValues { entry -> entry.value.mapNotNull { it.prerequisiteSession?.id } }
            .filterKeys { it != null }
            .mapKeys { it.key!! }
        
        val connectedSessions = (dependencyMap.keys + prerequisiteMap.keys).toSet()
        val isolatedSessions = sessions.mapNotNull { it.id }.filter { it !in connectedSessions }
        
        val circularDependencies = detectCircularDependencies(eventId)
        
        // Find longest dependency chain
        val longestChain = sessions.mapNotNull { session ->
            try {
                calculateDependencyChainLength(session.id!!, mutableSetOf())
            } catch (e: Exception) {
                0
            }
        }.maxOrNull() ?: 0
        
        return DependencyAnalysisDto(
            eventId = eventId,
            totalSessions = sessions.size,
            sessionsWithPrerequisites = sessionsWithPrerequisites,
            sessionsWithDependencies = sessionsWithDependencies,
            circularDependencies = circularDependencies,
            longestDependencyChain = longestChain,
            dependencyMap = dependencyMap,
            prerequisiteMap = prerequisiteMap,
            isolatedSessions = isolatedSessions
        )
    }

    private fun validateSinglePrerequisite(
        prerequisite: SessionPrerequisite, 
        registrationId: UUID
    ): PrerequisiteValidationResultDto {
        
        val isPassed = when (prerequisite.type) {
            PrerequisiteType.SESSION_ATTENDANCE -> {
                prerequisite.prerequisiteSession?.let { prereqSession ->
                    hasAttendedSession(registrationId, prereqSession.id!!)
                } ?: false
            }
            PrerequisiteType.SESSION_REGISTRATION -> {
                prerequisite.prerequisiteSession?.let { prereqSession ->
                    isRegisteredForSession(registrationId, prereqSession.id!!)
                } ?: false
            }
            PrerequisiteType.PROFILE_REQUIREMENT -> {
                // Would implement profile-based validation here
                validateProfileRequirement(prerequisite.ruleDefinition, registrationId)
            }
            PrerequisiteType.CUSTOM_RULE -> {
                // Would implement custom rule evaluation here
                validateCustomRule(prerequisite.ruleDefinition, registrationId)
            }
        }
        
        val message = if (!isPassed) {
            prerequisite.errorMessage ?: generateDefaultErrorMessage(prerequisite)
        } else null
        
        return PrerequisiteValidationResultDto(
            prerequisiteId = prerequisite.id!!,
            type = prerequisite.type,
            isPassed = isPassed,
            isRequired = prerequisite.isRequired,
            message = message,
            canBeOverridden = prerequisite.allowAdminOverride,
            prerequisiteSessionTitle = prerequisite.prerequisiteSession?.title
        )
    }

    private fun validatePrerequisiteGroups(
        prerequisites: List<SessionPrerequisite>, 
        registrationId: UUID
    ): Boolean {
        // Group by groupId and evaluate with logical operators
        val groups = prerequisites.groupBy { it.groupId ?: "default" }
        
        return groups.all { (_, groupPrerequisites) ->
            evaluateLogicalGroup(groupPrerequisites, registrationId)
        }
    }

    private fun evaluateLogicalGroup(
        prerequisites: List<SessionPrerequisite>,
        registrationId: UUID
    ): Boolean {
        if (prerequisites.isEmpty()) return true
        
        // For simplicity, treating all as AND operations
        // In production, would implement full logical expression parsing
        return prerequisites.all { prerequisite ->
            !prerequisite.isRequired || validateSinglePrerequisite(prerequisite, registrationId).isPassed
        }
    }

    private fun hasAttendedSession(registrationId: UUID, sessionId: UUID): Boolean {
        // Check if user has a check-in record for the session
        val checkIns = checkInRepository.findByRegistrationIdOrderByCheckedInAtDesc(registrationId)
        return checkIns.any { checkIn -> 
            checkIn.session?.id == sessionId && checkIn.type == CheckInType.SESSION 
        }
    }

    private fun isRegisteredForSession(registrationId: UUID, sessionId: UUID): Boolean {
        val sessionReg = sessionRegistrationRepository.findByRegistrationIdAndSessionId(registrationId, sessionId)
        return sessionReg?.status == SessionRegistrationStatus.REGISTERED
    }

    private fun validateProfileRequirement(ruleDefinition: String?, registrationId: UUID): Boolean {
        // Placeholder for profile-based validation
        // Would parse JSON rule and validate against user profile
        return true
    }

    private fun validateCustomRule(ruleDefinition: String?, registrationId: UUID): Boolean {
        // Placeholder for custom rule validation
        // Would implement custom business logic evaluation
        return true
    }

    private fun generateDefaultErrorMessage(prerequisite: SessionPrerequisite): String {
        return when (prerequisite.type) {
            PrerequisiteType.SESSION_ATTENDANCE -> 
                "You must attend '${prerequisite.prerequisiteSession?.title}' before registering for this session"
            PrerequisiteType.SESSION_REGISTRATION -> 
                "You must be registered for '${prerequisite.prerequisiteSession?.title}' to register for this session"
            PrerequisiteType.PROFILE_REQUIREMENT -> 
                "Your profile does not meet the requirements for this session"
            PrerequisiteType.CUSTOM_RULE -> 
                "You do not meet the custom requirements for this session"
        }
    }

    private fun calculateDependencyChainLength(sessionId: UUID, visited: MutableSet<UUID>): Int {
        if (sessionId in visited) return 0 // Prevent infinite loops
        
        visited.add(sessionId)
        
        val dependencies = sessionDependencyRepository.findByParentSessionId(sessionId)
        if (dependencies.isEmpty()) return 1
        
        val maxChainLength = dependencies.maxOfOrNull { dependency ->
            calculateDependencyChainLength(dependency.dependentSession!!.id!!, visited.toMutableSet())
        } ?: 0
        
        return 1 + maxChainLength
    }
}