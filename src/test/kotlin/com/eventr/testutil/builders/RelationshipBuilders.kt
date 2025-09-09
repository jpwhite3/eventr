package com.eventr.testutil.builders

import com.eventr.model.*
import com.eventr.testutil.TestConstants
import com.eventr.testutil.TestDataBuilders
import java.util.*

/**
 * Builders for complex entity relationships and scenarios.
 * These builders help create interconnected entities for integration testing.
 */

// ================================================================================
// Session Relationship Builders
// ================================================================================

class SessionWithRegistrationsBuilder(
    private val sessionBuilder: SessionBuilder,
    private val registrationCount: Int
) {
    fun build(): Pair<Session, List<Registration>> {
        val session = sessionBuilder.build()
        val registrations = (1..registrationCount).map { index ->
            TestDataBuilders.registration()
                .withUniqueUser("Attendee$index")
                .build()
        }
        return session to registrations
    }
}

class SessionWithResourcesBuilder(
    private val sessionBuilder: SessionBuilder,
    private val resources: List<Resource>
) {
    fun build(): Pair<Session, List<Resource>> {
        val session = sessionBuilder.build()
        return session to resources
    }
}

// ================================================================================
// Registration Relationship Builders
// ================================================================================

class RegistrationWithSessionsBuilder(
    private val registrationBuilder: RegistrationBuilder,
    private val sessionIds: List<UUID>
) {
    fun build(): Pair<Registration, List<SessionRegistration>> {
        val registration = registrationBuilder.build()
        val sessionRegistrations = sessionIds.map { sessionId ->
            SessionRegistration(id = TestConstants.uniqueId()).apply {
                this.registration = registration
                this.session = Session(id = sessionId)
                this.status = SessionRegistrationStatus.REGISTERED
                this.registeredAt = TestConstants.REGISTRATION_DATE
            }
        }
        return registration to sessionRegistrations
    }
}

// ================================================================================
// Resource Relationship Builders
// ================================================================================

class ResourceWithBookingsBuilder(
    private val resourceBuilder: ResourceBuilder,
    private val bookingCount: Int
) {
    fun build(): Pair<Resource, List<SessionResource>> {
        val resource = resourceBuilder.build()
        val bookings = (1..bookingCount).map { index ->
            SessionResource(id = TestConstants.uniqueId()).apply {
                this.resource = resource
                this.session = TestDataBuilders.session()
                    .withId(TestConstants.uniqueId())
                    .withTitle("Session $index")
                    .build()
                this.quantityNeeded = 1
                this.quantityAllocated = 1
                this.status = ResourceBookingStatus.ALLOCATED
                this.bookingStart = TestConstants.SESSION_START_TIME.plusHours(index.toLong())
                this.bookingEnd = TestConstants.SESSION_START_TIME.plusHours(index.toLong() + 1)
            }
        }
        return resource to bookings
    }
}

class ResourceWithConflictsBuilder(
    private val resourceBuilder: ResourceBuilder
) {
    fun build(): Pair<Resource, List<SessionResource>> {
        val resource = resourceBuilder.build()
        
        // Create overlapping bookings to simulate conflicts
        val conflictingBookings = listOf(
            SessionResource(id = TestConstants.uniqueId()).apply {
                this.resource = resource
                this.session = TestDataBuilders.session()
                    .withId(TestConstants.uniqueId())
                    .withTitle("Conflicting Session 1")
                    .build()
                this.quantityNeeded = 1
                this.quantityAllocated = 1
                this.status = ResourceBookingStatus.ALLOCATED
                this.bookingStart = TestConstants.SESSION_START_TIME
                this.bookingEnd = TestConstants.SESSION_START_TIME.plusHours(2)
            },
            SessionResource(id = TestConstants.uniqueId()).apply {
                this.resource = resource
                this.session = TestDataBuilders.session()
                    .withId(TestConstants.uniqueId())
                    .withTitle("Conflicting Session 2")
                    .build()
                this.quantityNeeded = 1
                this.quantityAllocated = 1
                this.status = ResourceBookingStatus.ALLOCATED
                // Overlapping time slot
                this.bookingStart = TestConstants.SESSION_START_TIME.plusHours(1)
                this.bookingEnd = TestConstants.SESSION_START_TIME.plusHours(3)
            }
        )
        
        return resource to conflictingBookings
    }
}

// ================================================================================
// Placeholder Builders (to be implemented later)
// ================================================================================

class SessionRegistrationBuilder {
    fun build(): SessionRegistration {
        return SessionRegistration(id = TestConstants.DEFAULT_SESSION_REGISTRATION_ID).apply {
            this.status = SessionRegistrationStatus.REGISTERED
            this.registeredAt = TestConstants.REGISTRATION_DATE
        }
    }
}

class UserBuilder {
    fun build(): User {
        // This would be implemented when User entity is available
        // For now, return a mock or simplified version
        throw NotImplementedError("UserBuilder not yet implemented - User entity may not be available")
    }
}