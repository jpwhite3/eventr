package com.eventr.service

import com.eventr.dto.*
import java.time.LocalDate
import java.util.*

/**
 * Service interface for event management operations.
 * 
 * Provides high-level business operations for events, abstracting away
 * data access and infrastructure concerns from controllers.
 * 
 * Responsibilities:
 * - Event CRUD operations with business validation
 * - Event publishing and status management
 * - Event filtering and search operations
 * - Event registration management
 * - Event notifications and email handling
 * - DTO conversion and mapping
 * - Transaction management
 */
interface EventService {

    /**
     * Create a new event with business validation.
     * 
     * @param eventDto Event creation data
     * @return Created event with generated ID
     * @throws ValidationException if event data is invalid
     * @throws BusinessRuleException if business rules are violated
     */
    fun createEvent(eventDto: EventCreateDto): EventDto

    /**
     * Find events by filtering criteria with pagination support.
     * 
     * @param city Optional city filter
     * @param category Optional category filter
     * @param eventType Optional event type filter
     * @param search Optional search term
     * @param published Optional published status filter
     * @param startDate Optional start date filter
     * @param endDate Optional end date filter
     * @param sortBy Optional sort field
     * @param sortDirection Optional sort direction
     * @return List of events matching criteria
     */
    fun findEvents(
        city: String? = null,
        category: String? = null,
        eventType: String? = null,
        search: String? = null,
        published: Boolean? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        sortBy: String? = null,
        sortDirection: String? = null
    ): List<EventDto>

    /**
     * Find event by ID with full details.
     * 
     * @param id Event ID
     * @return Event details or null if not found
     */
    fun findEventById(id: UUID): EventDto?

    /**
     * Update existing event with business validation.
     * 
     * @param id Event ID to update
     * @param updateDto Event update data
     * @return Updated event
     * @throws EntityNotFoundException if event not found
     * @throws ValidationException if update data is invalid
     * @throws BusinessRuleException if business rules are violated
     */
    fun updateEvent(id: UUID, updateDto: EventUpdateDto): EventDto

    /**
     * Publish an event, making it public and sending notifications.
     * 
     * @param id Event ID to publish
     * @return Published event
     * @throws EntityNotFoundException if event not found
     * @throws BusinessRuleException if event cannot be published
     */
    fun publishEvent(id: UUID): EventDto

    /**
     * Clone an existing event with new details.
     * 
     * @param id Source event ID
     * @param cloneData Optional override data
     * @return Cloned event
     * @throws EntityNotFoundException if source event not found
     */
    fun cloneEvent(id: UUID, cloneData: EventCreateDto? = null): EventDto

    /**
     * Delete an event and handle associated data cleanup.
     * 
     * @param id Event ID to delete
     * @throws EntityNotFoundException if event not found
     * @throws BusinessRuleException if event cannot be deleted
     */
    fun deleteEvent(id: UUID)

    /**
     * Cancel multiple registrations for an event.
     * 
     * @param id Event ID
     * @param registrationIds List of registration IDs to cancel
     * @param reason Optional cancellation reason
     * @return Cancellation results
     */
    fun cancelRegistrations(id: UUID, registrationIds: List<UUID>, reason: String? = null): Map<String, Any>

    /**
     * Send custom email to event registrations.
     * 
     * @param id Event ID
     * @param subject Email subject
     * @param body Email body
     * @param registrationIds Optional specific registration IDs
     * @return Email sending results
     */
    fun sendEventEmail(id: UUID, subject: String, body: String, registrationIds: List<UUID>? = null): Map<String, Any>

    /**
     * Get event registrations with filtering.
     * 
     * @param id Event ID
     * @param status Optional registration status filter
     * @return List of registrations
     */
    fun getEventRegistrations(id: UUID, status: String? = null): List<RegistrationDto>

    /**
     * Get event instances for recurring events.
     * 
     * @param id Event ID
     * @return List of event instances
     */
    fun getEventInstances(id: UUID): List<EventInstanceDto>
}