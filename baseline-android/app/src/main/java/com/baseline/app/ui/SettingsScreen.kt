package com.baseline.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baseline.app.BaselineViewModel
import com.baseline.app.UiState
import java.io.File

@Composable
fun SettingsScreen(
    state: UiState,
    vm: BaselineViewModel,
    onClose: () -> Unit,
    onOpenCoach: () -> Unit,
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit,
    onExportSummary: () -> Unit,
    onEditProfile: () -> Unit
) {
    val context = LocalContext.current
    var apiKey by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf(BaselineViewModel.DEFAULT_BASE_URL) }
    var model by remember { mutableStateOf(BaselineViewModel.DEFAULT_MODEL) }
    var notice by remember { mutableStateOf<String?>(null) }
    var pendingRestore by remember { mutableStateOf<File?>(null) }

    fun readAndImport(uri: android.net.Uri, isJson: Boolean) {
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
        if (text == null) {
            notice = "Could not read that file."
            return
        }
        vm.importText(text, isJson) { count, error ->
            notice = if (error != null) error else "Imported $count days."
        }
    }

    val csvPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { readAndImport(it, isJson = false) }
    }
    val jsonPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { readAndImport(it, isJson = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.gutter, vertical = Spacing.gutter)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Settings", color = Palette.Text, fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Filled.Close, contentDescription = "Close", tint = Palette.TextDim,
                modifier = Modifier.size(22.dp).clickable { onClose() }
            )
        }

        notice?.let {
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Palette.AccentDim.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) { Text(it, color = Palette.Text, fontSize = 12.sp) }
        }

        SectionLabel("Profile")
        CardBox {
            Column {
                val band = state.profile.band
                Text(
                    if (band == null) "No age set" else "Age band: ${band.label}",
                    color = Palette.Text, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold
                )
                if (band != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(band.note, color = Palette.TextDim, fontSize = 11.5.sp, lineHeight = 17.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (state.profile.problems.isEmpty()) "Nothing selected — showing the general rotation."
                    else "Working on: " + state.profile.problems.joinToString(", ") { it.label },
                    color = Palette.TextDim, fontSize = 12.sp, lineHeight = 18.sp
                )
                Spacer(Modifier.height(12.dp))
                GhostSmallButton("Edit profile") { onEditProfile() }
            }
        }

        SectionLabel("Privacy")
        ToggleRow(
            title = "App lock",
            subtitle = "Ask for your device PIN, pattern or password when the app opens. " +
                "Needs a screen lock to be set on the phone.",
            checked = state.lockEnabled
        ) { vm.setToggle("lock_enabled", it) }
        ToggleRow(
            title = "Show run length",
            subtitle = "Consecutive days logged. Missing a day resets the count and costs nothing else.",
            checked = state.streakEnabled
        ) { vm.setToggle("streak_enabled", it) }

        SectionLabel("Coach")
        CardBox {
            Column {
                Text(
                    if (state.coachConfigured) "Connected${if (state.coachModel.isNotBlank()) " · ${state.coachModel}" else ""}"
                    else "Not connected — using the offline library",
                    color = if (state.coachConfigured) Palette.Accent else Palette.TextDim,
                    fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Bring your own key for any OpenAI-compatible endpoint. The key is stored in this " +
                        "app's private storage and is only used when you press send. Leave it empty and " +
                        "the coach stays fully offline.",
                    color = Palette.TextDim, fontSize = 11.5.sp, lineHeight = 17.sp
                )
                Spacer(Modifier.height(12.dp))
                SettingsField("API key", apiKey, secret = true) { apiKey = it }
                Spacer(Modifier.height(8.dp))
                SettingsField("Base URL", baseUrl) { baseUrl = it }
                Spacer(Modifier.height(8.dp))
                SettingsField("Model", model) { model = it }
                Spacer(Modifier.height(12.dp))
                Row {
                    PrimarySmallButton("Save") {
                        vm.saveCoachConfig(apiKey, baseUrl, model)
                        notice = if (apiKey.isBlank()) "Coach set to offline." else "Coach connected."
                    }
                    Spacer(Modifier.size(8.dp))
                    GhostSmallButton("Open coach") { onOpenCoach() }
                }
                if (state.coachConfigured) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = {
                        vm.saveCoachConfig("", baseUrl, model)
                        apiKey = ""
                        notice = "Coach disconnected. Offline mode."
                    }) { Text("Disconnect", color = Palette.Bad, fontSize = 12.sp) }
                }
            }
        }

        SectionLabel("Backup")
        CardBox {
            Column {
                Text(
                    "Automatic daily snapshot",
                    color = Palette.Text, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Last backup: ${state.lastBackupLabel}. Kept in app-private storage, 7 most recent, " +
                        "written automatically once a day when you open the app.",
                    color = Palette.TextDim, fontSize = 11.5.sp, lineHeight = 17.sp
                )
                Spacer(Modifier.height(12.dp))
                Row {
                    PrimarySmallButton("Back up now") { vm.runBackup() }
                    Spacer(Modifier.size(8.dp))
                    GhostSmallButton("Export JSON") { onExportJson() }
                }
                if (state.backups.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "SNAPSHOTS", color = Palette.TextDim, fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 0.9.sp
                    )
                    state.backups.take(7).forEach { file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pendingRestore = file }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                file.name.removePrefix("baseline-").removeSuffix(".json"),
                                color = Palette.Text, fontSize = 12.5.sp, modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${(file.length() / 1024).coerceAtLeast(1)} KB",
                                color = Palette.TextDim, fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        SectionLabel("Import")
        CardBox {
            Column {
                Text(
                    "Backfill from a file",
                    color = Palette.Text, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "CSV with a date column (this app's export, or a tracker export), or a Baseline " +
                        "JSON backup. Existing days with the same date are replaced.",
                    color = Palette.TextDim, fontSize = 11.5.sp, lineHeight = 17.sp
                )
                Spacer(Modifier.height(12.dp))
                Row {
                    PrimarySmallButton("Import CSV") {
                        csvPicker.launch(arrayOf("text/*", "text/comma-separated-values", "*/*"))
                    }
                    Spacer(Modifier.size(8.dp))
                    GhostSmallButton("Restore JSON") {
                        jsonPicker.launch(arrayOf("application/json", "text/*", "*/*"))
                    }
                }
            }
        }

        SectionLabel("Export")
        Row {
            GhostSmallButton("CSV") { onExportCsv() }
            Spacer(Modifier.size(8.dp))
            GhostSmallButton("JSON") { onExportJson() }
            Spacer(Modifier.size(8.dp))
            GhostSmallButton("Summary") { onExportSummary() }
        }

        SectionLabel("About")
        Text(
            "Baseline · personal build\n" +
                "No accounts, no analytics. Data lives in one SQLite file on this device. " +
                "Health data is only ever sent anywhere when you press send in the coach and a key is configured.",
            color = Palette.TextDim, fontSize = 11.5.sp, lineHeight = 17.sp
        )
        Spacer(Modifier.height(40.dp))
    }

    pendingRestore?.let { file ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingRestore = null },
            containerColor = Palette.SurfaceAlt,
            title = { Text("Restore this snapshot?", color = Palette.Text) },
            text = {
                Text(
                    "Days in the snapshot replace days already in the app. Anything logged after " +
                        "that snapshot and not in it stays as it is.",
                    color = Palette.TextDim, fontSize = 13.sp, lineHeight = 19.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = file
                    pendingRestore = null
                    vm.restoreBackup(target) { count ->
                        notice = if (count < 0) "Restore failed." else "Restored $count days."
                    }
                }) { Text("Restore", color = Palette.Accent) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestore = null }) { Text("Cancel", color = Palette.TextDim) }
            }
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Palette.Text, fontSize = 14.sp)
            Text(subtitle, color = Palette.TextDim, fontSize = 11.5.sp, lineHeight = 16.sp)
        }
        Spacer(Modifier.size(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF06231A),
                checkedTrackColor = Palette.Accent,
                uncheckedTrackColor = Palette.SurfaceAlt,
                uncheckedBorderColor = Palette.Line
            )
        )
    }
}

@Composable
private fun SettingsField(
    label: String,
    value: String,
    secret: Boolean = false,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, color = Palette.TextDim, fontSize = 11.sp) },
        singleLine = true,
        visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Palette.SurfaceAlt,
            unfocusedContainerColor = Palette.SurfaceAlt,
            focusedTextColor = Palette.Text,
            unfocusedTextColor = Palette.Text,
            focusedIndicatorColor = Palette.AccentDim,
            unfocusedIndicatorColor = Palette.Line
        )
    )
}

@Composable
private fun PrimarySmallButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .heightIn(min = Spacing.touchTarget)
            .background(Palette.Accent, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color(0xFF06231A), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun GhostSmallButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .heightIn(min = Spacing.touchTarget)
            .border(1.dp, Palette.AccentDim, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Palette.Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
