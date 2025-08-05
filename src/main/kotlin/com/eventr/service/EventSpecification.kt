package com.eventr.service

import com.eventr.model.Event
import com.eventr.model.EventInstance
import com.eventr.model.EventStatus
import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.Predicate
import org.springframework.data.jpa.domain.Specification
import java.time.LocalDate

object EventSpecification {
    
    @JvmStatic
    fun filterBy(
        location: String?, 
        dateStart: LocalDate?, 
        dateEnd: LocalDate?, 
        tags: List<String>?, 
        publishedOnly: Boolean
    ): Specification<Event> {
        return Specification { root, _, criteriaBuilder ->
            val predicates = mutableListOf<Predicate>()
            
            if (publishedOnly) {
                predicates.add(criteriaBuilder.equal(root.get<EventStatus>("status"), EventStatus.PUBLISHED))
            }
            
            if (!location.isNullOrEmpty() || dateStart != null || dateEnd != null) {
                val eventInstanceJoin: Join<Event, EventInstance> = root.join("instances")
                
                location?.let { loc ->
                    if (loc.isNotEmpty()) {
                        predicates.add(criteriaBuilder.like(
                            criteriaBuilder.lower(eventInstanceJoin.get("location")), 
                            "%${loc.lowercase()}%"
                        ))
                    }
                }
                
                dateStart?.let { start ->
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        eventInstanceJoin.get<LocalDate>("dateTime").`as`(LocalDate::class.java), 
                        start
                    ))
                }
                
                dateEnd?.let { end ->
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        eventInstanceJoin.get<LocalDate>("dateTime").`as`(LocalDate::class.java), 
                        end
                    ))
                }
            }
            
            if (!tags.isNullOrEmpty()) {
                predicates.add(root.get<List<String>>("tags").`in`(tags))
            }
            
            criteriaBuilder.and(*predicates.toTypedArray())
        }
    }
}
