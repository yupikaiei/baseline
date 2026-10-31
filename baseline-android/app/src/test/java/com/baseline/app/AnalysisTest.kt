package com.baseline.app

import com.baseline.app.data.Checkin
import com.baseline.app.data.Experiment
import com.baseline.app.data.Grade
import com.baseline.app.data.Metric
import com.baseline.app.logic.ExperimentEngine
import com.baseline.app.logic.Format
import com.baseline.app.logic.Insights
import com.baseline.app.logic.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class AnalysisTest {

    private val today: LocalDate = LocalDate.of(2026, 10, 30)

    /** 14 days baseline at 400 min, 21 days intervention at 470 min, then a bad week at 350. */
    private fun seededHistory(): List<Checkin> {
        val start = today.minusDays(40)
        return (0 until 40).map { i ->
            val d = start.plusDays(i.toLong())
            val sleep = when {
                i < 14 -> 400
                i < 35 -> 470
                else -> 350
            }
            Checkin(
                date = Format.isoDate(d),
                sleepMin = sleep,
                sleepQuality = if (sleep >= 460) 5 else 3,
                energy = 4,
                mood = 4,
                stress = if (sleep >= 460) 2 else 4
            )
        }
    }

    private fun experiment(daysAgo: Long, baseline: Int = 14, intervention: Int = 21, washout: Int = 7, repeat: Int = 0): Experiment {
        val startMillis = today.minusDays(daysAgo).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return Experiment(
            id = "e1",
            title = "Screen cutoff at 22:00",
            metric = Metric.SLEEP_MIN,
            expectedDirection = 1,
            confounders = listOf("alcohol"),
            baselineDays = baseline,
            interventionDays = intervention,
            washoutDays = washout,
            repeatDays = repeat,
            preregisteredAt = startMillis
        )
    }

    private fun nowMillis(): Long =
        today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + 43_200_000L

    @Test
    fun armsAreDerivedFromDates() {
        val a = ExperimentEngine.analyse(experiment(40), seededHistory(), nowMillis())
        assertEquals(14, a.baseline.size)
        assertEquals(21, a.intervention.size)
        assertTrue(a.washout.size >= 5)
    }

    @Test
    fun armMeansMatchTheDesign() {
        val a = ExperimentEngine.analyse(experiment(40), seededHistory(), nowMillis())
        assertTrue(a.baseline.all { it == 400.0 })
        assertTrue(a.intervention.all { it == 470.0 })
    }

    @Test
    fun seventyMinuteEffectIsReportedAsReal() {
        val a = ExperimentEngine.analyse(experiment(40), seededHistory(), nowMillis())
        val p = a.primary!!
        assertEquals(70.0, p.diff, 1e-9)
        assertTrue(p.enoughData)
        assertTrue(p.isLikelyReal)
        assertTrue(a.verdict.contains("likely real"))
        assertEquals("EFFECT LIKELY REAL", a.verdictTag)
    }

    @Test
    fun elapsedDaysNeverExceedDesign() {
        val e = experiment(40)
        val a = ExperimentEngine.analyse(e, seededHistory(), nowMillis())
        assertTrue(a.elapsedDays <= e.totalDays)
    }

    @Test
    fun halfFinishedExperimentIsNotEnoughData() {
        val a = ExperimentEngine.analyse(experiment(20), seededHistory(), nowMillis())
        assertTrue(a.intervention.size < 10)
        assertFalse(a.primary?.enoughData ?: false)
        assertTrue(a.verdict.lowercase().contains("not enough data"))
        assertEquals("IN PROGRESS", a.verdictTag)
    }

    @Test
    fun driftDetectedForBadWeek() {
        val snap = ExperimentEngine.snapshot(seededHistory(), Metric.SLEEP_MIN, today)
        assertEquals(40, snap.band90.n)
        assertTrue(snap.drift.detected)
        assertTrue(snap.drift.delta < 0)
    }

    @Test
    fun dailyCardLeadsWithDrift() {
        val card = Insights.dailyCard(seededHistory(), today)
        assertTrue(card.isDrift)
        assertEquals("Sleep", card.pillar)
        assertEquals(Grade.A, card.grade)
        assertEquals("sleep-7h", card.evidenceId)
    }

    @Test
    fun flatSeriesFallsBackToTip() {
        val flat = (0 until 30).map {
            Checkin(date = Format.isoDate(today.minusDays((29 - it).toLong())), sleepMin = 430)
        }
        val card = Insights.dailyCard(flat, today)
        assertFalse(card.isDrift)
    }

    @Test
    fun minutesFormatAsHoursAndMinutes() {
        assertEquals("6h40", Format.minutes(400))
        assertEquals("7h05", Format.minutes(425))
        assertEquals("—", Format.minutes(null as Int?))
    }

    @Test
    fun bandIsEmptyWithoutData() {
        assertTrue(Stats.band(emptyList()).isEmpty)
    }
}
