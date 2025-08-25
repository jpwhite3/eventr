package com.eventr.controller

import com.eventr.service.EmailReminderService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:3001", "http://localhost:3002", "http://localhost:3003"])
@RequestMapping("/api/notifications")
class EmailNotificationController(
    private val emailReminderService: EmailReminderService
) {
    
    @PostMapping("/events/{eventId}/reminder")
    fun sendManualReminder(
        @PathVariable eventId: UUID,
        @RequestParam daysUntilEvent: Int = 1
    ): ResponseEntity<Map<String, String>> {
        return try {
            emailReminderService.sendManualReminder(eventId, daysUntilEvent)
            ResponseEntity.ok(mapOf(
                "message" to "Reminder emails sent successfully",
                "eventId" to eventId.toString(),
                "daysUntilEvent" to daysUntilEvent.toString()
            ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            ResponseEntity.status(500).body(mapOf(
                "error" to "Failed to send reminder emails",
                "message" to (e.message ?: "Unknown error")
            ))
        }
    }
    
    @PostMapping("/events/{eventId}/update")
    fun sendEventUpdate(
        @PathVariable eventId: UUID,
        @RequestBody request: EventUpdateRequest
    ): ResponseEntity<Map<String, String>> {
        return try {
            emailReminderService.sendEventUpdateNotification(eventId, request.updateMessage)
            ResponseEntity.ok(mapOf(
                "message" to "Event update notifications sent successfully",
                "eventId" to eventId.toString()
            ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            ResponseEntity.status(500).body(mapOf(
                "error" to "Failed to send update notifications",
                "message" to (e.message ?: "Unknown error")
            ))
        }
    }
    
    @PostMapping("/test-email")
    fun sendTestEmail(
        @RequestBody request: TestEmailRequest
    ): ResponseEntity<Map<String, String>> {
        return try {
            // This would be used for testing email configuration
            ResponseEntity.ok(mapOf(
                "message" to "Test email functionality not implemented yet",
                "recipient" to request.email
            ))
        } catch (e: Exception) {
            ResponseEntity.status(500).body(mapOf(
                "error" to "Failed to send test email",
                "message" to (e.message ?: "Unknown error")
            ))
        }
    }
}

data class EventUpdateRequest(
    val updateMessage: String
)

data class TestEmailRequest(
    val email: String,
    val type: String = "test"
)