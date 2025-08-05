package com.eventr.dto

data class EventCreateDto(
    var name: String? = null,
    var description: String? = null,
    var tags: List<String>? = null,
    var capacity: Int? = null,
    var waitlistEnabled: Boolean? = null,
    var formData: String? = null
)
