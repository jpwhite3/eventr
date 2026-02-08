package com.eventr.modules.event

import com.eventr.model.*
import com.eventr.modules.event.api.EventModuleApi
import com.eventr.modules.event.api.EventNotFoundException
import com.eventr.modules.event.api.dto.*
import com.eventr.modules.event.internal.EventModuleApiImpl
import com.eventr.repository.EventInstanceRepository
import com.eventr.repository.EventRepository
import com.eventr.shared.event.EventPublisher
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.data.domain.Sort
import java.time.LocalDateTime
import java.util.*

/**
 * Tests for EventModuleApiImpl - the core of the Event module in the modular monolith architecture.
 * 
 * These tests verify:
 * 1. Event CRUD operations work through the module API
 * 2. Auto-create EventInstance on event creation (Issue #54)
 * 3. Domain events are published on lifecycle changes
 * 4. Event module properly encapsulates business logic
 */
@ExtendWith(MockitoExtension::class)
@DisplayName("EventModuleApi Tests")
class EventModuleApiImplTest {

    @Mock
    private lateinit var eventRepository: EventRepository

    @Mock
    private lateinit var eventInstanceRepository: EventInstanceRepository

    @Mock
    private lateinit var eventPublisher: EventPublisher

    @Captor
    private lateinit var eventCaptor: ArgumentCaptor<Event>

    @Captor
    private lateinit var instanceCaptor: ArgumentCaptor<EventInstance>

    private lateinit var eventModule: EventModuleApi

    private val testEventId = UUID.randomUUID()
    private val testInstanceId = UUID.randomUUID()
    private val now = LocalDateTime.now()

    @BeforeEach
    fun setUp() {
        eventModule = EventModuleApiImpl(
            eventRepository,
            eventInstanceRepository,
            eventPublisher
        )
    }

    @Nested
    @DisplayName("Create Event Tests")
    inner class CreateEventTests {

        @Test
        @DisplayName("Should create event with DRAFT status")
        fun shouldCreateEventWithDraftStatus() {
            // Arrange
            val request = createEventRequest()
            val savedEvent = createEvent(testEventId, EventStatus.DRAFT)
            val savedInstance = createEventInstance(testInstanceId, savedEvent)

            whenever(eventRepository.save(any<Event>())).thenReturn(savedEvent)
            whenever(eventInstanceRepository.save(any<EventInstance>())).thenReturn(savedInstance)

            // Act
            val response = eventModule.createEvent(request)

            // Assert
            assertEquals(testEventId, response.id)
            assertEquals(EventStatus.DRAFT, response.status)
            assertEquals("Test Event", response.name)
            verify(eventRepository).save(any<Event>())
        }

        @Test
        @DisplayName("Should auto-create EventInstance on event creation (Issue #54)")
        fun shouldAutoCreateEventInstanceOnEventCreation() {
            // Arrange
            val request = createEventRequest(
                startDateTime = now.plusDays(7),
                venueName = "Conference Center",
                city = "San Francisco"
            )
            val savedEvent = createEvent(testEventId, EventStatus.DRAFT).apply {
                startDateTime = now.plusDays(7)
                venueName = "Conference Center"
                city = "San Francisco"
            }
            val savedInstance = createEventInstance(testInstanceId, savedEvent)

            whenever(eventRepository.save(any<Event>())).thenReturn(savedEvent)
            whenever(eventInstanceRepository.save(any<EventInstance>())).thenReturn(savedInstance)

            // Act
            val response = eventModule.createEvent(request)

            // Assert
            verify(eventInstanceRepository).save(capture(instanceCaptor))
            val createdInstance = instanceCaptor.value
            
            assertNotNull(createdInstance)
            assertEquals(savedEvent, createdInstance.event)
            assertEquals(now.plusDays(7), createdInstance.dateTime)
            assertNotNull(createdInstance.location)
            
            // Response should include the instance
            assertEquals(1, response.instances.size)
            assertEquals(testInstanceId, response.instances[0].id)
        }

        @Test
        @DisplayName("Should build correct location string for IN_PERSON event")
        fun shouldBuildCorrectLocationForInPersonEvent() {
            // Arrange
            val request = createEventRequest(
                eventType = EventType.IN_PERSON,
                venueName = "Main Hall",
                address = "123 Main St",
                city = "Austin",
                state = "TX"
            )
            val savedEvent = createEvent(testEventId, EventStatus.DRAFT).apply {
                eventType = EventType.IN_PERSON
                venueName = "Main Hall"
                address = "123 Main St"
                city = "Austin"
                state = "TX"
            }
            val savedInstance = createEventInstance(testInstanceId, savedEvent)

            whenever(eventRepository.save(any<Event>())).thenReturn(savedEvent)
            whenever(eventInstanceRepository.save(any<EventInstance>())).thenReturn(savedInstance)

            // Act
            eventModule.createEvent(request)

            // Assert
            verify(eventInstanceRepository).save(capture(instanceCaptor))
            val location = instanceCaptor.value.location
            assertTrue(location?.contains("Main Hall") == true)
            assertTrue(location?.contains("Austin") == true)
        }

        @Test
        @DisplayName("Should build correct location string for VIRTUAL event")
        fun shouldBuildCorrectLocationForVirtualEvent() {
            // Arrange
            val request = createEventRequest(
                eventType = EventType.VIRTUAL,
                virtualUrl = "https://zoom.us/meeting/123"
            )
            val savedEvent = createEvent(testEventId, EventStatus.DRAFT).apply {
                eventType = EventType.VIRTUAL
                virtualUrl = "https://zoom.us/meeting/123"
            }
            val savedInstance = createEventInstance(testInstanceId, savedEvent)

            whenever(eventRepository.save(any<Event>())).thenReturn(savedEvent)
            whenever(eventInstanceRepository.save(any<EventInstance>())).thenReturn(savedInstance)

            // Act
            eventModule.createEvent(request)

            // Assert
            verify(eventInstanceRepository).save(capture(instanceCaptor))
            val location = instanceCaptor.value.location
            assertEquals("https://zoom.us/meeting/123", location)
        }

        @Test
        @DisplayName("Should publish EventCreated domain event")
        fun shouldPublishEventCreatedDomainEvent() {
            // Arrange
            val request = createEventRequest()
            val savedEvent = createEvent(testEventId, EventStatus.DRAFT)
            val savedInstance = createEventInstance(testInstanceId, savedEvent)

            whenever(eventRepository.save(any<Event>())).thenReturn(savedEvent)
            whenever(eventInstanceRepository.save(any<EventInstance>())).thenReturn(savedInstance)

            // Act
            eventModule.createEvent(request)

            // Assert
            verify(eventPublisher).publish(argThat { event ->
                event.aggregateId == testEventId
            })
        }
    }

