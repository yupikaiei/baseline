package com.baseline.app.data

/**
 * The launch library. Every item carries a grade, a citation and a required
 * "what we don't know" field. Items that could not be traced to a primary source
 * were excluded rather than softened.
 */
val evidenceLibrary: List<EvidenceItem> = listOf(

    // ---------------- MOVE ----------------
    EvidenceItem(
        "move-who-150", "Move", "150–300 minutes a week, plus two strength sessions", Grade.A,
        "Adults should do 150–300 min of moderate activity (or 75–150 min vigorous) per week, " +
            "plus muscle-strengthening activity on at least two days.",
        "This is the WHO 2020 guideline. The strength clause is the part most people skip, and it " +
            "is the part that matters most after 35: muscle mass and strength decline with age, and " +
            "resistance training is the most direct lever on both.",
        "Bull FC et al. World Health Organization 2020 guidelines on physical activity and sedentary " +
            "behaviour. Br J Sports Med. 2020;54(24):1451-1462.",
        "33239350",
        "The evidence is strongest for total volume and for \"some strength training\". Optimal " +
            "weekly sets, rep ranges and progression for a given person are not settled, and most " +
            "trials run under 12 weeks — long-term adherence is the weak link, not the prescription."
    ),
    EvidenceItem(
        "move-two-sessions", "Move", "Two strength sessions a week is the threshold", Grade.A,
        "Two resistance sessions per week is the minimum dose associated with meaningful strength " +
            "and muscle outcomes in adults.",
        "Above two sessions, returns diminish for most people who are not training for performance. " +
            "If you currently do none, going from zero to two is the single highest-value change " +
            "available to you.",
        "WHO 2020 guidelines (strength component); see Bull FC et al. Br J Sports Med. 2020;54(24):1451-1462.",
        "33239350",
        "Whether two full-body sessions beat three shorter ones for a 45-year-old with a desk job is " +
            "not directly tested. Volume-equated comparisons are sparse."
    ),
    EvidenceItem(
        "move-fitness-mortality", "Move", "Cardiorespiratory fitness predicts mortality independently", Grade.B,
        "Cardiorespiratory fitness is an independent predictor of long-term mortality in adults " +
            "undergoing exercise testing.",
        "Fitness behaves like a vital sign. It is also the metric most improved by the least " +
            "intimidating intervention: walking more, more often.",
        "Harb SC et al. Associations between cardiorespiratory fitness, sex and long-term mortality " +
            "amongst adults undergoing exercise treadmill testing. Int J Cardiol. 2021;342:103-107.",
        "34363868",
        "These are observational cohorts. How much of the association is fitness itself versus the " +
            "health that produces fitness cannot be fully separated, and fitness is measured " +
            "differently across studies."
    ),
    EvidenceItem(
        "move-grip", "Move", "Grip strength is prognostic across 21 countries", Grade.B,
        "Handgrip strength is associated with all-cause and cardiovascular mortality, with reference " +
            "ranges established in 125,462 adults across 21 countries.",
        "A cheap proxy for total-body strength. You do not need to measure it to benefit from it — " +
            "but it explains why strength training is a health behaviour and not a vanity project.",
        "Leong DP et al. Reference ranges of handgrip strength from 125,462 healthy adults in 21 " +
            "countries (PURE). J Cachexia Sarcopenia Muscle. 2016;7(5):535-546.",
        "27104109",
        "Reference ranges are descriptive, not targets. Whether raising grip strength independently " +
            "lowers mortality, rather than marking general health, is not established by this design."
    ),

    // ---------------- SLEEP ----------------
    EvidenceItem(
        "sleep-7h", "Sleep", "Seven hours or more", Grade.A,
        "Adults should sleep 7 or more hours per night on a regular basis to promote optimal health.",
        "An AASM and Sleep Research Society consensus statement. Seven is the floor, not the target. " +
            "Sleeping less than seven on a regular basis is associated with worse cardiometabolic and " +
            "cognitive outcomes.",
        "Watson NF et al. Recommended amount of sleep for a healthy adult. J Clin Sleep Med. " +
            "2015;11(6):591-592.",
        "25979105",
        "This is a consensus recommendation built on observational and experimental evidence; " +
            "individual need varies, and no test reliably establishes your personal requirement. " +
            "Regularity may matter independently of duration, but that literature is younger."
    ),
    EvidenceItem(
        "sleep-alcohol", "Sleep", "Alcohol shortens and fragments sleep", Grade.B,
        "Alcohol before bed reduces sleep quality and increases fragmentation, even when it shortens " +
            "sleep onset time.",
        "It helps you fall asleep and then works against the sleep itself. If you drink most nights, " +
            "this is usually the largest single available improvement — and it is measurable in a " +
            "3-week experiment.",
        "Consistent across polysomnography studies of alcohol and sleep architecture; see AASM " +
            "guidance for clinical framing.",
        null,
        "Dose-response varies widely between people, and self-reported sleep quality is a noisy " +
            "outcome — objective measures are more reliable but harder to collect at home."
    ),

    // ---------------- FUEL ----------------
    EvidenceItem(
        "fuel-mediterranean", "Fuel", "A Mediterranean-style pattern, not a diet", Grade.A,
        "A Mediterranean-style dietary pattern supplemented with olive oil or nuts reduced major " +
            "cardiovascular events versus a control diet in a high-risk population.",
        "PREDIMED randomised 7,447 people. This is the strongest dietary trial evidence available for " +
            "a pattern rather than a nutrient. Note the retraction history below; we cite the " +
            "republished analysis.",
        "Estruch R et al. Primary prevention of cardiovascular disease with a Mediterranean diet " +
            "supplemented with extra-virgin olive oil or nuts. N Engl J Med. 2018;378:e34 " +
            "(republished; original 2013 paper retracted).",
        "29897866",
        "The 2013 publication was RETRACTED and republished in 2018 after protocol deviations were " +
            "identified at some sites; the re-analysis upheld the finding. The trial enrolled people " +
            "at high cardiovascular risk aged 55-80, so effects in younger, lower-risk men are " +
            "extrapolated, not demonstrated."
    ),
    EvidenceItem(
        "fuel-protein", "Fuel", "Protein matters more with resistance training", Grade.B,
        "Higher protein intake supports muscle protein synthesis and lean mass retention when " +
            "combined with resistance training.",
        "Protein without training does little for muscle. Training without adequate protein wastes " +
            "some of the work. The two together are the point.",
        "Position stands and meta-analyses on protein and resistance training in adults.",
        null,
        "Optimal grams per kilogram are debated and interact with age, training status and total " +
            "energy intake. Long-term effects on hard outcomes, rather than lean mass, are not established."
    ),
    EvidenceItem(
        "fuel-alcohol", "Fuel", "Alcohol risk rises with dose", Grade.B,
        "Alcohol consumption is associated with risk across a range of outcomes in a dose-dependent " +
            "way, with cancer risk rising from low levels of intake.",
        "The \"a glass of red is good for you\" story has not held up. If you drink, less is better; " +
            "the practical target is fewer drinking days per week, not a different drink.",
        "WHO European region position on alcohol and health; large-scale dose-response meta-analyses.",
        null,
        "Observational data cannot fully exclude confounding by health status and socioeconomic " +
            "position; some cardiovascular analyses show non-linear patterns. Absolute risk changes at " +
            "low intake are small and vary by individual."
    ),

    // ---------------- MIND ----------------
    EvidenceItem(
        "mind-behavioural-activation", "Mind", "Behavioural activation works", Grade.A,
        "Behavioural activation — scheduling and doing valued activity regardless of mood — is an " +
            "effective treatment for depression.",
        "For men who find \"how does that make you feel\" useless, this is the practical alternative: " +
            "act first, mood follows. It is one of the better-evidenced psychological treatments available.",
        "Cochrane and large trial evidence on behavioural activation for depression, including " +
            "non-inferiority to CBT.",
        null,
        "Effect sizes vary with severity. Self-directed behavioural activation in an app is less well " +
            "studied than therapist-delivered, and is not a substitute for care if symptoms are moderate " +
            "or severe."
    ),
    EvidenceItem(
        "mind-loneliness", "Mind", "Loneliness is a mortality risk factor", Grade.B,
        "Loneliness and social isolation are associated with increased risk of all-cause mortality.",
        "A meta-analytic review found both loneliness and isolation predicting mortality, with effect " +
            "sizes comparable to established risk factors. This is why Connect is a pillar.",
        "Holt-Lunstad J et al. Loneliness and social isolation as risk factors for mortality: a " +
            "meta-analytic review. Perspect Psychol Sci. 2015;10(2):227-237.",
        "25910392",
        "Observational; reverse causation is plausible (illness isolates). Which interventions reliably " +
            "reduce loneliness is far less established than the association itself."
    ),
    EvidenceItem(
        "mind-anger-depression", "Mind", "Irritability is a common male presentation", Grade.B,
        "Irritability and anger can be presenting features of depression, particularly in men, and are " +
            "frequently missed by screening focused on sadness.",
        "If you have been shorter with people lately, that is a symptom worth tracking, not just a " +
            "personality trait. It is one of the eight daily fields for this reason.",
        "Clinical literature on male depression phenotypes and irritability as a diagnostic feature.",
        null,
        "How much this reflects biology versus how men are socialised to report symptoms is genuinely " +
            "unresolved. Screening instruments still under-detect it."
    ),

    // ---------------- DRIVE ----------------
    EvidenceItem(
        "drive-vascular", "Drive", "Erectile function is a vascular signal", Grade.B,
        "Erectile dysfunction is commonly vascular in origin and is associated with cardiovascular " +
            "risk factors.",
        "Small vessels show problems earlier than large ones. Treating it as purely psychological " +
            "misses the part where sleep, blood pressure, alcohol and fitness are modifiable inputs.",
        "Clinical guidance and cohort literature on erectile dysfunction as a marker of " +
            "cardiovascular risk.",
        null,
        "Causal direction and the added predictive value over standard risk factors are still debated. " +
            "This is not a diagnostic screen and should not be used as one."
    ),
    EvidenceItem(
        "drive-boosters", "Drive", "\"Testosterone boosters\" are Grade D", Grade.D,
        "Over-the-counter \"testosterone booster\" supplements have no consistent trial support for " +
            "raising testosterone or improving outcomes in healthy men.",
        "A large, heavily marketed category with weak and inconsistent evidence, often funded by the " +
            "sellers. The three things with real evidence behind them are sleep, resistance training, " +
            "and fat loss. This app sells none of them.",
        "Reviews of testosterone-booster supplement trials; generally small, short, industry-linked, " +
            "with inconsistent outcomes.",
        null,
        "The literature is poor quality in both directions — few well-powered independent trials exist. " +
            "\"No consistent support\" is the honest finding, not \"proven ineffective for everyone\"."
    ),

    // ---------------- CONNECT ----------------
    EvidenceItem(
        "connect-men-friends", "Connect", "Male friendship networks narrow sharply", Grade.C,
        "Men report fewer close confidants than women, and friendship networks tend to contract " +
            "through the 30s and 40s.",
        "Partly structural — work, children, relocation — and partly behavioural. The intervention " +
            "with any support is deliberate, scheduled contact rather than hoping it happens.",
        "Survey research on American friendship networks and social support.",
        null,
        "Survey measures of \"close friends\" are crude, and the trend is better documented than any " +
            "specific fix. We treat this as a prompt to act, not a finding to fear."
    ),

    // ---------------- MYTHS ----------------
    EvidenceItem(
        "myth-cold-plunge", "Myths", "Cold plunges are Grade C", Grade.C,
        "Cold-water immersion has plausible mechanistic effects and some small trials, but no " +
            "established benefit on the outcomes people buy it for.",
        "It will wake you up. Claims about immunity, fat loss and dopamine outrun the evidence by a " +
            "wide margin. If you enjoy it, keep it; do not pay for it as treatment.",
        "Small trials and mechanistic studies of cold-water immersion; heterogeneous protocols.",
        null,
        "Protocols vary wildly (temperature, duration, timing), which makes pooling nearly impossible. " +
            "Absence of good trials is not proof of no effect."
    ),
    EvidenceItem(
        "myth-supplements", "Myths", "Most single-nutrient supplements are Grade C or D", Grade.D,
        "For men without a diagnosed deficiency, most single-nutrient supplements have not shown " +
            "benefit on hard outcomes in well-conducted trials.",
        "The exception is treating a measured deficiency, which requires a measurement. Buying " +
            "supplements to fix a feeling is the most common waste of money in this category.",
        "Large randomised trials of multivitamins and single nutrients on cardiovascular and cancer outcomes.",
        null,
        "Specific subgroups (older adults, vegans, diagnosed deficiency) do benefit. Blanket statements " +
            "in either direction are wrong; this is why we ask for a measurement first."
    ),
    EvidenceItem(
        "myth-retraction", "Myths", "We cite the retracted trial — and say so", Grade.A,
        "The 2013 PREDIMED paper was retracted and republished in 2018 after protocol deviations were " +
            "identified; the re-analysis upheld the original finding.",
        "This entry exists to demonstrate the standard. Any app that cites PREDIMED 2013 without noting " +
            "the retraction is either unaware of it or hoping you are. Both are informative.",
        "Estruch R et al. N Engl J Med. 2018;378:e34 (republished); original 2013 paper retracted by " +
            "the authors.",
        "29897866",
        "The retraction concerned randomisation irregularities at some sites, not the dietary finding. " +
            "Subgroup analyses from the republished dataset remain exploratory."
    )
)
