package com.eventr.dto

import java.util.UUID

data class RegistrationCreateDto(
    var eventInstanceId: UUID? = null,
    var userId: UUID? = null,
    // Keep these for backward compatibility
    var userEmail: String? = null,
    var userName: String? = null,
    var formData: String? = null
)
