package com.eventr.controller

import com.eventr.service.ResourceService
import com.eventr.service.ResourceDto
import com.eventr.service.CreateResourceDto
import com.eventr.service.UpdateResourceDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:3001", "http://localhost:3002", "http://localhost:3003"])
@RequestMapping("/api/resources")
class ResourceController(
    private val resourceService: ResourceService
) {

    @GetMapping
    fun getAllResources(): ResponseEntity<List<ResourceDto>> {
        val resources = resourceService.getAllResources()
        return ResponseEntity.ok(resources)
    }

    @GetMapping("/{id}")
    fun getResourceById(@PathVariable id: UUID): ResponseEntity<ResourceDto> {
        val resource = resourceService.getResourceById(id)
        return if (resource != null) {
            ResponseEntity.ok(resource)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping
    fun createResource(@RequestBody createDto: CreateResourceDto): ResponseEntity<ResourceDto> {
        return try {
            val resource = resourceService.createResource(createDto)
            ResponseEntity.ok(resource)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PutMapping("/{id}")
    fun updateResource(
        @PathVariable id: UUID,
        @RequestBody updateDto: UpdateResourceDto
    ): ResponseEntity<ResourceDto> {
        return try {
            val resource = resourceService.updateResource(id, updateDto)
            if (resource != null) {
                ResponseEntity.ok(resource)
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @DeleteMapping("/{id}")
    fun deleteResource(@PathVariable id: UUID): ResponseEntity<Map<String, String>> {
        return if (resourceService.deleteResource(id)) {
            ResponseEntity.ok(mapOf("message" to "Resource deleted successfully"))
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/event/{eventId}")
    fun getResourcesByEvent(@PathVariable eventId: UUID): ResponseEntity<List<ResourceDto>> {
        val resources = resourceService.getResourcesByEvent(eventId)
        return ResponseEntity.ok(resources)
    }

    @GetMapping("/available")
    fun getAvailableResources(
        @RequestParam(required = false) startDate: String?,
        @RequestParam(required = false) endDate: String?
    ): ResponseEntity<List<ResourceDto>> {
        val resources = resourceService.getAvailableResources(startDate, endDate)
        return ResponseEntity.ok(resources)
    }
}