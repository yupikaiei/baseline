package com.baseline.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baseline.app.data.Backup
import com.baseline.app.data.Checkin
import com.baseline.app.data.Db
import com.baseline.app.data.Experiment
import com.baseline.app.data.ExperimentStatus
import com.baseline.app.data.LlmClient
import com.baseline.app.data.Metric
import com.baseline.app.data.programById
import com.baseline.app.logic.Coach
import com.baseline.app.logic.CoachReply
import com.baseline.app.logic.Format
import com.baseline.app.logic.Importers
import com.baseline.app.logic.Streaks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class UiState(
    val loading: Boolean = true,
    val checkins: List<Checkin> = emptyList(),
    val experiments: List<Experiment> = emptyList(),
    val focus: Metric = Metric.SLEEP_MIN,
    val today: String = Format.today(),
    val programId: String? = null,
    val programStart: String? = null,
    val programDone: Set<Int> = emptySet(),
    val streakEnabled: Boolean = true,
    val lockEnabled: Boolean = false,
    val coachConfigured: Boolean = false,
    val coachModel: String = "",
    val backups: List<File> = emptyList(),
    val lastBackupLabel: String = "never"
) {
    val todayCheckin: Checkin? get() = checkins.firstOrNull { it.date == today }
    val loggedDates: Set<String> get() = checkins.map { it.date }.toSet()

    val last7: List<Boolean>
        get() {
            val logged = loggedDates
            return (6 downTo 0).map { offset ->
                logged.contains(Format.isoDate(LocalDate.now().minusDays(offset.toLong())))
            }
        }

    val streak: Int get() = Streaks.current(loggedDates, LocalDate.now())

    val activeProgram get() = programById(programId)

    /** 1-based day number inside the active program. */
    val programDay: Int
        get() {
            val start = programStart?.takeIf { it.isNotBlank() }
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return 1
            val elapsed = ChronoUnit.DAYS.between(start, LocalDate.now()).toInt()
            val total = activeProgram?.totalDays ?: 28
            return (elapsed + 1).coerceIn(1, total)
        }

    val programComplete: Boolean
        get() {
            val p = activeProgram ?: return false
            return programDone.size >= p.totalDays
        }
}

class BaselineViewModel(app: Application) : AndroidViewModel(app) {

