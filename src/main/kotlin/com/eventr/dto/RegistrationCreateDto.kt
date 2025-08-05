package com.eventr.dto

import java.util.UUID

data class RegistrationCreateDto(
    var eventInstanceId: UUID? = null,
    var userEmail: String? = null,
    var userName: String? = null,
    var formData: String? = null
)
