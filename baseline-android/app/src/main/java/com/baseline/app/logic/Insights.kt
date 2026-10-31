package com.baseline.app.logic

import com.baseline.app.data.Checkin
import com.baseline.app.data.Grade
import com.baseline.app.data.Metric
import com.baseline.app.data.Profile
import com.baseline.app.data.ProgramDay
import java.time.LocalDate

data class DailyCard(
    val pillar: String,
    val title: String,
    val body: String,
    val action: String,
    val grade: Grade,
    val evidenceId: String? = null,
    val isDrift: Boolean = false,
    /** Why this card appeared — shown to the user so the choice is never mysterious. */
    val reason: String? = null
)

object Insights {

    val PILLARS = listOf(
        Metric.SLEEP_MIN, Metric.ENERGY, Metric.MOOD,
        Metric.STRESS, Metric.ALCOHOL, Metric.TRAINING
    )

    /**
     * Order of precedence: drift (the reason to open the app), then today's
     * program step if one is active and not yet done, then a rotation.
     */
    fun dailyCard(
        checkins: List<Checkin>,
        today: LocalDate = LocalDate.now(),
        programStep: ProgramDay? = null,
        programDoneToday: Boolean = false,
        profile: Profile = Profile()
    ): DailyCard {
        for (m in PILLARS) {
            val snap = ExperimentEngine.snapshot(checkins, m, today)
            if (!snap.drift.detected) continue
            val up = snap.drift.delta > 0
            val worse = (up && !m.higherIsBetter) || (!up && m.higherIsBetter)
            if (worse) {
                return driftCard(m, snap).copy(
                    reason = Personalisation.reasonFor(driftCard(m, snap), profile, today)
                )
            }
        }
        if (programStep != null && !programDoneToday) {
            return DailyCard(
                pillar = "Program",
                title = "Week ${programStep.week} — ${programStep.theme}",
                body = programStep.action,
                action = programStep.ifThen,
                grade = Grade.A,
                evidenceId = null,
                reason = Personalisation.reasonFor(
                    DailyCard("Program", "", "", "", Grade.A), profile, today
                )
            )
        }
        // Problem tier: personalised, but one day in seven belongs to the general
        // rotation so the app cannot become an echo of what the user already thinks.
        if (profile.problems.isNotEmpty() && !Personalisation.isRotationDay(today)) {
            val cards = Personalisation.cardsFor(profile)
            if (cards.isNotEmpty()) {
                val card = cards[today.dayOfYear % cards.size]
                return card.copy(reason = Personalisation.reasonFor(card, profile, today))
            }
        }
        val idx = today.dayOfYear % BASE_CARDS.size
        return BASE_CARDS[idx].copy(reason = Personalisation.reasonFor(BASE_CARDS[idx], profile, today))
    }


