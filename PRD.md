# BASELINE — a free, personal, science-based wellness app for men

**PRD v3.0 · single user · zero cost · 30 October 2026**
*Supersedes v2.0 (consumer wellness subscription). See §0 for what changed and what it costs.*

> **Thesis, unchanged:** you cannot improve what you haven't measured, and you cannot measure against other people. Baseline establishes *your* baseline, grades the evidence behind every recommendation, and lets you test whether a change actually worked — on yourself.

---

## 0. What changed, and what it cost

v2.0 was a venture-scale product: paywall, subscription tiers, community, moderation, B2B, market sizing. With **one user and no revenue**, almost all of that is now dead weight. This version keeps the science and the mechanism, and throws away the business.

| v2.0 | v3.0 | Why |
|---|---|---|
| Free / Premium / Couples / B2B tiers | **Everything free, no accounts** | There is no business model because there is no business |
| Paywall after first experiment | **No paywall** | — |
| Community ("Locker Room"), pre-moderation | **Cut** | Moderation exists to protect strangers from each other; n=1 has no strangers |
| AI Coach | **Cut from v1** (see §7.4) | Per-call inference cost. Replaced with a curated, cited answer library — free forever and arguably better |
| Partner view, GP sharing | **Cut** → replaced by a **one-page export** | You can hand a PDF to a doctor without a sharing feature |
| Wellness lab panel | **Cut** | Costs money, and pulls the product back toward clinical framing |
| Modes (Building / Performing / Second Wind / Guard) | **Cut** → one mode, your current life stage | You are one person in one life stage at a time |
| Acquisition, conversion, D30 retention metrics | **Cut** → personal adherence + behaviour change | Nobody is acquiring you |
| 600-piece content library | **~60 pieces + a growing personal library** | You don't need a content factory; you need the 60 things that matter |
| 14-FTE team, 18-month roadmap | **1 person, ~6 weeks** | — |
| Store fees, hosting, APIs | **Native APK, local-only, $0/yr** | "Completely free" has to mean free, not cheap |

**What this costs you, honestly stated:**

1. **No social accountability.** The single strongest retention driver in consumer fitness/wellness is other people. You are removing it. §9 is therefore the most important section in this document.
2. **No zero-effort logging.** A cloud app can pull sleep from a wearable server-side. A free local app either uses Apple Health/Health Connect import or you type it. Typing eight numbers takes 20 seconds; that's the budget.
3. **No AI.** The curated library is better-cited than an LLM and cannot hallucinate, but it cannot answer an arbitrary question. Accept that trade.

**What it does not cost:** the scientific rigour. Nothing in §2 depends on revenue.

---

## 1. Scope

**In scope**
- One user (you). One device of record, local data.
- Daily self-monitoring, personal baseline, drift detection.
- Evidence-graded recommendations — every claim shows its source.
- N-of-1 experiments: prove whether a change works for *you*.
- 4-week guided programs built on behaviour-change technique.
- Export: your data as CSV/JSON, and a one-page summary for a doctor.

**Out of scope, permanently**
- Other users, accounts, sync conflicts, social features, leaderboards.
- Diagnosis, disease prediction, dosing, prescribing, supplement commerce.
- Anything with a per-use cost: LLM calls, lab orders, cloud functions.
- Dark patterns — there's no conversion to optimise.

---

## 2. The scientific foundation

This is the part that justifies the whole project, and it is unchanged by the business model.

### 2.1 Why the daily check-in is the product, not a diary

The strongest empirical finding available for a self-tracking tool:

- Meta-regression of **122 evaluations (N=44,747)** of physical-activity and healthy-eating interventions pooled at an effect size of **0.31**, with high heterogeneity (I²=69%).
- **"Self-monitoring" explained the largest share of that heterogeneity — 13%** — of any of the 26 techniques coded.
- Interventions combining self-monitoring **with at least one other control-theory technique** (goal-setting, feedback, action planning) were significantly more effective: **0.42 vs 0.26**.

Two design rules follow, and they are non-negotiable:

1. **The check-in must be ≤30 seconds.** The mechanism *is* the logging; if logging is effortful, the mechanism dies. This is the single most important constraint in the app.
2. **A log alone is not enough.** Every observation must be paired with comparison (feedback) or a plan (goal-setting / if-then). A screen that shows you a chart and nothing else is a failure of the design, not a feature.

