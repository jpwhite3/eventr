package com.eventr.testutil

import com.eventr.model.*
import com.eventr.testutil.builders.*

/**
 * Standardized test fixtures using the builder framework.
 * Provides consistent, reusable test data patterns for all test scenarios.
 * 
 * These fixtures replace scattered test data creation throughout the test suite
 * and provide named, documented scenarios for different testing contexts.
 * 
 * Usage:
 * ```kotlin
 * // Use pre-configured fixtures
 * val event = TestFixtures.PUBLISHED_CONFERENCE_EVENT
 * val session = TestFixtures.KEYNOTE_SESSION
 * 
 * // Get fresh instances
 * val events = TestFixtures.sampleEvents()
 * val scenario = TestFixtures.workshopScenario()
 * ```
 */
object TestFixtures {
    
    // ================================================================================
    // Event Fixtures
    // ================================================================================
    
    /**
     * Standard published conference event with full details.
     * Use for integration testing and general event operations.
     */
    val PUBLISHED_CONFERENCE_EVENT: Event by lazy {
        TestDataBuilders.event()
            .asConference()
            .asPublished()
            .withName("Tech Conference 2024")
            .withDescription("Annual technology conference featuring the latest innovations")
            .withCapacity(500)
            .build()
    }
    
    /**
     * Draft workshop event for testing event lifecycle.
     */
    val DRAFT_WORKSHOP_EVENT: Event by lazy {
        TestDataBuilders.event()
            .asWorkshop()
            .asDraft()
            .withName("Hands-on Development Workshop")
            .withCapacity(30)
            .build()
    }
    
    /**
     * Virtual event for testing online event functionality.
     */
    val VIRTUAL_MEETUP_EVENT: Event by lazy {
        TestDataBuilders.event()
            .asMeetup()
            .asVirtual("https://meet.example.com/tech-meetup", "+1-555-0123", "123456")
            .asPublished()
            .withName("Virtual Tech Meetup")
            .withCapacity(200)
            .build()
    }
    
    /**
     * Hybrid event combining in-person and virtual attendance.
     */
    val HYBRID_CONFERENCE_EVENT by lazy {
        TestDataBuilders.event()
            .asConference()
            .asHybrid("https://stream.example.com/hybrid-conf", "https://meet.example.com/hybrid")
            .asPublished()
            .withName("Hybrid Innovation Conference")
            .withCapacity(300)
            .build()
    }
    
    /**
     * Small capacity event for testing capacity limits and waitlists.
     */
    val SMALL_CAPACITY_EVENT: Event by lazy {
        TestDataBuilders.event()
            .asWorkshop()
            .asPublished()
            .withName("Exclusive Workshop")
            .withCapacity(TestConstants.SMALL_CAPACITY)
            .build()
    }
    
    /**
     * Draft event not yet ready for public access.
     */
    val ANOTHER_DRAFT_EVENT: Event by lazy {
        TestDataBuilders.event()
            .asDraft()
            .withName("Another Draft Event")
            .build()
    }
    
    // ================================================================================
    // Session Fixtures
    // ================================================================================
    
    /**
     * Standard keynote session for conference events.
     */
    val KEYNOTE_SESSION: Session by lazy {
        TestDataBuilders.session()
            .asKeynote()
            .withTitle("Opening Keynote: The Future of Technology")
            .withPresenter("Dr. Jane Smith", "Leading technology visionary and researcher")
            .withCapacity(500)
            .build()
    }
    
    /**
     * Technical presentation session.
     */
    val TECHNICAL_PRESENTATION: Session by lazy {
        TestDataBuilders.session()
            .asPresentation()
            .withTitle("Advanced Kotlin Coroutines")
            .withPresenter("John Developer", "Senior Kotlin Engineer")
            .withCapacity(100)
            .withDuration(TestConstants.SESSION_START_TIME, 60)
            .build()
    }
    
    /**
     * Hands-on workshop session with requirements.
     */
    val WORKSHOP_SESSION: Session by lazy {
        TestDataBuilders.session()
            .asWorkshop()
            .withTitle("Full-Stack Development Workshop")
            .withPresenter("Sarah Engineer", "Full-stack development expert")
            .withCapacity(30)
            .withDuration(TestConstants.SESSION_START_TIME, 180)
            .withPrerequisites("Laptop with IDE installed, Basic programming knowledge")
            .withMaterials("Workshop materials will be provided")
            .build()
    }
    
    /**
     * Panel discussion session.
     */
    val PANEL_SESSION: Session by lazy {
        TestDataBuilders.session()
            .asPanel()
            .withTitle("Industry Leaders Panel: Future of Tech")
            .withCapacity(200)
            .withDuration(TestConstants.SESSION_START_TIME, 90)
            .build()
    }
    