    private val db = Db.get(app)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        reload()
        maybeAutoBackup()
    }

    fun reload() {
        viewModelScope.launch {
            val data = withContext(Dispatchers.IO) {
                val programId = db.setting("active_program")?.takeIf { it.isNotBlank() }
                Data(
                    checkins = db.recentCheckins(365),
                    experiments = db.experiments(),
                    focus = db.setting("focus_metric"),
                    programId = programId,
                    programStart = db.setting("active_program_start"),
                    programDone = if (programId != null) db.programProgress(programId) else emptySet(),
                    streakEnabled = db.setting("streak_enabled") != "false",
                    lockEnabled = db.setting("lock_enabled") == "true",
                    apiKey = db.setting("coach_api_key"),
                    model = db.setting("coach_model"),
                    backups = Backup.list(getApplication())
                )
            }
            _state.value = _state.value.copy(
                loading = false,
                checkins = data.checkins,
                experiments = data.experiments,
                focus = Metric.entries.firstOrNull { it.key == data.focus } ?: Metric.SLEEP_MIN,
                today = Format.today(),
                programId = data.programId,
                programStart = data.programStart,
                programDone = data.programDone,
                streakEnabled = data.streakEnabled,
                lockEnabled = data.lockEnabled,
                coachConfigured = !data.apiKey.isNullOrBlank(),
                coachModel = data.model ?: "",
                backups = data.backups,
                lastBackupLabel = data.backups.firstOrNull()?.name
                    ?.removePrefix("baseline-")?.removeSuffix(".json") ?: "never"
            )
        }
    }

    private data class Data(
        val checkins: List<Checkin>,
        val experiments: List<Experiment>,
        val focus: String?,
        val programId: String?,
        val programStart: String?,
        val programDone: Set<Int>,
        val streakEnabled: Boolean,
        val lockEnabled: Boolean,
        val apiKey: String?,
        val model: String?,
        val backups: List<File>
    )

    // ---------------- check-ins ----------------

    fun saveCheckin(checkin: Checkin) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { db.saveCheckin(checkin) }
            reload()
        }
    }

    fun setFocus(metric: Metric) = write("focus_metric", metric.key)

    // ---------------- experiments ----------------

    fun startExperiment(experiment: Experiment) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { db.saveExperiment(experiment) }
            reload()
        }
    }

    fun stopExperiment(id: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { db.setExperimentStatus(id, ExperimentStatus.ABANDONED) }
            reload()
        }
    }

    // ---------------- programs ----------------

    fun startProgram(id: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.setSetting("active_program", id)
                db.setSetting("active_program_start", Format.today())
            }
            reload()
        }
    }

    fun markProgramDay(day: Int, done: Boolean) {
        val id = _state.value.programId ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { db.markProgramDay(id, day, done) }
            reload()
        }
    }

    fun leaveProgram() {
        val id = _state.value.programId ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.clearProgram(id)
                db.setSetting("active_program", "")
                db.setSetting("active_program_start", "")
            }
            reload()
        }
    }

    // ---------------- settings ----------------

    fun setToggle(key: String, value: Boolean) = write(key, if (value) "true" else "false")

    private fun write(key: String, value: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { db.setSetting(key, value) }
            reload()
        }
    }

    fun saveCoachConfig(apiKey: String, baseUrl: String, model: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.setSetting("coach_api_key", apiKey.trim())
                db.setSetting("coach_base_url", baseUrl.trim())
                db.setSetting("coach_model", model.trim())
            }
            reload()
        }
    }

    private fun coachConfig(): LlmClient.Config = LlmClient.Config(
        apiKey = db.setting("coach_api_key") ?: "",
        baseUrl = db.setting("coach_base_url")?.takeIf { it.isNotBlank() } ?: DEFAULT_BASE_URL,
        model = db.setting("coach_model")?.takeIf { it.isNotBlank() } ?: DEFAULT_MODEL
    )

    // ---------------- backup ----------------

    private fun maybeAutoBackup() {
        viewModelScope.launch {
            val due = withContext(Dispatchers.IO) { Backup.isDue(getApplication()) }
            if (due) runBackup()
        }
    }

    fun runBackup() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                Backup.write(getApplication(), db.allCheckins(), db.experiments())
            }
            reload()
        }
    }

    fun restoreBackup(file: File, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val count = runCatching { withContext(Dispatchers.IO) { Backup.restoreInto(db, file) } }
                .getOrElse { -1 }
            reload()
            onResult(count)
        }
    }

    // ---------------- import ----------------

    fun importText(text: String, isJson: Boolean, onResult: (Int, String?) -> Unit) {
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val checkins =
                        if (isJson) Importers.parseCheckinsJson(text) else Importers.parseCsv(text)
                    checkins.forEach { db.saveCheckin(it) }
                    checkins.size
                }
            }
            reload()
            result.fold(
                onSuccess = { onResult(it, null) },
                onFailure = { onResult(0, it.message ?: "Could not read that file.") }
            )
        }
    }

    fun allCheckins(onResult: (List<Checkin>) -> Unit) {
        viewModelScope.launch {
            onResult(withContext(Dispatchers.IO) { db.allCheckins() })
        }
    }

    fun allExperiments(onResult: (List<Experiment>) -> Unit) {
        viewModelScope.launch {
            onResult(withContext(Dispatchers.IO) { db.experiments() })
        }
    }

    // ---------------- coach ----------------

    /** Safety first: emergencies and dosing questions never reach a remote model. */
    suspend fun ask(question: String, allowRemote: Boolean): CoachReply {
        if (Coach.isEmergency(question)) return Coach.emergencyReply()
        if (Coach.isDosing(question)) return Coach.refusalReply()

        if (!allowRemote) return Coach.offlineAnswer(question)

        val config = withContext(Dispatchers.IO) { coachConfig() }
        if (!config.isConfigured) return Coach.offlineAnswer(question)

        return LlmClient.ask(config, Coach.systemPrompt(), question).fold(
            onSuccess = { text ->
                CoachReply(
                    text = text,
                    evidenceIds = Coach.retrieve(question).map { it.id },
                    source = "model"
                )
            },
            onFailure = { e ->
                val fallback = Coach.offlineAnswer(question)
                fallback.copy(
                    text = fallback.text +
                        "\n\n(Model call failed: ${e.message?.take(120)}. This answer is from the offline library.)",
                    source = "offline"
                )
            }
        )
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.deepseek.com/v1"
        const val DEFAULT_MODEL = "deepseek-chat"
    }
}
