package com.eventr.dto

import com.eventr.model.EventStatus
import java.util.UUID

data class EventDto(
    var id: UUID? = null,
    var name: String? = null,
    var description: String? = null,
    var status: EventStatus? = null,
    var bannerImageUrl: String? = null,
    var thumbnailImageUrl: String? = null,
    var tags: List<String>? = null,
    var capacity: Int? = null,
    var waitlistEnabled: Boolean? = null,
    var instances: List<EventInstanceDto>? = null
)