    /**
     * Breakout session for smaller group discussions.
     */
    val BREAKOUT_SESSION: Session by lazy {
        TestDataBuilders.session()
            .asBreakout()
            .withTitle("Small Group Discussion: Best Practices")
            .withCapacity(TestConstants.SMALL_CAPACITY)
            .withDuration(TestConstants.SESSION_START_TIME.plusHours(2), 45)
            .build()
    }
    
    // ================================================================================
    // Registration Fixtures
    // ================================================================================
    
    /**
     * Standard registered attendee.
     */
    val REGULAR_ATTENDEE: Registration by lazy {
        TestDataBuilders.registration()
            .asRegistered()
            .withUserDetails("Alice Johnson", "alice.johnson@company.com")
            .withSimpleFormData()
            .build()
    }
    
    /**
     * Checked-in attendee for attendance testing.
     */
    val CHECKED_IN_ATTENDEE: Registration by lazy {
        TestDataBuilders.registration()
            .asCheckedIn()
            .withUserDetails("Bob Wilson", "bob.wilson@tech.com")
            .withDetailedFormData()
            .build()
    }
    
    /**
     * VIP attendee with special requirements.
     */
    val VIP_ATTENDEE: Registration by lazy {
        TestDataBuilders.registration()
            .asVIPAttendee()
            .withUserDetails("Carol Executive", "carol@bigcorp.com")
            .build()
    }
    
    /**
     * Speaker registration.
     */
    val SPEAKER_REGISTRATION: Registration by lazy {
        TestDataBuilders.registration()
            .asSpeaker()
            .withUserDetails("David Speaker", "david.speaker@expert.com")
            .build()
    }
    
    /**
     * No-show registration for attendance analytics.
     */
    val NO_SHOW_ATTENDEE: Registration by lazy {
        TestDataBuilders.registration()
            .asNoShow()
            .withUserDetails("Eve NoShow", "eve@absent.com")
            .build()
    }
    
    /**
     * Waitlisted registration for capacity testing.
     */
    val WAITLISTED_ATTENDEE: Registration by lazy {
        TestDataBuilders.registration()
            .asWaitlisted()
            .withUserDetails("Frank Wait", "frank@waiting.com")
            .build()
    }
    
    // ================================================================================
    // Resource Fixtures
    // ================================================================================
    
    /**
     * Standard conference room resource.
     */
    val MAIN_CONFERENCE_ROOM: Resource by lazy {
        TestDataBuilders.resource()
            .asConferenceRoom()
            .withName("Main Conference Hall")
            .withCapacity(500)
            .withAdvancedSpecs()
            .asAvailable()
            .build()
    }
    
    /**
     * Small meeting room for workshops.
     */
    val WORKSHOP_ROOM: Resource by lazy {
        TestDataBuilders.resource()
            .asRoom()
            .withName("Workshop Room A")
            .withCapacity(30)
            .withBasicSpecs()
            .asAvailable()
            .build()
    }
    
    /**
     * AV equipment resource.
     */
    val PROJECTOR_EQUIPMENT: Resource by lazy {
        TestDataBuilders.resource()
            .asEquipment()
            .withName("4K Projector with Screen")
            .withoutCapacity()
            .asAvailable()
            .build()
    }
    
    /**
     * Resource under maintenance.
     */
    val MAINTENANCE_RESOURCE: Resource by lazy {
        TestDataBuilders.resource()
            .asRoom()
            .withName("Room Under Maintenance")
            .needsMaintenance()
            .build()
    }
    
    /**
     * High-demand resource for conflict testing.
     */
    val POPULAR_RESOURCE: Resource by lazy {
        TestDataBuilders.resource()
            .asConferenceRoom()
            .withName("Premium Conference Suite")
            .asPremiumRate()
            .heavilyUsed()
            .build()
    }
    
    // ================================================================================
    // Complex Scenario Fixtures
    // ================================================================================
    
    /**
     * Complete conference scenario with multiple sessions and attendees.
     */
    fun conferenceScenario(): ConferenceScenario {
        return TestDataBuilders.conferenceScenario()
            .withSessions(8)
            .withRegistrations(150)
            .withDuration(8)
            .build()
    }
    
    /**
     * Workshop scenario with limited capacity.
     */
    fun workshopScenario(): WorkshopScenario {
        return TestDataBuilders.workshopScenario()
            .withCapacity(25)
            .withDuration(4)
            .withPrerequisites()
            .withMaterials()
            .build()
    }
    