    @Nested
    @DisplayName("Get Event Tests")
    inner class GetEventTests {

        @Test
        @DisplayName("Should return event when found")
        fun shouldReturnEventWhenFound() {
            // Arrange
            val event = createEvent(testEventId, EventStatus.PUBLISHED)
            whenever(eventRepository.findById(testEventId)).thenReturn(Optional.of(event))

            // Act
            val response = eventModule.getEvent(testEventId)

            // Assert
            assertNotNull(response)
            assertEquals(testEventId, response?.id)
            assertEquals("Test Event", response?.name)
        }

        @Test
        @DisplayName("Should return null when event not found")
        fun shouldReturnNullWhenEventNotFound() {
            // Arrange
            whenever(eventRepository.findById(testEventId)).thenReturn(Optional.empty())

            // Act
            val response = eventModule.getEvent(testEventId)

            // Assert
            assertNull(response)
        }

        @Test
        @DisplayName("Should throw EventNotFoundException from getEventOrThrow")
        fun shouldThrowEventNotFoundExceptionFromGetEventOrThrow() {
            // Arrange
            whenever(eventRepository.findById(testEventId)).thenReturn(Optional.empty())

            // Act & Assert
            assertThrows<EventNotFoundException> {
                eventModule.getEventOrThrow(testEventId)
            }
        }
    }

    @Nested
    @DisplayName("Update Event Tests")
    inner class UpdateEventTests {

        @Test
        @DisplayName("Should update event fields")
        fun shouldUpdateEventFields() {
            // Arrange
            val existingEvent = createEvent(testEventId, EventStatus.DRAFT)
            val updateRequest = UpdateEventRequest(
                name = "Updated Event Name",
                description = "Updated description",
                capacity = 200
            )

            whenever(eventRepository.findById(testEventId)).thenReturn(Optional.of(existingEvent))
            whenever(eventRepository.save(any<Event>())).thenAnswer { it.arguments[0] }

            // Act
            val response = eventModule.updateEvent(testEventId, updateRequest)

            // Assert
            assertEquals("Updated Event Name", response.name)
            assertEquals("Updated description", response.description)
            assertEquals(200, response.capacity)
        }

        @Test
        @DisplayName("Should throw EventNotFoundException when updating non-existent event")
        fun shouldThrowWhenUpdatingNonExistentEvent() {
            // Arrange
            whenever(eventRepository.findById(testEventId)).thenReturn(Optional.empty())

            // Act & Assert
            assertThrows<EventNotFoundException> {
                eventModule.updateEvent(testEventId, UpdateEventRequest(name = "New Name"))
            }
        }

        @Test
        @DisplayName("Should update default instance when timing changes")
        fun shouldUpdateDefaultInstanceWhenTimingChanges() {
            // Arrange
            val existingEvent = createEvent(testEventId, EventStatus.DRAFT).apply {
                instances = mutableListOf(createEventInstance(testInstanceId, this))
            }
            val newStartTime = now.plusDays(14)
            val updateRequest = UpdateEventRequest(startDateTime = newStartTime)

            whenever(eventRepository.findById(testEventId)).thenReturn(Optional.of(existingEvent))
            whenever(eventRepository.save(any<Event>())).thenAnswer { it.arguments[0] }
            whenever(eventInstanceRepository.save(any<EventInstance>())).thenAnswer { it.arguments[0] }

            // Act
            eventModule.updateEvent(testEventId, updateRequest)

            // Assert
            verify(eventInstanceRepository).save(capture(instanceCaptor))
            assertEquals(newStartTime, instanceCaptor.value.dateTime)
        }
    }

