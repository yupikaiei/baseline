package com.baseline.app.logic

import com.baseline.app.data.AgeBand
import com.baseline.app.data.Checkin
import com.baseline.app.data.Grade
import com.baseline.app.data.Metric
import com.baseline.app.data.Problem
import com.baseline.app.data.Profile
import com.baseline.app.data.Program
import com.baseline.app.data.programs
import java.time.LocalDate

data class Recommendation(val program: Program, val reason: String)

/**
 * Turns a profile into ordering and content. The rule that governs everything
 * here: measured data outranks what the user says about themselves.
 *
 *   1. drift      - your own logs contradict your theory
 *   2. program     - you committed to this block
 *   3. problem     - what you selected (cold start, and the default)
 *   4. rotation    - one day in seven, so nothing is starved
 */
object Personalisation {

    /** Problem-specific cards, keyed by the problem they answer. */
    private val problemCards: Map<Problem, List<DailyCard>> = mapOf(
        Problem.SLEEP to listOf(
            DailyCard(
                "Sleep", "Seven hours is the floor, not a target to beat",
                "The AASM and Sleep Research Society consensus is 7+ hours on a regular basis. " +
                    "Sleeping less most nights is associated with worse cardiometabolic and cognitive " +
                    "outcomes — and it makes every other metric you are tracking noisier.",
                "Set a lights-out alarm for tonight, not just a wake alarm.",
                Grade.A, "sleep-7h"
            ),
            DailyCard(
                "Sleep", "Alcohol helps you fall asleep, then breaks the sleep",
                "It shortens sleep onset and then fragments the night. If you drink most evenings, " +
                    "this is usually the largest single improvement available to you.",
                "Two alcohol-free nights this week. Keep logging sleep as usual.",
                Grade.B, "sleep-alcohol"
            )
        ),
        Problem.ENERGY to listOf(
            DailyCard(
                "Move", "Fitness behaves like a vital sign",
                "Cardiorespiratory fitness predicts long-term mortality independently of other risk " +
                    "factors, and it is the metric most improved by the least intimidating change: " +
                    "walking more, more often.",
                "Two 10-minute walks today, after meals if that is easier to remember.",
                Grade.B, "move-fitness-mortality"
            ),
            DailyCard(
                "Sleep", "Persistent tiredness is usually a sleep problem first",
                "Before assuming it is age or workload, check the 7-hour floor. Short sleep on repeat " +
                    "nights affects mood, training quality and appetite regulation.",
                "Compare your last 7 nights against your 90-day range on the Baseline tab.",
                Grade.A, "sleep-7h"
            )
        ),
        Problem.STRESS to listOf(
            DailyCard(
                "Mind", "Act first, mood follows",
                "Behavioural activation — doing valued activity regardless of how you feel — is an " +
                    "effective treatment for low mood and one of the better-evidenced psychological " +
                    "approaches available.",
                "Schedule one thing you would enjoy, before you feel like it.",
                Grade.A, "mind-behavioural-activation"
            ),
            DailyCard(
                "Mind", "Irritability is a symptom, not a personality",
                "Anger and short temper are common presentations of low mood in men, and get missed " +
                    "by screening that only looks for sadness.",
                "Rate stress tonight and tomorrow morning. Note the gap.",
                Grade.B, "mind-anger-depression"
            )
        ),
        Problem.ALCOHOL to listOf(
            DailyCard(
                "Fuel", "Risk rises with dose, with no established safe threshold",
                "The \"a glass of red is good for you\" story has not held up. The practical target is " +
                    "fewer drinking days per week, not a different drink.",
                "Pick two alcohol-free nights this week and put them in your calendar now.",
                Grade.B, "fuel-alcohol"
            ),
            DailyCard(
                "Sleep", "The sleep cost shows up before anything else",
                "Alcohol fragments sleep even when it shortens the time it takes to fall asleep. " +
                    "Your sleep duration is the fastest-responding measure you have.",
                "Log units honestly tonight, including the ones you would round down.",
                Grade.B, "sleep-alcohol"
            )
        ),
        Problem.STRENGTH to listOf(
            DailyCard(
                "Move", "Two sessions a week is the whole threshold",
                "That is the WHO guidance, and after 35 it is the part most people skip. Muscle and " +
                    "strength decline with age; resistance training is the most direct lever on both.",
                "Book two 40-minute sessions into this week, like meetings.",
                Grade.A, "move-two-sessions"
            ),
            DailyCard(
                "Move", "Strength is a healthspan metric",
                "Grip strength is prognostic for all-cause mortality across 21 countries. This is not " +
                    "a vanity project — it is one of the few things that reliably tracks how you will age.",
                "Add one set to your main lift today. Nothing else changes.",
                Grade.B, "move-grip"
            )
        ),
        Problem.WEIGHT to listOf(
            DailyCard(
                "Fuel", "A pattern, not a diet",
                "PREDIMED randomised 7,447 people and found fewer major cardiovascular events on a " +
                    "Mediterranean-style pattern. Vegetables, legumes, fish, olive oil, nuts, less " +
                    "processed food — a shape of eating you can hold for years.",
                "One meal today: add a vegetable, subtract a processed item.",
                Grade.A, "fuel-mediterranean"
            ),
            DailyCard(
                "Fuel", "Protein matters most alongside training",
                "Protein without training does little for muscle. Training without adequate protein " +
                    "wastes some of the work. The two together are the point.",
                "Put a protein source in your next two meals.",
                Grade.B, "fuel-protein"
            )
        ),
        Problem.HEART to listOf(
            DailyCard(
                "Move", "The fitness conversation is the heart conversation",
                "Cardiorespiratory fitness predicts long-term mortality independently of other risk " +
                    "factors. It is also the thing most improved by walking more, more often.",
                "Two 10-minute walks today. Log training honestly.",
                Grade.B, "move-fitness-mortality"
            ),
            DailyCard(
                "Move", "150 to 300 minutes, plus two strength sessions",
                "That is the WHO 2020 guidance for adults. The strength clause is the part most people " +
                    "skip, and the part that matters most after 35.",
                "Plan the week's activity now rather than deciding each morning.",
                Grade.A, "move-who-150"
            )
        ),
        Problem.MOOD to listOf(
            DailyCard(
                "Mind", "Doing beats waiting to feel ready",
                "Behavioural activation is one of the better-evidenced treatments for low mood: " +
                    "schedule the activity, then let mood catch up.",
                "Pick one thing for tomorrow and put a time on it.",
                Grade.A, "mind-behavioural-activation"
            ),
            DailyCard(
                "Mind", "Isolation is a physical risk factor",
                "Loneliness and social isolation are associated with higher all-cause mortality. " +
                    "This is not a soft extra — it behaves like a risk factor.",
                "Message one person you have not spoken to in a month.",
                Grade.B, "mind-loneliness"
            )
        ),
        Problem.LIBIDO to listOf(
            DailyCard(
                "Drive", "It is usually a blood-vessel story",
                "Erectile function is commonly vascular in origin and associated with cardiovascular " +
                    "risk factors. That makes sleep, blood pressure, alcohol and fitness modifiable inputs " +
                    "rather than background noise.",
                "Log libido today. If it is persistently low, note it for your doctor.",
                Grade.B, "drive-vascular"
            ),
            DailyCard(
                "Drive", "The things that do not work are the ones being sold",
                "\"Testosterone boosters\" are a large, heavily marketed category with no consistent " +
                    "trial support. The three things with real evidence are sleep, resistance training " +
                    "and fat loss.",
                "Skip the supplement. Book two strength sessions instead.",
                Grade.D, "drive-boosters"
            )
        ),
        Problem.LONELY to listOf(
            DailyCard(
                "Connect", "Friendship needs scheduling, not hoping",
                "Male friendship networks narrow sharply in the 30s and 40s. The intervention with any " +
                    "support is deliberate, repeated contact.",
                "Send one message today to someone you have not spoken to in a month.",
                Grade.C, "connect-men-friends"
            ),
            DailyCard(
                "Mind", "Isolation behaves like a risk factor",
                "Loneliness and social isolation are associated with increased all-cause mortality, " +
                    "with effect sizes comparable to established risk factors.",
                "Put one recurring thing in the calendar with another person.",
                Grade.B, "mind-loneliness"
            )
        )
    )

