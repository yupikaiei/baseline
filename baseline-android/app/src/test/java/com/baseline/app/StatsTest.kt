package com.baseline.app

import com.baseline.app.logic.StatResult
import com.baseline.app.logic.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsTest {

    @Test
    fun meanAveragesValues() {
        assertEquals(4.0, Stats.mean(listOf(2.0, 4.0, 6.0)), 1e-9)
        assertEquals(0.0, Stats.mean(emptyList()), 1e-9)
    }

    @Test
    fun emptySeriesYieldsEmptyBand() {
        assertTrue(Stats.band(emptyList()).isEmpty)
    }

    @Test
    fun bandStaysInsideObservedValues() {
        val b = Stats.band(listOf(400.0, 410.0, 420.0, 430.0, 440.0, 450.0, 460.0, 470.0))
        assertTrue(b.low >= 400.0)
        assertTrue(b.high <= 470.0)
        assertEquals(435.0, b.mean, 1e-6)
    }

    @Test
    fun smallSamplesCollapseToMean() {
        val b = Stats.band(listOf(420.0, 430.0, 440.0))
        assertEquals(b.mean, b.low, 1e-9)
        assertEquals(b.mean, b.high, 1e-9)
    }

    @Test
    fun driftDetectsChangeAgainstConstantBaseline() {
        val baseline = Stats.band(List(30) { 420.0 })
        val drift = Stats.drift(listOf(300.0, 310.0, 300.0, 305.0, 300.0), baseline)
        assertTrue(drift.detected)
        assertEquals(-117.0, drift.delta, 1e-9)
    }

    @Test
    fun driftDetectsDropOutsideRealisticBand() {
        val baseline = Stats.band(listOf(420.0, 430.0, 440.0, 450.0, 460.0, 470.0, 480.0))
        assertTrue(Stats.drift(listOf(300.0, 305.0, 310.0, 300.0, 302.0), baseline).detected)
    }

    @Test
    fun driftIgnoresChangeInsideBand() {
        val baseline = Stats.band(listOf(420.0, 430.0, 440.0, 450.0, 460.0, 470.0, 480.0))
        assertFalse(Stats.drift(listOf(440.0, 445.0, 450.0), baseline).detected)
    }

    @Test
    fun driftNeedsFourRecentPoints() {
        val baseline = Stats.band(listOf(420.0, 430.0, 440.0, 450.0, 460.0, 470.0, 480.0))
        assertFalse(Stats.drift(listOf(300.0, 300.0, 300.0), baseline).detected)
    }

    @Test
    fun driftNeedsSevenDayBaseline() {
        val baseline = Stats.band(listOf(420.0, 430.0, 440.0))
        assertFalse(Stats.drift(listOf(300.0, 300.0, 300.0, 300.0), baseline).detected)
    }

    @Test
    fun bootstrapIsDeterministic() {
        val a = listOf(10.0, 12.0, 11.0, 13.0, 14.0, 10.0, 12.0)
        val b = listOf(20.0, 22.0, 21.0, 23.0, 24.0, 20.0, 22.0)
        val ci1 = Stats.bootstrapCi(a, b)
        val ci2 = Stats.bootstrapCi(a, b)
        assertEquals(ci1[0], ci2[0], 1e-12)
        assertEquals(ci1[1], ci2[1], 1e-12)
    }

    @Test
    fun cleanSeparationExcludesZero() {
        val intervention = listOf(20.0, 22.0, 21.0, 23.0, 24.0, 20.0, 22.0)
        val baseline = listOf(10.0, 12.0, 11.0, 13.0, 14.0, 10.0, 12.0)
        val ci = Stats.bootstrapCi(intervention, baseline)
        assertTrue(ci[0] > 0)
        assertTrue(ci[1] > ci[0])
    }

    @Test
    fun identicalArmsStraddleZero() {
        val a = listOf(10.0, 12.0, 11.0, 13.0, 14.0, 10.0, 12.0)
        val ci = Stats.bootstrapCi(a, a)
        assertTrue(ci[0] <= 0.0)
        assertTrue(ci[1] >= 0.0)
    }

    @Test
    fun emptyInputIsSafe() {
        val ci = Stats.bootstrapCi(emptyList(), listOf(1.0, 2.0))
        assertEquals(0.0, ci[0], 1e-12)
        assertEquals(0.0, ci[1], 1e-12)
    }

    @Test
    fun permutationSmallPForSeparatedArms() {
        val a = listOf(20.0, 21.0, 22.0, 23.0, 24.0, 25.0, 26.0)
        val b = listOf(10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0)
        assertTrue(Stats.permutationP(a, b) < 0.05)
    }

    @Test
    fun permutationLargePForOverlappingArms() {
        val a = listOf(10.0, 12.0, 11.0, 13.0, 14.0, 10.0, 12.0)
        val b = listOf(11.0, 10.0, 13.0, 12.0, 14.0, 11.0, 10.0)
        assertTrue(Stats.permutationP(a, b) > 0.2)
    }

    @Test
    fun permutationNearOneForIdenticalArms() {
        val a = listOf(10.0, 12.0, 11.0, 13.0, 14.0, 10.0, 12.0)
        assertTrue(Stats.permutationP(a, a) > 0.9)
    }

    @Test
    fun compareCallsRealEffectReal() {
        val baseline = List(14) { 400.0 + (it % 5) * 8 }
        val intervention = List(21) { 440.0 + (it % 5) * 8 }
        val r = Stats.compare(intervention, baseline)
        assertTrue(r.diff > 0)
        assertTrue(r.enoughData)
        assertTrue(r.isLikelyReal)
    }

    @Test
    fun compareFlagsInsufficientData() {
        val r = Stats.compare(listOf(440.0, 450.0, 460.0), listOf(400.0, 410.0, 420.0))
        assertFalse(r.enoughData)
        assertFalse(r.isLikelyReal)
        assertEquals(10, StatResult.MIN_PER_ARM)
    }

    @Test
    fun noisyDifferenceIsNotCalledReal() {
        val baseline = List(15) { 400.0 + (it % 2) * 90 }
        val intervention = List(15) { 405.0 + (it % 2) * 90 }
        assertFalse(Stats.compare(intervention, baseline).isLikelyReal)
    }
}