    @Nested
    @DisplayName("Delete Event Tests")
    inner class DeleteEventTests {

        @Test
        @DisplayName("Should delete event and publish EventDeleted")
        fun shouldDeleteEventAndPublishEvent() {
            // Arrange
            val event = createEvent(testEventId, EventStatus.DRAFT)
            whenever(eventRepository.findById(testEventId)).thenReturn(Optional.of(event))

            // Act
            eventModule.deleteEvent(testEventId)

            // Assert
            verify(eventRepository).delete(event)
            verify(eventPublisher).publish(argThat { domainEvent ->
                domainEvent.aggregateId == testEventId
            })
        }

        @Test
        @DisplayName("Should throw EventNotFoundException when deleting non-existent event")
        fun shouldThrowWhenDeletingNonExistentEvent() {
            // Arrange
            whenever(eventRepository.findById(testEventId)).thenReturn(Optional.empty())

            // Act & Assert
            assertThrows<EventNotFoundException> {
                eventModule.deleteEvent(testEventId)
            }
        }
    }

    @Nested
    @DisplayName("Publish Event Tests")
    inner class PublishEventTests {

        @Test
        @DisplayName("Should change status to PUBLISHED")
        fun shouldChangeStatusToPublished() {
            // Arrange
            val event = createEvent(testEventId, EventStatus.DRAFT).apply {
                instances = mutableListOf(createEventInstance(testInstanceId, this))
            }
            whenever(eventRepository.findById(testEventId)).thenReturn(Optional.of(event))
            whenever(eventRepository.save(any<Event>())).thenAnswer { it.arguments[0] }

            // Act
            val response = eventModule.publishEvent(testEventId)

            // Assert
            assertEquals(EventStatus.PUBLISHED, response.status)
            verify(eventPublisher).publish(argThat { domainEvent ->
                domainEvent.aggregateId == testEventId
            })
        }

        @Test
        @DisplayName("Should create default instance if none exists when publishing")
        fun shouldCreateDefaultInstanceIfNoneExistsWhenPublishing() {
            // Arrange
            val event = createEvent(testEventId, EventStatus.DRAFT).apply {
                instances = mutableListOf() // No instances
            }
            val savedInstance = createEventInstance(testInstanceId, event)
            
            whenever(eventRepository.findById(testEventId)).thenReturn(Optional.of(event))
            whenever(eventRepository.save(any<Event>())).thenAnswer { it.arguments[0] }
            whenever(eventInstanceRepository.save(any<EventInstance>())).thenReturn(savedInstance)

            // Act
            eventModule.publishEvent(testEventId)

            // Assert
            verify(eventInstanceRepository).save(any<EventInstance>())
        }
    }

    @Nested
    @DisplayName("Clone Event Tests")
    inner class CloneEventTests {

        @Test
        @DisplayName("Should clone event with DRAFT status and (Copy) suffix")
        fun shouldCloneEventWithDraftStatusAndCopySuffix() {
            // Arrange
            val original = createEvent(testEventId, EventStatus.PUBLISHED).apply {
                name = "Original Event"
            }
            val clonedId = UUID.randomUUID()
            val clonedEvent = createEvent(clonedId, EventStatus.DRAFT).apply {
                name = "Original Event (Copy)"
            }
            val savedInstance = createEventInstance(UUID.randomUUID(), clonedEvent)

            whenever(eventRepository.findById(testEventId)).thenReturn(Optional.of(original))
            whenever(eventRepository.save(any<Event>())).thenReturn(clonedEvent)
            whenever(eventInstanceRepository.save(any<EventInstance>())).thenReturn(savedInstance)

            // Act
            val response = eventModule.cloneEvent(testEventId)

            // Assert
            assertEquals(EventStatus.DRAFT, response.status)
            assertTrue(response.name.contains("(Copy)"))
        }
    }

