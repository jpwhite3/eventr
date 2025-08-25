package com.eventr.service

import com.eventr.model.Event
import com.eventr.repository.RegistrationRepository
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import java.util.*

@Service
class WebSocketEventService(
    private val messagingTemplate: SimpMessagingTemplate,
    private val registrationRepository: RegistrationRepository
) {
    
    fun broadcastAttendanceUpdate(eventId: UUID) {
        val attendanceCount = getAttendanceCount(eventId)
        val registrationCount = getRegistrationCount(eventId)
        
        val update = mapOf(
            "type" to "ATTENDANCE_UPDATE",
            "eventId" to eventId.toString(),
            "attendanceCount" to attendanceCount,
            "registrationCount" to registrationCount,
            "timestamp" to System.currentTimeMillis()
        )
        
        messagingTemplate.convertAndSend("/topic/events/$eventId/attendance", update)
        messagingTemplate.convertAndSend("/topic/events/attendance", update)
    }
    
    fun broadcastCapacityUpdate(eventId: UUID, currentCapacity: Int, maxCapacity: Int?) {
        val capacityPercentage = if (maxCapacity != null && maxCapacity > 0) {
            (currentCapacity.toDouble() / maxCapacity * 100).toInt()
        } else null
        
        val update = mapOf(
            "type" to "CAPACITY_UPDATE",
            "eventId" to eventId.toString(),
            "currentCapacity" to currentCapacity,
            "maxCapacity" to maxCapacity,
            "capacityPercentage" to capacityPercentage,
            "isNearCapacity" to (capacityPercentage != null && capacityPercentage >= 90),
            "timestamp" to System.currentTimeMillis()
        )
        
        messagingTemplate.convertAndSend("/topic/events/$eventId/capacity", update)
        messagingTemplate.convertAndSend("/topic/events/capacity", update)
    }
    
    fun broadcastEventStatusChange(eventId: UUID, oldStatus: String?, newStatus: String, message: String? = null) {
        val update = mapOf(
            "type" to "EVENT_STATUS_CHANGE",
            "eventId" to eventId.toString(),
            "oldStatus" to oldStatus,
            "newStatus" to newStatus,
            "message" to message,
            "timestamp" to System.currentTimeMillis()
        )
        
        messagingTemplate.convertAndSend("/topic/events/$eventId/status", update)
        messagingTemplate.convertAndSend("/topic/events/status", update)
    }
    
    fun broadcastRegistrationUpdate(eventId: UUID, registrationType: String, registrationData: Map<String, Any>) {
        val update = mapOf(
            "type" to "REGISTRATION_UPDATE",
            "eventId" to eventId.toString(),
            "registrationType" to registrationType, // NEW, CANCELLED, UPDATED
            "registrationData" to registrationData,
            "timestamp" to System.currentTimeMillis()
        )
        
        messagingTemplate.convertAndSend("/topic/events/$eventId/registrations", update)
        messagingTemplate.convertAndSend("/topic/events/registrations", update)
        
        // Also broadcast attendance update since registration affects attendance
        broadcastAttendanceUpdate(eventId)
    }
    
    fun broadcastCheckInUpdate(eventId: UUID, userName: String, userEmail: String, status: String) {
        val update = mapOf(
            "type" to "CHECKIN_UPDATE",
            "eventId" to eventId.toString(),
            "userName" to userName,
            "userEmail" to userEmail,
            "status" to status, // CHECKED_IN, ALREADY_CHECKED_IN, FAILED
            "timestamp" to System.currentTimeMillis()
        )
        
        messagingTemplate.convertAndSend("/topic/events/$eventId/checkins", update)
        messagingTemplate.convertAndSend("/topic/events/checkins", update)
        
        // Update attendance count
        broadcastAttendanceUpdate(eventId)
    }
    
    fun broadcastEventUpdate(eventId: UUID, updateType: String, updateData: Map<String, Any>) {
        val update = mapOf(
            "type" to "EVENT_UPDATE",
            "eventId" to eventId.toString(),
            "updateType" to updateType, // DETAILS_CHANGED, TIME_CHANGED, LOCATION_CHANGED, etc.
            "updateData" to updateData,
            "timestamp" to System.currentTimeMillis()
        )
        
        messagingTemplate.convertAndSend("/topic/events/$eventId/updates", update)
        messagingTemplate.convertAndSend("/topic/events/updates", update)
    }
    
    // Send message to specific user
    fun sendUserNotification(userId: String, notification: Map<String, Any>) {
        messagingTemplate.convertAndSend("/queue/users/$userId/notifications", notification)
    }
    
    // Broadcast system-wide announcements
    fun broadcastSystemAnnouncement(announcement: Map<String, Any>) {
        messagingTemplate.convertAndSend("/topic/system/announcements", announcement)
    }
    
    private fun getAttendanceCount(eventId: UUID): Int {
        // This would need to be implemented based on your check-in logic
        // For now, return a placeholder
        return registrationRepository.findByEventInstanceEventId(eventId).size
    }
    
    private fun getRegistrationCount(eventId: UUID): Int {
        return registrationRepository.findByEventInstanceEventId(eventId).size
    }
}