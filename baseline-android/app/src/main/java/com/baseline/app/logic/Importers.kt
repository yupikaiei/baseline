package com.baseline.app.logic

import com.baseline.app.data.Checkin
import com.baseline.app.data.Metric
import com.baseline.app.data.Training
import org.json.JSONArray
import org.json.JSONObject

class ImportException(message: String) : Exception(message)

object Importers {

    private val HEADER = listOf(
        "date", "sleep_min", "sleep_quality", "energy", "mood", "stress",
        "alcohol_units", "training", "libido", "note", "source"
    )

    /**
     * Accepts the CSV this app exports, and any tracker export whose header
     * contains at least a date column. Unknown columns are ignored.
     */
    fun parseCsv(text: String): List<Checkin> {
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.size < 2) throw ImportException("The file has no data rows.")

        val header = splitCsvLine(lines.first()).map { it.trim().lowercase() }
        if (!header.contains("date")) throw ImportException("No 'date' column found in the header.")

        fun idx(name: String) = header.indexOf(name)
        val iDate = idx("date")
        val iSleep = idx("sleep_min")
        val iQuality = idx("sleep_quality")
        val iEnergy = idx("energy")
        val iMood = idx("mood")
        val iStress = idx("stress")
        val iAlcohol = idx("alcohol_units")
        val iTraining = idx("training")
        val iLibido = idx("libido")
        val iNote = idx("note")

        val out = mutableListOf<Checkin>()
        lines.drop(1).forEachIndexed { rowIndex, line ->
            val cells = splitCsvLine(line)
            fun cell(i: Int): String? = if (i >= 0 && i < cells.size) cells[i].trim().ifEmpty { null } else null
            fun intAt(i: Int): Int? = cell(i)?.toIntOrNull()
            fun doubleAt(i: Int): Double? = cell(i)?.toDoubleOrNull()

            val rawDate = cell(iDate) ?: return@forEachIndexed
            val date = normalizeDate(rawDate)
                ?: throw ImportException("Row ${rowIndex + 2}: '${rawDate}' is not a date (expected YYYY-MM-DD).")

            out.add(
                Checkin(
                    date = date,
                    sleepMin = intAt(iSleep),
                    sleepQuality = intAt(iQuality),
                    energy = intAt(iEnergy),
                    mood = intAt(iMood),
                    stress = intAt(iStress),
                    alcoholUnits = doubleAt(iAlcohol) ?: 0.0,
                    training = parseTraining(cell(iTraining)),
                    libido = intAt(iLibido),
                    note = cell(iNote),
                    source = "import"
                )
            )
        }
        if (out.isEmpty()) throw ImportException("No usable rows found.")
        return out
    }

    /** Restores the JSON this app exports. Tolerates missing optional fields. */
    fun parseCheckinsJson(text: String): List<Checkin> {
        val root = try {
            JSONObject(text)
        } catch (e: Exception) {
            throw ImportException("That does not look like a Baseline JSON export.")
        }
        val array: JSONArray = root.optJSONArray("checkins")
            ?: throw ImportException("No 'checkins' array in the file.")
        val out = mutableListOf<Checkin>()
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val date = o.optString("date", "")
            if (date.isBlank()) continue
            out.add(
                Checkin(
                    date = date,
                    sleepMin = o.optIntOrNull("sleep_min"),
                    sleepQuality = o.optIntOrNull("sleep_quality"),
                    energy = o.optIntOrNull("energy"),
                    mood = o.optIntOrNull("mood"),
                    stress = o.optIntOrNull("stress"),
                    alcoholUnits = if (o.has("alcohol_units")) o.optDouble("alcohol_units", 0.0) else 0.0,
                    training = parseTraining(o.optString("training", "")),
                    libido = o.optIntOrNull("libido"),
                    note = o.optString("note", "").ifBlank { null },
                    source = "restore"
                )
            )
        }
        if (out.isEmpty()) throw ImportException("No usable check-ins in the file.")
        return out
    }

    private fun JSONObject.optIntOrNull(key: String): Int? =
        if (!has(key) || isNull(key)) null else optInt(key)

    private fun normalizeDate(raw: String): String? {
        val trimmed = raw.trim()
        if (Regex("\\d{4}-\\d{2}-\\d{2}").matches(trimmed)) return trimmed
        // Accept dd/MM/yyyy and yyyy/MM/dd without pulling in a formatter dependency.
        val parts = trimmed.split('/', '-', '.')
        if (parts.size == 3) {
            val a = parts[0].toIntOrNull()
            val b = parts[1].toIntOrNull()
            val c = parts[2].toIntOrNull()
            if (a != null && b != null && c != null) {
                return when {
                    parts[0].length == 4 -> "%04d-%02d-%02d".format(a, b, c)
                    parts[2].length == 4 -> "%04d-%02d-%02d".format(c, b, a)
                    else -> null
                }
            }
        }
        return null
    }

    private fun parseTraining(raw: String?): Training {
        if (raw == null) return Training.NONE
        val v = raw.trim().lowercase()
        return when {
            v.startsWith("hard") || v == "2" -> Training.HARD
            v.startsWith("light") || v == "1" -> Training.LIGHT
            else -> Training.NONE
        }
    }

    /** Minimal CSV splitter that respects quoted fields. */
    fun splitCsvLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                inQuotes && ch == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    sb.append('"'); i++
                }
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    out.add(sb.toString()); sb.clear()
                }
                else -> sb.append(ch)
            }
            i++
        }
        out.add(sb.toString())
        return out
    }

    fun headerForTests(): List<String> = HEADER
}

object Streaks {
    /**
     * Consecutive logged days ending today, or ending yesterday if today is not
     * logged yet — so an unfinished day never reads as a broken run.
     */
    fun current(dates: Set<String>, today: java.time.LocalDate): Int {
        var cursor = today
        if (!dates.contains(Format.isoDate(cursor))) {
            cursor = cursor.minusDays(1)
            if (!dates.contains(Format.isoDate(cursor))) return 0
        }
        var run = 0
        while (dates.contains(Format.isoDate(cursor))) {
            run++
            cursor = cursor.minusDays(1)
        }
        return run
    }

    fun best(dates: Set<String>): Int {
        if (dates.isEmpty()) return 0
        val sorted = dates.map { java.time.LocalDate.parse(it) }.sorted()
        var best = 1
        var run = 1
        for (i in 1 until sorted.size) {
            run = if (sorted[i] == sorted[i - 1].plusDays(1)) run + 1 else 1
            if (run > best) best = run
        }
        return best
    }
}

/** Convenience for tests and callers that only need a metric series. */
fun metricSeries(checkins: List<Checkin>, metric: Metric): List<Double> =
    checkins.mapNotNull { it.valueOf(metric) }
