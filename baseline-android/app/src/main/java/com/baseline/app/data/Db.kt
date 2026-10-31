package com.baseline.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class Db private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, NAME, null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE checkin (
                date TEXT PRIMARY KEY,
                sleep_min INTEGER,
                sleep_quality INTEGER,
                energy INTEGER,
                mood INTEGER,
                stress INTEGER,
                alcohol_units REAL NOT NULL DEFAULT 0,
                training INTEGER NOT NULL DEFAULT 0,
                libido INTEGER,
                note TEXT,
                source TEXT NOT NULL DEFAULT 'typed',
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE experiment (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                metric TEXT NOT NULL,
                expected_direction INTEGER NOT NULL,
                confounders TEXT NOT NULL,
                baseline_days INTEGER NOT NULL,
                intervention_days INTEGER NOT NULL,
                washout_days INTEGER NOT NULL,
                repeat_days INTEGER NOT NULL,
                preregistered_at INTEGER NOT NULL,
                status TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE TABLE settings (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
        db.execSQL(PROGRAM_PROGRESS_DDL)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) db.execSQL(PROGRAM_PROGRESS_DDL)
    }

    // ---------------- check-ins ----------------

    fun saveCheckin(c: Checkin) {
        val values = ContentValues().apply {
            put("date", c.date)
            put("sleep_min", c.sleepMin)
            put("sleep_quality", c.sleepQuality)
            put("energy", c.energy)
            put("mood", c.mood)
            put("stress", c.stress)
            put("alcohol_units", c.alcoholUnits)
            put("training", c.training.value)
            put("libido", c.libido)
            put("note", c.note)
            put("source", c.source)
            put("created_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict("checkin", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun checkinOn(date: String): Checkin? =
        readableDatabase.query("checkin", null, "date = ?", arrayOf(date), null, null, null)
            .use { if (it.moveToFirst()) it.toCheckin() else null }

    fun recentCheckins(days: Int = 180): List<Checkin> {
        val since = java.time.LocalDate.now().minusDays(days.toLong()).toString()
        return readableDatabase.query(
            "checkin", null, "date >= ?", arrayOf(since), null, null, "date DESC"
        ).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.toCheckin()) }
        }
    }

    fun allCheckins(): List<Checkin> =
        readableDatabase.query("checkin", null, null, null, null, null, "date ASC").use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.toCheckin()) }
        }

    // ---------------- experiments ----------------

    fun saveExperiment(e: Experiment) {
        val values = ContentValues().apply {
            put("id", e.id)
            put("title", e.title)
            put("metric", e.metric.name)
            put("expected_direction", e.expectedDirection)
            put("confounders", e.confounders.joinToString(","))
            put("baseline_days", e.baselineDays)
            put("intervention_days", e.interventionDays)
            put("washout_days", e.washoutDays)
            put("repeat_days", e.repeatDays)
            put("preregistered_at", e.preregisteredAt)
            put("status", e.status.name)
        }
        writableDatabase.insertWithOnConflict("experiment", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun experiments(): List<Experiment> =
        readableDatabase.query("experiment", null, null, null, null, null, "preregistered_at DESC")
            .use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.toExperiment()) }
            }

    fun setExperimentStatus(id: String, status: ExperimentStatus) {
        writableDatabase.update(
            "experiment",
            ContentValues().apply { put("status", status.name) },
            "id = ?", arrayOf(id)
        )
    }

    // ---------------- programs ----------------

    fun markProgramDay(programId: String, day: Int, done: Boolean = true) {
        if (done) {
            writableDatabase.insertWithOnConflict(
                "program_progress", null,
                ContentValues().apply {
                    put("program_id", programId)
                    put("day", day)
                    put("completed_at", System.currentTimeMillis())
                },
                SQLiteDatabase.CONFLICT_REPLACE
            )
        } else {
            writableDatabase.delete("program_progress", "program_id = ? AND day = ?", arrayOf(programId, day.toString()))
        }
    }

    fun programProgress(programId: String): Set<Int> =
        readableDatabase.query(
            "program_progress", arrayOf("day"), "program_id = ?", arrayOf(programId), null, null, null
        ).use { cursor ->
            buildSet { while (cursor.moveToNext()) add(cursor.getInt(0)) }
        }

    fun clearProgram(programId: String) {
        writableDatabase.delete("program_progress", "program_id = ?", arrayOf(programId))
    }

    // ---------------- settings ----------------

    fun setting(key: String): String? =
        readableDatabase.query("settings", arrayOf("value"), "key = ?", arrayOf(key), null, null, null)
            .use { if (it.moveToFirst()) it.getString(0) else null }

    fun setSetting(key: String, value: String) {
        writableDatabase.insertWithOnConflict(
            "settings", null,
            ContentValues().apply { put("key", key); put("value", value) },
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    private fun android.database.Cursor.toCheckin() = Checkin(
        date = getString(getColumnIndexOrThrow("date")),
        sleepMin = getIntOrNull("sleep_min"),
        sleepQuality = getIntOrNull("sleep_quality"),
        energy = getIntOrNull("energy"),
        mood = getIntOrNull("mood"),
        stress = getIntOrNull("stress"),
        alcoholUnits = getDouble(getColumnIndexOrThrow("alcohol_units")),
        training = Training.from(getInt(getColumnIndexOrThrow("training"))),
        libido = getIntOrNull("libido"),
        note = if (isNull(getColumnIndexOrThrow("note"))) null else getString(getColumnIndexOrThrow("note")),
        source = getString(getColumnIndexOrThrow("source"))
    )

    private fun android.database.Cursor.getIntOrNull(name: String): Int? {
        val i = getColumnIndexOrThrow(name)
        return if (isNull(i)) null else getInt(i)
    }

    private fun android.database.Cursor.toExperiment() = Experiment(
        id = getString(getColumnIndexOrThrow("id")),
        title = getString(getColumnIndexOrThrow("title")),
        metric = Metric.entries.firstOrNull { it.name == getString(getColumnIndexOrThrow("metric")) }
            ?: Metric.SLEEP_MIN,
        expectedDirection = getInt(getColumnIndexOrThrow("expected_direction")),
        confounders = getString(getColumnIndexOrThrow("confounders"))
            .split(",").filter { it.isNotBlank() },
        baselineDays = getInt(getColumnIndexOrThrow("baseline_days")),
        interventionDays = getInt(getColumnIndexOrThrow("intervention_days")),
        washoutDays = getInt(getColumnIndexOrThrow("washout_days")),
        repeatDays = getInt(getColumnIndexOrThrow("repeat_days")),
        preregisteredAt = getLong(getColumnIndexOrThrow("preregistered_at")),
        status = ExperimentStatus.entries
            .firstOrNull { it.name == getString(getColumnIndexOrThrow("status")) }
            ?: ExperimentStatus.RUNNING
    )

    companion object {
        private const val NAME = "baseline.db"

        private const val PROGRAM_PROGRESS_DDL =
            "CREATE TABLE IF NOT EXISTS program_progress (" +
                "program_id TEXT NOT NULL, day INTEGER NOT NULL, completed_at INTEGER NOT NULL, " +
                "PRIMARY KEY (program_id, day))"

        @Volatile
        private var instance: Db? = null

        fun get(context: Context): Db =
            instance ?: synchronized(this) {
                instance ?: Db(context).also { instance = it }
            }
    }
}
