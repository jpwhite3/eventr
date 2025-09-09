package com.eventr.testutil

import com.eventr.model.*
import com.eventr.testutil.builders.*
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime

/**
 * Test data validation utilities to ensure consistency and correctness of test data.
 * These utilities help catch test data configuration errors early and ensure
 * that test scenarios are realistic and properly structured.
 * 
 * Usage:
 * ```kotlin
 * val event = TestDataBuilders.event().build()
 * TestDataValidation.validateEvent(event)
 * 
 * val scenario = TestFixtures.conferenceScenario()
 * TestDataValidation.validateConferenceScenario(scenario)
 * ```
 */
object TestDataValidation {
    
    // ================================================================================
    // Entity Validation Methods
    // ================================================================================
    
    /**
     * Validates that an Event entity has consistent and realistic data.
     */
    fun validateEvent(event: Event, strict: Boolean = false) {
        assertNotNull(event.id, "Event ID should not be null")
        assertNotNull(event.name, "Event name should not be null")
        assertFalse(event.name?.isBlank() == true, "Event name should not be blank")
        
        // Validate capacity constraints
        if (event.capacity != null) {
            assertTrue(event.capacity!! > 0, "Event capacity should be positive")
            assertTrue(event.capacity!! <= 10000, "Event capacity should be realistic (≤10000)")
        }
        
        // Validate timing
        assertNotNull(event.startDateTime, "Event start time should not be null")
        assertNotNull(event.endDateTime, "Event end time should not be null")
        assertTrue(event.endDateTime?.isAfter(event.startDateTime) == true, "Event end time should be after start time")
        
        // Validate event type specific requirements
        when (event.eventType ?: EventType.IN_PERSON) {
            EventType.VIRTUAL -> {
                assertNotNull(event.virtualUrl, "Virtual events must have a virtual URL")
                assertTrue(event.virtualUrl!!.startsWith("http"), "Virtual URL should be a valid URL")
            }
            EventType.HYBRID -> {
                assertNotNull(event.virtualUrl, "Hybrid events must have a virtual URL")
                assertNotNull(event.venueName, "Hybrid events must have a venue")
            }
            EventType.IN_PERSON -> {
                assertNotNull(event.venueName, "In-person events must have a venue")
                assertNotNull(event.city, "In-person events must have a city")
            }
        }
        
        // Strict validation for production-like requirements
        if (strict) {
            assertNotNull(event.description, "Event description should be provided")
            assertFalse(event.description!!.isBlank(), "Event description should not be blank")
            assertTrue(event.description!!.length >= 10, "Event description should be meaningful (≥10 chars)")
            
            if (event.eventType == EventType.IN_PERSON) {
                assertNotNull(event.state, "In-person events should have state information")
            }
        }
    }
    
    /**
     * Validates that a Session entity has consistent and realistic data.
     */
    fun validateSession(session: Session, strict: Boolean = false) {
        assertNotNull(session.id, "Session ID should not be null")
        assertNotNull(session.title, "Session title should not be null")
        assertFalse(session.title.isBlank(), "Session title should not be blank")
        
        // Validate timing
        assertNotNull(session.startTime, "Session start time should not be null")
        assertNotNull(session.endTime, "Session end time should not be null")
        assertTrue(session.endTime?.isAfter(session.startTime) == true, "Session end time should be after start time")
        
        // Validate duration reasonableness
        val durationHours = if (session.startTime != null && session.endTime != null) {
            java.time.Duration.between(session.startTime!!, session.endTime!!).toHours()
        } else {
            1L // default duration for null times
        }
        assertTrue(durationHours >= 0, "Session duration should be non-negative")
        assertTrue(durationHours <= 12, "Session duration should be realistic (≤12 hours)")
        
        // Validate capacity
        if (session.capacity != null) {
            assertTrue(session.capacity!! > 0, "Session capacity should be positive")
        }
        
        // Validate session type specific requirements
        when (session.type) {
            SessionType.KEYNOTE -> {
                if (strict) {
                    assertTrue(session.capacity == null || session.capacity!! >= 100, 
                        "Keynote sessions typically have large capacity")
                }
            }
            SessionType.WORKSHOP -> {
                if (strict) {
                    assertTrue(session.capacity == null || session.capacity!! <= 50, 
                        "Workshops typically have limited capacity")
                    assertTrue(durationHours >= 2, "Workshops should be at least 2 hours")
                }
            }
            SessionType.BREAKOUT -> {
                if (strict) {
                    assertTrue(session.capacity == null || session.capacity!! <= 30, 
                        "Breakout sessions should have small capacity")
                }
            }
            else -> { /* Other types have flexible requirements */ }
        }
    }
    
