package com.eventr.service

import com.eventr.model.Event
import com.eventr.model.EventInstance
import com.eventr.model.Registration
import jakarta.mail.MessagingException
import jakarta.mail.internet.MimeMessage
import net.fortuna.ical4j.data.CalendarOutputter
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.ProdId
import net.fortuna.ical4j.model.property.Uid
import net.fortuna.ical4j.model.property.Version
import org.springframework.core.io.ByteArrayResource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.UUID

@Service
class EmailService(private val mailSender: JavaMailSender) {
    
    private val baseUrl = "http://localhost:3002" // Should be configurable
    
    @Throws(MessagingException::class, IOException::class)
    fun sendRegistrationConfirmation(registration: Registration) {
        val event = registration.eventInstance?.event ?: throw IllegalArgumentException("Event cannot be null")
        val eventInstance = registration.eventInstance ?: throw IllegalArgumentException("Event instance cannot be null")
        
        val message: MimeMessage = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true, "UTF-8")
        
        registration.userEmail?.let { email ->
            helper.setTo(email)
        } ?: throw IllegalArgumentException("User email cannot be null")
        
        helper.setSubject("✅ Registration Confirmed: ${event.name}")
        helper.setText(buildRegistrationConfirmationEmail(registration, event), true)
        
        // Add calendar attachment
        val icsFile = createIcsFile(eventInstance)
        helper.addAttachment("${event.name?.replace("[^a-zA-Z0-9]".toRegex(), "_")}.ics", ByteArrayResource(icsFile))
        