    private fun driftCard(m: Metric, snap: ExperimentEngine.Snapshot): DailyCard {
        val delta = Format.value(m, Math.abs(snap.drift.delta))
        val range = "${Format.value(m, snap.band90.low)}–${Format.value(m, snap.band90.high)}"
        val recent = Format.value(m, snap.drift.recentMean)
        return when (m) {
            Metric.SLEEP_MIN -> DailyCard(
                "Sleep", "Sleep is outside your normal",
                "Last 7 days average $recent against your own 90-day range of $range. " +
                    "Under seven hours on repeat nights shifts mood and training quality, and makes " +
                    "any \"how am I doing?\" judgement unreliable.",
                "Lights out 30 minutes earlier tonight. We re-check in 7 days.",
                Grade.A, "sleep-7h", isDrift = true
            )
            Metric.ALCOHOL -> DailyCard(
                "Fuel", "Drinking has crept up",
                "Last 7 days average $delta per night, above your own range of $range. " +
                    "Alcohol helps you fall asleep and then fragments the sleep itself.",
                "Two alcohol-free nights this week. Nothing else changes.",
                Grade.B, "sleep-alcohol", isDrift = true
            )
            Metric.STRESS -> DailyCard(
                "Mind", "Stress is running high",
                "Last 7 days average $recent against your own range of $range. Sustained stress " +
                    "shows up as irritability and worse sleep long before you would call it a problem.",
                "One 20-minute walk without your phone today.",
                Grade.B, "mind-anger-depression", isDrift = true
            )
            Metric.TRAINING -> DailyCard(
                "Move", "Training has dropped off",
                "You have trained less than your own normal over the last 7 days.",
                "Two strength sessions this week is the threshold that matters.",
                Grade.A, "move-two-sessions", isDrift = true
            )
            Metric.MOOD -> DailyCard(
                "Mind", "Mood is below your normal",
                "Last 7 days average $recent against your own range of $range. Act first, mood " +
                    "follows — that is the best-evidenced approach for low mood.",
                "Schedule one thing you would enjoy, before you feel like it.",
                Grade.A, "mind-behavioural-activation", isDrift = true
            )
            else -> DailyCard(
                m.pillar, "${m.label} is below your normal",
                "Last 7 days average $recent against your own range of $range.",
                "Note anything unusual today, then re-check in a week.",
                Grade.C, null, isDrift = true
            )
        }
    }

    private val BASE_CARDS = listOf(
        DailyCard(
            "Move", "Two strength sessions a week",
            "That is the WHO threshold, and after 35 it is the part most people skip. Muscle and " +
                "strength decline with age; resistance training is the most direct lever on both.",
            "Book two 40-minute sessions into this week now.",
            Grade.A, "move-two-sessions"
        ),
        DailyCard(
            "Sleep", "Seven hours is the floor, not the target",
            "The AASM and Sleep Research Society consensus is 7+ hours on a regular basis. Sleeping " +
                "less most nights is associated with worse cardiometabolic and cognitive outcomes.",
            "Set a lights-out alarm, not just a wake alarm.",
            Grade.A, "sleep-7h"
        ),
        DailyCard(
            "Mind", "Anger is a symptom worth logging",
            "Irritability and anger are common presentations of low mood in men, and get missed by " +
                "screening that only looks for sadness.",
            "If you have been shorter with people lately, rate this week honestly.",
            Grade.B, "mind-anger-depression"
        ),
        DailyCard(
            "Myths", "\"Testosterone boosters\" are Grade D",
            "A large, heavily marketed category with no consistent trial support. The three things " +
                "with real evidence: sleep, resistance training, fat loss.",
            "Read why. We sell none of them.",
            Grade.D, "drive-boosters"
        ),
        DailyCard(
            "Connect", "Friendship needs scheduling",
            "Loneliness and social isolation are associated with higher all-cause mortality. Male " +
                "friendship networks narrow sharply in the 30s and 40s.",
            "Send one message today to someone you have not spoken to in a month.",
            Grade.B, "mind-loneliness"
        ),
        DailyCard(
            "Fuel", "A pattern, not a diet",
            "PREDIMED randomised 7,447 people and found fewer major cardiovascular events on a " +
                "Mediterranean-style pattern. Vegetables, legumes, fish, olive oil, nuts, less processed food.",
            "One meal today: add a vegetable, subtract a processed item.",
            Grade.A, "fuel-mediterranean"
        ),
        DailyCard(
            "Move", "Fitness behaves like a vital sign",
            "Cardiorespiratory fitness predicts long-term mortality independently of other risk " +
                "factors. It is also the metric most improved by the least intimidating change: walking more.",
            "Two 10-minute walks today.",
            Grade.B, "move-fitness-mortality"
        ),
        DailyCard(
            "Myths", "Cold plunges are Grade C",
            "It will wake you up. Claims about immunity, fat loss and dopamine outrun the evidence " +
                "by a wide margin.",
            "Keep it if you enjoy it. Do not pay for it as treatment.",
            Grade.C, "myth-cold-plunge"
        )
    )
}