    /**
     * Validates that a Registration entity has consistent data.
     */
    fun validateRegistration(registration: Registration, strict: Boolean = false) {
        assertNotNull(registration.id, "Registration ID should not be null")
        assertNotNull(registration.userEmail, "User email should not be null")
        assertNotNull(registration.userName, "User name should not be null")
        
        // Email format validation
        assertTrue(registration.userEmail?.contains("@") == true, "User email should be valid format")
        assertTrue(registration.userEmail?.contains(".") == true, "User email should have domain")
        
        // Name validation
        assertFalse(registration.userName?.isBlank() == true, "User name should not be blank")
        
        // Status consistency validation
        when (registration.status) {
            RegistrationStatus.CHECKED_IN -> {
                assertTrue(registration.checkedIn, "Checked-in registrations should have checkedIn = true")
            }
            RegistrationStatus.NO_SHOW -> {
                assertFalse(registration.checkedIn, "No-show registrations should have checkedIn = false")
            }
            RegistrationStatus.CANCELLED -> {
                assertFalse(registration.checkedIn, "Cancelled registrations should have checkedIn = false")
            }
            else -> { /* Other statuses are flexible */ }
        }
        
        if (strict && registration.formData != null) {
            assertTrue(registration.formData!!.isNotBlank(), "Form data should not be blank if present")
            // Could add JSON validation here if needed
        }
    }
    
    /**
     * Validates that a Resource entity has consistent data.
     */
    fun validateResource(resource: Resource, strict: Boolean = false) {
        assertNotNull(resource.id, "Resource ID should not be null")
        assertNotNull(resource.name, "Resource name should not be null")
        assertFalse(resource.name.isBlank(), "Resource name should not be blank")
        
        // Capacity validation by resource type
        when (resource.type) {
            ResourceType.ROOM -> {
                if (strict) {
                    assertNotNull(resource.capacity, "Rooms should typically have capacity specified")
                    assertTrue(resource.capacity!! > 0, "Room capacity should be positive")
                    assertTrue(resource.capacity!! <= 1000, "Room capacity should be realistic")
                }
            }
            ResourceType.VEHICLE -> {
                if (resource.capacity != null) {
                    assertTrue(resource.capacity!! >= 1, "Vehicle capacity should be at least 1")
                    assertTrue(resource.capacity!! <= 50, "Vehicle capacity should be realistic")
                }
            }
            ResourceType.EQUIPMENT, ResourceType.TECHNOLOGY -> {
                // Equipment typically doesn't have capacity
            }
            else -> { /* Other types have flexible capacity requirements */ }
        }
        
        // Rate validation
        if (resource.hourlyRate != null) {
            assertTrue(resource.hourlyRate!!.signum() >= 0, "Hourly rate should be non-negative")
            assertTrue(resource.hourlyRate!!.toDouble() <= 1000.0, "Hourly rate should be realistic")
        }
        
        // Status consistency
        when (resource.status) {
            ResourceStatus.MAINTENANCE -> {
                if (strict) {
                    assertFalse(resource.isBookable, "Resources under maintenance should not be bookable")
                }
            }
            ResourceStatus.OUT_OF_SERVICE -> {
                assertFalse(resource.isBookable, "Out-of-service resources should not be bookable")
                assertFalse(resource.isActive, "Out-of-service resources should not be active")
            }
            else -> { /* Other statuses are flexible */ }
        }
    }
    
    // ================================================================================
    // Scenario Validation Methods
    // ================================================================================
    
    /**
     * Validates a complete conference scenario for consistency.
     */
    fun validateConferenceScenario(scenario: ConferenceScenario) {
        validateEvent(scenario.event, strict = true)
        
        // Validate event is conference type
        assertTrue(scenario.event.category == EventCategory.TECHNOLOGY || 
                   scenario.event.category == EventCategory.BUSINESS ||
                   scenario.event.category == EventCategory.EDUCATION,
                   "Conference should have appropriate category")
        
        // Validate sessions
        assertTrue(scenario.sessions.isNotEmpty(), "Conference should have sessions")
        scenario.sessions.forEach { session ->
            validateSession(session, strict = true)
            // Validate session timing is within event duration
            assertTrue(session.startTime?.isAfter(scenario.event.startDateTime?.minusHours(1)) == true, 
                "Session should start within event timeframe")
            assertTrue(session.endTime?.isBefore(scenario.event.endDateTime?.plusHours(1)) == true, 
                "Session should end within event timeframe")
        }
        
        // Validate registrations
        assertTrue(scenario.registrations.isNotEmpty(), "Conference should have registrations")
        scenario.registrations.forEach { registration ->
            validateRegistration(registration, strict = true)
        }
        
        // Validate capacity constraints
        if (scenario.event.capacity != null) {
            assertTrue(scenario.registrations.size <= scenario.event.capacity!! * 1.2, 
                "Registration count should be reasonable relative to event capacity")
        }
    }
    
