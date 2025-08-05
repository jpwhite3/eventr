package com.eventr.repository

import com.eventr.model.Event
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import java.util.UUID

interface EventRepository : JpaRepository<Event, UUID>, JpaSpecificationExecutor<Event>
