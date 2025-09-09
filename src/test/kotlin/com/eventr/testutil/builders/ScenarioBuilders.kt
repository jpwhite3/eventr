package com.eventr.testutil.builders

import com.eventr.model.*
import com.eventr.testutil.TestConstants
import com.eventr.testutil.TestDataBuilders
import java.time.LocalDateTime

/**
 * Scenario builders create complete, realistic test scenarios with multiple interconnected entities.
 * These are perfect for integration testing and end-to-end workflow validation.
 */

// ================================================================================
// Conference Scenario Builder
// ================================================================================

class ConferenceScenarioBuilder {
    private var sessionCount: Int = 5
    private var registrationCount: Int = 100
    private var duration: Long = 8 // hours
    private var hasKeynote: Boolean = true
    private var hasWorkshops: Boolean = true
    
    fun withSessions(count: Int) = apply { this.sessionCount = count }
    fun withRegistrations(count: Int) = apply { this.registrationCount = count }
    fun withDuration(hours: Long) = apply { this.duration = hours }
    fun withKeynote() = apply { this.hasKeynote = true }
    fun withoutKeynote() = apply { this.hasKeynote = false }
    fun withWorkshops() = apply { this.hasWorkshops = true }
    fun withoutWorkshops() = apply { this.hasWorkshops = false }
    
    fun build(): ConferenceScenario {
        val event = TestDataBuilders.event()
            .asConference()
            .withDuration(TestConstants.EVENT_START_TIME, duration)
            .build()
        
        val sessions = mutableListOf<Session>()
        var currentTime = TestConstants.EVENT_START_TIME.plusHours(1)
        
        // Add keynote if requested
        if (hasKeynote) {
            sessions.add(
                TestDataBuilders.session()
                    .asKeynote()
                    .withEvent(event)
                    .withStartTime(currentTime)
                    .build()
            )
            currentTime = currentTime.plusHours(1).plusMinutes(15) // Keynote + break
        }
        
        // Add regular sessions
        repeat(sessionCount - if (hasKeynote) 1 else 0) { index ->
            val sessionBuilder = if (hasWorkshops && index % 3 == 0) {
                TestDataBuilders.session().asWorkshop()
            } else {
                TestDataBuilders.session().asPresentation()
            }
            
            sessions.add(
                sessionBuilder
                    .withEvent(event)
                    .withTitle("Conference Session ${index + 1}")
                    .withStartTime(currentTime)
                    .build()
            )
            currentTime = currentTime.plusHours(1).plusMinutes(15) // Session + break
        }
        
        // Create registrations
        val registrations = (1..registrationCount).map { index ->
            TestDataBuilders.registration()
                .withUniqueUser("Attendee$index")
                .asConferenceAttendee()
                .build()
        }
        
        return ConferenceScenario(event, sessions, registrations)
    }
}

data class ConferenceScenario(
    val event: Event,
    val sessions: List<Session>,
    val registrations: List<Registration>
)

// ================================================================================
// Workshop Scenario Builder  
// ================================================================================

class WorkshopScenarioBuilder {
    private var capacity: Int = TestConstants.SMALL_CAPACITY
    private var duration: Long = 4 // hours
    private var hasPrerequisites: Boolean = true
    private var hasMaterials: Boolean = true
    
    fun withCapacity(capacity: Int) = apply { this.capacity = capacity }
    fun withDuration(hours: Long) = apply { this.duration = hours }
    fun withPrerequisites() = apply { this.hasPrerequisites = true }
    fun withoutPrerequisites() = apply { this.hasPrerequisites = false }
    fun withMaterials() = apply { this.hasMaterials = true }
    fun withoutMaterials() = apply { this.hasMaterials = false }
    
