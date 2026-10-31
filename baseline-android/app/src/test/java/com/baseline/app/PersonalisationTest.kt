package com.baseline.app

import com.baseline.app.data.AgeBand
import com.baseline.app.data.Checkin
import com.baseline.app.data.Metric
import com.baseline.app.data.Problem
import com.baseline.app.data.Profile
import com.baseline.app.data.evidenceLibrary
import com.baseline.app.data.programs
import com.baseline.app.logic.ExperimentEngine
import com.baseline.app.logic.Format
import com.baseline.app.logic.Insights
import com.baseline.app.logic.Personalisation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AgeBandTest {

    @Test
    fun bandBoundariesAreExact() {
        assertEquals(AgeBand.UNDER_30, AgeBand.forAge(18))
        assertEquals(AgeBand.UNDER_30, AgeBand.forAge(29))
        assertEquals(AgeBand.THIRTIES, AgeBand.forAge(30))
        assertEquals(AgeBand.FORTIES, AgeBand.forAge(40))
        assertEquals(AgeBand.FIFTIES, AgeBand.forAge(50))
        assertEquals(AgeBand.SIXTY_PLUS, AgeBand.forAge(60))
        assertEquals(AgeBand.SIXTY_PLUS, AgeBand.forAge(90))
    }

    @Test
    fun everyBandHasConcreteFraming() {
        AgeBand.entries.forEach {
            assertTrue(it.label.isNotBlank())
            assertTrue("band ${it.label} needs a note", it.note.length > 40)
            assertTrue(it.minAge < it.maxAge)
        }
    }

    @Test
    fun bandRoundTripsThroughName() {
        AgeBand.entries.forEach { assertEquals(it, AgeBand.fromName(it.name)) }
        assertNull(AgeBand.fromName("NOPE"))
    }
}

class ProfileTest {

    @Test
    fun ageComesFromBirthYear() {
        val p = Profile(birthYear = 1986, now = 2026)
        assertEquals(40, p.age)
        assertEquals(AgeBand.FORTIES, p.band)
    }

    @Test
    fun ageIsClampedToSaneValues() {
        assertEquals(18, Profile(birthYear = 2026, now = 2026).age)
        assertEquals(120, Profile(birthYear = 1800, now = 2026).age)
    }

    @Test
    fun incompleteProfilesAreDetected() {
        assertFalse(Profile().isComplete)
        assertFalse(Profile(birthYear = 1990).isComplete)
        assertFalse(Profile(problems = setOf(Problem.SLEEP)).isComplete)
        assertTrue(Profile(birthYear = 1990, problems = setOf(Problem.SLEEP)).isComplete)
    }

    @Test
    fun problemsAreFilteredByAge() {
        val young = Profile(birthYear = 2003, now = 2026) // 23
        assertFalse(young.offeredProblems().contains(Problem.HEART))
        assertFalse(young.offeredProblems().contains(Problem.LIBIDO))
        assertTrue(young.offeredProblems().contains(Problem.SLEEP))

        val older = Profile(birthYear = 1976, now = 2026) // 50
        assertTrue(older.offeredProblems().contains(Problem.HEART))
        assertTrue(older.offeredProblems().contains(Problem.LIBIDO))
    }

    @Test
    fun settingsRoundTrip() {
        val p = Profile(birthYear = 1980, problems = setOf(Problem.SLEEP, Problem.ALCOHOL))
        val restored = Profile.fromSettings(p.toSettings()[Profile.KEY_BIRTH_YEAR], p.toSettings()[Profile.KEY_PROBLEMS])
        assertEquals(1980, restored.birthYear)
        assertEquals(setOf(Problem.SLEEP, Problem.ALCOHOL), restored.problems)
    }

    @Test
    fun unknownStoredProblemIsIgnoredNotCrashed() {
        val restored = Profile.fromSettings("1980", "SLEEP,NOT_A_PROBLEM,ALCOHOL")
        assertEquals(setOf(Problem.SLEEP, Problem.ALCOHOL), restored.problems)
    }
}

class PersonalisationIntegrityTest {

    @Test
    fun everyProblemIsBackedByRealEvidence() {
        Problem.entries.forEach { problem ->
            assertTrue("${problem.label} has no evidence", problem.evidenceIds.isNotEmpty())
            problem.evidenceIds.forEach { id ->
                assertNotNull(
                    "${problem.label} points at '$id', which is not in the library",
                    evidenceLibrary.firstOrNull { it.id == id }
                )
            }
        }
    }

    @Test
    fun everyRecommendedProgramExists() {
        Problem.entries.mapNotNull { it.programId }.forEach { id ->
            assertNotNull("program '$id' missing", programs.firstOrNull { it.id == id })
        }
    }

