package com.eventr.dto

import java.time.LocalDateTime
import java.util.*

data class ReportScheduleDto(
    val id: UUID? = null,
    val name: String,
    val description: String? = null,
    val frequency: ReportFrequency,
    val format: ReportFormat,
    val emailRecipients: List<String>,
    val dataSource: String,
    val filters: Map<String, Any>? = null,
    val lastGenerated: LocalDateTime? = null,
    val nextScheduled: LocalDateTime? = null,
    val active: Boolean = true,
    val template: String? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)

enum class ReportFrequency {
    DAILY, WEEKLY, MONTHLY, QUARTERLY
}

enum class ReportFormat {
    PDF, EXCEL, CSV
}

data class ReportExecutionDto(
    val scheduleId: UUID,
    val executionTime: LocalDateTime,
    val status: ReportExecutionStatus,
    val filename: String? = null,
    val errorMessage: String? = null,
    val recipients: List<String>,
    val generatedRecords: Int? = null
)

enum class ReportExecutionStatus {
    PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
}

data class ReportTemplateDto(
    val id: UUID? = null,
    val name: String,
    val description: String,
    val category: ReportCategory,
    val format: ReportFormat,
    val defaultFrequency: ReportFrequency,
    val requiredDataSources: List<String>,
    val availableFilters: List<ReportFilterDto>,
    val sampleOutput: String? = null
)

enum class ReportCategory {
    EVENTS, USERS, REGISTRATIONS, FINANCIAL, SYSTEM, ATTENDANCE, ANALYTICS
}

data class ReportFilterDto(
    val name: String,
    val displayName: String,
    val type: ReportFilterType,
    val required: Boolean = false,
    val defaultValue: Any? = null,
    val options: List<String>? = null,
    val validationRules: Map<String, Any>? = null
)

enum class ReportFilterType {
    TEXT, NUMBER, DATE, BOOLEAN, SELECT, MULTI_SELECT, DATE_RANGE
}

data class ScheduledReportSummaryDto(
    val totalSchedules: Int,
    val activeSchedules: Int,
    val dueSchedules: Int,
    val recentExecutions: List<ReportExecutionDto>,
    val popularFormats: Map<ReportFormat, Int>,
    val nextUpcoming: List<ReportScheduleDto>
)