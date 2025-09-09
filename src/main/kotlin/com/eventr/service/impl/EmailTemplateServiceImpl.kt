package com.eventr.service.impl

import com.eventr.model.*
import com.eventr.service.EmailTemplateService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

/**
 * Implementation of EmailTemplateService focused on email template generation.
 * 
 * Responsibilities:
 * - HTML email template creation
 * - Event data formatting for display
 * - Template personalization and customization
 * - Consistent email branding
 */
@Service
class EmailTemplateServiceImpl : EmailTemplateService {
    
    @Value("\${app.base-url:http://localhost:3002}")
    private lateinit var baseUrl: String
    
    private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    
    override fun generateRegistrationTemplate(registration: Registration, event: Event): String {
        val eventInstance = registration.eventInstance
        val userInfo = formatUserInfo(registration)
        val eventDetails = formatEventDetails(event, eventInstance)
        
        return buildEmailTemplate(
            title = "Registration Confirmed",
            greeting = "Hello ${userInfo["firstName"]},",
            content = """
                <p style="font-size: 16px; margin-bottom: 20px;">
                    Great news! Your registration for <strong>${event.name}</strong> has been confirmed.
                </p>
                
                $eventDetails
                
                <div style="background-color: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0;">
                    <h3 style="color: #495057; margin-top: 0;">Next Steps:</h3>
                    <ul style="margin: 0; padding-left: 20px;">
                        <li>Save the attached calendar file to your calendar</li>
                        <li>Arrive 15 minutes early for check-in</li>
                        <li>Bring a valid photo ID</li>
                        ${if (event.eventType == EventType.IN_PERSON) 
                            "<li>Review the location details above</li>" 
                        else 
                            "<li>Check your email for virtual meeting details</li>"}
                    </ul>
                </div>
                
                <p>
                    If you need to make changes to your registration or have questions, 
                    please contact our support team.
                </p>
            """.trimIndent()
        )
    }
    
    override fun generateReminderTemplate(registration: Registration, event: Event, daysUntil: Int): String {
        val eventInstance = registration.eventInstance
        val userInfo = formatUserInfo(registration)
        val eventDetails = formatEventDetails(event, eventInstance)
        
        val reminderMessage = when (daysUntil) {
            1 -> "Your event is tomorrow!"
            7 -> "Your event is in one week!"
            else -> "Your event is in $daysUntil days!"
        }
        
        return buildEmailTemplate(
            title = "Event Reminder",
            greeting = "Hello ${userInfo["firstName"]},",
            content = """
                <p style="font-size: 18px; font-weight: 600; color: #007bff; margin-bottom: 20px;">
                    $reminderMessage
                </p>
                
                <p style="font-size: 16px; margin-bottom: 20px;">
                    Don't forget about your upcoming event: <strong>${event.name}</strong>
                </p>
                
                $eventDetails
                
                <div style="background-color: #fff3cd; border: 1px solid #ffeaa7; padding: 15px; border-radius: 8px; margin: 20px 0;">
                    <p style="margin: 0; color: #856404;">
                        <strong>Reminder:</strong> Please arrive 15 minutes early for check-in.
                    </p>
                </div>
            """.trimIndent()
        )
    }
    
    override fun generateEventUpdateTemplate(registration: Registration, event: Event, updateMessage: String): String {
        val userInfo = formatUserInfo(registration)
        val eventDetails = formatEventDetails(event, registration.eventInstance)
        
        return buildEmailTemplate(
            title = "Event Update",
            greeting = "Hello ${userInfo["firstName"]},",
            content = """
                <p style="font-size: 16px; margin-bottom: 20px;">
                    We have an important update regarding your event: <strong>${event.name}</strong>
                </p>
                
                <div style="background-color: #d1ecf1; border: 1px solid #bee5eb; padding: 20px; border-radius: 8px; margin: 20px 0;">
                    <h3 style="color: #0c5460; margin-top: 0;">Update Details:</h3>
                    <div style="color: #0c5460;">${updateMessage.replace("\n", "<br>")}</div>
                </div>
                
                $eventDetails
                
                <p>
                    If you have any questions about this update, please don't hesitate to contact us.
                </p>
            """.trimIndent()
        )
    }
    
    override fun generateCancellationTemplate(registration: Registration, event: Event, reason: String): String {
        val userInfo = formatUserInfo(registration)
        
        return buildEmailTemplate(
            title = "Registration Cancelled",
            greeting = "Hello ${userInfo["firstName"]},",
            content = """
                <p style="font-size: 16px; margin-bottom: 20px;">
                    We're sorry to inform you that your registration for <strong>${event.name}</strong> has been cancelled.
                </p>
                
                <div style="background-color: #f8d7da; border: 1px solid #f5c6cb; padding: 20px; border-radius: 8px; margin: 20px 0;">
                    <h3 style="color: #721c24; margin-top: 0;">Cancellation Reason:</h3>
                    <p style="color: #721c24; margin: 0;">${reason.replace("\n", "<br>")}</p>
                </div>
                
                <p>
                    We apologize for any inconvenience this may cause. If you have any questions 
                    or concerns, please contact our support team.
                </p>
                
                <p>
                    We hope to see you at future events!
                </p>
            """.trimIndent()
        )
    }
    
