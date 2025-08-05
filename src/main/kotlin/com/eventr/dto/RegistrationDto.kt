package com.eventr.dto

import com.eventr.model.RegistrationStatus
import java.util.UUID

data class RegistrationDto(
    var id: UUID? = null,
    var eventInstanceId: UUID? = null,
    var userEmail: String? = null,
    var userName: String? = null,
    var status: RegistrationStatus? = null,
    var formData: String? = null
)