    fun build(): WorkshopScenario {
        val event = TestDataBuilders.event()
            .asWorkshop()
            .withCapacity(capacity)
            .withDuration(TestConstants.EVENT_START_TIME, duration)
            .build()
        
        val sessionBuilder = TestDataBuilders.session()
            .asWorkshop()
            .withEvent(event)
            .withCapacity(capacity)
        
        if (hasPrerequisites) {
            sessionBuilder.withPrerequisites("Laptop required, Basic programming knowledge")
        }
        
        if (hasMaterials) {
            sessionBuilder.withMaterials(TestConstants.TEST_MATERIAL_URL)
        }
        
        val session = sessionBuilder.build()
        
        // Create participants (fewer than capacity to allow for dropouts)
        val participantCount = (capacity * 0.8).toInt()
        val participants = (1..participantCount).map { index ->
            TestDataBuilders.registration()
                .withUniqueUser("Participant$index")
                .asWorkshopParticipant()
                .build()
        }
        
        return WorkshopScenario(event, session, participants)
    }
}

data class WorkshopScenario(
    val event: Event,
    val session: Session,
    val participants: List<Registration>
)

// ================================================================================
// Virtual Meeting Scenario Builder
// ================================================================================

class VirtualMeetingScenarioBuilder {
    private var attendeeCount: Int = TestConstants.MEDIUM_CAPACITY
    private var duration: Long = 2 // hours
    private var hasRecording: Boolean = true
    private var hasBreakoutRooms: Boolean = false
    
    fun withAttendees(count: Int) = apply { this.attendeeCount = count }
    fun withDuration(hours: Long) = apply { this.duration = hours }
    fun withRecording() = apply { this.hasRecording = true }
    fun withoutRecording() = apply { this.hasRecording = false }
    fun withBreakoutRooms() = apply { this.hasBreakoutRooms = true }
    
    fun build(): VirtualMeetingScenario {
        val event = TestDataBuilders.event()
            .asMeetup()
            .asVirtual("https://meet.example.com/virtual-meeting", "+1-555-0123", "123456")
            .withCapacity(attendeeCount * 2) // Virtual events can handle more
            .withDuration(TestConstants.EVENT_START_TIME, duration)
            .build()
        
        val sessions = mutableListOf<Session>()
        
        // Main session
        val mainSessionBuilder = TestDataBuilders.session()
            .asPresentation()
            .withEvent(event)
            .withTitle("Virtual Presentation")
            .withCapacity(attendeeCount * 2)
        
        if (hasRecording) {
            mainSessionBuilder.withRecording("https://recordings.example.com/meeting-123")
        }
        
        sessions.add(mainSessionBuilder.build())
        
        // Breakout sessions if requested
        if (hasBreakoutRooms) {
            sessions.add(
                TestDataBuilders.session()
                    .asBreakout()
                    .withEvent(event)
                    .withTitle("Breakout Room Discussion")
                    .withCapacity(TestConstants.SMALL_CAPACITY)
                    .withStartTime(TestConstants.SESSION_START_TIME.plusHours(1))
                    .build()
            )
        }
        
        // Create attendees
        val attendees = (1..attendeeCount).map { index ->
            TestDataBuilders.registration()
                .withUniqueUser("VirtualAttendee$index")
                .build()
        }
        
        return VirtualMeetingScenario(event, sessions, attendees)
    }
}

data class VirtualMeetingScenario(
    val event: Event,
    val sessions: List<Session>,
    val attendees: List<Registration>
)

// ================================================================================
// Check-in Scenario Builder
// ================================================================================

class CheckInScenarioBuilder {
    private var attendeeCount: Int = 50
    private var checkInRate: Double = TestConstants.HIGH_ATTENDANCE_RATE
    private var hasLateArrivals: Boolean = true
    private var hasNoShows: Boolean = true
    
    fun withAttendees(count: Int) = apply { this.attendeeCount = count }
    fun withCheckInRate(rate: Double) = apply { this.checkInRate = rate }
    fun withLateArrivals() = apply { this.hasLateArrivals = true }
    fun withoutLateArrivals() = apply { this.hasLateArrivals = false }
    fun withNoShows() = apply { this.hasNoShows = true }
    fun withoutNoShows() = apply { this.hasNoShows = false }
    
