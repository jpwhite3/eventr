package com.eventr.controller;

import com.eventr.config.TestConfig;
import com.eventr.model.Event;
import com.eventr.model.EventStatus;
import com.eventr.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
public class EventControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
    }

    @Test
    void shouldCreateEvent() throws Exception {
        String eventJson = "{\n" +
                "  \"name\": \"Test Event\",\n" +
                "  \"description\": \"This is a test event\",\n" +
                "  \"tags\": [\"test\", \"integration\"],\n" +
                "  \"capacity\": 100,\n" +
                "  \"waitlistEnabled\": true,\n" +
                "  \"formData\": \"{\\\"fields\\\":[{\\\"name\\\":\\\"fullName\\\",\\\"label\\\":\\\"Full Name\\\",\\\"type\\\":\\\"text\\\",\\\"required\\\":true}]}\"\n" +
                "}";

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Test Event")))
                .andExpect(jsonPath("$.description", is("This is a test event")))
                .andExpect(jsonPath("$.tags", hasSize(2)))
                .andExpect(jsonPath("$.tags", containsInAnyOrder("test", "integration")))
                .andExpect(jsonPath("$.capacity", is(100)))
                .andExpect(jsonPath("$.waitlistEnabled", is(true)));
    }

    @Test
    void shouldGetEventById() throws Exception {
        // Create test event
        Event event = new Event();
        event.setName("Test Event");
        event.setDescription("This is a test event");
        event.setStatus(EventStatus.DRAFT);
        event.setCapacity(100);
        event.setWaitlistEnabled(true);
        event = eventRepository.save(event);

        // Test retrieval
        mockMvc.perform(get("/api/events/" + event.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Test Event")))
                .andExpect(jsonPath("$.description", is("This is a test event")))
                .andExpect(jsonPath("$.capacity", is(100)))
                .andExpect(jsonPath("$.waitlistEnabled", is(true)));
    }

    @Test
    void shouldUpdateEvent() throws Exception {
        // Create test event
        Event event = new Event();
        event.setName("Original Name");
        event.setDescription("Original description");
        event.setStatus(EventStatus.DRAFT);
        event = eventRepository.save(event);

        String updateJson = "{\n" +
                "  \"name\": \"Updated Name\",\n" +
                "  \"description\": \"Updated description\",\n" +
                "  \"tags\": [\"updated\"],\n" +
                "  \"capacity\": 200\n" +
                "}";

        mockMvc.perform(put("/api/events/" + event.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Name")))
                .andExpect(jsonPath("$.description", is("Updated description")))
                .andExpect(jsonPath("$.capacity", is(200)));
    }

    @Test
    void shouldDeleteEvent() throws Exception {
        // Create test event
        Event event = new Event();
        event.setName("Event to Delete");
        event = eventRepository.save(event);

        mockMvc.perform(delete("/api/events/" + event.getId()))
                .andExpect(status().isOk());

        // Verify it's deleted
        mockMvc.perform(get("/api/events/" + event.getId()))
                .andExpect(status().isNotFound());
    }
}