    override fun generateCheckInTemplate(registration: Registration, event: Event): String {
        val userInfo = formatUserInfo(registration)
        val eventDetails = formatEventDetails(event, registration.eventInstance)
        
        return buildEmailTemplate(
            title = "Check-in Confirmed",
            greeting = "Hello ${userInfo["firstName"]},",
            content = """
                <p style="font-size: 16px; margin-bottom: 20px;">
                    Great! You have successfully checked in to <strong>${event.name}</strong>.
                </p>
                
                $eventDetails
                
                <div style="background-color: #d4edda; border: 1px solid #c3e6cb; padding: 20px; border-radius: 8px; margin: 20px 0;">
                    <h3 style="color: #155724; margin-top: 0;">✅ Check-in Complete</h3>
                    <p style="color: #155724; margin: 0;">
                        You're all set! Enjoy the event and don't forget to connect with other attendees.
                    </p>
                </div>
                
                <p>
                    Thank you for attending. We hope you have a great experience!
                </p>
            """.trimIndent()
        )
    }
    
    override fun generateCustomTemplate(registration: Registration, event: Event, subject: String, body: String): String {
        val userInfo = formatUserInfo(registration)
        val eventDetails = formatEventDetails(event, registration.eventInstance)
        
        return buildEmailTemplate(
            title = subject,
            greeting = "Hello ${userInfo["firstName"]},",
            content = """
                <div style="margin-bottom: 30px;">
                    ${body.replace("\n", "<br>")}
                </div>
                
                $eventDetails
            """.trimIndent()
        )
    }
    
    override fun formatEventDetails(event: Event, eventInstance: EventInstance?): String {
        val startDateTime = eventInstance?.startDateTime ?: event.startDateTime
        val endDateTime = eventInstance?.endDateTime ?: event.endDateTime
        
        return """
            <div style="background-color: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0;">
                <h3 style="color: #495057; margin-top: 0;">Event Details</h3>
                <table style="width: 100%; border-collapse: collapse;">
                    <tr>
                        <td style="padding: 8px 0; font-weight: 600; color: #6c757d;">Event:</td>
                        <td style="padding: 8px 0;">${event.name}</td>
                    </tr>
                    ${if (startDateTime != null) """
                    <tr>
                        <td style="padding: 8px 0; font-weight: 600; color: #6c757d;">Date:</td>
                        <td style="padding: 8px 0;">${startDateTime.format(dateFormatter)}</td>
                    </tr>
                    <tr>
                        <td style="padding: 8px 0; font-weight: 600; color: #6c757d;">Time:</td>
                        <td style="padding: 8px 0;">
                            ${startDateTime.format(timeFormatter)}${if (endDateTime != null) " - ${endDateTime.format(timeFormatter)}" else ""}
                        </td>
                    </tr>
                    """ else ""}
                    ${if (event.eventType == EventType.IN_PERSON && !event.location.isNullOrBlank()) """
                    <tr>
                        <td style="padding: 8px 0; font-weight: 600; color: #6c757d;">Location:</td>
                        <td style="padding: 8px 0;">${event.location}</td>
                    </tr>
                    ${if (!event.address.isNullOrBlank()) """
                    <tr>
                        <td style="padding: 8px 0; font-weight: 600; color: #6c757d;">Address:</td>
                        <td style="padding: 8px 0;">${event.address}</td>
                    </tr>
                    """ else ""}
                    """ else ""}
                    ${if (event.eventType == EventType.VIRTUAL && !event.meetingUrl.isNullOrBlank()) """
                    <tr>
                        <td style="padding: 8px 0; font-weight: 600; color: #6c757d;">Meeting Link:</td>
                        <td style="padding: 8px 0;">
                            <a href="${event.meetingUrl}" style="color: #007bff; text-decoration: none;">Join Virtual Event</a>
                        </td>
                    </tr>
                    """ else ""}
                    ${if (!event.description.isNullOrBlank()) """
                    <tr>
                        <td style="padding: 8px 0; font-weight: 600; color: #6c757d; vertical-align: top;">Description:</td>
                        <td style="padding: 8px 0;">${event.description}</td>
                    </tr>
                    """ else ""}
                </table>
            </div>
        """.trimIndent()
    }
    
    override fun formatUserInfo(registration: Registration): Map<String, String> {
        return mapOf(
            "firstName" to (registration.firstName ?: "Attendee"),
            "lastName" to (registration.lastName ?: ""),
            "email" to (registration.userEmail ?: ""),
            "fullName" to "${registration.firstName ?: "Attendee"} ${registration.lastName ?: ""}".trim()
        )
    }
    
    private fun buildEmailTemplate(title: String, greeting: String, content: String): String {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>$title - EventR</title>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #007bff; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { background-color: #ffffff; padding: 30px; border-radius: 0 0 8px 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .footer { text-align: center; margin-top: 30px; padding: 20px; color: #6c757d; font-size: 14px; }
                    a { color: #007bff; text-decoration: none; }
                    a:hover { text-decoration: underline; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1 style="margin: 0;">EventR</h1>
                        <p style="margin: 10px 0 0 0;">$title</p>
                    </div>
                    <div class="content">
                        <p style="font-size: 16px; margin-bottom: 20px;">$greeting</p>
                        $content
                        <div style="margin-top: 30px; padding-top: 20px; border-top: 1px solid #e9ecef;">
                            <p style="margin: 0;">
                                Best regards,<br>
                                The EventR Team
                            </p>
                        </div>
                    </div>
                    <div class="footer">
                        <p>© 2025 EventR. All rights reserved.</p>
                        <p>
                            <a href="$baseUrl">Visit our website</a> | 
                            <a href="$baseUrl/support">Support</a>
                        </p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}