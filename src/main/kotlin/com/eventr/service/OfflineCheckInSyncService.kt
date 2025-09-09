package com.eventr.service

import com.eventr.dto.*
import java.util.*

/**
 * Service interface for offline check-in synchronization.
 * 
 * Focuses exclusively on offline synchronization concerns:
 * - Offline check-in data synchronization
 * - Conflict resolution for duplicate check-ins
 * - Data validation and integrity checks
 * - Sync status tracking and reporting
 * 
 * Follows Single Responsibility Principle by handling only offline sync logic.
 */
interface OfflineCheckInSyncService {

    /**
     * Synchronize offline check-in data with the server.
     * 
     * @param offlineCheckIns List of offline check-in records
     * @return List of synchronized check-in records with sync results
     */
    fun syncOfflineCheckIns(offlineCheckIns: List<OfflineCheckInDto>): List<CheckInDto>

    /**
     * Process individual offline check-in with conflict detection.
     * 
     * @param offlineCheckIn Offline check-in data
     * @return Sync result with status and resolved data
     */
    fun processOfflineCheckIn(offlineCheckIn: OfflineCheckInDto): OfflineCheckInSyncResult

    /**
     * Validate offline check-in data before synchronization.
     * 
     * @param offlineCheckIn Offline check-in data
     * @return Validation result with errors if any
     */
    fun validateOfflineCheckIn(offlineCheckIn: OfflineCheckInDto): ValidationResult

    /**
     * Resolve conflicts when offline and online check-ins conflict.
     * 
     * @param offlineCheckIn Offline check-in data
     * @param existingCheckIn Existing online check-in
     * @return Conflict resolution result
     */
    fun resolveCheckInConflict(offlineCheckIn: OfflineCheckInDto, existingCheckIn: CheckInDto): ConflictResolutionResult

    /**
     * Get sync status for offline check-ins batch.
     * 
     * @param batchId Batch ID for offline sync operation
     * @return Sync status with detailed results
     */
    fun getSyncStatus(batchId: String): OfflineSyncStatus

    /**
     * Get all pending offline check-ins for synchronization.
     * 
     * @param deviceId Optional device ID filter
     * @param eventId Optional event ID filter
     * @return List of pending offline check-ins
     */
    fun getPendingOfflineCheckIns(deviceId: String? = null, eventId: UUID? = null): List<OfflineCheckInDto>

    /**
     * Mark offline check-ins as synchronized.
     * 
     * @param checkInIds List of offline check-in IDs
     * @param syncTimestamp Synchronization timestamp
     */
    fun markAsSynchronized(checkInIds: List<String>, syncTimestamp: String)

    /**
     * Clean up old synchronized offline check-in records.
     * 
     * @param olderThanDays Delete records older than specified days
     * @return Number of records cleaned up
     */
    fun cleanupSynchronizedRecords(olderThanDays: Int): Int

    /**
     * Generate offline sync report for analysis.
     * 
     * @param eventId Event ID
     * @param timeRange Time range for report
     * @return Offline sync analysis report
     */
    fun generateSyncReport(eventId: UUID, timeRange: TimeRange): OfflineSyncReport
}

/**
 * Offline check-in synchronization result.
 */
data class OfflineCheckInSyncResult(
    val offlineId: String,
    val status: SyncStatus,
    val checkInDto: CheckInDto? = null,
    val errors: List<String> = emptyList(),
    val conflictResolution: String? = null,
    val syncTimestamp: String
)

/**
 * Synchronization status enumeration.
 */
enum class SyncStatus {
    SUCCESS,           // Successfully synchronized
    DUPLICATE,         // Duplicate check-in detected and resolved
    CONFLICT,          // Conflict detected and resolved
    VALIDATION_ERROR,  // Data validation failed
    REGISTRATION_NOT_FOUND, // Registration not found
    EVENT_NOT_FOUND,   // Event not found
    SESSION_NOT_FOUND, // Session not found
    SYNC_ERROR         // General synchronization error
}

/**
 * Validation result for offline check-in data.
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<ValidationError> = emptyList(),
    val warnings: List<String> = emptyList()
)

/**
 * Validation error details.
 */
data class ValidationError(
    val field: String,
    val code: String,
    val message: String
)

/**
 * Conflict resolution result.
 */
data class ConflictResolutionResult(
    val strategy: ConflictResolutionStrategy,
    val resolvedCheckIn: CheckInDto,
    val explanation: String
)

/**
 * Conflict resolution strategies.
 */
enum class ConflictResolutionStrategy {
    KEEP_EXISTING,     // Keep the existing online check-in
    REPLACE_WITH_OFFLINE, // Replace with offline check-in data
    MERGE_DATA,        // Merge offline and online data
    CREATE_DUPLICATE,  // Allow duplicate check-in
    MANUAL_REVIEW      // Requires manual review
}

/**
 * Offline sync status for batch operations.
 */
data class OfflineSyncStatus(
    val batchId: String,
    val totalRecords: Int,
    val processedRecords: Int,
    val successfulSyncs: Int,
    val failedSyncs: Int,
    val duplicatesResolved: Int,
    val conflictsResolved: Int,
    val status: BatchSyncStatus,
    val startTime: String,
    val endTime: String? = null,
    val errorSummary: Map<SyncStatus, Int> = emptyMap()
)

/**
 * Batch synchronization status.
 */
enum class BatchSyncStatus {
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    PARTIALLY_COMPLETED
}

/**
 * Offline synchronization report.
 */
data class OfflineSyncReport(
    val eventId: UUID,
    val timeRange: TimeRange,
    val totalOfflineCheckIns: Int,
    val successfulSyncs: Int,
    val failedSyncs: Int,
    val syncSuccessRate: Double,
    val averageSyncTime: Double, // Milliseconds
    val deviceBreakdown: Map<String, DeviceSyncStats>,
    val errorBreakdown: Map<SyncStatus, Int>,
    val recommendations: List<String>
)

/**
 * Device-specific sync statistics.
 */
data class DeviceSyncStats(
    val deviceId: String,
    val deviceName: String?,
    val totalCheckIns: Int,
    val successfulSyncs: Int,
    val failedSyncs: Int,
    val averageSyncTime: Double,
    val lastSyncTime: String
)