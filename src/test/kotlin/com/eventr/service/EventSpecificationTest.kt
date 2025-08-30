package com.eventr.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate

@DisplayName("EventSpecification Tests")
class EventSpecificationTest {

    @Test
    @DisplayName("Should create specification with all null parameters")
    fun shouldCreateSpecificationWithAllNullParameters() {
        // When
        val specification = EventSpecification.filterBy(
            category = null,
            eventType = null,
            city = null,
            dateStart = null,
            dateEnd = null,
            tags = null,
            searchQuery = null,
            latitude = null,
            longitude = null,
            radius = null,
            publishedOnly = false
        )

        // Then
        assertNotNull(specification)
    }

    @Test
    @DisplayName("Should create specification with published only filter")
    fun shouldCreateSpecificationWithPublishedOnlyFilter() {
        // When
        val specification = EventSpecification.filterBy(
            publishedOnly = true
        )

        // Then
        assertNotNull(specification)
    }

    @Test
    @DisplayName("Should create specification with category filter")
    fun shouldCreateSpecificationWithCategoryFilter() {
        // When
        val specification = EventSpecification.filterBy(
            category = "MUSIC",
            publishedOnly = false
        )

        // Then
        assertNotNull(specification)
    }

    @Test
    @DisplayName("Should create specification with event type filter")
    fun shouldCreateSpecificationWithEventTypeFilter() {
        // When
        val specification = EventSpecification.filterBy(
            eventType = "IN_PERSON",
            publishedOnly = false
        )

        // Then
        assertNotNull(specification)
    }

    @Test
    @DisplayName("Should create specification with city filter")
    fun shouldCreateSpecificationWithCityFilter() {
        // When
        val specification = EventSpecification.filterBy(
            city = "New York",
            publishedOnly = false
        )

        // Then
        assertNotNull(specification)
    }

    @Test
    @DisplayName("Should create specification with date filters")
    fun shouldCreateSpecificationWithDateFilters() {
        // Given
        val startDate = LocalDate.of(2024, 1, 1)
        val endDate = LocalDate.of(2024, 12, 31)

        // When
        val specification = EventSpecification.filterBy(
            dateStart = startDate,
            dateEnd = endDate,
            publishedOnly = false
        )

        // Then
        assertNotNull(specification)
    }

    @Test
    @DisplayName("Should create specification with tags filter")
    fun shouldCreateSpecificationWithTagsFilter() {
        // Given
        val tags = listOf("music", "outdoor", "tech")

        // When
        val specification = EventSpecification.filterBy(
            tags = tags,
            publishedOnly = false
        )

        // Then
        assertNotNull(specification)
    }

    @Test
    @DisplayName("Should create specification with search query")
    fun shouldCreateSpecificationWithSearchQuery() {
        // When
        val specification = EventSpecification.filterBy(
            searchQuery = "conference",
            publishedOnly = false
        )

        // Then
        assertNotNull(specification)
    }

    @Test
    @DisplayName("Should create specification with all filters")
    fun shouldCreateSpecificationWithAllFilters() {
        // Given
        val category = "TECHNOLOGY"
        val eventType = "VIRTUAL"
        val city = "San Francisco"
        val startDate = LocalDate.of(2024, 6, 1)
        val endDate = LocalDate.of(2024, 6, 30)
        val tags = listOf("tech", "conference")
        val searchQuery = "developer"

        // When
        val specification = EventSpecification.filterBy(
            category = category,
            eventType = eventType,
            city = city,
            dateStart = startDate,
            dateEnd = endDate,
            tags = tags,
            searchQuery = searchQuery,
            latitude = 37.7749,
            longitude = -122.4194,
            radius = 50,
            publishedOnly = true
        )

        // Then
        assertNotNull(specification)
    }

    @Test
    @DisplayName("Should handle invalid category gracefully")
    fun shouldHandleInvalidCategoryGracefully() {
        // When
        val specification = EventSpecification.filterBy(
            category = "INVALID_CATEGORY",
            publishedOnly = false
        )

        // Then
        assertNotNull(specification)
    }

    @Test
    @DisplayName("Should handle invalid event type gracefully")
    fun shouldHandleInvalidEventTypeGracefully() {
        // When
        val specification = EventSpecification.filterBy(
            eventType = "INVALID_TYPE",
            publishedOnly = false
        )

        // Then
        assertNotNull(specification)
    }
}