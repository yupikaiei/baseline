package com.baseline.app.logic

import com.baseline.app.data.Training

/**
 * Pure mapping rules for wearable data. Kept out of the Android layer so the
 * conversions can be tested and reasoned about on their own — these numbers
 * decide what the user sees, so they should not be buried in a suspend function.
 */
object HealthMapping {

    /**
     * Health Connect does not carry Garmin's proprietary sleep score, so quality
     * is derived from sleep efficiency = time asleep / time in bed.
     *
     * Bands are deliberately coarse. Precision here would be false: a watch's
     * staging is itself an estimate.
     */
    fun sleepQualityFromEfficiency(efficiencyPercent: Double): Int? {
        if (efficiencyPercent.isNaN() || efficiencyPercent <= 0.0) return null
        return when {
            efficiencyPercent >= 90.0 -> 5
            efficiencyPercent >= 85.0 -> 4
            efficiencyPercent >= 75.0 -> 3
            efficiencyPercent >= 65.0 -> 2
            else -> 1
        }
    }

    /**
     * Exercise minutes to the app's three-value training field. 45 minutes is the
     * cut for "hard" because it separates a real session from a walk or a short
     * commute, and the app only needs to know whether the day carried load.
     */
    fun trainingFromMinutes(minutes: Long): Training? = when {
        minutes <= 0L -> null
        minutes >= 45L -> Training.HARD
        else -> Training.LIGHT
    }

    /** Sleep efficiency as a percentage, guarding against a zero-length session. */
    fun efficiencyPercent(totalMillis: Long, awakeMillis: Long): Double? {
        if (totalMillis <= 0L) return null
        val awake = awakeMillis.coerceIn(0L, totalMillis)
        return ((totalMillis - awake).toDouble() / totalMillis.toDouble()) * 100.0
    }
}
