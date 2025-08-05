package com.eventr.dto

import java.time.LocalDateTime
import java.util.UUID

data class EventInstanceDto(
    var id: UUID? = null,
    var dateTime: LocalDateTime? = null,
    var location: String? = null
)
