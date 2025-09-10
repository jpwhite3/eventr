package com.eventr.service

import com.eventr.dto.*
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
interface ReportSchedulingService {
    fun getAllSchedules(): List<ReportScheduleDto>
    fun getActiveSchedules(): List<ReportScheduleDto>
    fun getDueSchedules(): List<ReportScheduleDto>
    fun getScheduleById(id: UUID): ReportScheduleDto?
    fun createSchedule(scheduleDto: ReportScheduleDto): ReportScheduleDto
    fun updateSchedule(id: UUID, scheduleDto: ReportScheduleDto): ReportScheduleDto?
    fun deleteSchedule(id: UUID): Boolean
    fun toggleScheduleStatus(id: UUID): ReportScheduleDto?
    fun executeSchedule(id: UUID): Boolean
    fun testEmailDelivery(id: UUID): Boolean
    fun getExecutionHistory(scheduleId: UUID): List<ReportExecutionDto>
    fun getScheduleSummary(): ScheduledReportSummaryDto
}