package com.baseline.app.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.baseline.app.data.Checkin
import com.baseline.app.data.Experiment
import com.baseline.app.data.ExperimentStatus
import com.baseline.app.data.Metric
import com.baseline.app.logic.ExperimentEngine
import com.baseline.app.logic.Format
import com.baseline.app.logic.Insights
import com.baseline.app.logic.Stats
import java.io.File

object Export {

    fun csv(checkins: List<Checkin>): String = buildString {
        appendLine("date,sleep_min,sleep_quality,energy,mood,stress,alcohol_units,training,libido,note,source")
        checkins.forEach { c ->
            appendLine(
                listOf(
                    c.date,
                    c.sleepMin ?: "",
                    c.sleepQuality ?: "",
                    c.energy ?: "",
                    c.mood ?: "",
                    c.stress ?: "",
                    c.alcoholUnits,
                    c.training.label,
                    c.libido ?: "",
                    escapeCsv(c.note ?: ""),
                    c.source
                ).joinToString(",")
            )
        }
    }

    private fun escapeCsv(value: String): String =
        if (value.contains(',') || value.contains('"') || value.contains('\n'))
            "\"${value.replace("\"", "\"\"")}\"" else value

    fun json(checkins: List<Checkin>, experiments: List<Experiment>): String = buildString {
        appendLine("{")
        appendLine("  \"format\": \"baseline-v1\",")
        appendLine("  \"exported_at\": \"${java.time.Instant.now()}\",")
        appendLine("  \"checkins\": [")
        checkins.forEachIndexed { i, c ->
            val note = if (c.note == null) "null" else "\"" + c.note.replace("\"", "\\\"") + "\""
            val sleep = c.sleepMin?.toString() ?: "null"
            val quality = c.sleepQuality?.toString() ?: "null"
            val energy = c.energy?.toString() ?: "null"
            val mood = c.mood?.toString() ?: "null"
            val stress = c.stress?.toString() ?: "null"
            val libido = c.libido?.toString() ?: "null"
            append("    {\"date\":\"" + c.date + "\",\"sleep_min\":" + sleep)
            append(",\"sleep_quality\":" + quality + ",\"energy\":" + energy)
            append(",\"mood\":" + mood + ",\"stress\":" + stress)
            append(",\"alcohol_units\":" + c.alcoholUnits + ",\"training\":\"" + c.training.label + "\"")
            append(",\"libido\":" + libido + ",\"note\":" + note)
            append(",\"source\":\"" + c.source + "\"}")
            if (i != checkins.lastIndex) append(",")
            appendLine()
        }
        appendLine("  ],")
        appendLine("  \"experiments\": [")
        experiments.forEachIndexed { i, e ->
            val title = e.title.replace("\"", "\\\"")
            append("    {\"id\":\"" + e.id + "\",\"title\":\"" + title + "\"")
            append(",\"metric\":\"" + e.metric.key + "\",\"expected_direction\":" + e.expectedDirection)
            append(",\"baseline_days\":" + e.baselineDays + ",\"intervention_days\":" + e.interventionDays)
            append(",\"washout_days\":" + e.washoutDays + ",\"repeat_days\":" + e.repeatDays)
            append(",\"preregistered_at\":" + e.preregisteredAt)
            append(",\"status\":\"" + e.status.name.lowercase() + "\"}")
            if (i != experiments.lastIndex) append(",")
            appendLine()
        }
        appendLine("  ]")
        append("}")
    }

    /** A one-pager a clinician could actually read. Free, always. */
    fun summary(checkins: List<Checkin>, experiments: List<Experiment>): String = buildString {
        val today = java.time.LocalDate.now()
        appendLine("BASELINE — PERSONAL SUMMARY")
        appendLine("Generated ${Format.isoDate(today)} · self-tracked, not a clinical record")
        appendLine("=".repeat(46))
        appendLine()
        appendLine("LOGGED DAYS: ${checkins.count { !it.isEmpty }}")
        appendLine()
        appendLine("PILLAR TRENDS (90 days)")
        Insights.PILLARS.forEach { m ->
            val values = ExperimentEngine.seriesFor(checkins, m, 90, today)
            if (values.isEmpty()) return@forEach
            val band = Stats.band(values)
            val drift = Stats.drift(ExperimentEngine.recentValues(checkins, m, 7, today), band)
            appendLine(
                "  " + m.label.padEnd(14) +
                    Format.value(m, band.mean).padStart(8) +
                    "  range ${Format.value(m, band.low)}–${Format.value(m, band.high)}" +
                    " (${values.size} days)" +
                    if (drift.detected) "  [outside usual range this week]" else ""
            )
        }
        appendLine()
        val finished = experiments.filter { it.status == ExperimentStatus.COMPLETE || it.finishedNow() }
        if (finished.isNotEmpty()) {
            appendLine("COMPLETED PERSONAL EXPERIMENTS")
            finished.forEach { e ->
                appendLine("  ${e.title}")
                appendLine("    ${ExperimentEngine.analyse(e, checkins).verdict}")
            }
            appendLine()
        }
        appendLine("THREE QUESTIONS TO RAISE WITH A DOCTOR")
        val sleep = ExperimentEngine.seriesFor(checkins, Metric.SLEEP_MIN, 90, today)
        val sleepLine = if (sleep.isEmpty()) "no data" else Format.minutes(Stats.mean(sleep))
        appendLine("  1. My sleep has averaged $sleepLine over 90 days — is that contributing")
        appendLine("     to how I feel?")
        appendLine("  2. Is there anything in these trends you would want to investigate?")
        appendLine("  3. Which screening is due for someone my age, given nothing here is a diagnosis?")
        appendLine()
        appendLine("NOTE: This summary is self-reported wellness data. It is not a diagnosis,")
        appendLine("      not a medical record, and not a substitute for clinical assessment.")
    }

    private fun Experiment.finishedNow(): Boolean {
        val elapsed = dayIndex() + 1
        return status == ExperimentStatus.RUNNING && elapsed >= totalDays
    }

    fun share(context: Context, filename: String, content: String, mime: String) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, filename).apply { writeText(content) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Baseline — $filename")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export Baseline"))
    }
}
