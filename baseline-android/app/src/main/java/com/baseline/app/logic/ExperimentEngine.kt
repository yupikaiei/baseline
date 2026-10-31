package com.baseline.app.logic

import com.baseline.app.data.Arm
import com.baseline.app.data.Checkin
import com.baseline.app.data.Experiment
import com.baseline.app.data.Metric
import com.baseline.app.data.Training
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ExperimentAnalysis(
    val experiment: Experiment,
    val baseline: List<Double>,
    val intervention: List<Double>,
    val washout: List<Double>,
    val repeat: List<Double>,
    val primary: StatResult?,
    val repeatVsBaseline: StatResult?,
    val confoundedDays: Int,
    val missingDays: Int,
    val elapsedDays: Int,
    val finished: Boolean
) {
    val totalLogged: Int
        get() = baseline.size + intervention.size + washout.size + repeat.size

    /** "Inconclusive" is a first-class outcome, not a failure to report. */
    val verdict: String
        get() {
            val s = primary ?: return "Not started"
            if (!s.enoughData) {
                return "Not enough data yet — ${s.nBaseline} baseline vs ${s.nIntervention} " +
                    "intervention days (10 each needed)."
            }
            if (s.isLikelyReal) {
                val dir = if (s.diff > 0) "higher" else "lower"
                return "Effect likely real for you: " +
                    Format.signedDelta(s.diff, experiment.metric) + " against baseline ($dir)."
            }
            if (s.excludesZero) {
                return "A difference is visible but the test is borderline " +
                    "(p=${String.format("%.2f", s.p)}). Treat as suggestive, not settled."
            }
            return "No detectable effect. 95% CI includes zero."
        }

    val verdictTag: String
        get() {
            val s = primary ?: return "IN PROGRESS"
            return when {
                !s.enoughData -> "IN PROGRESS"
                s.isLikelyReal -> "EFFECT LIKELY REAL"
                else -> "INCONCLUSIVE"
            }
        }
}

object ExperimentEngine {

    fun analyse(experiment: Experiment, checkins: List<Checkin>, now: Long = System.currentTimeMillis()): ExperimentAnalysis {
        val start: LocalDate = Instant.ofEpochMilli(experiment.preregisteredAt)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        val today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()

        val byDate = checkins.associateBy { it.date }
        val arms = mutableMapOf(
            Arm.BASELINE to mutableListOf<Double>(),
            Arm.INTERVENTION to mutableListOf<Double>(),
            Arm.WASHOUT to mutableListOf<Double>(),
            Arm.REPEAT to mutableListOf<Double>()
        )

        var confounded = 0
        var missing = 0
        var elapsed = 0

        for (i in 0 until experiment.totalDays) {
            val date = start.plusDays(i.toLong())
            if (date.isAfter(today)) break
            elapsed++
            val c = byDate[Format.isoDate(date)]
            val v = c?.valueOf(experiment.metric)
            if (v == null) {
                missing++
                continue
            }
            // Confounders are counted and disclosed, never silently dropped.
            if (c.alcoholUnits > 2.0 || c.training == Training.HARD) confounded++
            arms.getValue(experiment.armForDay(i)).add(v)
        }

        val baseline = arms.getValue(Arm.BASELINE)
        val intervention = arms.getValue(Arm.INTERVENTION)
        val repeat = arms.getValue(Arm.REPEAT)

        val primary = if (baseline.isNotEmpty() && intervention.isNotEmpty())
            Stats.compare(intervention, baseline) else null
        val rep = if (baseline.isNotEmpty() && repeat.isNotEmpty())
            Stats.compare(repeat, baseline) else null

        return ExperimentAnalysis(
            experiment = experiment,
            baseline = baseline,
            intervention = intervention,
            washout = arms.getValue(Arm.WASHOUT),
            repeat = repeat,
            primary = primary,
            repeatVsBaseline = rep,
            confoundedDays = confounded,
            missingDays = missing,
            elapsedDays = elapsed,
            finished = elapsed >= experiment.totalDays
        )
    }

    fun seriesFor(checkins: List<Checkin>, metric: Metric, windowDays: Int, today: LocalDate = LocalDate.now()): List<Double> {
        val cutoff = today.minusDays(windowDays.toLong())
        return checkins
            .filter { LocalDate.parse(it.date).isAfter(cutoff) }
            .mapNotNull { it.valueOf(metric) }
    }

    fun recentValues(checkins: List<Checkin>, metric: Metric, days: Int, today: LocalDate = LocalDate.now()): List<Double> {
        val cutoff = today.minusDays(days.toLong())
        return checkins
            .filter { LocalDate.parse(it.date).isAfter(cutoff) }
            .mapNotNull { it.valueOf(metric) }
    }

    data class Snapshot(val metric: Metric, val band90: Band, val band28: Band, val drift: Drift)

    fun snapshot(checkins: List<Checkin>, metric: Metric, today: LocalDate = LocalDate.now()): Snapshot {
        val b90 = Stats.band(seriesFor(checkins, metric, 90, today))
        return Snapshot(
            metric = metric,
            band90 = b90,
            band28 = Stats.band(seriesFor(checkins, metric, 28, today)),
            drift = Stats.drift(recentValues(checkins, metric, 7, today), b90)
        )
    }
}

data class Template(
    val id: String,
    val title: String,
    val variable: String,
    val metric: Metric,
    val expectedDirection: Int,
    val confounders: List<String>,
    val baselineDays: Int,
    val interventionDays: Int,
    val washoutDays: Int,
    val repeatDays: Int,
    val instruction: String
) {
    val totalDays: Int get() = baselineDays + interventionDays + washoutDays + repeatDays
}

val templates = listOf(
    Template(
        "screen-cutoff", "Screen cutoff at 22:00", "No screens after 22:00",
        Metric.SLEEP_MIN, 1, listOf("alcohol", "late training", "travel"),
        14, 21, 7, 14,
        "Put the phone on the charger outside the bedroom. Log sleep as usual."
    ),
    Template(
        "alcohol-free", "Alcohol-free block", "Zero alcohol",
        Metric.SLEEP_QUALITY, 1, listOf("late training", "travel", "illness"),
        14, 21, 7, 0,
        "No alcohol for the intervention arm. Keep logging — it should stay at 0."
    ),
    Template(
        "walk-after-meals", "Walk after meals", "10 min walking after lunch and dinner",
        Metric.ENERGY, 1, listOf("poor sleep", "alcohol", "illness"),
        14, 21, 7, 0,
        "Two 10-minute walks a day, after meals. Rate energy each evening."
    ),
    Template(
        "strength-2x", "Strength twice a week", "Two resistance sessions per week",
        Metric.ENERGY, 1, listOf("poor sleep", "alcohol", "illness"),
        14, 28, 0, 0,
        "Two full-body sessions per week. Book them like meetings. Log honestly."
    ),
    Template(
        "caffeine-cutoff", "Caffeine cutoff at noon", "No caffeine after 12:00",
        Metric.SLEEP_QUALITY, 1, listOf("alcohol", "late training", "stress"),
        14, 21, 7, 0,
        "Coffee, tea and cola: none after midday."
    ),
    Template(
        "step-floor", "Step floor of 8,000", "At least 8,000 steps a day",
        Metric.MOOD, 1, listOf("illness", "travel", "alcohol"),
        14, 21, 0, 0,
        "8,000 steps minimum, every day. Rate mood each evening."
    )
)
