package com.eventr.service

import com.eventr.dto.*
import com.eventr.model.*
import com.eventr.repository.*
import org.springframework.beans.BeanUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class CapacityManagementService(
    private val sessionCapacityRepository: SessionCapacityRepository,
    private val sessionRepository: SessionRepository,
    private val sessionRegistrationRepository: SessionRegistrationRepository,
    private val registrationRepository: RegistrationRepository,
    private val eventRepository: EventRepository
) {

    fun createSessionCapacity(sessionId: UUID, capacityDto: SessionCapacityDto): SessionCapacityDto {
        val session = sessionRepository.findById(sessionId)
            .orElseThrow { IllegalArgumentException("Session not found") }
        
        // Check if capacity already exists
        sessionCapacityRepository.findBySessionId(sessionId)?.let {
            throw IllegalArgumentException("Capacity configuration already exists for this session")
        }
        
        val capacity = SessionCapacity().apply {
            this.session = session
            this.capacityType = capacityDto.capacityType
            this.maximumCapacity = capacityDto.maximumCapacity
            this.minimumCapacity = capacityDto.minimumCapacity
            this.enableWaitlist = capacityDto.enableWaitlist
            this.waitlistCapacity = capacityDto.waitlistCapacity
            this.waitlistStrategy = capacityDto.waitlistStrategy
            this.allowOverbooking = capacityDto.allowOverbooking
            this.overbookingPercentage = capacityDto.overbookingPercentage
            this.autoPromoteFromWaitlist = capacityDto.autoPromoteFromWaitlist
            this.lowCapacityThreshold = capacityDto.lowCapacityThreshold
            this.highDemandThreshold = capacityDto.highDemandThreshold
            
            // Initialize current counts
            updateCurrentCounts()
        }
        
        val saved = sessionCapacityRepository.save(capacity)
        return convertToDto(saved)
    }

    fun updateSessionCapacity(sessionId: UUID, updateDto: CapacityUpdateDto): SessionCapacityDto {
        val capacity = sessionCapacityRepository.findBySessionId(sessionId)
            ?: throw IllegalArgumentException("Session capacity not found")
        
        updateDto.maximumCapacity?.let { capacity.maximumCapacity = it }
        updateDto.minimumCapacity?.let { capacity.minimumCapacity = it }
        updateDto.capacityType?.let { capacity.capacityType = it }
        updateDto.enableWaitlist?.let { capacity.enableWaitlist = it }
        updateDto.waitlistCapacity?.let { capacity.waitlistCapacity = it }
        updateDto.waitlistStrategy?.let { capacity.waitlistStrategy = it }
        updateDto.allowOverbooking?.let { capacity.allowOverbooking = it }
        updateDto.overbookingPercentage?.let { capacity.overbookingPercentage = it }
        
        capacity.updateCurrentCounts()
        capacity.lastCapacityUpdate = LocalDateTime.now()
        capacity.updatedAt = LocalDateTime.now()
        
        // Record capacity change in history
        recordCapacityChange(capacity, updateDto.reason)
        
        val saved = sessionCapacityRepository.save(capacity)
        return convertToDto(saved)
    }

    fun getSessionCapacity(sessionId: UUID): SessionCapacityDto {
        val capacity = sessionCapacityRepository.findBySessionId(sessionId)
            ?: throw IllegalArgumentException("Session capacity not found")
        
        return convertToDto(capacity)
    }

    fun getEventCapacityAnalytics(eventId: UUID): CapacityAnalyticsDto {
        val event = eventRepository.findById(eventId)
            .orElseThrow { IllegalArgumentException("Event not found") }
        
        val sessions = sessionRepository.findByEventIdAndIsActiveTrue(eventId)
        val capacities = sessions.mapNotNull { session ->
            sessionCapacityRepository.findBySessionId(session.id!!)
        }
        
        val sessionCapacityDtos = capacities.map { convertToDto(it) }
        val averageUtilization = sessionCapacityRepository.getAverageCapacityUtilization(eventId) ?: 0.0
        val fullSessionsCount = sessionCapacityRepository.countFullSessions(eventId).toInt()
        val underCapacitySessions = sessionCapacityRepository.findUnderCapacitySessions(eventId)
        val totalWaitlistCount = sessionCapacityRepository.getTotalWaitlistCount(eventId)?.toInt() ?: 0
        val overbookedSessions = sessionCapacityRepository.findOverBookedSessions()
        
        return CapacityAnalyticsDto(
            eventId = eventId,
            eventName = event.name ?: "Unknown Event",
            totalSessions = sessions.size,
            averageUtilization = averageUtilization,
            fullSessionsCount = fullSessionsCount,
            underCapacitySessionsCount = underCapacitySessions.size,
            totalWaitlistCount = totalWaitlistCount,
            overbookedSessionsCount = overbookedSessions.size,
            sessionCapacities = sessionCapacityDtos,
            waitlistDistribution = capacities.groupBy { it.waitlistStrategy }
                .mapValues { it.value.sumOf { capacity -> capacity.currentWaitlistCount } }
        )
    }

    fun promoteFromWaitlist(promotionDto: WaitlistPromotionDto): List<SessionRegistrationDto> {
        val capacity = sessionCapacityRepository.findBySessionId(promotionDto.sessionId)
            ?: throw IllegalArgumentException("Session capacity not found")
        
        if (!capacity.enableWaitlist || capacity.currentWaitlistCount == 0) {
            throw IllegalArgumentException("No waitlist entries to promote")
        }
        
        if (capacity.availableSpots <= 0) {
            throw IllegalArgumentException("No available spots for promotion")
        }
        
        val promotedRegistrations = mutableListOf<SessionRegistrationDto>()
        val availableSpots = minOf(capacity.availableSpots, promotionDto.registrationIds.size)
        
        promotionDto.registrationIds.take(availableSpots).forEach { registrationId ->
            val sessionReg = sessionRegistrationRepository.findByRegistrationIdAndSessionId(registrationId, promotionDto.sessionId)
            
            if (sessionReg != null && sessionReg.status == SessionRegistrationStatus.WAITLIST) {
                sessionReg.status = SessionRegistrationStatus.REGISTERED
                sessionReg.waitlistPosition = null
                sessionReg.waitlistRegisteredAt = null
                sessionReg.notes = "${sessionReg.notes ?: ""} Promoted from waitlist: ${promotionDto.promotionReason ?: "Auto-promotion"}".trim()
                sessionReg.updatedAt = LocalDateTime.now()
                
                val saved = sessionRegistrationRepository.save(sessionReg)
                promotedRegistrations.add(convertSessionRegistrationToDto(saved))
            }
        }
        
        // Update capacity counts
        capacity.updateCurrentCounts()
        sessionCapacityRepository.save(capacity)
        
        return promotedRegistrations
    }

    fun autoPromoteWaitlistedUsers(): List<SessionCapacityDto> {
        val sessionsForPromotion = sessionCapacityRepository.findSessionsForAutoPromotion()
        val updatedCapacities = mutableListOf<SessionCapacityDto>()
        
        sessionsForPromotion.forEach { capacity ->
            try {
                val waitlistedRegistrations = sessionRegistrationRepository
                    .findBySessionIdAndStatusOrderByWaitlistRegisteredAtAsc(
                        capacity.session!!.id!!, 
                        SessionRegistrationStatus.WAITLIST
                    )
                
                val toPromote = waitlistedRegistrations.take(capacity.availableSpots)
                
                if (toPromote.isNotEmpty()) {
                    val promotionDto = WaitlistPromotionDto(
                        sessionId = capacity.session!!.id!!,
                        registrationIds = toPromote.map { it.registration!!.id!! },
                        promotionReason = "Automatic promotion - spots became available",
                        notifyAttendees = true
                    )
                    
                    promoteFromWaitlist(promotionDto)
                    updatedCapacities.add(convertToDto(capacity))
                }
            } catch (e: Exception) {
                // Log error but continue with other sessions
                println("Failed to auto-promote for session ${capacity.session?.id}: ${e.message}")
            }
        }
        
        return updatedCapacities
    }

    fun getCapacityOptimizationSuggestions(eventId: UUID): List<CapacityOptimizationSuggestionDto> {
        val sessions = sessionRepository.findByEventIdAndIsActiveTrue(eventId)
        val capacities = sessions.mapNotNull { session ->
            sessionCapacityRepository.findBySessionId(session.id!!)
        }
        val suggestions = mutableListOf<CapacityOptimizationSuggestionDto>()
        
        capacities.forEach { capacity ->
            val utilizationRate = if (capacity.maximumCapacity > 0) {
                capacity.currentRegistrations.toDouble() / capacity.maximumCapacity.toDouble()
            } else 0.0
            
            when {
                utilizationRate >= 1.0 && capacity.currentWaitlistCount > 0 -> {
                    suggestions.add(CapacityOptimizationSuggestionDto(
                        sessionId = capacity.session!!.id!!,
                        sessionTitle = capacity.session!!.title,
                        currentCapacity = capacity.maximumCapacity,
                        currentRegistrations = capacity.currentRegistrations,
                        suggestedCapacity = capacity.maximumCapacity + capacity.currentWaitlistCount,
                        optimizationType = "INCREASE",
                        reason = "Session is full with ${capacity.currentWaitlistCount} people waitlisted",
                        potentialImpact = "Accommodate all waitlisted attendees",
                        priority = if (capacity.currentWaitlistCount > 10) "HIGH" else "MEDIUM"
                    ))
                }
                
                utilizationRate < 0.5 && capacity.currentRegistrations < capacity.minimumCapacity -> {
                    suggestions.add(CapacityOptimizationSuggestionDto(
                        sessionId = capacity.session!!.id!!,
                        sessionTitle = capacity.session!!.title,
                        currentCapacity = capacity.maximumCapacity,
                        currentRegistrations = capacity.currentRegistrations,
                        suggestedCapacity = maxOf(capacity.minimumCapacity, capacity.currentRegistrations + 5),
                        optimizationType = "DECREASE",
                        reason = "Low utilization (${String.format("%.1f", utilizationRate * 100)}%) and below minimum capacity",
                        potentialImpact = "Reduce costs and create more intimate setting",
                        priority = "MEDIUM"
                    ))
                }
                
                capacity.currentWaitlistCount > capacity.maximumCapacity * 0.2 -> {
                    suggestions.add(CapacityOptimizationSuggestionDto(
                        sessionId = capacity.session!!.id!!,
                        sessionTitle = capacity.session!!.title,
                        currentCapacity = capacity.maximumCapacity,
                        currentRegistrations = capacity.currentRegistrations,
                        suggestedCapacity = (capacity.maximumCapacity * 1.2).toInt(),
                        optimizationType = "INCREASE",
                        reason = "High waitlist demand (${capacity.currentWaitlistCount} waiting)",
                        potentialImpact = "Accommodate more interested attendees",
                        priority = "HIGH"
                    ))
                }
            }
        }
        
        return suggestions.sortedByDescending { 
            when(it.priority) {
                "HIGH" -> 3
                "MEDIUM" -> 2
                "LOW" -> 1
                else -> 0
            }
        }
    }

    private fun SessionCapacity.updateCurrentCounts() {
        val registrationCount = sessionRegistrationRepository.countBySessionIdAndStatus(
            this.session!!.id!!, SessionRegistrationStatus.REGISTERED
        ).toInt()
        
        val waitlistCount = sessionRegistrationRepository.countBySessionIdAndStatus(
            this.session!!.id!!, SessionRegistrationStatus.WAITLIST
        ).toInt()
        
        this.currentRegistrations = registrationCount
        this.currentWaitlistCount = waitlistCount
        this.availableSpots = maxOf(0, this.maximumCapacity - registrationCount)
    }

    private fun recordCapacityChange(capacity: SessionCapacity, reason: String?) {
        // This could store capacity history in JSON format or separate audit table
        mapOf(
            "timestamp" to LocalDateTime.now(),
            "maximumCapacity" to capacity.maximumCapacity,
            "currentRegistrations" to capacity.currentRegistrations,
            "reason" to reason
        ) // TODO: Implement capacity change audit logging
        // For now, just update the timestamp. In production, you'd store in history
        capacity.lastCapacityUpdate = LocalDateTime.now()
    }

    private fun convertToDto(capacity: SessionCapacity): SessionCapacityDto {
        val utilizationPercentage = if (capacity.maximumCapacity > 0) {
            (capacity.currentRegistrations.toDouble() / capacity.maximumCapacity.toDouble()) * 100
        } else 0.0
        
        return SessionCapacityDto(
            id = capacity.id,
            sessionId = capacity.session?.id,
            sessionTitle = capacity.session?.title,
            capacityType = capacity.capacityType,
            maximumCapacity = capacity.maximumCapacity,
            minimumCapacity = capacity.minimumCapacity,
            currentRegistrations = capacity.currentRegistrations,
            availableSpots = capacity.availableSpots,
            utilizationPercentage = utilizationPercentage,
            enableWaitlist = capacity.enableWaitlist,
            waitlistCapacity = capacity.waitlistCapacity,
            currentWaitlistCount = capacity.currentWaitlistCount,
            waitlistStrategy = capacity.waitlistStrategy,
            allowOverbooking = capacity.allowOverbooking,
            overbookingPercentage = capacity.overbookingPercentage,
            autoPromoteFromWaitlist = capacity.autoPromoteFromWaitlist,
            lowCapacityThreshold = capacity.lowCapacityThreshold,
            highDemandThreshold = capacity.highDemandThreshold,
            isLowCapacity = capacity.availableSpots <= capacity.lowCapacityThreshold,
            isHighDemand = utilizationPercentage >= capacity.highDemandThreshold * 100,
            lastCapacityUpdate = capacity.lastCapacityUpdate
        )
    }

    private fun convertSessionRegistrationToDto(sessionRegistration: SessionRegistration): SessionRegistrationDto {
        return SessionRegistrationDto().apply {
            BeanUtils.copyProperties(sessionRegistration, this)
            sessionTitle = sessionRegistration.session?.title
            userName = sessionRegistration.registration?.userName
            userEmail = sessionRegistration.registration?.userEmail
        }
    }
}