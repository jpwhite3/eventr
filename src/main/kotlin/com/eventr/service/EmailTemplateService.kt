package com.eventr.service

import com.eventr.model.*

/**
 * Service interface for email template generation and formatting.
 * 
 * Focuses exclusively on email content generation:
 * - HTML email template creation
 * - Event data formatting for emails
 * - Template customization and personalization
 * 
 * Follows Single Responsibility Principle by handling only template generation logic.
 */
interface EmailTemplateService {

    /**
     * Generate HTML email template for registration confirmation.
     * 
     * @param registration User registration details
     * @param event Event information
     * @return Formatted HTML email content
     */
    fun generateRegistrationTemplate(registration: Registration, event: Event): String

    /**
     * Generate HTML email template for event reminder.
     * 
     * @param registration User registration details
     * @param event Event information
     * @param daysUntil Number of days until event
     * @return Formatted HTML email content
     */
    fun generateReminderTemplate(registration: Registration, event: Event, daysUntil: Int): String

    /**
     * Generate HTML email template for event updates.
     * 
     * @param registration User registration details
     * @param event Event information
     * @param updateMessage Update message content
     * @return Formatted HTML email content
     */
    fun generateEventUpdateTemplate(registration: Registration, event: Event, updateMessage: String): String

    /**
     * Generate HTML email template for cancellation notification.
     * 
     * @param registration User registration details
     * @param event Event information
     * @param reason Cancellation reason
     * @return Formatted HTML email content
     */
    fun generateCancellationTemplate(registration: Registration, event: Event, reason: String): String

    /**
     * Generate HTML email template for check-in confirmation.
     * 
     * @param registration User registration details
     * @param event Event information
     * @return Formatted HTML email content
     */
    fun generateCheckInTemplate(registration: Registration, event: Event): String

    /**
     * Generate custom HTML email template with provided content.
     * 
     * @param registration User registration details
     * @param event Event information
     * @param subject Email subject
     * @param body Email body content
     * @return Formatted HTML email content
     */
    fun generateCustomTemplate(registration: Registration, event: Event, subject: String, body: String): String

    /**
     * Format event details for email display.
     * 
     * @param event Event information
     * @param eventInstance Specific event instance (for recurring events)
     * @return Formatted event details HTML
     */
    fun formatEventDetails(event: Event, eventInstance: EventInstance?): String

    /**
     * Format user information for email personalization.
     * 
     * @param registration User registration details
     * @return Formatted user information for templates
     */
    fun formatUserInfo(registration: Registration): Map<String, String>
}