    fun build(): CheckInScenario {
        val event = TestDataBuilders.event()
            .asPublished()
            .withCapacity(attendeeCount + 10)
            .build()
        
        val session = TestDataBuilders.session()
            .withEvent(event)
            .withCapacity(attendeeCount + 10)
            .build()
        
        // Calculate attendance numbers
        val checkedInCount = (attendeeCount * (checkInRate / 100)).toInt()
        val noShowCount = if (hasNoShows) attendeeCount - checkedInCount else 0
        val lateArrivalCount = if (hasLateArrivals) (checkedInCount * 0.2).toInt() else 0
        
        val registrations = mutableListOf<Registration>()
        
        // Regular checked-in attendees
        repeat(checkedInCount - lateArrivalCount) { index ->
            registrations.add(
                TestDataBuilders.registration()
                    .withUniqueUser("CheckedInAttendee$index")
                    .asCheckedIn()
                    .build()
            )
        }
        
        // Late arrivals
        repeat(lateArrivalCount) { index ->
            registrations.add(
                TestDataBuilders.registration()
                    .withUniqueUser("LateArrival$index")
                    .asCheckedIn()
                    .build()
            )
        }
        
        // No-shows
        repeat(noShowCount) { index ->
            registrations.add(
                TestDataBuilders.registration()
                    .withUniqueUser("NoShow$index")
                    .asRegistered() // Registered but not checked in
                    .build()
            )
        }
        
        return CheckInScenario(event, session, registrations, checkedInCount, noShowCount)
    }
}

data class CheckInScenario(
    val event: Event,
    val session: Session,
    val registrations: List<Registration>,
    val checkedInCount: Int,
    val noShowCount: Int
)

// ================================================================================
// Resource Booking Scenario Builder
// ================================================================================

class ResourceBookingScenarioBuilder {
    private var resourceCount: Int = 5
    private var bookingCount: Int = 10
    private var hasConflicts: Boolean = true
    private var utilizationRate: Double = 0.7
    
    fun withResources(count: Int) = apply { this.resourceCount = count }
    fun withBookings(count: Int) = apply { this.bookingCount = count }
    fun withConflicts() = apply { this.hasConflicts = true }
    fun withoutConflicts() = apply { this.hasConflicts = false }
    fun withUtilizationRate(rate: Double) = apply { this.utilizationRate = rate }
    
    fun build(): ResourceBookingScenario {
        // Create diverse resources
        val resources = (1..resourceCount).map { index ->
            when (index % 4) {
                0 -> TestDataBuilders.resource().asConferenceRoom().withId(TestConstants.uniqueId())
                1 -> TestDataBuilders.resource().asEquipment().withId(TestConstants.uniqueId())
                2 -> TestDataBuilders.resource().asRoom().withId(TestConstants.uniqueId())
                else -> TestDataBuilders.resource().asTechnology().withId(TestConstants.uniqueId())
            }.build()
        }
        
        // Create bookings
        val bookings = mutableListOf<SessionResource>()
        val usedResourceCount = (resourceCount * utilizationRate).toInt()
        
        repeat(bookingCount) { index ->
            val resource = resources[index % usedResourceCount]
            val startTime = TestConstants.SESSION_START_TIME.plusHours((index * 2).toLong())
            
            // Create potential conflicts if requested
            val endTime = if (hasConflicts && index > 0 && index % 5 == 0) {
                // Overlap with previous booking
                startTime.plusHours(3) 
            } else {
                startTime.plusHours(1)
            }
            
            bookings.add(
                SessionResource(id = TestConstants.uniqueId()).apply {
                    this.resource = resource
                    this.session = TestDataBuilders.session()
                        .withId(TestConstants.uniqueId())
                        .withTitle("Resource Booking Session $index")
                        .withStartTime(startTime)
                        .withEndTime(endTime)
                        .build()
                    this.quantityNeeded = 1
                    this.quantityAllocated = 1
                    this.status = ResourceBookingStatus.ALLOCATED
                    this.bookingStart = startTime
                    this.bookingEnd = endTime
                }
            )
        }
        
        return ResourceBookingScenario(resources, bookings, utilizationRate)
    }
}

data class ResourceBookingScenario(
    val resources: List<Resource>,
    val bookings: List<SessionResource>,
    val utilizationRate: Double
)