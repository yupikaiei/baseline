package com.baseline.app.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.baseline.app.logic.HealthMapping
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * What a wearable could contribute to one day's check-in.
 * Every field is optional: a partial prefill is still useful, and nothing is
 * ever saved without the user confirming it.
 */
data class HealthPrefill(
    val sleepMin: Int? = null,
    val sleepQuality: Int? = null,
    val training: Training? = null,
    val notes: List<String> = emptyList()
) {
    val isEmpty: Boolean
        get() = sleepMin == null && sleepQuality == null && training == null
}

object HealthPermissions {
    val READ: Set<String> = setOf(
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    )
}

enum class HealthStatus { UNAVAILABLE, NEEDS_PROVIDER_UPDATE, AVAILABLE }

/**
 * Read-only bridge from Android Health Connect. The app never writes health data
 * and never asks for write permission.
 */
class HealthConnectSource(private val context: Context) {

    fun status(): HealthStatus = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> HealthStatus.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthStatus.NEEDS_PROVIDER_UPDATE
        else -> HealthStatus.UNAVAILABLE
    }

    private fun client(): HealthConnectClient? =
        if (status() == HealthStatus.AVAILABLE) HealthConnectClient.getOrCreate(context) else null

    suspend fun hasPermissions(): Boolean {
        val client = client() ?: return false
        return runCatching {
            client.permissionController.getGrantedPermissions().containsAll(HealthPermissions.READ)
        }.getOrDefault(false)
    }

    /**
     * Reads the day's sleep and exercise. A sleep session counts for the date it
     * *ends* on, which is how people think about a night.
     */
    suspend fun read(date: LocalDate): HealthPrefill {
        val client = client() ?: return HealthPrefill(
            notes = listOf("Health Connect is not available on this device.")
        )

        val zone = ZoneId.systemDefault()
        val dayStart = date.atStartOfDay(zone).toInstant()
        val dayEnd = date.atTime(LocalTime.MAX).atZone(zone).toInstant()

        // Health Connect's time filter matches a sleep session on when it *starts*,
        // so a night that begins before midnight falls outside [dayStart, dayEnd].
        // Query from the previous afternoon and keep the end-time filter below.
        val sleepQueryStart = date.minusDays(1).atTime(12, 0).atZone(zone).toInstant()

        val notes = mutableListOf<String>()

        val sleepMinutes = readSleepMinutes(client, sleepQueryStart, dayStart, dayEnd, zone, notes)
        val quality = readSleepQuality(client, sleepQueryStart, dayStart, dayEnd, zone, notes)
        val training = readTraining(client, dayStart, dayEnd)

        if (sleepMinutes == null && training == null) {
            notes.add("No sleep or exercise records from connected apps for this date.")
        }

        return HealthPrefill(
            sleepMin = sleepMinutes,
            sleepQuality = quality,
            training = training,
            notes = notes
        )
    }

    private suspend fun readSleepMinutes(
        client: HealthConnectClient,
        queryStart: java.time.Instant,
        start: java.time.Instant,
        end: java.time.Instant,
        zone: ZoneId,
        notes: MutableList<String>
    ): Int? {
        val sessions = runCatching {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(queryStart, end)
                )
            ).records.filter { it.endTime >= start && it.endTime <= end }
        }.getOrElse {
            notes.add("Could not read sleep: ${it.message?.take(80)}")
            return null
        }

        if (sessions.isEmpty()) return null

        val totalMinutes = sessions.sumOf {
            Duration.between(it.startTime, it.endTime).toMinutes().toInt().coerceAtLeast(0)
        }
        if (totalMinutes == 0) return null
        notes.add("Sleep from Health Connect: ${totalMinutes / 60}h${(totalMinutes % 60).toString().padStart(2, '0')} across ${sessions.size} session(s).")
        return totalMinutes
    }

    /**
     * Health Connect does not carry Garmin's proprietary sleep score, so quality
     * is derived from sleep efficiency (time asleep / time in bed). If a source
     * provides no stages at all, quality is left blank rather than guessed.
     */
    private suspend fun readSleepQuality(
        client: HealthConnectClient,
        queryStart: java.time.Instant,
        start: java.time.Instant,
        end: java.time.Instant,
        zone: ZoneId,
        notes: MutableList<String>
    ): Int? {
        val sessions = runCatching {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(queryStart, end)
                )
            ).records.filter { it.endTime >= start && it.endTime <= end }
        }.getOrDefault(emptyList())

        var total = 0L
        var awake = 0L
        var hasStages = false

        sessions.forEach { session ->
            total += Duration.between(session.startTime, session.endTime).toMillis()
            session.stages.forEach { stage ->
                hasStages = true
                val duration = Duration.between(stage.startTime, stage.endTime).toMillis()
                if (stage.stage == SleepSessionRecord.STAGE_TYPE_AWAKE ||
                    stage.stage == SleepSessionRecord.STAGE_TYPE_OUT_OF_BED
                ) {
                    awake += duration
                }
            }
        }

        if (!hasStages || total <= 0) {
            notes.add("Sleep stages were not provided, so quality was left blank.")
            return null
        }

        val efficiency = HealthMapping.efficiencyPercent(total, awake)
        val quality = efficiency?.let { HealthMapping.sleepQualityFromEfficiency(it) }
        if (efficiency != null && quality != null) {
            notes.add(
                "Quality $quality/5 derived from ${efficiency.toInt()}% sleep efficiency " +
                    "(no sleep score is shared by the watch)."
            )
        }
        return quality
    }

    private suspend fun readTraining(
        client: HealthConnectClient,
        start: java.time.Instant,
        end: java.time.Instant
    ): Training? {
        val sessions = runCatching {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            ).records
        }.getOrDefault(emptyList())

        if (sessions.isEmpty()) return null
        val minutes = sessions.sumOf {
            Duration.between(it.startTime, it.endTime).toMinutes()
        }
        return HealthMapping.trainingFromMinutes(minutes)
    }
}
