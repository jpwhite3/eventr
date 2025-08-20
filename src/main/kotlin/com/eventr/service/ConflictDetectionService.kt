package com.eventr.service

import com.eventr.dto.*
import com.eventr.model.*
import com.eventr.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

@Service
@Transactional
class ConflictDetectionService(
    private val scheduleConflictRepository: ScheduleConflictRepository,
    private val conflictResolutionRepository: ConflictResolutionRepository,
    private val sessionRepository: SessionRepository,
    private val sessionResourceRepository: SessionResourceRepository,
    private val sessionRegistrationRepository: SessionRegistrationRepository,
    private val resourceRepository: ResourceRepository,
    private val registrationRepository: RegistrationRepository
) {

    fun detectAllConflicts(eventId: UUID): List<ScheduleConflictDto> {
        val detectedConflicts = mutableListOf<ScheduleConflictDto>()
        
        // Detect different types of conflicts
        detectedConflicts.addAll(detectTimeOverlapConflicts(eventId))
        detectedConflicts.addAll(detectResourceConflicts(eventId))
        detectedConflicts.addAll(detectCapacityConflicts(eventId))
        detectedConflicts.addAll(detectUserConflicts(eventId))
        
        // Save new conflicts to database
        detectedConflicts.forEach { conflict ->
            if (!isExistingConflict(conflict)) {
                saveConflict(conflict)
            }
        }
        
        return detectedConflicts
    }

    fun detectTimeOverlapConflicts(eventId: UUID): List<ScheduleConflictDto> {
        val sessions = sessionRepository.findByEventIdAndIsActiveTrue(eventId)
        val conflicts = mutableListOf<ScheduleConflictDto>()
        
        for (i in sessions.indices) {
            for (j in i + 1 until sessions.size) {
                val session1 = sessions[i]
                val session2 = sessions[j]
                
                if (sessionsOverlap(session1, session2)) {
                    val overlapStart = maxOf(session1.startTime!!, session2.startTime!!)
                    val overlapEnd = minOf(session1.endTime!!, session2.endTime!!)
                    val overlapMinutes = ChronoUnit.MINUTES.between(overlapStart, overlapEnd)
                    
                    // Check if they share resources or have registered attendees
                    val severity = calculateTimeOverlapSeverity(session1, session2)
                    
                    conflicts.add(ScheduleConflictDto(
                        type = ConflictType.TIME_OVERLAP,
                        severity = severity,
                        title = "Session Time Overlap",
                        description = "Sessions '${session1.title}' and '${session2.title}' overlap for $overlapMinutes minutes",
                        primarySessionId = session1.id,
                        primarySessionTitle = session1.title,
                        secondarySessionId = session2.id,
                        secondarySessionTitle = session2.title,
                        conflictStart = overlapStart,
                        conflictEnd = overlapEnd,
                        affectedCount = calculateAffectedAttendees(session1, session2),
                        canAutoResolve = severity == ConflictSeverity.WARNING,
                        autoResolutionStrategy = "Adjust session timing",
                        suggestedResolutions = listOf(
                            "Move '${session2.title}' to start after '${session1.title}' ends",
                            "Reduce duration of one or both sessions",
                            "Move one session to a different time slot"
                        )
                    ))
                }
            }
        }
        
        return conflicts
    }

    fun detectResourceConflicts(eventId: UUID): List<ScheduleConflictDto> {
        val sessions = sessionRepository.findByEventIdAndIsActiveTrue(eventId)
        val conflicts = mutableListOf<ScheduleConflictDto>()
        
        val allBookings = sessions.flatMap { session ->
            sessionResourceRepository.findBySessionId(session.id!!)
        }
        
        // Group bookings by resource
        val bookingsByResource = allBookings.groupBy { it.resource!!.id!! }
        
        bookingsByResource.forEach { (resourceId, bookings) ->
            val resource = resourceRepository.findById(resourceId).orElse(null)
            if (resource != null) {
                val conflictingBookings = findConflictingResourceBookings(bookings)
                
                conflictingBookings.forEach { (booking1, booking2) ->
                    conflicts.add(ScheduleConflictDto(
                        type = ConflictType.RESOURCE_CONFLICT,
                        severity = ConflictSeverity.ERROR,
                        title = "Resource Double-Booking",
                        description = "Resource '${resource.name}' is double-booked between sessions '${booking1.session?.title}' and '${booking2.session?.title}'",
                        primarySessionId = booking1.session?.id,
                        primarySessionTitle = booking1.session?.title,
                        secondarySessionId = booking2.session?.id,
                        secondarySessionTitle = booking2.session?.title,
                        resourceId = resourceId,
                        resourceName = resource.name,
                        conflictStart = maxOf(booking1.bookingStart!!, booking2.bookingStart!!),
                        conflictEnd = minOf(booking1.bookingEnd!!, booking2.bookingEnd!!),
                        affectedCount = 1,
                        canAutoResolve = false,
                        suggestedResolutions = listOf(
                            "Find alternative resource for one session",
                            "Reschedule one of the sessions",
                            "Split resource usage if possible"
                        )
                    ))
                }
            }
        }
        
        return conflicts
    }

    fun detectCapacityConflicts(eventId: UUID): List<ScheduleConflictDto> {
        val conflicts = mutableListOf<ScheduleConflictDto>()
        val sessions = sessionRepository.findByEventIdAndIsActiveTrue(eventId)
        
        sessions.forEach { session ->
            val registrationCount = sessionRegistrationRepository.countBySessionIdAndStatus(
                session.id!!, SessionRegistrationStatus.REGISTERED
            ).toInt()
            
            // This would integrate with SessionCapacity once that relationship is established
            val sessionCapacity = session.capacity ?: Int.MAX_VALUE // Default if no capacity set
            
            if (registrationCount > sessionCapacity) {
                conflicts.add(ScheduleConflictDto(
                    type = ConflictType.CAPACITY_EXCEEDED,
                    severity = ConflictSeverity.ERROR,
                    title = "Session Capacity Exceeded",
                    description = "Session '${session.title}' has $registrationCount registrations but capacity is only $sessionCapacity",
                    primarySessionId = session.id,
                    primarySessionTitle = session.title,
                    affectedCount = registrationCount - sessionCapacity,
                    canAutoResolve = true,
                    autoResolutionStrategy = "Move excess registrations to waitlist",
                    suggestedResolutions = listOf(
                        "Increase session capacity",
                        "Move excess registrations to waitlist", 
                        "Create additional session instance",
                        "Move some attendees to alternative sessions"
                    )
                ))
            }
        }
        
        return conflicts
    }

    fun detectUserConflicts(eventId: UUID): List<ScheduleConflictDto> {
        val conflicts = mutableListOf<ScheduleConflictDto>()
        val registrations = registrationRepository.findByEventId(eventId)
        
        registrations.forEach { registration ->
            val userSessions = sessionRegistrationRepository.findByRegistrationIdOrderBySessionStartTime(registration.id!!)
                .filter { it.status == SessionRegistrationStatus.REGISTERED }
                .sortedBy { it.session?.startTime }
            
            // Check for overlapping sessions for the same user
            for (i in userSessions.indices) {
                for (j in i + 1 until userSessions.size) {
                    val sessionReg1 = userSessions[i]
                    val sessionReg2 = userSessions[j]
                    
                    if (sessionReg1.session != null && sessionReg2.session != null) {
                        if (sessionsOverlap(sessionReg1.session!!, sessionReg2.session!!)) {
                            conflicts.add(ScheduleConflictDto(
                                type = ConflictType.USER_CONFLICT,
                                severity = ConflictSeverity.WARNING,
                                title = "User Double-Booking",
                                description = "User ${registration.userName} is registered for overlapping sessions '${sessionReg1.session!!.title}' and '${sessionReg2.session!!.title}'",
                                primarySessionId = sessionReg1.session!!.id,
                                primarySessionTitle = sessionReg1.session!!.title,
                                secondarySessionId = sessionReg2.session!!.id,
                                secondarySessionTitle = sessionReg2.session!!.title,
                                registrationId = registration.id,
                                userEmail = registration.userEmail,
                                affectedCount = 1,
                                canAutoResolve = true,
                                autoResolutionStrategy = "Cancel one registration",
                                suggestedResolutions = listOf(
                                    "Cancel registration for less preferred session",
                                    "Move user to waitlist for one session",
                                    "Notify user to choose between sessions"
                                )
                            ))
                        }
                    }
                }
            }
        }
        
        return conflicts
    }

    fun resolveConflict(conflictId: UUID, resolutionDto: ConflictResolutionDto): ScheduleConflictDto {
        val conflict = scheduleConflictRepository.findById(conflictId)
            .orElseThrow { IllegalArgumentException("Conflict not found") }
        
        // Create resolution record
        val resolution = ConflictResolution(
            conflict = conflict,
            resolutionType = resolutionDto.resolutionType,
            description = resolutionDto.description,
            changesSummary = resolutionDto.changesSummary,
            implementedBy = resolutionDto.implementedBy,
            affectedSessions = resolutionDto.affectedSessions,
            affectedRegistrations = resolutionDto.affectedRegistrations,
            affectedResources = resolutionDto.affectedResources,
            notes = resolutionDto.notes
        )
        
        conflictResolutionRepository.save(resolution)
        
        // Update conflict status
        conflict.apply {
            resolutionStatus = ConflictResolutionStatus.RESOLVED
            resolvedBy = resolutionDto.implementedBy
            resolvedAt = LocalDateTime.now()
            resolutionNotes = resolutionDto.description
            lastCheckedAt = LocalDateTime.now()
            updatedAt = LocalDateTime.now()
        }
        
        val resolvedConflict = scheduleConflictRepository.save(conflict)
        return convertToDto(resolvedConflict)
    }

    fun autoResolveConflicts(): List<ScheduleConflictDto> {
        val autoResolvableConflicts = scheduleConflictRepository.findAutoResolvableConflicts()
        val resolvedConflicts = mutableListOf<ScheduleConflictDto>()
        
        autoResolvableConflicts.forEach { conflict ->
            try {
                val resolved = attemptAutoResolution(conflict)
                if (resolved != null) {
                    resolvedConflicts.add(resolved)
                }
            } catch (e: Exception) {
                // Log error but continue with other conflicts
                println("Failed to auto-resolve conflict ${conflict.id}: ${e.message}")
            }
        }
        
        return resolvedConflicts
    }

    fun getConflictSummary(eventId: UUID): ConflictSummaryDto {
        val allConflicts = scheduleConflictRepository.findEventConflicts(eventId)
        
        val totalConflicts = allConflicts.size
        val unresolvedConflicts = allConflicts.count { it.resolutionStatus == ConflictResolutionStatus.UNRESOLVED }
        val criticalConflicts = allConflicts.count { it.severity == ConflictSeverity.CRITICAL }
        val autoResolvableConflicts = allConflicts.count { it.canAutoResolve && it.resolutionStatus == ConflictResolutionStatus.UNRESOLVED }
        
        val conflictsByType = allConflicts.groupBy { it.type }
            .mapValues { it.value.size }
        
        val conflictsBySeverity = allConflicts.groupBy { it.severity }
            .mapValues { it.value.size }
        
        val oldestUnresolvedConflict = allConflicts
            .filter { it.resolutionStatus == ConflictResolutionStatus.UNRESOLVED }
            .minByOrNull { it.detectedAt }
            ?.detectedAt
        
        val resolvedConflicts = allConflicts.filter { it.resolvedAt != null }
        val averageResolutionTime = if (resolvedConflicts.isNotEmpty()) {
            resolvedConflicts.map { 
                ChronoUnit.HOURS.between(it.detectedAt, it.resolvedAt).toDouble()
            }.average()
        } else 0.0
        
        return ConflictSummaryDto(
            eventId = eventId,
            totalConflicts = totalConflicts,
            unresolvedConflicts = unresolvedConflicts,
            criticalConflicts = criticalConflicts,
            autoResolvableConflicts = autoResolvableConflicts,
            conflictsByType = conflictsByType,
            conflictsBySeverity = conflictsBySeverity,
            oldestUnresolvedConflict = oldestUnresolvedConflict,
            averageResolutionTimeHours = averageResolutionTime
        )
    }

    fun generateConflictAnalytics(eventId: UUID): ConflictAnalyticsDto {
        val conflicts = scheduleConflictRepository.findEventConflicts(eventId)
        val sessions = sessionRepository.findByEventIdAndIsActiveTrue(eventId)
        val resources = resourceRepository.findByIsActiveTrue()
        val registrations = registrationRepository.findByEventId(eventId)
        
        val conflictRate = if (sessions.isNotEmpty()) conflicts.size.toDouble() / sessions.size else 0.0
        val resolvedConflicts = conflicts.filter { it.resolutionStatus == ConflictResolutionStatus.RESOLVED }
        val resolutionRate = if (conflicts.isNotEmpty()) resolvedConflicts.size.toDouble() / conflicts.size * 100 else 0.0
        
        val mostCommonConflictType = conflicts.groupBy { it.type }
            .maxByOrNull { it.value.size }?.key
        
        // Analyze resource conflicts
        val resourceConflictCounts = conflicts.filter { it.type == ConflictType.RESOURCE_CONFLICT }
            .groupBy { it.resource?.id }
            .mapValues { it.value.size }
        
        val mostConflictedResources = resourceConflictCounts.entries
            .sortedByDescending { it.value }
            .take(5)
            .mapNotNull { entry ->
                resourceRepository.findById(entry.key!!).orElse(null)?.let { resource ->
                    ResourceConflictSummaryDto(
                        resourceId = resource.id!!,
                        resourceName = resource.name,
                        resourceType = resource.type.name,
                        conflictCount = entry.value,
                        utilizationRate = 0.0, // Would calculate based on bookings
                        averageConflictDuration = 0.0 // Would calculate from conflict durations
                    )
                }
            }
        
        // Analyze session conflicts
        val sessionConflictCounts = conflicts
            .flatMap { listOfNotNull(it.primarySession?.id, it.secondarySession?.id) }
            .groupBy { it }
            .mapValues { it.value.size }
        
        val sessionsWithMostConflicts = sessionConflictCounts.entries
            .sortedByDescending { it.value }
            .take(5)
            .mapNotNull { entry ->
                sessionRepository.findById(entry.key).orElse(null)?.let { session ->
                    val sessionConflicts = conflicts.filter { 
                        it.primarySession?.id == entry.key || it.secondarySession?.id == entry.key 
                    }
                    val registrationCount = sessionRegistrationRepository
                        .countBySessionIdAndStatus(session.id!!, SessionRegistrationStatus.REGISTERED)
                        .toInt()
                    
                    SessionConflictSummaryDto(
                        sessionId = session.id!!,
                        sessionTitle = session.title,
                        conflictCount = entry.value,
                        conflictTypes = sessionConflicts.map { it.type }.distinct(),
                        registrationCount = registrationCount
                    )
                }
            }
        
        return ConflictAnalyticsDto(
            eventId = eventId,
            totalSessions = sessions.size,
            totalResources = resources.size,
            totalRegistrations = registrations.size,
            conflictRate = conflictRate,
            resolutionRate = resolutionRate,
            mostCommonConflictType = mostCommonConflictType,
            mostConflictedResources = mostConflictedResources,
            sessionsWithMostConflicts = sessionsWithMostConflicts,
            preventionRecommendations = generatePreventionRecommendations(conflicts),
            schedulingOptimizationSuggestions = generateOptimizationSuggestions(conflicts, sessions)
        )
    }

    private fun sessionsOverlap(session1: Session, session2: Session): Boolean {
        return session1.startTime!! < session2.endTime!! && session2.startTime!! < session1.endTime!!
    }

    private fun calculateTimeOverlapSeverity(session1: Session, session2: Session): ConflictSeverity {
        // Check if sessions share resources
        val session1Resources = sessionResourceRepository.findBySessionId(session1.id!!)
            .map { it.resource?.id }.toSet()
        val session2Resources = sessionResourceRepository.findBySessionId(session2.id!!)
            .map { it.resource?.id }.toSet()
        
        return when {
            session1Resources.intersect(session2Resources).isNotEmpty() -> ConflictSeverity.ERROR
            calculateAffectedAttendees(session1, session2) > 0 -> ConflictSeverity.WARNING
            else -> ConflictSeverity.INFO
        }
    }

    private fun calculateAffectedAttendees(session1: Session, session2: Session): Int {
        val session1Attendees = sessionRegistrationRepository.findBySessionIdAndStatus(
            session1.id!!, SessionRegistrationStatus.REGISTERED
        ).map { it.registration?.id }.toSet()
        
        val session2Attendees = sessionRegistrationRepository.findBySessionIdAndStatus(
            session2.id!!, SessionRegistrationStatus.REGISTERED
        ).map { it.registration?.id }.toSet()
        
        return session1Attendees.intersect(session2Attendees).size
    }

    private fun findConflictingResourceBookings(bookings: List<SessionResource>): List<Pair<SessionResource, SessionResource>> {
        val conflicts = mutableListOf<Pair<SessionResource, SessionResource>>()
        
        for (i in bookings.indices) {
            for (j in i + 1 until bookings.size) {
                val booking1 = bookings[i]
                val booking2 = bookings[j]
                
                if (booking1.bookingStart!! < booking2.bookingEnd!! && 
                    booking2.bookingStart!! < booking1.bookingEnd!!) {
                    conflicts.add(Pair(booking1, booking2))
                }
            }
        }
        
        return conflicts
    }

    private fun isExistingConflict(conflict: ScheduleConflictDto): Boolean {
        // Check if similar conflict already exists
        val existing = scheduleConflictRepository.findBySessionId(conflict.primarySessionId!!)
        return existing.any { 
            it.type == conflict.type && 
            it.isActive && 
            it.resolutionStatus == ConflictResolutionStatus.UNRESOLVED
        }
    }

    private fun saveConflict(conflictDto: ScheduleConflictDto): ScheduleConflictDto {
        val conflict = ScheduleConflict(
            type = conflictDto.type,
            severity = conflictDto.severity,
            title = conflictDto.title,
            description = conflictDto.description,
            primarySession = conflictDto.primarySessionId?.let { sessionRepository.findById(it).orElse(null) },
            secondarySession = conflictDto.secondarySessionId?.let { sessionRepository.findById(it).orElse(null) },
            resource = conflictDto.resourceId?.let { resourceRepository.findById(it).orElse(null) },
            registration = conflictDto.registrationId?.let { registrationRepository.findById(it).orElse(null) },
            conflictStart = conflictDto.conflictStart,
            conflictEnd = conflictDto.conflictEnd,
            affectedCount = conflictDto.affectedCount,
            canAutoResolve = conflictDto.canAutoResolve,
            autoResolutionStrategy = conflictDto.autoResolutionStrategy
        )
        
        val saved = scheduleConflictRepository.save(conflict)
        return convertToDto(saved)
    }

    private fun attemptAutoResolution(conflict: ScheduleConflict): ScheduleConflictDto? {
        return when (conflict.type) {
            ConflictType.CAPACITY_EXCEEDED -> attemptCapacityResolution(conflict)
            ConflictType.USER_CONFLICT -> attemptUserConflictResolution(conflict)
            else -> null // Other types require manual intervention
        }
    }

    private fun attemptCapacityResolution(conflict: ScheduleConflict): ScheduleConflictDto? {
        // Auto-resolve by moving excess registrations to waitlist
        val session = conflict.primarySession ?: return null
        val sessionCapacity = session.capacity ?: return null
        
        val registrations = sessionRegistrationRepository.findBySessionIdAndStatus(
            session.id!!, SessionRegistrationStatus.REGISTERED
        ).sortedBy { it.registeredAt }
        
        val excessCount = registrations.size - sessionCapacity
        if (excessCount > 0) {
            val toWaitlist = registrations.takeLast(excessCount)
            toWaitlist.forEach { reg ->
                reg.status = SessionRegistrationStatus.WAITLIST
                reg.waitlistRegisteredAt = LocalDateTime.now()
                reg.notes = "${reg.notes ?: ""} Auto-moved to waitlist due to capacity conflict".trim()
                sessionRegistrationRepository.save(reg)
            }
            
            return resolveConflict(conflict.id!!, ConflictResolutionDto(
                conflictId = conflict.id!!,
                resolutionType = "AUTO_CAPACITY_ADJUSTMENT",
                description = "Moved $excessCount excess registrations to waitlist",
                implementedBy = "SYSTEM_AUTO_RESOLVE",
                affectedRegistrations = excessCount
            ))
        }
        
        return null
    }

    private fun attemptUserConflictResolution(conflict: ScheduleConflict): ScheduleConflictDto? {
        // For user conflicts, we could implement logic to cancel the later registration
        // or move to waitlist, but this might require user notification
        // For now, just mark as acknowledged
        conflict.resolutionStatus = ConflictResolutionStatus.ACKNOWLEDGED
        conflict.resolutionNotes = "User conflict detected - requires manual review"
        val updated = scheduleConflictRepository.save(conflict)
        return convertToDto(updated)
    }

    private fun generatePreventionRecommendations(conflicts: List<ScheduleConflict>): List<String> {
        val recommendations = mutableListOf<String>()
        
        val timeConflicts = conflicts.count { it.type == ConflictType.TIME_OVERLAP }
        if (timeConflicts > 0) {
            recommendations.add("Implement buffer time between sessions to prevent scheduling overlaps")
        }
        
        val resourceConflicts = conflicts.count { it.type == ConflictType.RESOURCE_CONFLICT }
        if (resourceConflicts > 0) {
            recommendations.add("Add resource availability validation before confirming session bookings")
            recommendations.add("Consider acquiring additional resources for high-demand items")
        }
        
        val capacityConflicts = conflicts.count { it.type == ConflictType.CAPACITY_EXCEEDED }
        if (capacityConflicts > 0) {
            recommendations.add("Enable automatic waitlist management for popular sessions")
            recommendations.add("Monitor registration trends to adjust capacity proactively")
        }
        
        return recommendations
    }

    private fun generateOptimizationSuggestions(conflicts: List<ScheduleConflict>, sessions: List<Session>): List<String> {
        val suggestions = mutableListOf<String>()
        
        // Analyze time distribution
        val sessionsByHour = sessions.groupBy { it.startTime?.hour ?: 0 }
        val peakHour = sessionsByHour.maxByOrNull { it.value.size }?.key
        
        if (peakHour != null) {
            suggestions.add("Consider spreading sessions more evenly - peak hour $peakHour has ${sessionsByHour[peakHour]?.size} sessions")
        }
        
        // Analyze conflict patterns
        if (conflicts.size > sessions.size * 0.1) {
            suggestions.add("High conflict rate detected - consider extending event duration or adding more time slots")
        }
        
        return suggestions
    }

    private fun convertToDto(conflict: ScheduleConflict): ScheduleConflictDto {
        val ageInHours = ChronoUnit.HOURS.between(conflict.detectedAt, LocalDateTime.now())
        val duration = if (conflict.conflictStart != null && conflict.conflictEnd != null) {
            val minutes = ChronoUnit.MINUTES.between(conflict.conflictStart, conflict.conflictEnd)
            "${minutes} minutes"
        } else null
        
        return ScheduleConflictDto(
            id = conflict.id,
            type = conflict.type,
            severity = conflict.severity,
            title = conflict.title,
            description = conflict.description,
            primarySessionId = conflict.primarySession?.id,
            primarySessionTitle = conflict.primarySession?.title,
            secondarySessionId = conflict.secondarySession?.id,
            secondarySessionTitle = conflict.secondarySession?.title,
            resourceId = conflict.resource?.id,
            resourceName = conflict.resource?.name,
            registrationId = conflict.registration?.id,
            userEmail = conflict.registration?.userEmail,
            conflictStart = conflict.conflictStart,
            conflictEnd = conflict.conflictEnd,
            affectedCount = conflict.affectedCount,
            duration = duration,
            resolutionStatus = conflict.resolutionStatus,
            resolutionNotes = conflict.resolutionNotes,
            resolvedBy = conflict.resolvedBy,
            resolvedAt = conflict.resolvedAt,
            canAutoResolve = conflict.canAutoResolve,
            autoResolutionStrategy = conflict.autoResolutionStrategy,
            detectedAt = conflict.detectedAt,
            lastCheckedAt = conflict.lastCheckedAt,
            ageInHours = ageInHours,
            notificationsSent = conflict.notificationsSent,
            isActive = conflict.isActive
        )
    }
}