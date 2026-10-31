package com.baseline.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.lifecycleScope
import com.baseline.app.ui.BaselineTheme
import com.baseline.app.ui.Palette
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * DEBUG ONLY. Writes a sleep session and an exercise session into Health Connect
 * so the read path can be tested without a physical watch, then reads them back
 * and reports what it sees. Lives in src/debug, so release builds never include it.
 */
class SeedHealthActivity : ComponentActivity() {

    private var status by mutableStateOf("Seeding…")

    private val writePermissions = setOf(
        HealthPermission.getWritePermission(SleepSessionRecord::class),
        HealthPermission.getWritePermission(ExerciseSessionRecord::class)
    )

    private val launcher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { seed() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaselineTheme {
                Box(Modifier.fillMaxSize()) {
                    Text(
                        status,
                        color = Palette.Text,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    )
                }
            }
        }
        launcher.launch(writePermissions.toTypedArray())
    }

    private fun seed() {
        lifecycleScope.launch {
            status = runCatching { run() }
                .getOrElse { "SEED FAILED: ${it::class.simpleName}: ${it.message}" }
            android.util.Log.i("SeedHealth", status)
        }
    }

    private suspend fun run(): String {
        val client = HealthConnectClient.getOrCreate(this)
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()

        // Last night 23:00 -> 07:30, 30 minutes awake => ~94% efficiency.
        val bedStart = today.minusDays(1).atTime(23, 0).atZone(zone)
        val wakeTime = today.atTime(7, 30).atZone(zone)
        fun beforeWake(minutes: Long): Instant = wakeTime.minusMinutes(minutes).toInstant()

        val stages = listOf(
            SleepSessionRecord.Stage(
                startTime = bedStart.toInstant(),
                endTime = beforeWake(420),
                stage = SleepSessionRecord.STAGE_TYPE_LIGHT
            ),
            SleepSessionRecord.Stage(
                startTime = beforeWake(420),
                endTime = beforeWake(300),
                stage = SleepSessionRecord.STAGE_TYPE_DEEP
            ),
            SleepSessionRecord.Stage(
                startTime = beforeWake(300),
                endTime = beforeWake(30),
                stage = SleepSessionRecord.STAGE_TYPE_REM
            ),
            SleepSessionRecord.Stage(
                startTime = beforeWake(30),
                endTime = wakeTime.toInstant(),
                stage = SleepSessionRecord.STAGE_TYPE_AWAKE
            )
        )

        val sleep = SleepSessionRecord(
            startTime = bedStart.toInstant(),
            startZoneOffset = bedStart.offset,
            endTime = wakeTime.toInstant(),
            endZoneOffset = wakeTime.offset,
            metadata = Metadata.manualEntry(),
            title = "Seeded night",
            notes = "debug seed",
            stages = stages
        )

        val exerciseStart = today.atTime(18, 0).atZone(zone)
        val exercise = ExerciseSessionRecord(
            startTime = exerciseStart.toInstant(),
            startZoneOffset = exerciseStart.offset,
            endTime = exerciseStart.toInstant().plus(Duration.ofMinutes(50)),
            endZoneOffset = exerciseStart.offset,
            metadata = Metadata.manualEntry(),
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
            title = "Seeded session"
        )

        val ids = client.insertRecords(listOf(sleep, exercise))

        val wideStart = today.minusDays(3).atStartOfDay(zone).toInstant()
        val wideEnd = today.plusDays(1).atTime(23, 59).atZone(zone).toInstant()

        val sleepBack = client.readRecords(
            ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(wideStart, wideEnd)
            )
        ).records
        val exBack = client.readRecords(
            ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(wideStart, wideEnd)
            )
        ).records

        val granted = client.permissionController.getGrantedPermissions()
            .filter { it.startsWith("android.permission.health.") }
            .map { it.removePrefix("android.permission.health.") }
            .sorted()

        val first = sleepBack.firstOrNull()

        return buildString {
            appendLine("wrote=${ids.recordIdsList.size}")
            appendLine("readback sleep=${sleepBack.size} exercise=${exBack.size}")
            appendLine("granted=$granted")
            appendLine("zone=${zone.id}")
            appendLine("now=${ZonedDateTime.now()}")
            if (first != null) {
                appendLine("sleep start=${first.startTime}")
                appendLine("sleep end=${first.endTime}")
                appendLine("stages=${first.stages.size}")
            } else {
                appendLine("no sleep records in 3-day window")
            }
            appendLine("todayStart=${today.atStartOfDay(zone).toInstant()}")
            appendLine("todayEnd=${today.atTime(23, 59, 59).atZone(zone).toInstant()}")
        }
    }
}
