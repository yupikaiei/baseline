package com.baseline.app

import com.baseline.app.data.Grade
import com.baseline.app.data.Metric
import com.baseline.app.data.Program
import com.baseline.app.data.Training
import com.baseline.app.data.programById
import com.baseline.app.data.programs
import com.baseline.app.logic.Coach
import com.baseline.app.logic.HealthMapping
import com.baseline.app.logic.Importers
import com.baseline.app.logic.ImportException
import com.baseline.app.logic.Streaks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ProgramsTest {

    @Test
    fun everyProgramIsFourWeeks() {
        assertEquals(5, programs.size)
        programs.forEach {
            assertEquals(28, it.totalDays)
            assertEquals(28, it.days.size)
        }
    }

    @Test
    fun dayNumberingIsOneBasedAndContiguous() {
        val p = programs.first()
        assertEquals(1, p.days.first().day)
        assertEquals(28, p.days.last().day)
        p.days.forEachIndexed { i, d -> assertEquals(i + 1, d.day) }
    }

    @Test
    fun weeksAdvanceEverySevenDays() {
        val p = programs.first()
        assertEquals(1, p.day(1).week)
        assertEquals(1, p.day(7).week)
        assertEquals(2, p.day(8).week)
        assertEquals(4, p.day(28).week)
    }

    @Test
    fun actionsRotateAcrossSevenDays() {
        val p = programs.first()
        val week1 = (1..7).map { p.day(it).action }
        val week2 = (8..14).map { p.day(it).action }
        assertEquals(week1, week2)
        assertEquals(7, week1.distinct().size)
    }

    @Test
    fun everyProgramHasAnIfThenPlan() {
        programs.forEach { assertTrue(it.ifThen.isNotBlank()) }
    }

    @Test
    fun lookupByIdWorksAndRejectsUnknown() {
        assertEquals("Sleep Reset", programById("sleep-reset")?.title)
        assertEquals(null, programById("nope"))
        assertEquals(null, programById(null))
    }

    @Test
    fun outOfRangeDayClamps() {
        val p = programs.first()
        assertEquals(1, p.day(0).day)
        assertEquals(28, p.day(99).day)
    }

    @Test
    fun programDaysCarryTheirTheme() {
        val p = Program(
            id = "x", title = "T", subtitle = "S", focus = Metric.SLEEP_MIN,
            weeklyThemes = listOf("A", "B"), actions = listOf("a1", "a2"), ifThen = "then"
        )
        assertEquals("A", p.day(1).theme)
        assertEquals("B", p.day(8).theme)
        assertEquals(14, p.totalDays)
    }
}

class CoachTest {

    @Test
    fun emergencyPhrasesAreCaught() {
        assertTrue(Coach.isEmergency("I have crushing chest pain right now"))
        assertTrue(Coach.isEmergency("I keep thinking I want to kill myself"))
        assertTrue(Coach.isEmergency("sudden vision loss in one eye"))
    }

    @Test
    fun ordinaryQuestionsAreNotEmergencies() {
        assertFalse(Coach.isEmergency("how much sleep do I need"))
    }

    @Test
    fun emergencyReplyPointsToRealHelp() {
        val r = Coach.emergencyReply()
        assertTrue(r.isEmergency)
        assertTrue(r.text.contains("emergency"))
    }

    @Test
    fun dosingQuestionsAreRefused() {
        assertTrue(Coach.isDosing("what dose of testosterone should i take"))
        assertTrue(Coach.isDosing("should i stop taking my tablets"))
        assertFalse(Coach.isDosing("is two strength sessions a week enough"))
    }

    @Test
    fun refusalDoesNotDiagnose() {
        val r = Coach.refusalReply()
        assertTrue(r.isRefusal)
        assertTrue(r.text.contains("can't diagnose"))
    }

    @Test
    fun retrievalFindsBoostersForTestosterone() {
        val hits = Coach.retrieve("do testosterone boosters work")
        assertTrue(hits.isNotEmpty())
        assertTrue(hits.any { it.id == "drive-boosters" })
        assertEquals(Grade.D, hits.first { it.id == "drive-boosters" }.grade)
    }