        mailSender.send(message)
    }
    
    @Throws(MessagingException::class)
    fun sendEventReminder(registration: Registration, daysUntilEvent: Int) {
        val event = registration.eventInstance?.event ?: return
        
        val message: MimeMessage = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true, "UTF-8")
        
        registration.userEmail?.let { helper.setTo(it) } ?: return
        helper.setSubject("🔔 Reminder: ${event.name} is in $daysUntilEvent day${if (daysUntilEvent != 1) "s" else ""}")
        helper.setText(buildEventReminderEmail(registration, event, daysUntilEvent), true)
        
        mailSender.send(message)
    }
    
    @Throws(MessagingException::class)
    fun sendEventUpdate(event: Event, updateMessage: String, registrations: List<Registration>) {
        registrations.forEach { registration ->
            val message: MimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")
            
            registration.userEmail?.let { helper.setTo(it) } ?: return@forEach
            helper.setSubject("📢 Event Update: ${event.name}")
            helper.setText(buildEventUpdateEmail(registration, event, updateMessage), true)
            
            mailSender.send(message)
        }
    }
    
    @Throws(MessagingException::class)
    fun sendCancellationNotification(registration: Registration, reason: String = "") {
        val event = registration.eventInstance?.event ?: return
        
        val message: MimeMessage = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true, "UTF-8")
        
        registration.userEmail?.let { helper.setTo(it) } ?: return
        helper.setSubject("❌ Event Cancelled: ${event.name}")
        helper.setText(buildCancellationEmail(registration, event, reason), true)
        
        mailSender.send(message)
    }
    
    @Throws(IOException::class)
    private fun createIcsFile(eventInstance: EventInstance): ByteArray {
        val calendar = Calendar().apply {
            properties.add(ProdId("-//Eventr//EN"))
            properties.add(Version.VERSION_2_0)
        }
        
        val event = eventInstance.dateTime?.let { dateTime ->
            VEvent(
                net.fortuna.ical4j.model.DateTime(
                    Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant())
                ),
                eventInstance.event?.name
            ).apply {
                properties.add(Uid(eventInstance.id.toString()))
            }
        }
        
        event?.let { calendar.components.add(it) }
        
        val out = ByteArrayOutputStream()
        CalendarOutputter().output(calendar, out)
        
        return out.toByteArray()
    }
    
    private fun buildRegistrationConfirmationEmail(registration: Registration, event: Event): String {
        val eventUrl = "$baseUrl/events/${event.id}"
        val startDateTime = event.startDateTime?.let { ZonedDateTime.parse(it) }
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .header { background: #007bff; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; }
                    .event-details { background: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0; }
                    .button { background: #007bff; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; display: inline-block; }
                    .footer { background: #f8f9fa; padding: 15px; text-align: center; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>✅ Registration Confirmed!</h1>
                </div>
                
                <div class="content">
                    <h2>Hello ${registration.userName ?: "Event Attendee"},</h2>
                    
                    <p>Thank you for registering for <strong>${event.name}</strong>! We're excited to have you join us.</p>
                    
                    <div class="event-details">
                        <h3>Event Details:</h3>
                        <p><strong>📅 Date & Time:</strong> ${formatEventDateTime(startDateTime)}</p>
                        <p><strong>📍 Location:</strong> ${formatEventLocation(event)}</p>
                        ${if (event.organizerName != null) "<p><strong>👤 Organizer:</strong> ${event.organizerName}</p>" else ""}
                        <p><strong>🎟️ Registration ID:</strong> ${registration.id}</p>
                    </div>
                    
                    ${if (!event.description.isNullOrBlank()) """
                        <h3>About This Event:</h3>
                        <p>${event.description}</p>
                    """ else ""}
                    
                    <p style="text-align: center; margin: 30px 0;">
                        <a href="$eventUrl" class="button">View Event Details</a>
                    </p>
                    
                    <h3>What's Next?</h3>
                    <ul>
                        <li>Add the event to your calendar using the attached file</li>
                        <li>Save your registration ID for check-in</li>
                        <li>We'll send you reminders as the event approaches</li>
                    </ul>
                    
                    ${buildVirtualEventInfo(event)}
                </div>
                
                <div class="footer">
                    <p>Questions? Contact us at ${event.organizerEmail ?: "support@eventr.com"}</p>
                    <p>This is an automated message from Eventr Event Management System.</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
    
    private fun buildEventReminderEmail(registration: Registration, event: Event, daysUntilEvent: Int): String {
        val eventUrl = "$baseUrl/events/${event.id}"
        val startDateTime = event.startDateTime?.let { ZonedDateTime.parse(it) }
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .header { background: #28a745; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; }
                    .reminder-box { background: #fff3cd; border: 1px solid #ffeaa7; padding: 15px; border-radius: 5px; margin: 20px 0; }
                    .button { background: #28a745; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; display: inline-block; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>🔔 Event Reminder</h1>
                </div>
                
                <div class="content">
                    <h2>Hello ${registration.userName ?: "Event Attendee"},</h2>
                    
                    <div class="reminder-box">
                        <h3>⏰ Just ${daysUntilEvent} day${if (daysUntilEvent != 1) "s" else ""} until:</h3>
                        <h2>${event.name}</h2>
                        <p><strong>📅 When:</strong> ${formatEventDateTime(startDateTime)}</p>
                        <p><strong>📍 Where:</strong> ${formatEventLocation(event)}</p>
                    </div>
                    
                    <p>We're looking forward to seeing you there!</p>
                    
                    <h3>Don't Forget:</h3>
                    <ul>
                        <li>Bring your registration ID: <strong>${registration.id}</strong></li>
                        <li>Check the event details for any last-minute updates</li>
                        <li>Plan your route if attending in person</li>
                    </ul>
                    
                    ${buildVirtualEventInfo(event)}
                    
                    <p style="text-align: center; margin: 30px 0;">
                        <a href="$eventUrl" class="button">View Event Details</a>
                    </p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
    
    private fun buildEventUpdateEmail(registration: Registration, event: Event, updateMessage: String): String {
        val eventUrl = "$baseUrl/events/${event.id}"
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .header { background: #17a2b8; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; }
                    .update-box { background: #d4edda; border: 1px solid #c3e6cb; padding: 15px; border-radius: 5px; margin: 20px 0; }
                    .button { background: #17a2b8; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; display: inline-block; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>📢 Event Update</h1>
                </div>
                
                <div class="content">
                    <h2>Hello ${registration.userName ?: "Event Attendee"},</h2>
                    
                    <p>We have an important update regarding your registered event:</p>
                    
                    <h3>${event.name}</h3>
                    
                    <div class="update-box">
                        <h4>What's New:</h4>
                        <p>${updateMessage}</p>
                    </div>
                    
                    <p style="text-align: center; margin: 30px 0;">
                        <a href="$eventUrl" class="button">View Full Event Details</a>
                    </p>
                    
                    <p>If you have any questions about these changes, please don't hesitate to contact the event organizer.</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
    
    private fun buildCancellationEmail(registration: Registration, event: Event, reason: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .header { background: #dc3545; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; }
                    .cancellation-box { background: #f8d7da; border: 1px solid #f5c6cb; padding: 15px; border-radius: 5px; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>❌ Event Cancelled</h1>
                </div>
                
                <div class="content">
                    <h2>Hello ${registration.userName ?: "Event Attendee"},</h2>
                    
                    <p>We regret to inform you that the following event has been cancelled:</p>
                    
                    <div class="cancellation-box">
                        <h3>${event.name}</h3>
                        <p><strong>Registration ID:</strong> ${registration.id}</p>
                        ${if (reason.isNotBlank()) "<p><strong>Reason:</strong> $reason</p>" else ""}
                    </div>
                    
                    <p>We apologize for any inconvenience this may cause. If this event is rescheduled, we will notify you immediately.</p>
                    
                    <p>Thank you for your understanding.</p>
                    
                    ${if (event.organizerEmail != null) """
                        <p>For questions, please contact: <a href="mailto:${event.organizerEmail}">${event.organizerEmail}</a></p>
                    """ else ""}
                </div>
            </body>
            </html>
        """.trimIndent()
    }
    
    private fun formatEventDateTime(dateTime: ZonedDateTime?): String {
        return dateTime?.let {
            val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm a z")
            it.format(formatter)
        } ?: "Date/Time TBD"
    }
    
    private fun formatEventLocation(event: Event): String {
        return when (event.eventType) {
            "VIRTUAL" -> "Virtual Event"
            "HYBRID" -> {
                val physicalLocation = buildPhysicalLocationString(event)
                if (physicalLocation.isNotEmpty()) "$physicalLocation (Hybrid Event)" else "Hybrid Event"
            }
            else -> buildPhysicalLocationString(event).ifEmpty { "Location TBD" }
        }
    }
    
    private fun buildPhysicalLocationString(event: Event): String {
        val parts = mutableListOf<String>()
        event.venueName?.let { parts.add(it) }
        event.address?.let { parts.add(it) }
        val cityState = listOfNotNull(event.city, event.state).joinToString(", ")
        if (cityState.isNotEmpty()) parts.add(cityState)
        return parts.joinToString(", ")
    }
    
    private fun buildVirtualEventInfo(event: Event): String {
        return if (event.eventType == "VIRTUAL" || event.eventType == "HYBRID") {
            """
                <h3>🖥️ Virtual Event Information:</h3>
                <div style="background: #e3f2fd; padding: 15px; border-radius: 5px; margin: 20px 0;">
                    ${if (!event.virtualUrl.isNullOrBlank()) "<p><strong>Meeting URL:</strong> <a href='${event.virtualUrl}'>${event.virtualUrl}</a></p>" else ""}
                    ${if (!event.dialInNumber.isNullOrBlank()) "<p><strong>Dial-in Number:</strong> ${event.dialInNumber}</p>" else ""}
                    ${if (!event.accessCode.isNullOrBlank()) "<p><strong>Access Code:</strong> ${event.accessCode}</p>" else ""}
                    <p><em>Connection details will be provided closer to the event date if not available above.</em></p>
                </div>
            """.trimIndent()
        } else ""
    }
}