    /**
     * Virtual event scenario for online testing.
     */
    fun virtualMeetingScenario(): VirtualMeetingScenario {
        return TestDataBuilders.virtualMeetingScenario()
            .withAttendees(75)
            .withDuration(2)
            .withRecording()
            .withBreakoutRooms()
            .build()
    }
    
    /**
     * Check-in scenario with various attendance patterns.
     */
    fun checkInScenario(): CheckInScenario {
        return TestDataBuilders.checkInScenario()
            .withAttendees(100)
            .withCheckInRate(TestConstants.HIGH_ATTENDANCE_RATE)
            .withLateArrivals()
            .withNoShows()
            .build()
    }
    
    /**
     * Resource booking scenario with conflicts.
     */
    fun resourceBookingScenario(): ResourceBookingScenario {
        return TestDataBuilders.resourceBookingScenario()
            .withResources(8)
            .withBookings(15)
            .withConflicts()
            .withUtilizationRate(0.8)
            .build()
    }
    
    // ================================================================================
    // Collection Fixtures
    // ================================================================================
    
    /**
     * Sample events covering different types and statuses.
     */
    fun sampleEvents(): List<Event> = listOf(
        PUBLISHED_CONFERENCE_EVENT,
        DRAFT_WORKSHOP_EVENT,
        VIRTUAL_MEETUP_EVENT,
        HYBRID_CONFERENCE_EVENT,
        SMALL_CAPACITY_EVENT
    )
    
    /**
     * Sample sessions for multi-session events.
     */
    fun sampleSessions(): List<Session> = listOf(
        KEYNOTE_SESSION,
        TECHNICAL_PRESENTATION,
        WORKSHOP_SESSION,
        PANEL_SESSION,
        BREAKOUT_SESSION
    )
    
    /**
     * Sample registrations with different statuses.
     */
    fun sampleRegistrations(): List<Registration> = listOf(
        REGULAR_ATTENDEE,
        CHECKED_IN_ATTENDEE,
        VIP_ATTENDEE,
        SPEAKER_REGISTRATION,
        NO_SHOW_ATTENDEE,
        WAITLISTED_ATTENDEE
    )
    
    /**
     * Sample resources for booking tests.
     */
    fun sampleResources(): List<Resource> = listOf(
        MAIN_CONFERENCE_ROOM,
        WORKSHOP_ROOM,
        PROJECTOR_EQUIPMENT,
        MAINTENANCE_RESOURCE,
        POPULAR_RESOURCE
    )
    
    // ================================================================================
    // Dynamic Fixture Generators
    // ================================================================================
    
    /**
     * Generates multiple events with varied characteristics for load testing.
     */
    fun generateEvents(count: Int): List<Event> {
        return (1..count).map { index ->
            when (index % 4) {
                0 -> TestDataBuilders.event().asConference().withName("Conference $index").build()
                1 -> TestDataBuilders.event().asWorkshop().withName("Workshop $index").build()
                2 -> TestDataBuilders.event().asMeetup().withName("Meetup $index").build()
                else -> TestDataBuilders.event().asVirtual("https://virtual$index.com").withName("Virtual Event $index").build()
            }
        }
    }
    
    /**
     * Generates registrations with different form data patterns.
     */
    fun generateRegistrations(count: Int): List<Registration> {
        return (1..count).map { index ->
            when (index % 5) {
                0 -> TestDataBuilders.registration().asConferenceAttendee().withUserDetails("Attendee $index", "attendee$index@test.com").build()
                1 -> TestDataBuilders.registration().asWorkshopParticipant().withUserDetails("Participant $index", "participant$index@test.com").build()
                2 -> TestDataBuilders.registration().asVIPAttendee().withUserDetails("VIP $index", "vip$index@test.com").build()
                3 -> TestDataBuilders.registration().asSpeaker().withUserDetails("Speaker $index", "speaker$index@test.com").build()
                else -> TestDataBuilders.registration().asVolunteer().withUserDetails("Volunteer $index", "volunteer$index@test.com").build()
            }
        }
    }
    
    /**
     * Generates resources with different types and availability.
     */
    fun generateResources(count: Int): List<Resource> {
        return (1..count).map { index ->
            when (index % 6) {
                0 -> TestDataBuilders.resource().asConferenceRoom().withName("Conference Room $index").build()
                1 -> TestDataBuilders.resource().asRoom().withName("Meeting Room $index").build()
                2 -> TestDataBuilders.resource().asEquipment().withName("Equipment $index").build()
                3 -> TestDataBuilders.resource().asVehicle().withName("Vehicle $index").build()
                4 -> TestDataBuilders.resource().asStaff().withName("Staff Member $index").build()
                else -> TestDataBuilders.resource().asTechnology().withName("Tech Resource $index").build()
            }
        }
    }
}