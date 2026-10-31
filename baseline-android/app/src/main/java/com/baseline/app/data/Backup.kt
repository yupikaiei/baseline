package com.baseline.app.data

import android.content.Context
import com.baseline.app.logic.Importers
import com.baseline.app.ui.Export
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Rolling local backups. No cloud: snapshots go to app-private storage and are
 * rotated, so a cleared browser/app cache cannot take the history with it.
 */
object Backup {

    private const val DIR = "backups"
    private const val KEEP = 7

    fun dir(context: Context): File = File(context.filesDir, DIR).apply { mkdirs() }

    fun write(context: Context, checkins: List<Checkin>, experiments: List<Experiment>): File {
        val stamp = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val file = File(dir(context), "baseline-$stamp.json")
        file.writeText(Export.json(checkins, experiments))
        rotate(context)
        return file
    }

    fun rotate(context: Context, keep: Int = KEEP) {
        val files = list(context)
        files.drop(keep).forEach { it.delete() }
    }

    /** Newest first. */
    fun list(context: Context): List<File> =
        dir(context).listFiles { f -> f.isFile && f.name.endsWith(".json") }
            ?.sortedByDescending { it.name }
            ?: emptyList()

    fun lastBackupMillis(context: Context): Long =
        list(context).firstOrNull()?.let { file ->
            val date = file.name.removePrefix("baseline-").removeSuffix(".json")
            runCatching {
                LocalDate.parse(date)
                    .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }.getOrNull()
        } ?: 0L

    fun isDue(context: Context, hours: Long = 24): Boolean {
        val last = lastBackupMillis(context)
        if (last == 0L) return true
        val ageHours = (Instant.now().toEpochMilli() - last) / 3_600_000L
        return ageHours >= hours
    }

    /** Restores check-ins from a backup or export file. Returns rows written. */
    fun restoreInto(db: Db, file: File): Int {
        val text = file.readText()
        val checkins = Importers.parseCheckinsJson(text)
        checkins.forEach { db.saveCheckin(it) }
        return checkins.size
    }

    fun restoreInto(db: Db, text: String): Int {
        val checkins = Importers.parseCheckinsJson(text)
        checkins.forEach { db.saveCheckin(it) }
        return checkins.size
    }
}
