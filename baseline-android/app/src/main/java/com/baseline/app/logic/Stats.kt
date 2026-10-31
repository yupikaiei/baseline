package com.baseline.app.logic

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

/** The person's own normal range: mean +/- 1 SD, clamped to what was actually logged. */
data class Band(val n: Int, val mean: Double, val low: Double, val high: Double) {
    val isEmpty: Boolean get() = n == 0
    companion object {
        val EMPTY = Band(0, 0.0, 0.0, 0.0)
    }
}

data class Drift(val detected: Boolean, val recentMean: Double, val delta: Double, val n: Int) {
    companion object {
        fun none(recentMean: Double = 0.0, n: Int = 0) = Drift(false, recentMean, 0.0, n)
    }
}

data class StatResult(
    val diff: Double,
    val low: Double,
    val high: Double,
    val p: Double,
    val nBaseline: Int,
    val nIntervention: Int
) {
    val enoughData: Boolean
        get() = nBaseline >= MIN_PER_ARM && nIntervention >= MIN_PER_ARM

    val excludesZero: Boolean get() = low > 0 || high < 0

    /**
     * Deliberately conservative. Most personal experiments are underpowered,
     * and saying so is the feature rather than a limitation.
     */
    val isLikelyReal: Boolean get() = enoughData && excludesZero && p < 0.10

    companion object {
        const val MIN_PER_ARM = 10
    }
}

object Stats {

    fun mean(xs: List<Double>): Double = if (xs.isEmpty()) 0.0 else xs.sum() / xs.size

    private fun sd(xs: List<Double>): Double {
        if (xs.size < 2) return 0.0
        val m = mean(xs)
        return sqrt(xs.sumOf { (it - m) * (it - m) } / (xs.size - 1))
    }

    /**
     * Not a percentile range: with 30-90 points the edges collapse into noise
     * and read as false precision.
     */
    fun band(xs: List<Double>): Band {
        if (xs.isEmpty()) return Band.EMPTY
        val m = mean(xs)
        val s = if (xs.size < 5) 0.0 else sd(xs)
        val lo = maxOf(m - s, xs.min())
        val hi = minOf(m + s, xs.max())
        return Band(xs.size, m, lo, hi)
    }

    /** Effect-size based, so a 2% wobble never fires an alert. */
    fun drift(recent: List<Double>, baseline: Band): Drift {
        val rm = mean(recent)
        if (recent.size < 4 || baseline.n < 7) return Drift.none(rm, recent.size)
        val delta = rm - baseline.mean
        val halfBand = (baseline.high - baseline.low) / 2.0
        val outside = rm < baseline.low || rm > baseline.high
        val material = if (halfBand <= 0.0) delta != 0.0 else abs(delta) >= halfBand
        return Drift(outside && material, rm, delta, recent.size)
    }

    /** Percentile bootstrap CI for (a - b). Deterministic for a given seed. */
    fun bootstrapCi(a: List<Double>, b: List<Double>, iterations: Int = 1000, seed: Int = 42): DoubleArray {
        if (a.isEmpty() || b.isEmpty()) return doubleArrayOf(0.0, 0.0)
        val rnd = Random(seed)
        val diffs = DoubleArray(iterations)
        for (i in 0 until iterations) {
            var sa = 0.0
            repeat(a.size) { sa += a[rnd.nextInt(a.size)] }
            var sb = 0.0
            repeat(b.size) { sb += b[rnd.nextInt(b.size)] }
            diffs[i] = sa / a.size - sb / b.size
        }
        diffs.sort()
        val lo = diffs[(iterations * 0.025).toInt().coerceIn(0, iterations - 1)]
        val hi = diffs[(iterations * 0.975).toInt().coerceIn(0, iterations - 1)]
        return doubleArrayOf(lo, hi)
    }

    /** Two-sided permutation test on arm labels. No distributional assumptions. */
    fun permutationP(a: List<Double>, b: List<Double>, iterations: Int = 2000, seed: Int = 7): Double {
        if (a.isEmpty() || b.isEmpty()) return 1.0
        val observed = abs(mean(a) - mean(b))
        val pooled = (a + b).toMutableList()
        val rnd = Random(seed)
        var hits = 0
        repeat(iterations) {
            pooled.shuffle(rnd)
            val pa = pooled.subList(0, a.size)
            val pb = pooled.subList(a.size, pooled.size)
            if (abs(mean(pa) - mean(pb)) >= observed - 1e-9) hits++
        }
        return (hits + 1).toDouble() / (iterations + 1).toDouble()
    }

    fun compare(intervention: List<Double>, baseline: List<Double>): StatResult {
        val ci = bootstrapCi(intervention, baseline)
        return StatResult(
            diff = mean(intervention) - mean(baseline),
            low = ci[0],
            high = ci[1],
            p = permutationP(intervention, baseline),
            nBaseline = baseline.size,
            nIntervention = intervention.size
        )
    }
}
