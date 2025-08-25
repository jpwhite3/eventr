package com.eventr.controller

import com.eventr.dto.*
import com.eventr.model.*
import com.eventr.repository.*
import com.eventr.service.PrerequisiteValidationService
import org.springframework.beans.BeanUtils
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.util.*

@RestController
@RequestMapping("/api/prerequisites")
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:3001", "http://localhost:3002", "http://localhost:3003"])
class PrerequisiteManagementController(
    private val prerequisiteValidationService: PrerequisiteValidationService,
    private val sessionPrerequisiteRepository: SessionPrerequisiteRepository,
    private val sessionDependencyRepository: SessionDependencyRepository,
    private val sessionRepository: SessionRepository
) {

    @PostMapping("/sessions/{sessionId}")
    @Transactional
    fun createPrerequisite(
        @PathVariable sessionId: UUID,
        @RequestBody createDto: PrerequisiteCreateDto
    ): ResponseEntity<SessionPrerequisiteDto> {
        return try {
            val session = sessionRepository.findById(sessionId)
                .orElseThrow { IllegalArgumentException("Session not found") }
            
            val prerequisite = SessionPrerequisite().apply {
                this.session = session
                type = createDto.type
                prerequisiteSession = createDto.prerequisiteSessionId?.let { 
                    sessionRepository.findById(it).orElse(null)
                }
                groupId = createDto.groupId
                operator = createDto.operator
                priority = createDto.priority
                isRequired = createDto.isRequired
                ruleDefinition = createDto.ruleDefinition
                errorMessage = createDto.errorMessage
                allowGracePeriod = createDto.allowGracePeriod
                gracePeriodHours = createDto.gracePeriodHours
                allowAdminOverride = createDto.allowAdminOverride
            }
            
            val saved = sessionPrerequisiteRepository.save(prerequisite)
            ResponseEntity.ok(convertPrerequisiteToDto(saved))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/dependencies")
    @Transactional
    fun createSessionDependency(@RequestBody createDto: DependencyCreateDto): ResponseEntity<SessionDependencyDto> {
        return try {
            val parentSession = sessionRepository.findById(createDto.parentSessionId)
                .orElseThrow { IllegalArgumentException("Parent session not found") }
            
            val dependentSession = sessionRepository.findById(createDto.dependentSessionId)
                .orElseThrow { IllegalArgumentException("Dependent session not found") }
            
            // Prevent self-dependencies
            if (createDto.parentSessionId == createDto.dependentSessionId) {
                throw IllegalArgumentException("Session cannot depend on itself")
            }
            
            val dependency = SessionDependency().apply {
                this.parentSession = parentSession
                this.dependentSession = dependentSession
                dependencyType = createDto.dependencyType
                isStrict = createDto.isStrict
                timingGapMinutes = createDto.timingGapMinutes
                description = createDto.description
            }
            
            val saved = sessionDependencyRepository.save(dependency)
            ResponseEntity.ok(convertDependencyToDto(saved))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @GetMapping("/sessions/{sessionId}")
    fun getSessionPrerequisites(@PathVariable sessionId: UUID): ResponseEntity<List<SessionPrerequisiteDto>> {
        val prerequisites = sessionPrerequisiteRepository.findBySessionIdAndIsActiveTrue(sessionId)
        val dtos = prerequisites.map { convertPrerequisiteToDto(it) }
        return ResponseEntity.ok(dtos)
    }

    @PostMapping("/sessions/{sessionId}/validate")
    fun validatePrerequisites(
        @PathVariable sessionId: UUID,
        @RequestParam registrationId: UUID
    ): ResponseEntity<PrerequisiteValidationDto> {
        val validation = prerequisiteValidationService.validatePrerequisites(sessionId, registrationId)
        return ResponseEntity.ok(validation)
    }

    @PostMapping("/sessions/{sessionId}/validate-dependencies")
    fun validateSessionDependencies(
        @PathVariable sessionId: UUID,
        @RequestParam registrationId: UUID
    ): ResponseEntity<List<String>> {
        val violations = prerequisiteValidationService.validateSessionDependencies(sessionId, registrationId)
        return ResponseEntity.ok(violations)
    }

    @GetMapping("/events/{eventId}/circular-dependencies")
    fun detectCircularDependencies(@PathVariable eventId: UUID): ResponseEntity<List<CircularDependencyDto>> {
        val circular = prerequisiteValidationService.detectCircularDependencies(eventId)
        return ResponseEntity.ok(circular)
    }

    @GetMapping("/sessions/{fromSessionId}/path-to/{toSessionId}")
    fun getSessionDependencyPath(
        @PathVariable fromSessionId: UUID,
        @PathVariable toSessionId: UUID
    ): ResponseEntity<SessionPathDto> {
        val path = prerequisiteValidationService.getSessionDependencyPath(fromSessionId, toSessionId)
        return ResponseEntity.ok(path)
    }

    @GetMapping("/events/{eventId}/analysis")
    fun analyzeDependencyStructure(@PathVariable eventId: UUID): ResponseEntity<DependencyAnalysisDto> {
        val analysis = prerequisiteValidationService.analyzeDependencyStructure(eventId)
        return ResponseEntity.ok(analysis)
    }

    @DeleteMapping("/prerequisites/{prerequisiteId}")
    @Transactional
    fun deletePrerequisite(@PathVariable prerequisiteId: UUID): ResponseEntity<Void> {
        return try {
            val prerequisite = sessionPrerequisiteRepository.findById(prerequisiteId)
                .orElseThrow { IllegalArgumentException("Prerequisite not found") }
            
            prerequisite.isActive = false
            prerequisite.updatedAt = LocalDateTime.now()
            sessionPrerequisiteRepository.save(prerequisite)
            
            ResponseEntity.noContent().build()
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/dependencies/{dependencyId}")
    @Transactional
    fun deleteDependency(@PathVariable dependencyId: UUID): ResponseEntity<Void> {
        return try {
            sessionDependencyRepository.deleteById(dependencyId)
            ResponseEntity.noContent().build()
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }

    private fun convertPrerequisiteToDto(prerequisite: SessionPrerequisite): SessionPrerequisiteDto {
        return SessionPrerequisiteDto().apply {
            BeanUtils.copyProperties(prerequisite, this)
            sessionTitle = prerequisite.session?.title
            prerequisiteSessionTitle = prerequisite.prerequisiteSession?.title
        }
    }

    private fun convertDependencyToDto(dependency: SessionDependency): SessionDependencyDto {
        return SessionDependencyDto(
            id = dependency.id,
            parentSessionId = dependency.parentSession!!.id!!,
            parentSessionTitle = dependency.parentSession!!.title,
            dependentSessionId = dependency.dependentSession!!.id!!,
            dependentSessionTitle = dependency.dependentSession!!.title,
            dependencyType = dependency.dependencyType,
            isStrict = dependency.isStrict,
            timingGapMinutes = dependency.timingGapMinutes,
            description = dependency.description
        )
    }
}