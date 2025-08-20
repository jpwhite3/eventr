package com.eventr.controller

import com.eventr.config.TestConfig
import com.eventr.model.Event
import com.eventr.model.EventStatus
import com.eventr.model.EventType
import com.eventr.model.EventCategory
import com.eventr.repository.EventRepository
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig::class)
@TestPropertySource(properties = [
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
])
class EventControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var eventRepository: EventRepository

    @BeforeEach
    fun setUp() {
        eventRepository.deleteAll()
    }

    @Test
    fun shouldCreateEvent() {
        val eventJson = """
            {
              "name": "Test Event",
              "description": "This is a test event",
              "tags": ["test", "integration"],
              "capacity": 100,
              "waitlistEnabled": true,
              "eventType": "IN_PERSON",
              "category": "BUSINESS",
              "requiresApproval": false,
              "maxRegistrations": 50,
              "organizerName": "Test Organizer",
              "venueName": "Test Venue",
              "city": "Test City",
              "formData": "{\"fields\":[{\"id\":\"field_1\",\"name\":\"fullName\",\"label\":\"Full Name\",\"type\":\"text\",\"required\":true}]}"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventJson)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name", `is`("Test Event")))
            .andExpect(jsonPath("$.description", `is`("This is a test event")))
            .andExpect(jsonPath("$.tags", hasSize<Any>(2)))
            .andExpect(jsonPath("$.tags", containsInAnyOrder("test", "integration")))
            .andExpect(jsonPath("$.capacity", `is`(100)))
            .andExpect(jsonPath("$.waitlistEnabled", `is`(true)))
            .andExpect(jsonPath("$.eventType", `is`("IN_PERSON")))
            .andExpect(jsonPath("$.category", `is`("BUSINESS")))
            .andExpect(jsonPath("$.requiresApproval", `is`(false)))
            .andExpect(jsonPath("$.maxRegistrations", `is`(50)))
            .andExpect(jsonPath("$.organizerName", `is`("Test Organizer")))
            .andExpect(jsonPath("$.venueName", `is`("Test Venue")))
            .andExpect(jsonPath("$.city", `is`("Test City")))
    }

    @Test
    fun shouldGetEventById() {
        // Create test event
        val event = Event().apply {
            name = "Test Event"
            description = "This is a test event"
            status = EventStatus.DRAFT
            eventType = EventType.IN_PERSON
            category = EventCategory.BUSINESS
            capacity = 100
            waitlistEnabled = true
            requiresApproval = false
            maxRegistrations = 50
            organizerName = "Test Organizer"
            venueName = "Test Venue"
            city = "Test City"
        }
        val savedEvent = eventRepository.save(event)

        // Test retrieval
        mockMvc.perform(get("/api/events/${savedEvent.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name", `is`("Test Event")))
            .andExpect(jsonPath("$.description", `is`("This is a test event")))
            .andExpect(jsonPath("$.capacity", `is`(100)))
            .andExpect(jsonPath("$.waitlistEnabled", `is`(true)))
            .andExpect(jsonPath("$.eventType", `is`("IN_PERSON")))
            .andExpect(jsonPath("$.category", `is`("BUSINESS")))
            .andExpect(jsonPath("$.requiresApproval", `is`(false)))
            .andExpect(jsonPath("$.maxRegistrations", `is`(50)))
            .andExpect(jsonPath("$.organizerName", `is`("Test Organizer")))
            .andExpect(jsonPath("$.venueName", `is`("Test Venue")))
            .andExpect(jsonPath("$.city", `is`("Test City")))
    }

    @Test
    fun shouldUpdateEvent() {
        // Create test event
        val event = Event().apply {
            name = "Original Name"
            description = "Original description"
            status = EventStatus.DRAFT
            eventType = EventType.VIRTUAL
            category = EventCategory.TECHNOLOGY
            requiresApproval = true
        }
        val savedEvent = eventRepository.save(event)

        val updateJson = """
            {
              "name": "Updated Name",
              "description": "Updated description",
              "tags": ["updated"],
              "capacity": 200,
              "eventType": "IN_PERSON",
              "category": "BUSINESS",
              "requiresApproval": false,
              "maxRegistrations": 150,
              "organizerName": "Updated Organizer",
              "venueName": "Updated Venue"
            }
        """.trimIndent()

        mockMvc.perform(
            put("/api/events/${savedEvent.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name", `is`("Updated Name")))
            .andExpect(jsonPath("$.description", `is`("Updated description")))
            .andExpect(jsonPath("$.capacity", `is`(200)))
            .andExpect(jsonPath("$.eventType", `is`("IN_PERSON")))
            .andExpect(jsonPath("$.category", `is`("BUSINESS")))
            .andExpect(jsonPath("$.requiresApproval", `is`(false)))
            .andExpect(jsonPath("$.maxRegistrations", `is`(150)))
            .andExpect(jsonPath("$.organizerName", `is`("Updated Organizer")))
            .andExpect(jsonPath("$.venueName", `is`("Updated Venue")))
    }

    @Test
    fun shouldDeleteEvent() {
        // Create test event
        val event = Event().apply {
            name = "Event to Delete"
            eventType = EventType.IN_PERSON
            category = EventCategory.OTHER
        }
        val savedEvent = eventRepository.save(event)

        mockMvc.perform(delete("/api/events/${savedEvent.id}"))
            .andExpect(status().isNoContent)

        // Verify it's deleted
        mockMvc.perform(get("/api/events/${savedEvent.id}"))
            .andExpect(status().isNotFound)
            
        // Also verify using the repository directly
        savedEvent.id?.let { id ->
            assert(!eventRepository.existsById(id))
        }
    }

    @Test
    fun shouldListAllEvents() {
        // Create multiple test events
        val event1 = Event().apply {
            name = "Business Event"
            description = "A business focused event"
            status = EventStatus.PUBLISHED
            eventType = EventType.IN_PERSON
            category = EventCategory.BUSINESS
            city = "New York"
            requiresApproval = false
        }
        
        val event2 = Event().apply {
            name = "Tech Conference"
            description = "Technology conference"
            status = EventStatus.PUBLISHED
            eventType = EventType.VIRTUAL
            category = EventCategory.TECHNOLOGY
            city = "San Francisco"
            requiresApproval = true
        }

        eventRepository.save(event1)
        eventRepository.save(event2)

        // Test listing all events
        mockMvc.perform(get("/api/events"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(2)))
            .andExpect(jsonPath("$[*].name", containsInAnyOrder("Business Event", "Tech Conference")))
            .andExpect(jsonPath("$[*].eventType", containsInAnyOrder("IN_PERSON", "VIRTUAL")))
            .andExpect(jsonPath("$[*].category", containsInAnyOrder("BUSINESS", "TECHNOLOGY")))
    }
}
