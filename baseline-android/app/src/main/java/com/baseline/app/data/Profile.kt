package com.baseline.app.data

/**
 * Age bands rather than an exact age. The app only needs the band to decide
 * relevance, and a band is both less sensitive to store and less false-precise
 * than a birthday that has to be right.
 *
 * Each band's note is drawn from the leading-causes data in the PRD (NVSR 73(10),
 * male deaths 2022) — it says what actually kills men at that age, not what a
 * marketing deck would like to say.
 */
enum class AgeBand(
    val label: String,
    val minAge: Int,
    val maxAge: Int,
    val note: String
) {
    UNDER_30(
        "18–29", 18, 29,
        "For men in their twenties the leading causes of death are unintentional injury " +
            "and suicide, not heart disease. Sleep, alcohol and mood are the levers with " +
            "decades of runway left."
    ),
    THIRTIES(
        "30–39", 30, 39,
        "Heart disease enters the male top three between 35 and 44. This is the decade " +
            "where the numbers start drifting while you still feel fine."
    ),
    FORTIES(
        "40–49", 40, 49,
        "Obesity prevalence peaks in the 40s and 50s, and chronic liver disease enters the " +
            "male top ten from 45. Strength and alcohol are the two levers with the most headroom."
    ),
    FIFTIES(
        "50–59", 50, 59,
        "Heart disease is the leading cause of death for men from the mid-50s, with cancer " +
            "close behind. Blood pressure and fitness stop being optional."
    ),
    SIXTY_PLUS(
        "60+", 60, 120,
        "Heart disease and cancer dominate, and the median age of prostate cancer diagnosis " +
            "is 68. Strength preservation and screening cadence matter more than intensity."
    );

    companion object {
        fun forAge(age: Int): AgeBand = when {
            age < 30 -> UNDER_30
            age < 40 -> THIRTIES
            age < 50 -> FORTIES
            age < 60 -> FIFTIES
            else -> SIXTY_PLUS
        }

        fun fromName(name: String?): AgeBand? = entries.firstOrNull { it.name == name }
    }
}

/**
 * A problem the user says they have. Every entry maps to evidence already in the
 * library — a selectable problem with no supporting item would be fake
 * personalisation, so anything the library cannot speak to is deliberately absent.
 */
enum class Problem(
    val label: String,
    val question: String,
    val focus: Metric,
    val evidenceIds: List<String>,
    val programId: String?,
    val minAge: Int = 18
) {
    SLEEP(
        "Sleep", "Sleep is broken or too short",
        Metric.SLEEP_MIN, listOf("sleep-7h", "sleep-alcohol"), "sleep-reset"
    ),
    ENERGY(
        "Energy", "Tired all the time",
        Metric.ENERGY, listOf("sleep-7h", "move-fitness-mortality"), "move-more"
    ),
    STRESS(
        "Stress & anger", "Wound up, short-tempered, can't switch off",
        Metric.STRESS, listOf("mind-anger-depression", "mind-behavioural-activation"), "stress-anger"
    ),
    ALCOHOL(
        "Alcohol", "Drinking more than I want to",
        Metric.ALCOHOL, listOf("fuel-alcohol", "sleep-alcohol"), "three-drink-week"
    ),
    STRENGTH(
        "Strength", "Lost strength, or want to build it",
        Metric.TRAINING, listOf("move-two-sessions", "move-grip", "fuel-protein"), "strength-base"
    ),
    WEIGHT(
        "Weight", "Carrying more weight than I want",
        Metric.TRAINING, listOf("fuel-mediterranean", "fuel-protein", "move-who-150"), "move-more"
    ),
    HEART(
        "Heart & blood pressure", "Worried about heart risk or blood pressure",
        Metric.TRAINING, listOf("move-fitness-mortality", "fuel-mediterranean", "move-who-150"), "move-more",
        minAge = 30
    ),
    MOOD(
        "Low mood", "Flat, unmotivated, not myself",
        Metric.MOOD, listOf("mind-behavioural-activation", "mind-anger-depression"), "stress-anger"
    ),
    LIBIDO(
        "Libido", "Lower sex drive, or erection problems",
        Metric.LIBIDO, listOf("drive-vascular", "drive-boosters", "sleep-7h"), null, minAge = 25
    ),
    LONELY(
        "Isolation", "Lost touch with friends, spend too much time alone",
        Metric.MOOD, listOf("mind-loneliness", "connect-men-friends"), null
    );

    companion object {
        fun fromName(name: String?): Problem? = entries.firstOrNull { it.name == name }
    }
}

/** What the app knows about the person using it. Everything is optional and editable. */
data class Profile(
    val birthYear: Int? = null,
    val problems: Set<Problem> = emptySet(),
    val now: Int = java.time.Year.now().value
) {
    val age: Int? get() = birthYear?.let { (now - it).coerceIn(18, 120) }

    val band: AgeBand? get() = age?.let { AgeBand.forAge(it) }

    val isComplete: Boolean get() = birthYear != null && problems.isNotEmpty()

    /** Problems worth offering at this age — a 20-year-old is not shown "heart & blood pressure". */
    fun offeredProblems(): List<Problem> =
        Problem.entries.filter { (age ?: 18) >= it.minAge }

    val focusMetrics: List<Metric> get() = problems.map { it.focus }.distinct()

    fun toSettings(): Map<String, String> = buildMap {
        birthYear?.let { put(KEY_BIRTH_YEAR, it.toString()) }
        put(KEY_PROBLEMS, problems.joinToString(",") { p -> p.name })
    }

    companion object {
        const val KEY_BIRTH_YEAR = "profile_birth_year"
        const val KEY_PROBLEMS = "profile_problems"

        fun fromSettings(birthYear: String?, problems: String?): Profile = Profile(
            birthYear = birthYear?.toIntOrNull(),
            problems = (problems ?: "")
                .split(",")
                .mapNotNull { Problem.fromName(it.trim()) }
                .toSet()
        )
    }
}
