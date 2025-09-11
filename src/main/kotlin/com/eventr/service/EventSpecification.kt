package com.eventr.service

import com.eventr.model.Event
import com.eventr.model.EventCategory
import com.eventr.model.EventInstance
import com.eventr.model.EventStatus
import com.eventr.model.EventType
import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.Predicate
import org.springframework.data.jpa.domain.Specification
import java.time.LocalDate

object EventSpecification {
    
    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun filterBy(
        category: String? = null,
        eventType: String? = null,
        city: String? = null,
        dateStart: LocalDate? = null, 
        dateEnd: LocalDate? = null, 
        tags: List<String>? = null,
        searchQuery: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        radius: Int? = null,
        publishedOnly: Boolean = true
    ): Specification<Event> {
        return Specification { root, _, criteriaBuilder ->
            val predicates = mutableListOf<Predicate>()
            
            if (publishedOnly) {
                predicates.add(criteriaBuilder.equal(root.get<EventStatus>("status"), EventStatus.PUBLISHED))
            }
            
            // Category filter
            category?.let { cat ->
                if (cat.isNotEmpty() && cat != "All") {
                    try {
                        val categoryEnum = EventCategory.valueOf(cat.uppercase())
                        predicates.add(criteriaBuilder.equal(root.get<EventCategory>("category"), categoryEnum))
                    } catch (e: IllegalArgumentException) {
                        // Invalid category, ignore filter
                    }
                }
            }
            
            // Event type filter
            eventType?.let { type ->
                if (type.isNotEmpty()) {
                    try {
                        val eventTypeEnum = EventType.valueOf(type.uppercase())
                        predicates.add(criteriaBuilder.equal(root.get<EventType>("eventType"), eventTypeEnum))
                    } catch (e: IllegalArgumentException) {
                        // Invalid event type, ignore filter
                    }
                }
            }
            
            // City filter
            city?.let { cityName ->
                if (cityName.isNotEmpty()) {
                    predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("city")), 
                        "%${cityName.lowercase()}%"
                    ))
                }
            }
            
            // Date range filters
            dateStart?.let { start ->
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get<LocalDate>("startDateTime").`as`(LocalDate::class.java), 
                    start
                ))
            }
            
            dateEnd?.let { end ->
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get<LocalDate>("endDateTime").`as`(LocalDate::class.java), 
                    end
                ))
            }
            
            // Tags filter
            if (!tags.isNullOrEmpty()) {
                val tagPredicates = tags.map { tag ->
                    criteriaBuilder.isMember(tag.trim(), root.get<List<String>>("tags"))
                }
                predicates.add(criteriaBuilder.or(*tagPredicates.toTypedArray()))
            }
            
            // Search query filter (searches in name, description, organizer name)
            searchQuery?.let { query ->
                if (query.isNotEmpty()) {
                    val queryLower = "%${query.lowercase()}%"
                    val searchPredicates = listOf(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), queryLower),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), queryLower),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("organizerName")), queryLower)
                    )
                    predicates.add(criteriaBuilder.or(*searchPredicates.toTypedArray()))
                }
            }
            
            // Location-based filtering (latitude, longitude, radius) would require more complex geometry calculations
            // For now, we'll skip this as it requires PostGIS or similar
            
            criteriaBuilder.and(*predicates.toTypedArray())
        }
    }
}