    /**
     * Validates a workshop scenario for hands-on learning requirements.
     */
    fun validateWorkshopScenario(scenario: WorkshopScenario) {
        validateEvent(scenario.event, strict = true)
        validateSession(scenario.session, strict = true)
        
        // Workshop-specific validations
        assertTrue(scenario.session.type == SessionType.WORKSHOP, "Should be a workshop session")
        
        // Duration should be appropriate for hands-on learning
        val durationHours = if (scenario.session.startTime != null && scenario.session.endTime != null) {
            java.time.Duration.between(scenario.session.startTime!!, scenario.session.endTime!!).toHours()
        } else {
            1L // default duration for null times
        }
        assertTrue(durationHours >= 2, "Workshop should be at least 2 hours for hands-on learning")
        
        // Capacity should be limited for effective instruction
        if (scenario.session.capacity != null) {
            assertTrue(scenario.session.capacity!! <= 50, "Workshop capacity should be limited for effective instruction")
        }
        
        scenario.participants.forEach { participant ->
            validateRegistration(participant, strict = true)
        }
    }
    
    /**
     * Validates check-in scenario statistics and patterns.
     */
    fun validateCheckInScenario(scenario: CheckInScenario) {
        validateEvent(scenario.event, strict = true)
        validateSession(scenario.session, strict = true)
        
        // Validate statistical consistency
        val totalRegistrations = scenario.registrations.size
        assertTrue(totalRegistrations > 0, "Should have registrations for check-in scenario")
        
        val checkedInCount = scenario.registrations.count { it.status == RegistrationStatus.CHECKED_IN }
        val noShowCount = scenario.registrations.count { it.status == RegistrationStatus.NO_SHOW }
        
        assertTrue(checkedInCount == scenario.checkedInCount, 
            "Actual checked-in count should match scenario statistics")
        assertTrue(noShowCount == scenario.noShowCount, 
            "Actual no-show count should match scenario statistics")
        
        assertTrue(checkedInCount + noShowCount <= totalRegistrations, 
            "Check-in + no-show counts should not exceed total registrations")
    }
    
    /**
     * Validates resource booking scenario for realistic scheduling conflicts.
     */
    fun validateResourceBookingScenario(scenario: ResourceBookingScenario) {
        assertTrue(scenario.resources.isNotEmpty(), "Should have resources for booking scenario")
        assertTrue(scenario.bookings.isNotEmpty(), "Should have bookings for booking scenario")
        
        scenario.resources.forEach { resource ->
            validateResource(resource, strict = true)
        }
        
        // Validate booking details
        scenario.bookings.forEach { booking ->
            assertNotNull(booking.resource, "Booking should have associated resource")
            assertNotNull(booking.session, "Booking should have associated session")
            assertNotNull(booking.bookingStart, "Booking should have start time")
            assertNotNull(booking.bookingEnd, "Booking should have end time")
            
            assertTrue(booking.bookingEnd!!.isAfter(booking.bookingStart!!), 
                "Booking end should be after start")
            
            assertTrue(booking.quantityNeeded > 0, "Should need at least one resource unit")
            assertTrue(booking.quantityAllocated >= 0, "Allocated quantity should be non-negative")
        }
        
        // Validate utilization rate makes sense
        assertTrue(scenario.utilizationRate >= 0.0 && scenario.utilizationRate <= 1.0, 
            "Utilization rate should be between 0 and 1")
    }
    
    // ================================================================================
    // Collection Validation Methods
    // ================================================================================
    
    /**
     * Validates a collection of events for diversity and consistency.
     */
    fun validateEventCollection(events: List<Event>) {
        assertTrue(events.isNotEmpty(), "Event collection should not be empty")
        
        events.forEach { event ->
            validateEvent(event)
        }
        
        // Check for diversity in event types
        val eventTypes = events.map { it.eventType }.distinct()
        if (events.size >= 3) {
            assertTrue(eventTypes.size >= 2, "Event collection should have diverse event types")
        }
        
        // Check for unique names
        val names = events.map { it.name }
        assertTrue(names.size == names.distinct().size, "Events should have unique names")
    }
    
    /**
     * Validates a collection of registrations for realistic patterns.
     */
    fun validateRegistrationCollection(registrations: List<Registration>) {
        assertTrue(registrations.isNotEmpty(), "Registration collection should not be empty")
        
        registrations.forEach { registration ->
            validateRegistration(registration)
        }
        
        // Check for unique emails
        val emails = registrations.map { it.userEmail }
        assertTrue(emails.size == emails.distinct().size, "Registrations should have unique emails")
        
        // Check for realistic status distribution
        val statusCounts = registrations.groupBy { it.status }.mapValues { it.value.size }
        val registeredCount = statusCounts[RegistrationStatus.REGISTERED] ?: 0
        val totalCount = registrations.size
        
        // Most registrations should be in REGISTERED status for realistic scenarios
        assertTrue(registeredCount >= totalCount * 0.5, 
            "Majority of registrations should be in REGISTERED status")
    }
}