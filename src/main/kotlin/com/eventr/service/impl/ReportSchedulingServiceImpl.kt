package com.eventr.service.impl

import com.eventr.dto.*
import com.eventr.service.ReportSchedulingService
import com.eventr.service.AnalyticsService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
class ReportSchedulingServiceImpl(
    private val analyticsService: AnalyticsService
) : ReportSchedulingService {

    private val logger = LoggerFactory.getLogger(ReportSchedulingServiceImpl::class.java)
    
    // In-memory storage for demo purposes - in production, use database
    private val schedules = ConcurrentHashMap<UUID, ReportScheduleDto>()
    private val executions = ConcurrentHashMap<UUID, MutableList<ReportExecutionDto>>()

    init {
        // Initialize with some sample schedules for demonstration
        createSampleSchedules()
    }

    override fun getAllSchedules(): List<ReportScheduleDto> {
        return schedules.values.toList().sortedBy { it.name }
    }

    override fun getActiveSchedules(): List<ReportScheduleDto> {
        return schedules.values.filter { it.active }.sortedBy { it.nextScheduled }
    }

    override fun getDueSchedules(): List<ReportScheduleDto> {
        val now = LocalDateTime.now()
        return schedules.values.filter { 
            it.active && it.nextScheduled != null && it.nextScheduled <= now 
        }.sortedBy { it.nextScheduled }
    }

    override fun getScheduleById(id: UUID): ReportScheduleDto? {
        return schedules[id]
    }

    override fun createSchedule(scheduleDto: ReportScheduleDto): ReportScheduleDto {
        val id = UUID.randomUUID()
        val now = LocalDateTime.now()
        
        val schedule = scheduleDto.copy(
            id = id,
            nextScheduled = calculateNextRunTime(scheduleDto.frequency, now),
            createdAt = now,
            updatedAt = now
        )
        
        schedules[id] = schedule
        executions[id] = mutableListOf()
        
        logger.info("Created new report schedule: ${schedule.name} (${id})")
        return schedule
    }

    override fun updateSchedule(id: UUID, scheduleDto: ReportScheduleDto): ReportScheduleDto? {
        val existing = schedules[id] ?: return null
        
        val updated = scheduleDto.copy(
            id = id,
            createdAt = existing.createdAt,
            updatedAt = LocalDateTime.now(),
            nextScheduled = if (scheduleDto.frequency != existing.frequency) {
                calculateNextRunTime(scheduleDto.frequency, LocalDateTime.now())
            } else {
                existing.nextScheduled
            }
        )
        
        schedules[id] = updated
        logger.info("Updated report schedule: ${updated.name} (${id})")
        return updated
    }

    override fun deleteSchedule(id: UUID): Boolean {
        val removed = schedules.remove(id)
        executions.remove(id)
        
        if (removed != null) {
            logger.info("Deleted report schedule: ${removed.name} (${id})")
            return true
        }
        return false
    }

    override fun toggleScheduleStatus(id: UUID): ReportScheduleDto? {
        val existing = schedules[id] ?: return null
        
        val toggled = existing.copy(
            active = !existing.active,
            updatedAt = LocalDateTime.now()
        )
        
        schedules[id] = toggled
        logger.info("Toggled schedule status for: ${toggled.name} (${id}) - Active: ${toggled.active}")
        return toggled
    }

    override fun executeSchedule(id: UUID): Boolean {
        val schedule = schedules[id] ?: return false
        
        if (!schedule.active) {
            logger.warn("Attempted to execute inactive schedule: ${schedule.name} (${id})")
            return false
        }

        val execution = ReportExecutionDto(
            scheduleId = id,
            executionTime = LocalDateTime.now(),
            status = ReportExecutionStatus.RUNNING,
            recipients = schedule.emailRecipients
        )
        
        executions.computeIfAbsent(id) { mutableListOf() }.add(execution)
        
        try {
            // Simulate report generation
            logger.info("Executing scheduled report: ${schedule.name} (${id})")
            
            // In a real implementation, this would:
            // 1. Fetch data based on schedule.dataSource and filters
            // 2. Generate report in specified format
            // 3. Send email to recipients
            // 4. Update execution status
            
            // For demo, we'll simulate success after a brief delay
            Thread.sleep(1000) // Simulate processing time
            
            val completedExecution = execution.copy(
                status = ReportExecutionStatus.COMPLETED,
                filename = "${schedule.name}_${LocalDateTime.now().toString().replace(":", "-")}.${schedule.format.name.lowercase()}",
                generatedRecords = (100..5000).random()
            )
            
            // Update execution record
            executions[id]?.let { list ->
                val index = list.indexOf(execution)
                if (index >= 0) {
                    list[index] = completedExecution
                }
            }
            
            // Update next scheduled time
            val updatedSchedule = schedule.copy(
                lastGenerated = LocalDateTime.now(),
                nextScheduled = calculateNextRunTime(schedule.frequency, LocalDateTime.now()),
                updatedAt = LocalDateTime.now()
            )
            schedules[id] = updatedSchedule
            
            logger.info("Successfully executed scheduled report: ${schedule.name} (${id})")
            return true
            
        } catch (e: Exception) {
            logger.error("Failed to execute scheduled report: ${schedule.name} (${id})", e)
            
            val failedExecution = execution.copy(
                status = ReportExecutionStatus.FAILED,
                errorMessage = e.message ?: "Unknown error"
            )
            
            // Update execution record
            executions[id]?.let { list ->
                val index = list.indexOf(execution)
                if (index >= 0) {
                    list[index] = failedExecution
                }
            }
            
            return false
        }
    }

    override fun testEmailDelivery(id: UUID): Boolean {
        val schedule = schedules[id] ?: return false
        
        // In a real implementation, this would send a test email
        logger.info("Sending test email for schedule: ${schedule.name} to ${schedule.emailRecipients}")
        
        // Simulate email send
        return try {
            Thread.sleep(500) // Simulate email service call
            true
        } catch (e: Exception) {
            logger.error("Failed to send test email for schedule: ${schedule.name}", e)
            false
        }
    }

    override fun getExecutionHistory(scheduleId: UUID): List<ReportExecutionDto> {
        return executions[scheduleId]?.sortedByDescending { it.executionTime } ?: emptyList()
    }

    override fun getScheduleSummary(): ScheduledReportSummaryDto {
        val allSchedules = schedules.values.toList()
        val activeSchedules = allSchedules.filter { it.active }
        val dueSchedules = getDueSchedules()
        
        val recentExecutions = executions.values
            .flatten()
            .sortedByDescending { it.executionTime }
            .take(10)
        
        val formatCounts = allSchedules.groupingBy { it.format }.eachCount()
        
        val nextUpcoming = activeSchedules
            .filter { it.nextScheduled != null }
            .sortedBy { it.nextScheduled }
            .take(5)
        
        return ScheduledReportSummaryDto(
            totalSchedules = allSchedules.size,
            activeSchedules = activeSchedules.size,
            dueSchedules = dueSchedules.size,
            recentExecutions = recentExecutions,
            popularFormats = formatCounts,
            nextUpcoming = nextUpcoming
        )
    }

    private fun calculateNextRunTime(frequency: ReportFrequency, from: LocalDateTime): LocalDateTime {
        return when (frequency) {
            ReportFrequency.DAILY -> from.plusDays(1)
            ReportFrequency.WEEKLY -> from.plusWeeks(1)
            ReportFrequency.MONTHLY -> from.plusMonths(1)
            ReportFrequency.QUARTERLY -> from.plusMonths(3)
        }
    }

    private fun createSampleSchedules() {
        // Create sample schedules for demonstration
        val sampleSchedules = listOf(
            ReportScheduleDto(
                name = "Weekly Event Performance Report",
                description = "Comprehensive weekly analysis of event metrics and attendance",
                frequency = ReportFrequency.WEEKLY,
                format = ReportFormat.PDF,
                emailRecipients = listOf("manager@company.com", "analytics@company.com"),
                dataSource = "event_analytics",
                active = true
            ),
            ReportScheduleDto(
                name = "Monthly Revenue Analysis",
                description = "Detailed monthly financial performance and revenue trends",
                frequency = ReportFrequency.MONTHLY,
                format = ReportFormat.EXCEL,
                emailRecipients = listOf("finance@company.com", "ceo@company.com"),
                dataSource = "financial_analytics",
                active = true
            ),
            ReportScheduleDto(
                name = "Daily Registration Summary",
                description = "Daily overview of new registrations and user activity",
                frequency = ReportFrequency.DAILY,
                format = ReportFormat.CSV,
                emailRecipients = listOf("operations@company.com"),
                dataSource = "registration_analytics",
                active = false
            )
        )
        
        sampleSchedules.forEach { createSchedule(it) }
        
        logger.info("Initialized ${sampleSchedules.size} sample report schedules")
    }
}