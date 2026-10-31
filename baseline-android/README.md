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

CI keeps the signing key in the Actions cache so consecutive builds share a signature and install
over each other. If that cache is evicted the key is regenerated, the signature changes, and Android
will refuse the update until the old app is uninstalled. The APK is also uploaded as a workflow
artifact on every run.

Release APK: **12.5 MB**. Requires Android 8.0+ (minSdk 26, for `java.time` without desugaring).
Toolchain: AGP 8.13.2, Kotlin 2.2.21, Compose BOM 2025.12.01, Gradle 8.14.

---

## Tests

```bash
./gradlew :app:testDebugUnitTest     # 64 JVM tests
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
| Local SQLite | ✅ |

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
    LlmClient.kt               BYOK OpenAI-compatible client
    Backup.kt                  daily snapshots + rotation + restore
  logic/                       pure Kotlin, no Android imports → JVM-testable
    Stats.kt                   bands, drift, bootstrap CI, permutation test
    ExperimentEngine.kt        arm assignment by date, analysis, verdict, templates
    Insights.kt                daily-card selection (drift > program step > rotation)
    Coach.kt                   retrieval, stopwords, emergency/dosing safety, system prompt
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
    SettingsScreen.kt          lock, run length, coach key, backups, import/export
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
