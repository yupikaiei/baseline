# Baseline — native Android app

Kotlin + Jetpack Compose + SQLite. No server, no accounts, no analytics, no cost.
Built to the spec in [../PRD.md](../PRD.md).

The app does one thing: it turns evidence-graded health behaviours into a measurable personal
baseline, and then lets you test whether a change actually worked **on you**.

---

## Run it

```bash
cd baseline-android
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or run directly:

```bash
./gradlew :app:installDebug
adb shell am start -n com.baseline.app/.MainActivity
```

Release build (signed with the local debug key — fine for personal use):

```bash
./gradlew :app:assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

## Install on your phone

Every push to `main` builds an APK in CI and attaches it to the rolling release:

**<https://github.com/yupikaiei/baseline/releases/tag/latest>**

Open that link on the phone, download the `.apk`, tap it, and allow installing from that source
when Android prompts. Nothing else is needed — the release asset downloads without a GitHub session.

The CI build is signed with the **same key** as local builds (stored as the encrypted
`BASELINE_KEYSTORE_B64` repo secret), so any build installs over any other and your logged data
survives upgrades. This matters: Android refuses an update from a different signer, and the only
remedy is an uninstall, which deletes the database.

Release APK: **14 MB**. Requires Android 8.0+ (minSdk 26, for `java.time` without desugaring).
Toolchain: AGP 8.13.2, Kotlin 2.2.21, Compose BOM 2025.12.01, Gradle 8.14.

---

## Tests

```bash
./gradlew :app:testDebugUnitTest     # 93 JVM tests
```

Plain JUnit on the JVM — no instrumentation, no emulator, a few seconds. The statistics are the part
that has to be right, so they are covered first: bootstrap confidence intervals, permutation tests,
minimum-data guards, drift thresholds, arm assignment, daily-card precedence, program
scheduling, coach retrieval and safety routing, CSV/JSON parsing, and streak maths.

---

## What is implemented

| Area | Status |
|---|---|
| Daily check-in (8 fields, ≤ 30 s, skippable) | ✅ |
| Your baseline — 6 pillars with 28/90-day bands | ✅ |
| Drift detection vs your own band | ✅ |
| Daily card, triggered by drift, rotated otherwise | ✅ |
| Evidence library — 18 graded items with PMIDs | ✅ |
| Evidence detail with "what we don't know" | ✅ |
| N-of-1 experiments: pre-registration, arms, bootstrap CI, permutation test | ✅ |
| Results incl. "inconclusive" as a first-class outcome | ✅ |
| Export: CSV, full JSON backup, one-page wellness summary | ✅ |
| **Programs** — 5 guided 4-week blocks, one action/day, if-then plans | ✅ |
| **Coach** — BYOK model over any OpenAI-compatible endpoint, grounded in the library | ✅ |
| **Coach fallback** — offline retrieval when no key is set; emergencies/dosing never leave the device | ✅ |
| **Import** — CSV or JSON restore through the system file picker | ✅ |
| **Automatic backup** — daily snapshot, 7 kept, restore from list | ✅ |
| **App lock** — device PIN/pattern/password gate | ✅ |
| **Run length** — consecutive days, resets without penalty | ✅ |
| **Health Connect** — prefill sleep and training from a watch (read-only, confirm required) | ✅ |
| **Profile** — age band + up to three problems, drives cards, ordering and the coach | ✅ |
| Local SQLite | ✅ |

## UI standards

Every interactive element is at least **48 x 48 dp**, the Material minimum, and screens use a
consistent **16 dp gutter** with 10 dp between tappable rows. Small visuals (info icon, tick,
chevron) are wrapped in a full-size tap area rather than being clickable at their drawn size —
see `TouchTarget` and `Spacing` in `Components.kt`.

This is checked on device, not assumed: dumping the UI hierarchy and measuring every
`clickable="true"` node catches anything that regressed.

Two bugs this found, both real:

