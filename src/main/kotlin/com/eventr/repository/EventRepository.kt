package com.eventr.repository

import com.eventr.model.Event
import com.eventr.model.EventStatus
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EventRepository : JpaRepository<Event, UUID> {
    fun findByStatus(status: EventStatus, sort: Sort): List<Event>
}