    @Test
    fun retrievalFindsSleepGuidance() {
        val hits = Coach.retrieve("how many hours of sleep do I need")
        assertTrue(hits.any { it.id == "sleep-7h" })
    }

    @Test
    fun retrievalIsEmptyForNonsense() {
        assertTrue(Coach.retrieve("zzzz qqqq").isEmpty())
    }

    @Test
    fun offlineAnswerAlwaysCitesACaveat() {
        val r = Coach.offlineAnswer("how many hours of sleep do I need")
        assertTrue(r.text.contains("What we don't know"))
        assertTrue(r.evidenceIds.isNotEmpty())
        assertEquals("offline", r.source)
    }

    @Test
    fun offlineAnswerNeverDiagnosesEmergency() {
        val r = Coach.offlineAnswer("chest pain")
        assertTrue(r.isEmergency)
    }

    @Test
    fun systemPromptCarriesTheLibrary() {
        val p = Coach.systemPrompt()
        assertTrue(p.contains("Never diagnose"))
        assertTrue(p.contains("EVIDENCE LIBRARY"))
        assertTrue(p.contains("testosterone booster", ignoreCase = true))
    }
}

class ImportTest {

    @Test
    fun parsesItsOwnExport() {
        val csv = """
            date,sleep_min,sleep_quality,energy,mood,stress,alcohol_units,training,libido,note,source
            2026-10-01,420,4,3,4,2,0.0,None,,,typed
            2026-10-02,390,3,2,3,3,1.5,Hard,4,late night,typed
        """.trimIndent()
        val rows = Importers.parseCsv(csv)
        assertEquals(2, rows.size)
        assertEquals(420, rows[0].sleepMin)
        assertEquals(1.5, rows[1].alcoholUnits, 1e-9)
        assertEquals(Training.HARD, rows[1].training)
        assertEquals(4, rows[1].libido)
        assertEquals("late night", rows[1].note)
    }

    @Test
    fun toleratesAlternativeColumnOrderAndMissingColumns() {
        val csv = """
            date,energy
            2026-10-03,4
        """.trimIndent()
        val rows = Importers.parseCsv(csv)
        assertEquals(1, rows.size)
        assertEquals(4, rows[0].energy)
        assertEquals(null, rows[0].sleepMin)
    }

    @Test
    fun acceptsSlashDates() {
        val csv = "date,sleep_min\n01/10/2026,420"
        assertEquals("2026-10-01", Importers.parseCsv(csv).first().date)
    }

    @Test
    fun handlesQuotedFieldsWithCommas() {
        val csv = "date,note\n2026-10-04,\"slept badly, woke twice\""
        assertEquals("slept badly, woke twice", Importers.parseCsv(csv).first().note)
    }

    @Test(expected = ImportException::class)
    fun rejectsFileWithoutDateColumn() {
        Importers.parseCsv("foo,bar\n1,2")
    }

    @Test(expected = ImportException::class)
    fun rejectsEmptyFile() {
        Importers.parseCsv("date,sleep_min")
    }

    @Test
    fun parsesJsonExport() {
        val json = """
            {"format":"baseline-v1","checkins":[
              {"date":"2026-10-01","sleep_min":420,"sleep_quality":4,"alcohol_units":0.0,"training":"None"},
              {"date":"2026-10-02","sleep_min":390,"sleep_quality":3,"alcohol_units":2.0,"training":"Hard","note":"x"}
            ],"experiments":[]}
        """.trimIndent()
        val rows = Importers.parseCheckinsJson(json)
        assertEquals(2, rows.size)
        assertEquals(390, rows[1].sleepMin)
        assertEquals(Training.HARD, rows[1].training)
        assertEquals("x", rows[1].note)
    }

    @Test(expected = ImportException::class)
    fun rejectsNonJson() {
        Importers.parseCheckinsJson("not json at all")
    }
}

class StreakTest {

    private fun d(s: String) = LocalDate.parse(s)

    @Test
    fun countsConsecutiveDaysEndingToday() {
        val dates = setOf("2026-10-28", "2026-10-29", "2026-10-30")
        assertEquals(3, Streaks.current(dates, d("2026-10-30")))
    }

