package com.baseline.app.data

/** The eight things logged daily. The short list is the mechanism, not a compromise. */
enum class Metric(
    val label: String,
    val unit: String,
    val higherIsBetter: Boolean,
    val pillar: String
) {
    SLEEP_MIN("Sleep", "h", true, "Sleep"),
    SLEEP_QUALITY("Sleep quality", "/5", true, "Sleep"),
    ENERGY("Energy", "/5", true, "Move"),
    MOOD("Mood", "/5", true, "Mind"),
    STRESS("Stress", "/5", false, "Mind"),
    ALCOHOL("Alcohol", "u", false, "Fuel"),
    TRAINING("Training", "", true, "Move"),
    LIBIDO("Libido", "/5", true, "Drive");

    val key: String get() = name.lowercase()
}

enum class Training(val label: String, val value: Int) {
    NONE("None", 0), LIGHT("Light", 1), HARD("Hard", 2);

    companion object {
        fun from(v: Int) = entries.firstOrNull { it.value == v } ?: NONE
    }
}

data class Checkin(
    val date: String,
    val sleepMin: Int? = null,
    val sleepQuality: Int? = null,
    val energy: Int? = null,
    val mood: Int? = null,
    val stress: Int? = null,
    val alcoholUnits: Double = 0.0,
    val training: Training = Training.NONE,
    val libido: Int? = null,
    val note: String? = null,
    val source: String = "typed"
) {
    fun valueOf(m: Metric): Double? = when (m) {
        Metric.SLEEP_MIN -> sleepMin?.toDouble()
        Metric.SLEEP_QUALITY -> sleepQuality?.toDouble()
        Metric.ENERGY -> energy?.toDouble()
        Metric.MOOD -> mood?.toDouble()
        Metric.STRESS -> stress?.toDouble()
        Metric.ALCOHOL -> alcoholUnits
        Metric.TRAINING -> training.value.toDouble()
        Metric.LIBIDO -> libido?.toDouble()
    }

    val isEmpty: Boolean
        get() = sleepMin == null && sleepQuality == null && energy == null &&
            mood == null && stress == null && alcoholUnits == 0.0 &&
            training == Training.NONE && libido == null
}

enum class Grade(val label: String, val meaning: String) {
    A("A", "Multiple RCTs or meta-analyses, consistent, in humans"),
    B("B", "One good RCT, or strong consistent cohort evidence"),
    C("C", "Mechanistic plausibility, small or mixed trials"),
    D("D", "Popular and heavily sold, not supported")
}

data class EvidenceItem(
    val id: String,
    val pillar: String,
    val title: String,
    val grade: Grade,
    val claim: String,
    val summary: String,
    val citation: String,
    val pmid: String?,
    /** Required. Cannot be empty — content without this fails review. */
    val dontKnow: String,
    val reviewedAt: String = DEFAULT_REVIEWED
) {
    companion object {
        const val DEFAULT_REVIEWED = "2026-10-30"
    }
}

enum class Arm(val label: String) {
    BASELINE("Baseline"), INTERVENTION("Intervention"), WASHOUT("Washout"), REPEAT("Repeat")
}

enum class ExperimentStatus { RUNNING, COMPLETE, ABANDONED }

/** A pre-registered single-case experiment. Declaration fields are immutable. */
data class Experiment(
    val id: String,
    val title: String,
    val metric: Metric,
    val expectedDirection: Int,
    val confounders: List<String>,
    val baselineDays: Int,
    val interventionDays: Int,
    val washoutDays: Int,
    val repeatDays: Int,
    val preregisteredAt: Long,
    val status: ExperimentStatus = ExperimentStatus.RUNNING
) {
    val totalDays: Int get() = baselineDays + interventionDays + washoutDays + repeatDays

    /** Days elapsed since pre-registration, clamped to the trial length. */
    fun dayIndex(now: Long = System.currentTimeMillis()): Int =
        ((now - preregisteredAt) / 86_400_000L).toInt()

    fun dayNumber(now: Long = System.currentTimeMillis()): Int =
        (dayIndex(now) + 1).coerceIn(1, totalDays)

    fun progressPct(now: Long = System.currentTimeMillis()): Int =
        (dayNumber(now) * 100 / totalDays).coerceIn(0, 100)

    /** Arm assignment by date. A missed check-in can never desynchronise the design. */
    fun armForDay(idx: Int): Arm = when {
        idx < baselineDays -> Arm.BASELINE
        idx < baselineDays + interventionDays -> Arm.INTERVENTION
        idx < baselineDays + interventionDays + washoutDays -> Arm.WASHOUT
        else -> Arm.REPEAT
    }

    fun currentArm(now: Long = System.currentTimeMillis()): Arm =
        armForDay(dayIndex(now).coerceIn(0, totalDays))
}
