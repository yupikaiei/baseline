package com.baseline.app.data

/**
 * A guided 4-week block. Content is generated from four weekly themes plus a
 * seven-day action rotation, so the same actions repeat and become habit —
 * which is the evidence-based point, not a content shortcut.
 */
data class ProgramDay(
    val day: Int,
    val week: Int,
    val theme: String,
    val action: String,
    val ifThen: String
)

data class Program(
    val id: String,
    val title: String,
    val subtitle: String,
    val focus: Metric,
    val weeklyThemes: List<String>,
    val actions: List<String>,
    val ifThen: String
) {
    val totalDays: Int get() = weeklyThemes.size * 7

    val days: List<ProgramDay> get() = (1..totalDays).map { d ->
        val week = (d - 1) / 7
        ProgramDay(
            day = d,
            week = week + 1,
            theme = weeklyThemes[week],
            action = actions[(d - 1) % actions.size],
            ifThen = ifThen
        )
    }

    fun day(number: Int): ProgramDay = days[(number - 1).coerceIn(0, totalDays - 1)]
}

val programs: List<Program> = listOf(
    Program(
        id = "sleep-reset",
        title = "Sleep Reset",
        subtitle = "Four weeks to a fixed wake time and a 7-hour floor",
        focus = Metric.SLEEP_MIN,
        weeklyThemes = listOf(
            "Fix the wake time",
            "Protect the last hour",
            "Handle the 3am wake",
            "Lock it in"
        ),
        actions = listOf(
            "Get up at the same time you did yesterday, whatever happened.",
            "No caffeine after midday.",
            "Screens off 60 minutes before bed. Charger outside the bedroom.",
            "Cooler room, darker room. Note anything that woke you.",
            "No alcohol tonight.",
            "Write down tomorrow's first task so it stops running at night.",
            "Lights out at your target time. Log how it went."
        ),
        ifThen = "If it is 21:30 and I am still working, then I close the laptop and start winding down."
    ),
    Program(
        id = "three-drink-week",
        title = "The 3-Drink Week",
        subtitle = "Four weeks to fewer drinking days, not a different drink",
        focus = Metric.ALCOHOL,
        weeklyThemes = listOf(
            "Count it honestly",
            "Build alcohol-free days",
            "Replace the ritual",
            "Make it the default"
        ),
        actions = listOf(
            "Log every drink today. No judgement, just the number.",
            "Two alcohol-free nights this week. Pick them now.",
            "Alcohol-free tonight. Notice what you actually wanted at 19:00.",
            "Have a non-alcoholic alternative in the fridge before the evening starts.",
            "Three alcohol-free nights this week.",
            "Note what improved: sleep, morning energy, mood.",
            "Plan next week's alcohol-free nights before the week starts."
        ),
        ifThen = "If I get home and reach for the fridge out of habit, then I pour a cold non-alcoholic drink first."
    ),
    Program(
        id = "strength-base",
        title = "Strength Base",
        subtitle = "Four weeks to two sessions a week, which is the whole threshold",
        focus = Metric.TRAINING,
        weeklyThemes = listOf(
            "Show up twice",
            "Add a little load",
            "Protect the sessions",
            "Make it permanent"
        ),
        actions = listOf(
            "Book two 40-minute sessions in your calendar, like meetings.",
            "Session one: squat or leg press, push, pull. Three sets each.",
            "Session two: hinge, pull, carry. Three sets each.",
            "Add one set to your main lift.",
            "Session one. Nothing clever, just attendance.",
            "Session two. Log 'hard' if it was hard.",
            "Review: did you hit two sessions? If not, why not — that is the data."
        ),
        ifThen = "If I have not trained by Wednesday, then I do a 25-minute session that evening, minimum."
    ),
    Program(
        id = "stress-anger",
        title = "Stress & Anger",
        subtitle = "Four weeks of acting before the feeling, not after",
        focus = Metric.STRESS,
        weeklyThemes = listOf(
            "Notice the early signal",
            "Change the first ten minutes",
            "Rebuild the buffer",
            "Keep the ones that work"
        ),
        actions = listOf(
            "Rate stress evening and morning. Note the gap.",
            "One 20-minute walk without your phone.",
            "When you notice the jaw or the shoulders, name it out loud.",
            "Ten slow breaths before any conversation you are dreading.",
            "Get outside for daylight before 10:00.",
            "Message one person you have not spoken to in a month.",
            "Keep the two things that actually moved the number."
        ),
        ifThen = "If I notice my jaw tighten, then I take ten slow breaths before I say anything."
    ),
    Program(
        id = "move-more",
        title = "Move More",
        subtitle = "Four weeks to a step floor and two walks you keep",
        focus = Metric.ENERGY,
        weeklyThemes = listOf(
            "Establish the floor",
            "Make it automatic",
            "Add the second walk",
            "Hold the line"
        ),
        actions = listOf(
            "Walk 10 minutes after lunch.",
            "Walk 10 minutes after dinner.",
            "Take the stairs once today, deliberately.",
            "Park further away than you need to.",
            "Two 10-minute walks today.",
            "Walk somewhere you have not walked before.",
            "Check the week: did you hit the floor five days out of seven?"
        ),
        ifThen = "If I finish a meal and I am still at the table, then I get up and walk for ten minutes."
    )
)

fun programById(id: String?): Program? = programs.firstOrNull { it.id == id }