    fun cardsFor(profile: Profile): List<DailyCard> =
        profile.problems.flatMap { problemCards[it].orEmpty() }

    /**
     * One day in seven is reserved for the general rotation, whichever problems
     * are selected. Without this the app only ever reflects what you already
     * believe about yourself.
     */
    fun isRotationDay(today: LocalDate): Boolean = today.dayOfYear % 7 == 0

    fun programRecommendation(profile: Profile): Recommendation? {
        val match = profile.problems.firstNotNullOfOrNull { p ->
            p.programId?.let { id -> programs.firstOrNull { it.id == id } }
        }
        return match?.let {
            Recommendation(it, "You said: ${profile.problems.first().label.lowercase()}")
        } ?: programs.firstOrNull()?.let { Recommendation(it, "Start with sleep — it moves everything else") }
    }

    fun programsFor(profile: Profile): List<Program> {
        val recommended = programRecommendation(profile)?.program
        val rest = programs.filter { it != recommended }
        return listOfNotNull(recommended) + rest
    }

    /**
     * A short line explaining why today's card appeared. The date is passed in
     * rather than read from the clock so the reason always matches the card.
     */
    fun reasonFor(card: DailyCard, profile: Profile, today: LocalDate = LocalDate.now()): String? = when {
        card.isDrift -> "Because your last 7 days moved outside your own range"
        card.pillar == "Program" -> "Today's step in your program"
        isRotationDay(today) -> "Rotation — so you see things you did not ask about"
        else -> profile.problems
            .firstOrNull { p -> problemCards[p]?.contains(card) == true }
            ?.let { "You said: ${it.label}" }
    }

    /** Age framing shown on Today and in the coach prompt. */
    fun ageContext(profile: Profile): String? {
        val band = profile.band ?: return null
        return "You are in the ${band.label} band. ${band.note}"
    }

    fun coachContext(profile: Profile): String = buildString {
        appendLine("USER CONTEXT (use it to prioritise, never to change the evidence):")
        profile.age?.let { appendLine("- Age: $it (${profile.band?.label})") }
        profile.band?.let { appendLine("- Age band note: ${it.note}") }
        if (profile.problems.isNotEmpty()) {
            appendLine("- Self-reported problems: ${profile.problems.joinToString(", ") { it.label }}")
            appendLine("- Rank these above general topics, but if the user's tracked data contradicts " +
                "their stated problem, say so plainly and point at the data.")
        }
        if (profile.problems.isEmpty()) appendLine("- No problems selected yet.")
    }
}
