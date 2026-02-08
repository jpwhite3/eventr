package com.eventr.modules.event.api

import com.eventr.modules.event.api.dto.*
import java.util.UUID

/**
 * Public API for the Event module.
 * 
 * This is the ONLY way other modules should interact with events.
 * Do not access repositories or internal services directly.
 * 
 * Responsibilities:
 * - Event CRUD operations
 * - Event publishing lifecycle
 * - Event instance management
 * - Event queries and filtering
 */
interface EventModuleApi {
    
    // ==================== Event Operations ====================
    
    /**
     * Creates a new event in DRAFT status.
     * Automatically creates a default EventInstance if startDateTime is provided.
     * 
     * @param request Event creation data
     * @return Created event with generated ID and default instance
     */
    fun createEvent(request: CreateEventRequest): EventResponse
    
    /**
     * Gets an event by ID.
     * 
     * @param id Event ID
     * @return Event details or null if not found
     */
    fun getEvent(id: UUID): EventResponse?
    
    /**
     * Gets an event by ID, throwing exception if not found.
     * 
     * @param id Event ID
     * @return Event details
     * @throws EventNotFoundException if event doesn't exist
     */
    fun getEventOrThrow(id: UUID): EventResponse
    
    /**
     * Updates an existing event.
     * 
     * @param id Event ID
     * @param request Update data
     * @return Updated event
     */
    fun updateEvent(id: UUID, request: UpdateEventRequest): EventResponse
    
    /**
     * Deletes an event and all related data.
     * 
     * @param id Event ID
     */
    fun deleteEvent(id: UUID)
    
    /**
     * Finds events matching the given criteria.
     * 
     * @param criteria Filter and sort criteria
     * @return List of matching events
     */
    fun findEvents(criteria: EventFilterCriteria): List<EventResponse>
    
    // ==================== Event Lifecycle ====================
    
    /**
     * Publishes an event, making it visible to attendees.
     * Publishes EventPublished domain event.
     * 
     * @param id Event ID
     * @return Updated event with PUBLISHED status
     */
    fun publishEvent(id: UUID): EventResponse
    
    /**
     * Cancels an event.
     * Publishes EventCancelled domain event (for registration module to react).
     * 
     * @param id Event ID
     * @param reason Optional cancellation reason
     * @return Updated event with CANCELLED status
     */
    fun cancelEvent(id: UUID, reason: String? = null): EventResponse
    
    /**
     * Clones an event to create a new draft copy.
     * 
     * @param id Source event ID
     * @return New event in DRAFT status
     */
    fun cloneEvent(id: UUID): EventResponse
    
    // ==================== Event Instance Operations ====================
    
    /**
     * Gets an event instance by ID.
     * Used by Registration module to validate registration requests.
     * 
     * @param instanceId Event instance ID
     * @return Event instance or null if not found
     */
    fun getEventInstance(instanceId: UUID): EventInstanceResponse?
    
    /**
     * Gets the event ID for a given instance.
     * Used by other modules to find the parent event.
     * 
     * @param instanceId Event instance ID
     * @return Event ID or null if instance not found
     */
    fun getEventIdForInstance(instanceId: UUID): UUID?
    
    /**
     * Gets all instances for an event.
     * 
     * @param eventId Event ID
     * @return List of event instances
     */
    fun getEventInstances(eventId: UUID): List<EventInstanceResponse>
}

/**
 * Exception thrown when an event is not found
 */
class EventNotFoundException(id: UUID) : RuntimeException("Event not found: $id")

/**
 * Exception thrown when an event instance is not found
 */
class EventInstanceNotFoundException(id: UUID) : RuntimeException("Event instance not found: $id")