    @Test
    fun everyProblemHasCards() {
        Problem.entries.forEach {
            assertTrue("${it.label} has no cards", Personalisation.cardsFor(Profile(problems = setOf(it))).isNotEmpty())
        }
    }

    @Test
    fun cardEvidenceIdsResolve() {
        Problem.entries.forEach { p ->
            Personalisation.cardsFor(Profile(problems = setOf(p))).forEach { card ->
                card.evidenceId?.let { id ->
                    assertNotNull("card '${card.title}' cites missing '$id'", evidenceLibrary.firstOrNull { it.id == id })
                }
            }
        }
    }
}

class PersonalisationRankingTest {

    private val today = LocalDate.of(2026, 10, 31) // dayOfYear 304, 304 % 7 = 3 -> not a rotation day
    private val profile = Profile(birthYear = 1980, now = 2026, problems = setOf(Problem.SLEEP))

    private fun seededBadWeek(): List<Checkin> {
        val start = today.minusDays(40)
        return (0 until 40).map { i ->
            val d = start.plusDays(i.toLong())
            val sleep = if (i < 35) 440 else 320
            Checkin(date = Format.isoDate(d), sleepMin = sleep)
        }
    }

    @Test
    fun rotationDayIsOneInSeven() {
        val rotationDays = (1..28).map { today.plusDays(it.toLong()) }.filter { Personalisation.isRotationDay(it) }
        assertEquals(4, rotationDays.size)
    }

    @Test
    fun measuredDriftOutranksTheStatedProblem() {
        val card = Insights.dailyCard(checkins = seededBadWeek(), today = today, profile = profile)
        assertTrue("drift should win", card.isDrift)
        assertNotNull(card.reason)
        assertTrue(card.reason!!.contains("range"))
    }

    @Test
    fun activeProgramOutranksTheStatedProblem() {
        val step = programs.first { it.id == "sleep-reset" }.day(3)
        val card = Insights.dailyCard(
            checkins = emptyList(), today = today, programStep = step, programDoneToday = false, profile = profile
        )
        assertEquals("Program", card.pillar)
    }

    @Test
    fun statedProblemDrivesTheCardWhenNothingElseApplies() {
        val card = Insights.dailyCard(checkins = emptyList(), today = today, profile = profile)
        assertFalse(card.isDrift)
        assertTrue("expected a sleep card, got ${card.title}", card.pillar == "Sleep")
        assertNotNull(card.reason)
        assertTrue(card.reason!!.contains("You said"))
    }

    @Test
    fun rotationDayStillSurfacesSomethingUnasked() {
        val rotationDay = (1..7).map { today.plusDays(it.toLong()) }.first { Personalisation.isRotationDay(it) }
        val card = Insights.dailyCard(checkins = emptyList(), today = rotationDay, profile = profile)
        assertTrue("rotation should explain itself", card.reason?.contains("did not ask") == true)
    }

    @Test
    fun emptyProfileFallsBackToGeneralRotation() {
        val card = Insights.dailyCard(checkins = emptyList(), today = today, profile = Profile())
        assertFalse(card.isDrift)
        assertTrue(card.title.isNotBlank())
    }

    @Test
    fun programRecommendationMatchesTheProblem() {
        val alcohol = Profile(birthYear = 1980, now = 2026, problems = setOf(Problem.ALCOHOL))
        assertEquals("three-drink-week", Personalisation.programRecommendation(alcohol)?.program?.id)
        assertTrue(Personalisation.programRecommendation(alcohol)!!.reason.contains("alcohol"))
    }

    @Test
    fun recommendedProgramIsListedFirst() {
        val strength = Profile(birthYear = 1980, now = 2026, problems = setOf(Problem.STRENGTH))
        assertEquals("strength-base", Personalisation.programsFor(strength).first().id)
        assertEquals(programs.size, Personalisation.programsFor(strength).size)
    }

    @Test
    fun coachContextCarriesAgeAndProblems() {
        val ctx = Personalisation.coachContext(profile)
        assertTrue(ctx.contains("46"))
        assertTrue(ctx.contains("40–49"))
        assertTrue(ctx.contains("Sleep"))
        assertTrue(ctx.contains("contradicts"))
    }

    @Test
    fun coachContextIsSafeWithoutAProfile() {
        val ctx = Personalisation.coachContext(Profile())
        assertTrue(ctx.contains("No problems selected"))
    }

    @Test
    fun focusMetricsComeFromProblemsOnly() {
        val p = Profile(birthYear = 1980, now = 2026, problems = setOf(Problem.SLEEP, Problem.STRESS))
        assertEquals(setOf(Metric.SLEEP_MIN, Metric.STRESS), p.focusMetrics.toSet())
    }
}
