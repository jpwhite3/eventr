package com.eventr.controller

import com.eventr.config.TestConfig
import com.eventr.model.Event
import com.eventr.model.EventStatus
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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig::class)
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
              "formData": "{\"fields\":[{\"name\":\"fullName\",\"label\":\"Full Name\",\"type\":\"text\",\"required\":true}]}"
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
    }

    @Test
    fun shouldGetEventById() {
        // Create test event
        val event = Event().apply {
            name = "Test Event"
            description = "This is a test event"
            status = EventStatus.DRAFT
            capacity = 100
            waitlistEnabled = true
        }
        val savedEvent = eventRepository.save(event)

        // Test retrieval
        mockMvc.perform(get("/api/events/${savedEvent.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name", `is`("Test Event")))
            .andExpect(jsonPath("$.description", `is`("This is a test event")))
            .andExpect(jsonPath("$.capacity", `is`(100)))
            .andExpect(jsonPath("$.waitlistEnabled", `is`(true)))
    }

    @Test
    fun shouldUpdateEvent() {
        // Create test event
        val event = Event().apply {
            name = "Original Name"
            description = "Original description"
            status = EventStatus.DRAFT
        }
        val savedEvent = eventRepository.save(event)

        val updateJson = """
            {
              "name": "Updated Name",
              "description": "Updated description",
              "tags": ["updated"],
              "capacity": 200
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
    }

    @Test
    fun shouldDeleteEvent() {
        // Create test event
        val event = Event().apply {
            name = "Event to Delete"
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
}