- **Evidence filters were unreachable.** The row rendered only the first five pillars and did not
  scroll, so *Connect* and *Myths* could not be selected at all. Now horizontally scrollable.
- **Themed rows were 42 dp** on the check-in screen (Material's floor is 48), with 5 dp gutters
  between adjacent scale buttons — easy to hit the wrong one.

## Personalisation

First launch asks two things, both skippable: an age band, and up to three problems.

Age changes what the app leads with, and the framing is drawn from what actually kills men at
each age (NVSR 73(10), male deaths 2022) rather than generic life-stage copy:

| Band | What the app says |
|---|---|
| 18–29 | Injury and suicide lead, not heart disease. Decades of runway left. |
| 30–39 | Heart disease enters the top three at 35–44; numbers drift while you feel fine. |
| 40–49 | Obesity peaks here; chronic liver disease enters the top ten at 45. |
| 50–59 | Heart disease leads from the mid-50s; blood pressure stops being optional. |
| 60+ | Strength preservation and screening cadence beat intensity. |

Problems are limited to what the evidence library can actually speak to — ten of them, each
mapping to graded items and a program. Fertility and hair loss are deliberately absent: the PRD
covers them, but no evidence item supports them yet, and offering a problem the app cannot
answer honestly is theatre.

### Ranking: measured data outranks what you say about yourself

```
1. Drift        your own logs contradict your theory
2. Program      you committed to this block
3. Problem      what you selected (cold start, and the default)
4. Rotation     one day in seven, so nothing is starved
```

The rotation slot is the important one. An app that only shows you content matching your
self-diagnosis becomes an echo chamber that flatters your existing theory — the opposite of what
a measurement tool should do. Every card also shows *why* it appeared ("You said: Sleep",
"Rotation — so you see things you did not ask about"), so the ordering is never mysterious.

The profile also reorders the Baseline tab (your focus metrics first, marked), puts the relevant
program at the top of the catalogue, and is passed to the coach as context — with the instruction
to point out when your tracked data contradicts the problem you selected.

## Wearables (Health Connect)

Baseline reads **only** sleep sessions and exercise sessions from Android Health Connect,
and only when you tap *Fill from watch* while logging a day:

| Watch data | Becomes | Rule |
|---|---|---|
| Sleep duration | `sleep_min` | Straight copy |
| Sleep stages | `sleep_quality` (1–5) | Estimated from **sleep efficiency** = asleep ÷ in bed. Health Connect does not carry Garmin's sleep score, so the app labels this instead of pretending it has one. |
| Exercise sessions | `training` | ≥45 min in a day ⇒ Hard, any exercise ⇒ Light |

Nothing is written until you press Save, and every prefilled value stays editable. Prefilled days
are stored with `source='health-connect'` so you can tell them apart from typed ones.

The app requests `READ_SLEEP` and `READ_EXERCISE` **only** — no write permissions, no background
reads, no other data types. Verified in the release APK:

```
uses-permission: android.permission.INTERNET
uses-permission: android.permission.health.READ_SLEEP
uses-permission: android.permission.health.READ_EXERCISE
```

**If your watch's companion app doesn't write to Health Connect**, nothing appears and the app says
so. Check on the phone: *Settings → Security & Privacy → Privacy Controls → Health Connect →
App permissions* and look for Garmin Connect. Garmin is a closed ecosystem and support varies by
region and app version — if it isn't listed, the app still works, you just type the numbers.

### Testing without a watch

`app/src/debug/` contains a `SeedHealthActivity` that writes a sample night (23:00–07:30 with
stages) and a 50-minute exercise session, then reports what Health Connect returns. It lives only
in the debug source set and debug manifest, so the release APK has neither the class nor the write
permissions. Launch it with:

```bash
adb shell am start -n com.baseline.app/.SeedHealthActivity
```

Not built, and why:

- **Lab integration** — needs a diagnostic partner account (CLIA lab, API credentials, and a
  compliance review). Nothing honest can be built without those, and a fake "lab" would pull the
  product straight back toward diagnosis, which the wellness positioning exists to avoid.
- **Community** — a social feature with one user is a chat log with itself. It also brings
  moderation obligations that only make sense with strangers.

---

## Architecture

```
app/src/main/java/com/baseline/app/
  MainActivity.kt              activity, tab shell, menu, focus dialog, navigation
  BaselineViewModel.kt         UiState + coroutines; all DB access off the main thread
  data/
    Models.kt                  Metric, Checkin, EvidenceItem, Experiment, Arm, Grade
    Db.kt                      SQLiteOpenHelper + typed DAO
    EvidenceLibrary.kt         the 18 launch items
    Programs.kt                the 5 guided blocks
    Profile.kt                 age bands, problems, profile persistence
    LlmClient.kt               BYOK OpenAI-compatible client
    Backup.kt                  daily snapshots + rotation + restore
    HealthConnectSource.kt     read-only Health Connect bridge (sleep + exercise)
  logic/                       pure Kotlin, no Android imports → JVM-testable
    Stats.kt                   bands, drift, bootstrap CI, permutation test
    ExperimentEngine.kt        arm assignment by date, analysis, verdict, templates
    Insights.kt                daily-card selection (drift > program step > rotation)
    Coach.kt                   retrieval, stopwords, emergency/dosing safety, system prompt
    HealthMapping.kt           sleep efficiency -> quality, exercise minutes -> training
    Personalisation.kt         problem cards, ranking, program recommendation, coach context
    Importers.kt               CSV + JSON parsing, streak maths
    Format.kt                  metric-aware formatting ("1h10", "+0.4")
  ui/
    Theme.kt, Components.kt    palette, ring, sparkline, dot strip, grade chips
    TodayScreen.kt             Today + Baseline tabs
    CheckinScreen.kt           the 30-second form
    ExperimentsScreen.kt       list + pre-registration flow
    ExperimentDetailScreen.kt  result, CI bar, locked declaration
    EvidenceScreen.kt          library + detail sheet
    CoachScreen.kt             chat over the library or a BYOK model
    SettingsScreen.kt          profile, lock, run length, coach key, backups, import/export
    OnboardingScreen.kt        age + problems, doubles as the profile editor
    LockScreen.kt              device-credential gate
    Export.kt                  CSV / JSON / summary + share intent
app/src/test/java/com/baseline/app/
  StatsTest.kt                 19 tests
  AnalysisTest.kt              10 tests on seeded 40-day history
```

`logic/` has no Android dependencies, which is why the whole analytical core is testable on the JVM
in two seconds.

### Two design decisions worth knowing

**Arms are derived from dates, not logged per day.** An experiment stores its start timestamp and
phase lengths; each check-in is assigned to an arm by date arithmetic. A missed day cannot
desynchronise the design, and there is no second log to keep in sync.

**Pre-registration is enforced in the data layer.** The declared outcome, expected direction and
confounders are written once with a timestamp, and analysis runs against that declaration. The
target cannot be moved afterwards. That is what makes a personal experiment worth trusting — and
why it lives in the schema rather than in a settings screen.

---

## Your data

One SQLite file in app-private storage (`databases/baseline.db`), plus daily snapshots in
`files/backups/`.

The app requests `INTERNET` for exactly one reason: the bring-your-own-key coach. Nothing else
touches the network, and **no request is made unless you press send in the coach with a key
configured**. Emergencies and medicine questions are answered locally and never transmitted.
Without a key the coach is fully offline.

- **Export CSV** — one row per day, every field.
- **Export JSON** — full backup including pre-registrations. Use this for safekeeping.
- **Wellness summary** — plain-text one-pager with trends and three questions for a doctor.
  Deliberately states what it is not: a diagnosis.

No accounts and no sync, so **export weekly**. Until you do, the phone is the only copy.

---

## Not medical advice

Self-tracked wellness data. It does not diagnose, does not predict disease, and does not replace a
clinician. Every claim carries a grade and a source, and Grade A means "true on average in trials" —
not "true for you". That gap is what the experiments are for.
