package com.eventr.service

import com.eventr.model.*

/**
 * Service interface for email notification operations.
 * 
 * Focuses exclusively on sending email notifications:
 * - Event registration confirmations
 * - Event reminders and updates
 * - Cancellation notifications
 * - Custom event communications
 * 
 * Follows Single Responsibility Principle by handling only email delivery logic.
 */
interface EmailNotificationService {

    /**
     * Send registration confirmation email to user.
     * 
     * @param registration User registration details
     * @throws Exception if email sending fails
     */
    fun sendRegistrationConfirmation(registration: Registration)

    /**
     * Send event reminder email to registered users.
     * 
     * @param registration User registration details
     * @param daysUntil Number of days until event
     * @throws Exception if email sending fails
     */
    fun sendEventReminder(registration: Registration, daysUntil: Int)

    /**
     * Send event update notification to registrations.
     * 
     * @param event Event that was updated
     * @param updateMessage Update message to send
     * @param registrations List of registrations to notify
     * @return Number of emails successfully sent
     */
    fun sendEventUpdate(event: Event, updateMessage: String, registrations: List<Registration>): Int

    /**
     * Send event cancellation notification to registrations.
     * 
     * @param registration User registration to cancel
     * @param reason Cancellation reason
     * @throws Exception if email sending fails
     */
    fun sendCancellationNotification(registration: Registration, reason: String)

    /**
     * Send custom email to specific registration.
     * 
     * @param registration Target registration
     * @param subject Email subject
     * @param body Email body content
     * @throws Exception if email sending fails
     */
    fun sendCustomEmail(registration: Registration, subject: String, body: String)

    /**
     * Send bulk custom email to multiple registrations.
     * 
     * @param registrations List of target registrations
     * @param subject Email subject
     * @param body Email body content
     * @return Email sending results (sent count, failed count)
     */
    fun sendBulkCustomEmail(registrations: List<Registration>, subject: String, body: String): EmailBulkResult

    /**
     * Send check-in confirmation email to user.
     * 
     * @param registration User registration that was checked in
     * @throws Exception if email sending fails
     */
    fun sendCheckInConfirmation(registration: Registration)
}

/**
 * Result data for bulk email operations.
 */
data class EmailBulkResult(
    val totalRecipients: Int,
    val emailsSent: Int,
    val emailsFailed: Int
)