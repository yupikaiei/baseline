package com.baseline.app.logic

import com.baseline.app.data.Metric
import com.baseline.app.data.Training
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object Format {

    private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun isoDate(date: LocalDate): String = date.format(ISO)

    fun today(): String = isoDate(LocalDate.now())

    fun minutes(min: Int?): String {
        if (min == null) return "—"
        return "${min / 60}h${(min % 60).toString().padStart(2, '0')}"
    }

    fun minutes(min: Double): String {
        val total = Math.round(min).toInt()
        return "${total / 60}h${(total % 60).toString().padStart(2, '0')}"
    }

    fun value(metric: Metric, v: Double): String = when (metric) {
        Metric.SLEEP_MIN -> minutes(v)
        Metric.TRAINING -> Training.from(Math.round(v).toInt()).label
        Metric.ALCOHOL -> {
            val rounded = v
            if (rounded % 1.0 == 0.0) "${rounded.toInt()}u" else String.format("%.1fu", rounded)
        }
        else -> String.format("%.1f%s", v, metric.unit)
    }

    /** Signed delta, e.g. "+34 min" or "−0.4". */
    fun signedDelta(v: Double, metric: Metric): String {
        val sign = if (v >= 0) "+" else "−"
        return if (metric == Metric.SLEEP_MIN) "$sign${minutes(abs(v))}"
        else "$sign${String.format("%.1f", abs(v))}"
    }

    private fun abs(v: Double) = if (v < 0) -v else v
}
