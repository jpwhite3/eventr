package com.eventr.controller

import com.eventr.dto.ReportScheduleDto
import com.eventr.service.ReportSchedulingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/reports/schedules")
class ReportSchedulingController(
    private val reportSchedulingService: ReportSchedulingService
) {

    @GetMapping
    fun getAllSchedules(): ResponseEntity<List<ReportScheduleDto>> {
        return ResponseEntity.ok(reportSchedulingService.getAllSchedules())
    }

    @GetMapping("/{id}")
    fun getSchedule(@PathVariable id: UUID): ResponseEntity<ReportScheduleDto> {
        val schedule = reportSchedulingService.getScheduleById(id)
        return if (schedule != null) {
            ResponseEntity.ok(schedule)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping
    fun createSchedule(@RequestBody scheduleDto: ReportScheduleDto): ResponseEntity<ReportScheduleDto> {
        val created = reportSchedulingService.createSchedule(scheduleDto)
        return ResponseEntity.ok(created)
    }

    @PutMapping("/{id}")
    fun updateSchedule(
        @PathVariable id: UUID,
        @RequestBody scheduleDto: ReportScheduleDto
    ): ResponseEntity<ReportScheduleDto> {
        val updated = reportSchedulingService.updateSchedule(id, scheduleDto)
        return if (updated != null) {
            ResponseEntity.ok(updated)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    fun deleteSchedule(@PathVariable id: UUID): ResponseEntity<Void> {
        val deleted = reportSchedulingService.deleteSchedule(id)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/{id}/execute")
    fun executeSchedule(@PathVariable id: UUID): ResponseEntity<Map<String, String>> {
        val result = reportSchedulingService.executeSchedule(id)
        return if (result) {
            ResponseEntity.ok(mapOf("status" to "success", "message" to "Report generation initiated"))
        } else {
            ResponseEntity.badRequest().body(mapOf("status" to "error", "message" to "Failed to execute schedule"))
        }
    }

    @PostMapping("/{id}/toggle")
    fun toggleSchedule(@PathVariable id: UUID): ResponseEntity<ReportScheduleDto> {
        val toggled = reportSchedulingService.toggleScheduleStatus(id)
        return if (toggled != null) {
            ResponseEntity.ok(toggled)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/active")
    fun getActiveSchedules(): ResponseEntity<List<ReportScheduleDto>> {
        return ResponseEntity.ok(reportSchedulingService.getActiveSchedules())
    }

    @GetMapping("/due")
    fun getDueSchedules(): ResponseEntity<List<ReportScheduleDto>> {
        return ResponseEntity.ok(reportSchedulingService.getDueSchedules())
    }

    @PostMapping("/test-email/{id}")
    fun testEmailDelivery(@PathVariable id: UUID): ResponseEntity<Map<String, String>> {
        val result = reportSchedulingService.testEmailDelivery(id)
        return if (result) {
            ResponseEntity.ok(mapOf("status" to "success", "message" to "Test email sent successfully"))
        } else {
            ResponseEntity.badRequest().body(mapOf("status" to "error", "message" to "Failed to send test email"))
        }
    }
}