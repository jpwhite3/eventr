package com.eventr.repository

import com.eventr.model.EventInstance
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EventInstanceRepository : JpaRepository<EventInstance, UUID>
