package com.eventr.controller

import com.eventr.dto.*
import com.eventr.service.AnalyticsService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:3001", "http://localhost:3002", "http://localhost:3003"])
class AnalyticsController(private val analyticsService: AnalyticsService) {

    @GetMapping("/executive")
    fun getExecutiveMetrics(
        @RequestParam(defaultValue = "30d") timeframe: String
    ): ResponseEntity<ExecutiveMetricsDto> {
        return ResponseEntity.ok(analyticsService.getExecutiveMetrics(timeframe))
    }

    @GetMapping("/executive/events")
    fun getTopEvents(
        @RequestParam(defaultValue = "5") limit: Int
    ): ResponseEntity<List<EventSummaryDto>> {
        return ResponseEntity.ok(analyticsService.getTopEvents(limit))
    }

    @GetMapping("/executive/charts")
    fun getChartData(
        @RequestParam(defaultValue = "30d") timeframe: String
    ): ResponseEntity<ChartDataDto> {
        return ResponseEntity.ok(analyticsService.getChartData(timeframe))
    }

    @GetMapping("/registrations")
    fun getRegistrationTrends(
        @RequestParam(defaultValue = "30d") timeframe: String
    ): ResponseEntity<RegistrationTrendsDto> {
        return ResponseEntity.ok(analyticsService.getRegistrationTrends(timeframe))
    }

    @GetMapping("/events/{eventId}")
    fun getEventAnalytics(
        @PathVariable eventId: UUID
    ): ResponseEntity<EventAnalyticsDto> {
        return ResponseEntity.ok(analyticsService.getEventSpecificAnalytics(eventId))
    }

    @GetMapping("/attendance")
    fun getAttendanceAnalytics(
        @RequestParam(defaultValue = "30d") timeframe: String
    ): ResponseEntity<AttendanceAnalyticsDto> {
        return ResponseEntity.ok(analyticsService.getAttendanceAnalytics(timeframe))
    }
}