### 2.2 Evidence grading — visible, on every claim

| Grade | Meaning | Example |
|---|---|---|
| **A** | Multiple RCTs/meta-analyses, consistent, in humans | Aerobic activity and mortality; ≥2 resistance sessions/week; 7–9h sleep; Mediterranean-style diet; smoking cessation |
| **B** | One good RCT, or strong consistent cohort evidence | Reducing alcohol; protein with resistance training; daily step volume; social-connection interventions |
| **C** | Mechanistic plausibility, small or mixed trials | Cold-water immersion; most single-nutrient supplements; time-restricted eating beyond weight |
| **D** | Popular, heavily sold, **not supported** | "Testosterone boosters"; detox products; most adaptogen claims |

**The grade is shown before the advice, not after.** A C-grade tip must look like a C-grade tip. This is the whole difference between this app and a supplement ad, and it costs nothing to implement — it's a field in a JSON file.

**A-grade ≠ proven for you.** Group averages are not individual predictions. That gap is exactly what §2.3 fills.

### 2.3 N-of-1 — the reason this app exists for one user

With one user, personal experimentation isn't a nice feature; it's the only way "science-based" can be true *for you*. Formal single-case methodology has published reporting standards (SCRIBE 2016), so this can be done properly rather than as vibes.

```
Experiment: "Does a 10pm screen cutoff improve my sleep?"

Baseline     14 days   (no change)
Intervention 21 days   (the change)
Washout       7 days   (revert — critical, catches placebo/season effects)
Repeat       14 days   (confirmation)

Primary outcome: sleep duration (imported) + sleep quality (1–5, logged)
Confounders flagged automatically: alcohol, travel, late training, illness
Result: mean difference + 95% CI (bootstrap), plus a permutation test
```

**Design requirements that make it real rather than decorative:**

- **Pre-registration is enforced by the UI.** You declare the outcome, the expected direction, and the confounders *before* day 1. The app timestamps it and won't let you edit it afterwards. This is the single best defence against fooling yourself, and it's the feature no commercial app will ship because it makes marketing harder.
- **Washout is default-on.** Without it, you're measuring the seasons.
- **Report uncertainty, always.** A 21-vs-14-day comparison with a ±40-minute CI usually means "we don't know yet." The app must be allowed to say that. If it can't say "inconclusive," it isn't an instrument.
- **Minimum n enforced.** No result below 10 days per arm. Underpowered results are shown as "not enough data," never as a finding.

### 2.4 Integrity rules (cheap to honour when you're the only user)

- **Retraction watch.** The 2013 PREDIMED trial was **retracted and republished in 2018** after protocol deviations were identified; the re-analysis upheld the finding. Cite the republished version *and note the retraction*. Any app citing 2013 silently is telling you about its standards.
- **"What we don't know" is a required field** on every evidence item. It cannot be empty.
- **No commerce in the thing you advise on.** No supplement links. Ever. You're the user *and* the owner, so this is a rule about your future self.
- **Publish nothing you can't source.** See the integrity note at the end — several famous men's-health stats failed verification during research and were excluded. Hold the app to the same bar.

---

## 3. What's borrowed from Flo

Flo is the reference UX because it solved daily engagement better than anyone ($275M revenue, 77M MAU, ~5M subscribers, 4.74/5 from 2M iOS ratings — and, notably, no prescriptions). What transfers, and what doesn't:

**Borrow**

1. **The daily ritual, not the chart.** Home = one dominant ring + one card. Not a dashboard. The unit of engagement is *today*, not *your data*.
2. **Honest uncertainty.** When Flo can't predict precisely it shows a ~14-day window rather than faking precision. Inherit this as a hard rule, especially in experiment results.
3. **One card, one action.** 20 seconds to read, one thing to do.
4. **The shareable artefact.** A one-page PDF converted passive logging into external utility. Highest-value feature in the app, and now trivially cheap — generate it locally.
5. **No streaks.** Verified absent from Flo. A missed day must cost nothing, or you'll quit after the first one.

**Refuse**

