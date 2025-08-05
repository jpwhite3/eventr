package com.eventr.dto

data class EventUpdateDto(
    var name: String? = null,
    var description: String? = null,
    var tags: List<String>? = null,
    var capacity: Int? = null,
    var waitlistEnabled: Boolean? = null
)