    @Nested
    @DisplayName("Find Events Tests")
    inner class FindEventsTests {

        @Test
        @DisplayName("Should return only published events when publishedOnly is true")
        fun shouldReturnOnlyPublishedEventsWhenPublishedOnlyIsTrue() {
            // Arrange
            val publishedEvents = listOf(
                createEvent(UUID.randomUUID(), EventStatus.PUBLISHED),
                createEvent(UUID.randomUUID(), EventStatus.PUBLISHED)
            )
            val criteria = EventFilterCriteria(publishedOnly = true)

            whenever(eventRepository.findByStatus(eq(EventStatus.PUBLISHED), any<Sort>()))
                .thenReturn(publishedEvents)

            // Act
            val response = eventModule.findEvents(criteria)

            // Assert
            assertEquals(2, response.size)
            assertTrue(response.all { it.status == EventStatus.PUBLISHED })
        }

        @Test
        @DisplayName("Should filter by search text")
        fun shouldFilterBySearchText() {
            // Arrange
            val events = listOf(
                createEvent(UUID.randomUUID(), EventStatus.PUBLISHED).apply { name = "Tech Conference" },
                createEvent(UUID.randomUUID(), EventStatus.PUBLISHED).apply { name = "Business Summit" }
            )
            val criteria = EventFilterCriteria(search = "tech", publishedOnly = true)

            whenever(eventRepository.findByStatus(eq(EventStatus.PUBLISHED), any<Sort>()))
                .thenReturn(events)

            // Act
            val response = eventModule.findEvents(criteria)

            // Assert
            assertEquals(1, response.size)
            assertEquals("Tech Conference", response[0].name)
        }
    }

    @Nested
    @DisplayName("Event Instance Tests")
    inner class EventInstanceTests {

        @Test
        @DisplayName("Should get event instances for event")
        fun shouldGetEventInstancesForEvent() {
            // Arrange
            val event = createEvent(testEventId, EventStatus.PUBLISHED).apply {
                instances = mutableListOf(
                    createEventInstance(UUID.randomUUID(), this),
                    createEventInstance(UUID.randomUUID(), this)
                )
            }
            whenever(eventRepository.findById(testEventId)).thenReturn(Optional.of(event))

            // Act
            val response = eventModule.getEventInstances(testEventId)

            // Assert
            assertEquals(2, response.size)
        }

        @Test
        @DisplayName("Should get event ID for instance")
        fun shouldGetEventIdForInstance() {
            // Arrange
            val event = createEvent(testEventId, EventStatus.PUBLISHED)
            val instance = createEventInstance(testInstanceId, event)
            
            whenever(eventInstanceRepository.findById(testInstanceId)).thenReturn(Optional.of(instance))

            // Act
            val eventId = eventModule.getEventIdForInstance(testInstanceId)

            // Assert
            assertEquals(testEventId, eventId)
        }
    }

    // ==================== Helper Methods ====================

    private fun createEventRequest(
        name: String = "Test Event",
        eventType: EventType = EventType.IN_PERSON,
        startDateTime: LocalDateTime? = now.plusDays(7),
        venueName: String? = null,
        address: String? = null,
        city: String? = null,
        state: String? = null,
        virtualUrl: String? = null
    ): CreateEventRequest {
        return CreateEventRequest(
            name = name,
            description = "Test event description",
            eventType = eventType,
            category = EventCategory.BUSINESS,
            startDateTime = startDateTime,
            endDateTime = startDateTime?.plusHours(4),
            timezone = "America/Los_Angeles",
            venueName = venueName,
            address = address,
            city = city,
            state = state,
            virtualUrl = virtualUrl,
            organizerName = "Test Organizer",
            organizerEmail = "organizer@test.com"
        )
    }

    private fun createEvent(id: UUID, status: EventStatus): Event {
        return Event(id = id).apply {
            this.name = "Test Event"
            this.description = "Test event description"
            this.status = status
            this.eventType = EventType.IN_PERSON
            this.category = EventCategory.BUSINESS
            this.startDateTime = now.plusDays(7)
            this.endDateTime = now.plusDays(7).plusHours(4)
            this.timezone = "America/Los_Angeles"
            this.organizerName = "Test Organizer"
            this.organizerEmail = "organizer@test.com"
            this.instances = mutableListOf()
        }
    }

    private fun createEventInstance(id: UUID, event: Event): EventInstance {
        return EventInstance(id = id).apply {
            this.event = event
            this.dateTime = event.startDateTime
            this.location = "Test Location"
        }
    }
}
