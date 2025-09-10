package com.eventr.service.impl

import com.eventr.model.*
import com.eventr.service.EmailNotificationService
import com.eventr.service.EmailTemplateService
import com.eventr.service.CalendarAttachmentService
import com.eventr.service.EmailBulkResult
import com.eventr.util.SecureLogger
import jakarta.mail.MessagingException
import jakarta.mail.internet.MimeMessage
import org.springframework.core.io.ByteArrayResource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

/**
 * Implementation of EmailNotificationService focused on email delivery operations.
 * 
 * Responsibilities:
 * - Email sending operations
 * - Email delivery error handling
 * - Bulk email operations
 * - Email attachment handling
 */
@Service
class EmailNotificationServiceImpl(
    private val mailSender: JavaMailSender,
    private val templateService: EmailTemplateService,
    private val calendarService: CalendarAttachmentService
) : EmailNotificationService {
    
    private val secureLogger = SecureLogger(EmailNotificationServiceImpl::class.java)
    
    override fun sendRegistrationConfirmation(registration: Registration) {
        val event = registration.eventInstance?.event 
            ?: throw IllegalArgumentException("Event cannot be null")
        val eventInstance = registration.eventInstance 
            ?: throw IllegalArgumentException("Event instance cannot be null")
        
        try {
            val message: MimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")
            
            val userEmail = registration.userEmail 
                ?: throw IllegalArgumentException("User email cannot be null")
            
            helper.setTo(userEmail)
            helper.setSubject("✅ Registration Confirmed: ${event.name}")
            helper.setText(templateService.generateRegistrationTemplate(registration, event), true)
            
            // Add calendar attachment
            val icsFile = calendarService.createIcsFile(eventInstance)
            val filename = calendarService.generateCalendarFilename(event)
            helper.addAttachment(filename, ByteArrayResource(icsFile))
            
            mailSender.send(message)
            
            secureLogger.logEmailEvent(
                registration.user?.id!!, 
                "REGISTRATION_CONFIRMATION_SENT", 
                true, 
                "Registration confirmation email sent successfully"
            )
            
        } catch (e: Exception) {
            secureLogger.logErrorEvent(
                "REGISTRATION_EMAIL_FAILED", 
                registration.user?.id!!, 
                e, 
                "Failed to send registration confirmation email"
            )
            throw e
        }
    }
    
    override fun sendEventReminder(registration: Registration, daysUntil: Int) {
        val event = registration.eventInstance?.event 
            ?: throw IllegalArgumentException("Event cannot be null")
        
        try {
            val message: MimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")
            
            val userEmail = registration.userEmail 
                ?: throw IllegalArgumentException("User email cannot be null")
            
            helper.setTo(userEmail)
            helper.setSubject("🔔 Event Reminder: ${event.name} in $daysUntil days")
            helper.setText(templateService.generateReminderTemplate(registration, event, daysUntil), true)
            
            mailSender.send(message)
            
            secureLogger.logEmailEvent(
                registration.user?.id!!, 
                "EVENT_REMINDER_SENT", 
                true, 
                "Event reminder email sent for $daysUntil days"
            )
            
        } catch (e: Exception) {
            secureLogger.logErrorEvent(
                "REMINDER_EMAIL_FAILED", 
                registration.user?.id!!, 
                e, 
                "Failed to send event reminder email"
            )
            throw e
        }
    }
    
    override fun sendEventUpdate(event: Event, updateMessage: String, registrations: List<Registration>): Int {
        var emailsSent = 0
        
        registrations.forEach { registration ->
            try {
                val message: MimeMessage = mailSender.createMimeMessage()
                val helper = MimeMessageHelper(message, true, "UTF-8")
                
                val userEmail = registration.userEmail 
                    ?: throw IllegalArgumentException("User email cannot be null")
                
                helper.setTo(userEmail)
                helper.setSubject("📢 Event Update: ${event.name}")
                helper.setText(templateService.generateEventUpdateTemplate(registration, event, updateMessage), true)
                
                mailSender.send(message)
                emailsSent++
                
                secureLogger.logEmailEvent(
                    registration.user?.id!!, 
                    "EVENT_UPDATE_SENT", 
                    true, 
                    "Event update email sent successfully"
                )
                
            } catch (e: Exception) {
                secureLogger.logErrorEvent(
                    "EVENT_UPDATE_EMAIL_FAILED", 
                    registration.user?.id!!, 
                    e, 
                    "Failed to send event update email"
                )
            }
        }
        
        return emailsSent
    }
    
    override fun sendCancellationNotification(registration: Registration, reason: String) {
        val event = registration.eventInstance?.event 
            ?: throw IllegalArgumentException("Event cannot be null")
        
        try {
            val message: MimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")
            
            val userEmail = registration.userEmail 
                ?: throw IllegalArgumentException("User email cannot be null")
            
            helper.setTo(userEmail)
            helper.setSubject("❌ Registration Cancelled: ${event.name}")
            helper.setText(templateService.generateCancellationTemplate(registration, event, reason), true)
            
            mailSender.send(message)
            
            secureLogger.logEmailEvent(
                registration.user?.id!!, 
                "CANCELLATION_EMAIL_SENT", 
                true, 
                "Cancellation notification email sent"
            )
            
        } catch (e: Exception) {
            secureLogger.logErrorEvent(
                "CANCELLATION_EMAIL_FAILED", 
                registration.user?.id!!, 
                e, 
                "Failed to send cancellation notification email"
            )
            throw e
        }
    }
    
    override fun sendCustomEmail(registration: Registration, subject: String, body: String) {
        val event = registration.eventInstance?.event 
            ?: throw IllegalArgumentException("Event cannot be null")
        
        try {
            val message: MimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")
            
            val userEmail = registration.userEmail 
                ?: throw IllegalArgumentException("User email cannot be null")
            
            helper.setTo(userEmail)
            helper.setSubject(subject)
            helper.setText(templateService.generateCustomTemplate(registration, event, subject, body), true)
            
            mailSender.send(message)
            
            secureLogger.logEmailEvent(
                registration.user?.id!!, 
                "CUSTOM_EMAIL_SENT", 
                true, 
                "Custom email sent successfully"
            )
            
        } catch (e: Exception) {
            secureLogger.logErrorEvent(
                "CUSTOM_EMAIL_FAILED", 
                registration.user?.id!!, 
                e, 
                "Failed to send custom email"
            )
            throw e
        }
    }
    
    override fun sendBulkCustomEmail(registrations: List<Registration>, subject: String, body: String): EmailBulkResult {
        var emailsSent = 0
        var emailsFailed = 0
        
        registrations.forEach { registration ->
            try {
                sendCustomEmail(registration, subject, body)
                emailsSent++
            } catch (e: Exception) {
                emailsFailed++
                // Error already logged in sendCustomEmail
            }
        }
        
        return EmailBulkResult(
            totalRecipients = registrations.size,
            emailsSent = emailsSent,
            emailsFailed = emailsFailed
        )
    }
    
    override fun sendCheckInConfirmation(registration: Registration) {
        val event = registration.eventInstance?.event 
            ?: throw IllegalArgumentException("Event cannot be null")
        
        try {
            val message: MimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")
            
            val userEmail = registration.userEmail 
                ?: throw IllegalArgumentException("User email cannot be null")
            
            helper.setTo(userEmail)
            helper.setSubject("✅ Check-in Confirmed: ${event.name}")
            helper.setText(templateService.generateCheckInTemplate(registration, event), true)
            
            mailSender.send(message)
            
            secureLogger.logEmailEvent(
                registration.user?.id!!, 
                "CHECKIN_CONFIRMATION_SENT", 
                true, 
                "Check-in confirmation email sent"
            )
            
        } catch (e: Exception) {
            secureLogger.logErrorEvent(
                "CHECKIN_EMAIL_FAILED", 
                registration.user?.id!!, 
                e, 
                "Failed to send check-in confirmation email"
            )
            throw e
        }
    }
}