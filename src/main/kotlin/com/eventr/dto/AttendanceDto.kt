package com.eventr.dto

import java.util.UUID

data class AttendanceDto(
    var registrationId: UUID? = null,
    var userName: String? = null,
    var userEmail: String? = null,
    var checkedIn: Boolean = false
)