- Paywalling "is this normal?" (Flo's most-criticised behaviour — monetising anxiety).
- 7-section onboarding with uneditable answers.
- Splash + pop-up + push upsell stacking.
- Ad-tech SDKs. Flo paid a **$56M** settlement and a jury found Meta liable in 2025 over menstrual-health data. At n=1 the risk is smaller but the principle is identical: no third-party scripts touching health data, at all.

---

## 4. The product

Four screens, plus one modal. Everything else is a consequence.

### 4.1 Screen 1 — Today

```
        ╭─────────────╮
        │     6h12m   │   ← last night (imported or typed)
        │   SLEEP     │
        ╰─────────────╯
        ╭───────────────────────────╮
        │ TODAY · Grade A           │   ← one card, 20 seconds
        │ Third night under 6h.     │
        │ One action below.         │
        │  → Lights out 30m earlier │
        ╰───────────────────────────╯
   ●●●●●○○   last 7 days (dots, not streaks)
   [ Log today ]
```

- The ring shows the day's primary metric, whichever pillar you've set as focus.
- The seven dots are **completion, not streaks** — miss one, nothing breaks.
- `Log today` is the only required interaction, ≤30s.

### 4.2 Screen 2 — Check-in (the 30-second rule)

| Field | Input | Default |
|---|---|---|
| Sleep duration | auto from HealthKit/Health Connect, else stepper | imported |
| Sleep quality | 1–5 | — |
| Energy | 1–5 | — |
| Mood | 1–5 | — |
| Stress | 1–5 | — |
| Alcohol | units, stepper 0–10 | 0 |
| Training | none / light / hard | none |
| Libido *(optional, hidden by default)* | 1–5 | skipped |
| Note *(optional)* | free text | — |

Eight taps and a save. **Skippable, never punished, no "you broke your streak" copy anywhere in the codebase.**

### 4.3 Screen 3 — Baseline

Six pillars, each with a rolling **7 / 28 / 90-day band**. The band is the feature:

> **Sleep** — 90-day average **6h12m** · your range **5h50–6h40**
> Last 7 days: 6h02 (+3%) — *within your normal range.*

The point is not the number. The point is that "6h02" and "5h50" mean nothing without *your* range, and once you have it, a change is legible. This is also the honest antidote to every device that tells you that you're "recovered 42%."

### 4.4 Screen 4 — Experiments

State machine: `draft → pre-registered → baseline → intervention → washout → repeat → result`.

Templates ship with the app (each one is a known-good question with a known-good outcome measure):

| Template | Variable | Outcome | Length |
|---|---|---|---|
| Screen cutoff | no screens after 22:00 | sleep duration | 14+21+7+14 |
| Alcohol-free block | 0 units | sleep + energy | 14+21+7+14 |
| Walk after meals | 10 min × 2/day | energy + mood | 14+21+7 |
| Strength 2×/week | 2 sessions | energy + sleep quality | 14+28 |
| Caffeine cutoff | none after 12:00 | sleep quality | 14+21+7 |
| Step floor | ≥8,000/day | mood + energy | 14+21 |

**The result screen is the whole product:**

> **Screen cutoff · 21 days**
> Sleep duration: **+34 min** on compliant nights (n=18 vs 14). 95% CI **+12 to +55 min**. Washout held → likely real.
> ⚠️ 3 nights flagged: alcohol (2), late training (1).
> *Not medical advice. One person, one trial.*

And when it's null, it says so: *"No detectable effect (−4 min, CI −26 to +19). Consider a longer run or a different variable."*

### 4.5 Modal — Evidence

Reachable from every card and every program step:

```
GRADE A  ·  reviewed 2026-10-30
Strength training, twice a week
What it is: ...
Evidence: WHO 2020 guidelines recommend ≥2 muscle-strengthening
sessions/week for adults. [PMID 33239350]
What we don't know: optimal volume above 2 sessions is unclear;
most trials are short (<12 weeks).
```

### 4.6 Programs

Four weeks, one action per day, three minutes. Each step bundles self-monitoring with a goal or if-then plan (per §2.1), so this is enforced structure, not a reading list:

| Program | Weeks | Core technique |
|---|---|---|
| **Sleep Reset** | 4 | Fixed wake time + implementation intention |
| **The 3-Drink Week** | 4 | Self-monitoring + if-then substitution |
| **Strength Base** | 4 | Progressive goal-setting + feedback |
| **Stress & Anger** | 4 | Behavioural activation + stimulus control |
| **Move More** | 4 | Step goal + daily feedback |

### 4.7 Export

- **`baseline.csv`** — one row per day, every field. Your data, always.
- **`baseline.json`** — full fidelity, including experiment pre-registrations.
- **`summary.pdf`** (one page) — pillar trends, adherence, experiments run, and **three questions to raise with a doctor**. Deliberately states what the app is *not* (diagnosis) so a clinician doesn't have to wonder.

---

## 5. Content

~60 pieces at launch, not 600. You are the only reader.

**Seven pillars.** Six for the app, one for the reason to trust it:

| # | Pillar | Strongest evidence | Sample pieces |
|---|---|---|---|
| 1 | **Move** | WHO 2020: 150–300 min moderate **or** 75–150 vigorous/week **plus ≥2 muscle-strengthening sessions**; cardiorespiratory fitness as an independent predictor of mortality; grip strength prognostic in **125,462 adults across 21 countries** | "The two sessions that matter most" · "Walking counts — the dose" |
| 2 | **Sleep** | AASM/SRS: **7+ hours** for adults; regularity increasingly implicated independently of duration | "Seven, not six and a half" · "Why you wake at 3am" |
| 3 | **Fuel** | Mediterranean-style pattern (PREDIMED, n=7,447; **retracted and republished 2018**); protein with resistance training; alcohol dose-response | "What to eat when you can't cook" · "Protein after 35" |
| 4 | **Mind** | Behavioural activation and CBT for low mood; loneliness/social isolation as mortality risk factors | "Anger is the most common male symptom" · "What to say when you don't want to talk" |
| 5 | **Drive** | Sexual function as vascular + psychological signal; sleep/alcohol/weight as modifiable inputs | "ED is usually a blood-vessel story" · "What moves testosterone — and what doesn't" |
| 6 | **Connect** | Social connection interventions; the male "no close friends" pattern | "Keeping friends after 35" |
| 7 | **Myths** | The pillar that earns the other six | "Testosterone boosters: what the trials show (nothing)" · "Cold plunges: why they're C-grade" · "The trial that was retracted, and why we still cite it" |

**Editorial rules:** direct voice, no hype, no "optimise/biohack/alpha". Every piece ends with one action and one measurement. Grade + citation + "what we don't know" are required fields. Anything about suicide, fertility, body image, or alcohol gets written twice — once for content, once for tone.

**Card copy calibration:**

> **Day 12 · Sleep · Grade A**
> 5h40m, third night under 6. That's enough to shift mood and training quality — and to make any "how am I doing?" judgement unreliable.
> → *Lights out 30 min earlier tonight. Re-check in 7 days.*

> **Myth · Drive · Grade D**
> "Testosterone boosters" is a multi-billion-dollar category with no consistent trial support. The three things with A/B evidence: sleep, resistance training, fat loss. We sell none of them.
> → *Read the 9-trial summary (4 min).*

---

## 6. Build plan

### 6.1 Free means free — the $0 constraint

| Need | Free solution | Cost |
|---|---|---|
| Distribution | Native APK, sideloaded. No Play Store listing, no developer account. | $0 |
| Signing | Local debug keystore (fine for personal installs) | $0 |
| Storage | Android SQLite via `SQLiteOpenHelper` | $0 |
| Backend | **None.** No server, no auth, no database service | $0 |
| Notifications | Local `AlarmManager` / `WorkManager` if needed | $0 |
| AI | **Bring your own key**, optional. Offline retrieval library otherwise. | $0 to the app |
| Analytics | None. You are the only user | $0 |

> **Why native, not PWA:** a personal Android app costs nothing to build, sign or install, so the
> $99/yr argument only bites on iOS. Native also buys direct access to `SQLiteOpenHelper`,
> `KeyguardManager`, the system file picker and Health Connect — all of which a browser sandbox
> forces you to work around. The trade-off is that the app is Android-only.

### 6.2 Stack (as built)

```
Kotlin 2.2.21  +  Jetpack Compose (Material 3, Compose BOM 2025.12.01)
AGP 8.13.2  ·  Gradle 8.14  ·  minSdk 26  ·  targetSdk 36
SQLite via SQLiteOpenHelper (no Room, no annotation processing)
Charts: Compose Canvas — no chart library needed for a ring and six sparklines
Tests: plain JUnit on the JVM (no instrumentation, no emulator)
Release APK: 12.5 MB
```

The analytical core (`logic/`) has no Android imports, which is why 64 tests run in seconds on the
JVM rather than on a device.

### 6.3 Data model

```sql
-- one row per day
CREATE TABLE checkin (
  date TEXT PRIMARY KEY,           -- ISO yyyy-mm-dd
  sleep_min INTEGER,
  sleep_quality INTEGER,           -- 1..5
  energy INTEGER, mood INTEGER, stress INTEGER,
  alcohol_units REAL DEFAULT 0,
  training TEXT,                   -- none|light|hard
  libido INTEGER,                  -- nullable, hidden by default
  note TEXT,
  source TEXT,                     -- typed|healthkit|healthconnect
  created_at TEXT
);

-- evidence library (ships with the app, versioned)
CREATE TABLE evidence (
  id TEXT PRIMARY KEY,
  pillar TEXT,
  title TEXT,
  grade TEXT,                      -- A|B|C|D
  claim TEXT,
  summary TEXT,
  citation TEXT,
  pmid TEXT,
  dont_know TEXT NOT NULL,         -- required, cannot be empty
  reviewed_at TEXT
);

-- pre-registered single-case experiments
CREATE TABLE experiment (
  id TEXT PRIMARY KEY,
  title TEXT,
  variable TEXT,
  outcome TEXT,
  expected_direction TEXT,         -- declared BEFORE day 1, immutable
  confounders TEXT,                -- JSON list
  baseline_days INTEGER, intervention_days INTEGER,
  washout_days INTEGER, repeat_days INTEGER,
  preregistered_at TEXT NOT NULL,  -- timestamp; row is locked after this
  status TEXT                      -- draft|running|complete|abandoned
);

CREATE TABLE experiment_day (
  experiment_id TEXT, date TEXT,
  arm TEXT,                        -- baseline|intervention|washout|repeat
  compliant INTEGER,
  PRIMARY KEY (experiment_id, date)
);

CREATE TABLE program_progress (
  program_id TEXT, day INTEGER, completed_at TEXT,
  PRIMARY KEY (program_id, day)
);

CREATE TABLE settings (key TEXT PRIMARY KEY, value TEXT);
```

The `preregistered_at` immutability is a trigger or an application-level guard — it's the integrity feature, so it should be enforced in the schema, not the UI.

### 6.4 Statistics, kept honest and small

- **Effect size:** difference in means between arms, with **bootstrap 95% CI** (1,000 resamples; trivial in JS).
- **Significance:** **permutation test** over arm labels — no distributional assumptions, ~30 lines.
- **Confounder flags:** any day with alcohol > 2 units, training = hard, or a note containing illness is flagged and excluded-or-annotated (your choice, shown explicitly).
- **Minimum data:** ≥10 days per arm, else "not enough data."
- **Never** report a p-value without the CI, and never report either without the caveat line.

---

## 7. Deliberate omissions

Listed so the scope doesn't creep back:

1. **No AI coach.** Costs per call, needs a key, can hallucinate medical claims. The 60-piece cited library answers the questions you'll actually ask. If you later want one, bring your own key and never let it outrank a graded citation.
2. **No wearable integration in v1** beyond a manual/bulk import. API access to Oura/Whoop costs money and complexity for data you can type in 5 seconds. Start with manual; import if it becomes the friction.
3. **No community, no social, no sharing.** No moderation burden, no moderation risk.
4. **No lab integration.** Costs money; drags the app toward diagnosis.
5. **No multi-device sync.** One device of record; export/import covers the rest.

---

## 8. What success looks like for one person

There is no conversion funnel. Success is behavioural:

| Measure | Target | Why this one |
|---|---|---|
| Check-ins per week | **≥5, for 8+ consecutive weeks** | Self-monitoring is the mechanism; below 5 the signal and the habit both decay |
| Experiments completed | **≥3 in the first 3 months** | Proves the loop, not just the logging |
| Behaviour actually changed | **≥1 sustained change at day 90** | The only outcome that matters |
| Baseline coverage | **≥80% of days have a sleep value** | Determines whether drift detection is meaningful |
| Wellbeing | **WHO-5 at baseline and day 90** | Validated instrument; a number you can't argue with |
| Sleep | **PROMIS sleep disturbance short form**, baseline and day 90 | Same reason |

**The honest failure criterion:** if at day 90 you have fewer than 3 check-ins/week and no completed experiment, the product is wrong for you — and the correct response is to cut features, not add them.

---

## 9. Risks (personal, not commercial)

| Risk | Severity | Mitigation |
|---|---|---|
| **Solo abandonment — no social pressure, no subscription guilt** | 🔴 High | The experiment arc is the substitute: a running trial with a defined end date is a reason to open the app that a chart can't provide. Also: 30-second logging, and no streak-shaming. |
| **Over-engineering instead of using it** | 🔴 High | Ship the check-in in week 1 and use it for a week *before* building the baseline screen. If a feature isn't needed by your own data, don't build it. |
| **Self-deception / p-hacking your own life** | 🟠 Med | Pre-registration enforced in the schema; washout on by default; CIs reported; "inconclusive" is a valid, displayed result. |
| **Data loss** (app data cleared, phone lost) | 🟠 Med | Daily automatic JSON snapshot in app-private storage, 7 kept; manual export to a real file; never rely on the phone as the only copy. |
| **Privacy on a shared device** | 🟡 Med | Local-only data, optional app lock, neutral notification copy ("Your check-in is ready"), libido field hidden by default. |
| **Scope creep back toward a startup** | 🟡 Med | §7 is a standing list. Adding an item requires deleting one. |
| **Statistical overconfidence from n=1** | 🟡 Med | Every result carries the caveat; no result is ever phrased as a health finding, only as "for you, so far." |

---

## 10. Roadmap — six weeks, one person

| Week | Ship | Definition of done |
|---|---|---|
| **1** | Schema + check-in + local persistence + export | You log 7 consecutive days without opening the code |
| **2** | Today screen + 7-day dots + Baseline (bands) | You can answer "is this week normal for me?" |
| **3** | Evidence library (60 items) + evidence modal + daily cards | Every card you see cites a source |
| **4** | Experiment engine: pre-registration, arms, statistics, results | One experiment running with a real end date |
| **5** | Programs (2 of 5) + Wellness Summary PDF | A PDF you'd actually hand to a GP |
| **6** | Polish: app lock, automatic backup, import | Installed on your phone, data backed up daily |

**Stop rule:** if week 1 doesn't produce daily use, do not proceed to week 2. Fix the check-in instead.

### Implementation status — 31 Oct 2026

Built as a **native Kotlin + Jetpack Compose** app in [`baseline-android/`](baseline-android/README.md).

| Roadmap item | Status |
|---|---|
| Schema, check-in, local persistence, export | ✅ |
| Today screen, 7-day dots, Baseline bands | ✅ |
| Evidence library + evidence detail + daily cards | ✅ 18 items, graded, cited |
| Experiment engine: pre-registration, arms, statistics, results | ✅ bootstrap CI + permutation test |
| Programs (5 blocks × 4 weeks) | ✅ |
| Coach — BYOK model, grounded, with offline fallback | ✅ |
| Import (CSV / JSON restore), automatic daily backup | ✅ |
| App lock, run length | ✅ |
| Health Connect prefill (read-only, confirm required) | ✅ |
| Profile: age band + problems drive cards, ordering, programs and coach | ✅ |

93 JVM unit tests cover statistics, arm assignment, drift, card precedence, program scheduling,
coach retrieval and safety routing, import parsing, streak maths, and personalisation
(age-band boundaries, problem→evidence integrity, and the ranking order). Release APK **12.5 MB**.

**Deviations from the principles in §3, made deliberately:**

- **Streaks were built** (§3.5 argued against them). Implemented as a run length that resets without
  comment and can be switched off; the copy never shames a missed day. Whether that is enough to avoid
  the failure mode the PRD warned about is an empirical question, not a settled one.
- **The coach can call a model** (§7.1 assumed no per-call cost). It is bring-your-own-key, so the app
  itself stays free; without a key it falls back to offline retrieval. The safety layer (emergencies,
  dosing) runs locally and never reaches the network.
- **`INTERNET` permission is now requested.** This weakens the previous "nothing can leave the device"
  claim: nothing does leave unless you press send with a key configured, but the capability exists.

**Wearables arrived via Health Connect** (§5.2 item 18), not a vendor API: Garmin exposes no consumer
API, so the app reads sleep and exercise from the system health store and pre-fills the check-in. The
confirm step is deliberate — auto-filling the field would remove the daily act of logging, which §2.1
identifies as the active ingredient.

**Still not built:** lab integration (needs a diagnostic partner and would drag the product back to
diagnosis) and community (no other users).

---

## 11. Open questions

1. **What's your focus pillar for the first 90 days?** One, not six. Sleep is the highest-leverage and the easiest to measure.
2. **Android-only is fine?** The native build is Android. An iOS equivalent would need a separate codebase and a $99/yr developer account.
3. **Do you want to import Apple Health sleep**, or is typing 5 seconds acceptable? Import is a real build cost; typing is friction every night.
4. **How much of the 60-piece library do you actually need** before you'd trust it — or would 20 excellent pieces be better than 60 adequate ones?
5. **Is a GP summary genuinely useful to you**, or is it a feature borrowed from Flo's product logic that a single user never uses?

---

## Appendix — sources

**Behaviour change:** Michie et al., *Ann Behav Med* 2009 — 122 evaluations, N=44,747; self-monitoring explained 13% of heterogeneity; self-monitoring + control-theory technique 0.42 vs 0.26 — [PMID 19916637](https://pubmed.ncbi.nlm.nih.gov/19916637/) · Tate et al., SCRIBE 2016 single-case reporting guideline — [PMID 27499802](https://pubmed.ncbi.nlm.nih.gov/27499802/)

**Lifestyle evidence:** Bull et al., WHO 2020 physical activity guidelines — [PMID 33239350](https://pubmed.ncbi.nlm.nih.gov/33239350/) · Watson et al., AASM/SRS sleep duration consensus (7h+) — [PMID 25979105](https://pubmed.ncbi.nlm.nih.gov/25979105/) · Estruch et al., PREDIMED — **retracted & republished 2018** ([PMID 23432189](https://pubmed.ncbi.nlm.nih.gov/23432189/) → [PMID 29897866](https://pubmed.ncbi.nlm.nih.gov/29897866/)) · Leong et al., grip strength, PURE, n=125,462, 21 countries — [PMID 27104109](https://pubmed.ncbi.nlm.nih.gov/27104109/) · Harb et al., cardiorespiratory fitness and long-term mortality — [PMID 34363868](https://pubmed.ncbi.nlm.nih.gov/34363868/) · Holt-Lunstad et al., loneliness/isolation and mortality — [PMID 25910392](https://pubmed.ncbi.nlm.nih.gov/25910392/)

**Flo reference (UX only):** [about](https://flo.health/about-flo) · [onboarding](https://help.flo.health/hc/en-us/articles/4406826484500-Setting-up-your-Flo-account) · [Health Report](https://help.flo.health/hc/en-us/articles/51631220426900-I-want-to-generate-a-report-for-my-doctor) · [Business of Apps stats](https://www.businessofapps.com/data/flo-statistics/) · [Meta liability verdict](https://techcrunch.com/2025/08/05/jury-rules-meta-violated-california-privacy-laws-by-collecting-menstrual-health-data-from-flo/)

---

### Research integrity note

Several widely-cited men's-health figures could **not** be traced to a primary source and are therefore excluded from this document: ED prevalence (52%), hypogonadism (5.6%), male-factor share of infertility (40–50%), "80% of sleep apnea undiagnosed", hair loss "50% by 50", and the Cleveland Clinic "65% of men avoid the doctor" survey. Each may well be correct; none was verified here. The app holds itself to the same standard — **a claim that can't be sourced doesn't ship**, even when it would make better marketing. With one user there is no marketing, which is precisely why the standard is easy to keep.
