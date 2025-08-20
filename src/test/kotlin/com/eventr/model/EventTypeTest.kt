package com.eventr.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class EventTypeTest {

    @Test
    fun shouldHaveCorrectEventTypes() {
        val types = EventType.values()
        
        assertEquals(3, types.size)
        assertTrue(types.contains(EventType.IN_PERSON))
        assertTrue(types.contains(EventType.VIRTUAL))
        assertTrue(types.contains(EventType.HYBRID))
    }

    @Test
    fun shouldConvertFromString() {
        assertEquals(EventType.IN_PERSON, EventType.valueOf("IN_PERSON"))
        assertEquals(EventType.VIRTUAL, EventType.valueOf("VIRTUAL"))
        assertEquals(EventType.HYBRID, EventType.valueOf("HYBRID"))
    }

    @Test
    fun shouldHandleEnumProperties() {
        val inPersonEvent = Event().apply { eventType = EventType.IN_PERSON }
        val virtualEvent = Event().apply { eventType = EventType.VIRTUAL }
        val hybridEvent = Event().apply { eventType = EventType.HYBRID }
        
        assertEquals(EventType.IN_PERSON, inPersonEvent.eventType)
        assertEquals(EventType.VIRTUAL, virtualEvent.eventType)
        assertEquals(EventType.HYBRID, hybridEvent.eventType)
    }
}

class EventCategoryTest {

    @Test
    fun shouldHaveCorrectCategories() {
        val categories = EventCategory.values()
        
        assertTrue(categories.size >= 10) // Should have many corporate categories
        assertTrue(categories.contains(EventCategory.BUSINESS))
        assertTrue(categories.contains(EventCategory.TECHNOLOGY))
        assertTrue(categories.contains(EventCategory.EDUCATION))
        assertTrue(categories.contains(EventCategory.COMMUNITY))
        assertTrue(categories.contains(EventCategory.HEALTH_WELLNESS))
        assertTrue(categories.contains(EventCategory.FOOD_DRINK))
        assertTrue(categories.contains(EventCategory.OTHER))
    }

    @Test
    fun shouldConvertFromString() {
        assertEquals(EventCategory.BUSINESS, EventCategory.valueOf("BUSINESS"))
        assertEquals(EventCategory.TECHNOLOGY, EventCategory.valueOf("TECHNOLOGY"))
        assertEquals(EventCategory.EDUCATION, EventCategory.valueOf("EDUCATION"))
        assertEquals(EventCategory.FOOD_DRINK, EventCategory.valueOf("FOOD_DRINK"))
    }

    @Test
    fun shouldHandleCorporateCategories() {
        val businessEvent = Event().apply { category = EventCategory.BUSINESS }
        val techEvent = Event().apply { category = EventCategory.TECHNOLOGY }
        val eduEvent = Event().apply { category = EventCategory.EDUCATION }
        
        assertEquals(EventCategory.BUSINESS, businessEvent.category)
        assertEquals(EventCategory.TECHNOLOGY, techEvent.category)
        assertEquals(EventCategory.EDUCATION, eduEvent.category)
    }

    @Test
    fun shouldHandleAllCorporateRelevantCategories() {
        // Test that we have all the corporate-relevant categories
        val corporateCategories = listOf(
            EventCategory.BUSINESS,
            EventCategory.TECHNOLOGY,
            EventCategory.EDUCATION,
            EventCategory.COMMUNITY,
            EventCategory.HEALTH_WELLNESS,
            EventCategory.FOOD_DRINK,
            EventCategory.SPORTS_FITNESS
        )
        
        corporateCategories.forEach { category ->
            val event = Event().apply { this.category = category }
            assertEquals(category, event.category)
        }
    }
}

class EventStatusTest {

    @Test
    fun shouldHaveCorrectStatuses() {
        val statuses = EventStatus.values()
        
        assertTrue(statuses.contains(EventStatus.DRAFT))
        assertTrue(statuses.contains(EventStatus.PUBLISHED))
        // Only DRAFT and PUBLISHED are available
    }

    @Test
    fun shouldConvertFromString() {
        assertEquals(EventStatus.DRAFT, EventStatus.valueOf("DRAFT"))
        assertEquals(EventStatus.PUBLISHED, EventStatus.valueOf("PUBLISHED"))
        // Only DRAFT and PUBLISHED available
    }

    @Test
    fun shouldHandleEventLifecycle() {
        val event = Event()
        
        // Event starts as draft
        event.status = EventStatus.DRAFT
        assertEquals(EventStatus.DRAFT, event.status)
        
        // Event gets published
        event.status = EventStatus.PUBLISHED
        assertEquals(EventStatus.PUBLISHED, event.status)
        
        // Event lifecycle is DRAFT -> PUBLISHED
        assertEquals(EventStatus.PUBLISHED, event.status)
    }
}