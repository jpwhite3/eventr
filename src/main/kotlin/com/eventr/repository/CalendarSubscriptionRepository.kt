package com.eventr.repository

import com.eventr.model.CalendarSubscription
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CalendarSubscriptionRepository : JpaRepository<CalendarSubscription, UUID> {
    fun findByUserId(userId: UUID): CalendarSubscription?
    fun findByToken(token: String): CalendarSubscription?
    fun findByUserIdAndIsActive(userId: UUID, isActive: Boolean): CalendarSubscription?
}