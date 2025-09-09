package com.eventr.testutil

import com.eventr.testutil.builders.*

/**
 * Central entry point for all test data builders.
 * Provides a fluent API for creating test data with sensible defaults and easy customization.
 * 
 * Usage examples:
 * ```kotlin
 * // Simple entity creation with defaults
 * val event = TestDataBuilders.event().build()
 * 
 * // Customized entity
 * val event = TestDataBuilders.event()
 *     .withName("Custom Event")
 *     .withCapacity(200)
 *     .build()
 * 
 * // Complex scenario
 * val scenario = TestDataBuilders.conferenceScenario()
 *     .withSessions(3)
 *     .withRegistrations(50)
 *     .build()
 * ```
 */
object TestDataBuilders {
    
    // ================================================================================
    // Entity Builders
    // ================================================================================
    
    /**
     * Creates a new EventBuilder with default test values.
     */
    fun event(): EventBuilder = EventBuilder()
    
    /**
     * Creates a new SessionBuilder with default test values.
     */
    fun session(): SessionBuilder = SessionBuilder()
    
    /**
     * Creates a new RegistrationBuilder with default test values.
     */
    fun registration(): RegistrationBuilder = RegistrationBuilder()
    
    /**
     * Creates a new ResourceBuilder with default test values.
     */
    fun resource(): ResourceBuilder = ResourceBuilder()
    
    /**
     * Creates a new SessionRegistrationBuilder with default test values.
     */
    fun sessionRegistration(): SessionRegistrationBuilder = SessionRegistrationBuilder()
    
    /**
     * Creates a new UserBuilder with default test values.
     */
    fun user(): UserBuilder = UserBuilder()
    
    // ================================================================================
    // DTO Builders
    // ================================================================================
    
    /**
     * Creates a new CreateEventDtoBuilder for API request testing.
     */
    fun createEventDto(): CreateEventDtoBuilder = CreateEventDtoBuilder()
    
    /**
     * Creates a new UpdateEventDtoBuilder for API request testing.
     */
    fun updateEventDto(): UpdateEventDtoBuilder = UpdateEventDtoBuilder()
    
    /**
     * Creates a new CreateSessionDtoBuilder for API request testing.
     */
    fun createSessionDto(): CreateSessionDtoBuilder = CreateSessionDtoBuilder()
    
    /**
     * Creates a new UpdateSessionDtoBuilder for API request testing.
     */
    fun updateSessionDto(): UpdateSessionDtoBuilder = UpdateSessionDtoBuilder()
    
    /**
     * Creates a new CreateResourceDtoBuilder for API request testing.
     */
    fun createResourceDto(): CreateResourceDtoBuilder = CreateResourceDtoBuilder()
    
    /**
     * Creates a new UpdateResourceDtoBuilder for API request testing.
     */
    fun updateResourceDto(): UpdateResourceDtoBuilder = UpdateResourceDtoBuilder()
    
    // ================================================================================
    // Scenario Builders
    // ================================================================================
    
    /**
     * Creates a conference scenario with multiple sessions and registrations.
     */
    fun conferenceScenario(): ConferenceScenarioBuilder = ConferenceScenarioBuilder()
    
    /**
     * Creates a workshop scenario with hands-on sessions and limited capacity.
     */
    fun workshopScenario(): WorkshopScenarioBuilder = WorkshopScenarioBuilder()
    
    /**
     * Creates a virtual meeting scenario with online sessions.
     */
    fun virtualMeetingScenario(): VirtualMeetingScenarioBuilder = VirtualMeetingScenarioBuilder()
    
    /**
     * Creates a check-in scenario with various attendance patterns.
     */
    fun checkInScenario(): CheckInScenarioBuilder = CheckInScenarioBuilder()
    
    /**
     * Creates a resource booking scenario with conflicts and availability.
     */
    fun resourceBookingScenario(): ResourceBookingScenarioBuilder = ResourceBookingScenarioBuilder()
    
    // ================================================================================
    // Quick Access Methods for Common Patterns
    // ================================================================================
    
    /**
     * Creates a minimal event with just required fields for simple tests.
     */
    fun minimalEvent() = event()
        .withName("Minimal Event")
        .withDescription(null)
        .withLocation(TestConstants.DEFAULT_EVENT_LOCATION, TestConstants.DEFAULT_EVENT_CITY)
    
    /**
     * Creates a fully populated event with all optional fields for comprehensive tests.
     */
    fun fullEvent() = event()
        .withAllOptionalFields()
    
    /**
     * Creates a published event ready for registration.
     */
    fun publishedEvent() = event()
        .asPublished()
        .withCapacity(TestConstants.MEDIUM_CAPACITY)
    
    /**
     * Creates a draft event not yet ready for public access.
     */
    fun draftEvent() = event()
        .asDraft()
    
    /**
     * Creates a session with registrations for attendance testing.
     */
    fun sessionWithAttendees(attendeeCount: Int = 10) = session()
        .withCapacity(attendeeCount + 10)
        .withRegistrations(attendeeCount)
    
    /**
     * Creates a resource with booking conflicts for availability testing.
     */
    fun conflictedResource() = resource()
        .withBookingConflicts()
    
    /**
     * Creates a registration with check-in history for attendance tracking tests.
     */
    fun checkedInRegistration() = registration()
        .asCheckedIn()
        .asCheckedIn()
    
    // ================================================================================
    // Collection Builders
    // ================================================================================
    
    /**
     * Creates a list of events with different characteristics for bulk testing.
     */
    fun multipleEvents(count: Int = 3): List<EventBuilder> = 
        (1..count).map { index ->
            event()
                .withName("Event $index")
                .withId(TestConstants.uniqueId())
        }
    
    /**
     * Creates a list of sessions for a multi-session event.
     */
    fun multipleSessions(count: Int = 5): List<SessionBuilder> =
        (1..count).map { index ->
            session()
                .withTitle("Session $index")
                .withId(TestConstants.uniqueId())
                .withStartTime(TestConstants.SESSION_START_TIME.plusHours(index.toLong()))
        }
    
    /**
     * Creates a list of registrations with different statuses.
     */
    fun multipleRegistrations(count: Int = 10): List<RegistrationBuilder> =
        (1..count).map { index ->
            registration()
                .withUserName("User $index")
                .withUserEmail(TestConstants.uniqueEmail("user$index"))
                .withId(TestConstants.uniqueId())
        }
}