    @Test
    fun unfinishedTodayDoesNotBreakTheRun() {
        val dates = setOf("2026-10-28", "2026-10-29")
        assertEquals(2, Streaks.current(dates, d("2026-10-30")))
    }

    @Test
    fun gapResetsTheRun() {
        val dates = setOf("2026-10-20", "2026-10-21", "2026-10-28", "2026-10-29")
        assertEquals(2, Streaks.current(dates, d("2026-10-30")))
    }

    @Test
    fun emptyHistoryIsZero() {
        assertEquals(0, Streaks.current(emptySet(), d("2026-10-30")))
        assertEquals(0, Streaks.best(emptySet()))
    }

    @Test
    fun bestRunSpansTheWholeHistory() {
        val dates = setOf("2026-10-01", "2026-10-02", "2026-10-03", "2026-10-10")
        assertEquals(3, Streaks.best(dates))
    }
}

class RetrievalRelevanceTest {
    @Test
    fun commonVerbsDoNotDragInUnrelatedItems() {
        val hits = Coach.retrieve("do testosterone boosters work")
        assertTrue(hits.first().id == "drive-boosters")
        assertFalse(hits.any { it.id == "mind-behavioural-activation" })
    }

    @Test
    fun alcoholQuestionFindsAlcoholGuidance() {
        val hits = Coach.retrieve("how much alcohol is safe")
        assertTrue(hits.any { it.pillar == "Fuel" })
    }

    @Test
    fun strengthQuestionFindsWhoGuideline() {
        val hits = Coach.retrieve("how many strength sessions per week")
        assertTrue(hits.any { it.id == "move-two-sessions" || it.id == "move-who-150" })
    }
}

class HealthMappingTest {

    @Test
    fun efficiencyBandsMapToQuality() {
        assertEquals(5, HealthMapping.sleepQualityFromEfficiency(95.0))
        assertEquals(5, HealthMapping.sleepQualityFromEfficiency(90.0))
        assertEquals(4, HealthMapping.sleepQualityFromEfficiency(89.9))
        assertEquals(4, HealthMapping.sleepQualityFromEfficiency(85.0))
        assertEquals(3, HealthMapping.sleepQualityFromEfficiency(84.9))
        assertEquals(3, HealthMapping.sleepQualityFromEfficiency(75.0))
        assertEquals(2, HealthMapping.sleepQualityFromEfficiency(74.9))
        assertEquals(2, HealthMapping.sleepQualityFromEfficiency(65.0))
        assertEquals(1, HealthMapping.sleepQualityFromEfficiency(64.9))
    }

    @Test
    fun nonsenseEfficiencyYieldsNoQuality() {
        assertEquals(null, HealthMapping.sleepQualityFromEfficiency(0.0))
        assertEquals(null, HealthMapping.sleepQualityFromEfficiency(-5.0))
        assertEquals(null, HealthMapping.sleepQualityFromEfficiency(Double.NaN))
    }

    @Test
    fun efficiencyIsGuardedAgainstBadInput() {
        assertEquals(null, HealthMapping.efficiencyPercent(0, 0))
        assertEquals(null, HealthMapping.efficiencyPercent(-1, 0))
        assertEquals(90.0, HealthMapping.efficiencyPercent(1000, 100)!!, 1e-9)
    }

    @Test
    fun awakeLongerThanSessionIsClamped() {
        // A corrupt record must not produce a negative or >100% efficiency.
        assertEquals(0.0, HealthMapping.efficiencyPercent(1000, 5000)!!, 1e-9)
    }

    @Test
    fun trainingReflectsSessionLength() {
        assertEquals(null, HealthMapping.trainingFromMinutes(0))
        assertEquals(null, HealthMapping.trainingFromMinutes(-10))
        assertEquals(Training.LIGHT, HealthMapping.trainingFromMinutes(1))
        assertEquals(Training.LIGHT, HealthMapping.trainingFromMinutes(44))
        assertEquals(Training.HARD, HealthMapping.trainingFromMinutes(45))
        assertEquals(Training.HARD, HealthMapping.trainingFromMinutes(120))
    }
}
