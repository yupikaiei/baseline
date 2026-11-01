package com.baseline.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import com.baseline.app.data.Checkin
import com.baseline.app.data.HealthConnectSource
import com.baseline.app.data.HealthPermissions
import com.baseline.app.data.HealthPrefill
import com.baseline.app.data.HealthStatus
import com.baseline.app.data.Training
import com.baseline.app.logic.Format
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun CheckinScreen(
    existing: Checkin?,
    date: String,
    onSave: (Checkin) -> Unit,
    onCancel: () -> Unit
) {
    var sleepMin by remember { mutableStateOf(existing?.sleepMin) }
    var sleepQuality by remember { mutableStateOf(existing?.sleepQuality) }
    var energy by remember { mutableStateOf(existing?.energy) }
    var mood by remember { mutableStateOf(existing?.mood) }
    var stress by remember { mutableStateOf(existing?.stress) }
    var alcohol by remember { mutableStateOf(existing?.alcoholUnits ?: 0.0) }
    var training by remember { mutableStateOf(existing?.training ?: Training.NONE) }
    var libido by remember { mutableStateOf(existing?.libido) }
    var showLibido by remember { mutableStateOf(existing?.libido != null) }
    var note by remember { mutableStateOf(existing?.note ?: "") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val source = remember { HealthConnectSource(context) }
    var healthStatus by remember { mutableStateOf(HealthStatus.UNAVAILABLE) }
    var prefillBusy by remember { mutableStateOf(false) }
    var prefillNotes by remember { mutableStateOf<List<String>>(emptyList()) }
    var prefilled by remember { mutableStateOf(false) }

    fun applyPrefill(p: HealthPrefill) {
        p.sleepMin?.let { sleepMin = it }
        p.sleepQuality?.let { sleepQuality = it }
        p.training?.let { training = it }
        prefillNotes = p.notes
        if (!p.isEmpty) prefilled = true
    }

    fun readAndFill() {
        prefillBusy = true
        scope.launch {
            val target = runCatching { LocalDate.parse(date) }.getOrElse { LocalDate.now() }
            val result = runCatching { source.read(target) }.getOrElse {
                HealthPrefill(notes = listOf("Could not read Health Connect: ${it.message?.take(80)}"))
            }
            applyPrefill(result)
            prefillBusy = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(HealthPermissions.READ)) readAndFill()
        else prefillNotes = listOf("Access not granted. You can still log everything by hand.")
    }

    LaunchedEffect(Unit) {
        healthStatus = source.status()
        if (healthStatus == HealthStatus.AVAILABLE && source.hasPermissions()) {
            prefillNotes = listOf("Connected. Tap to fill today's sleep and training from Health Connect.")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.gutter, vertical = Spacing.gutter)
    ) {
        Text("Log today", color = Palette.Text, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "Eight taps. Nothing is required.",
            color = Palette.TextDim, fontSize = 12.sp
        )
        Spacer(Modifier.height(16.dp))

        if (healthStatus != HealthStatus.UNAVAILABLE) {
            PrefillCard(
                status = healthStatus,
                busy = prefillBusy,
                prefilled = prefilled,
                notes = prefillNotes,
                onFill = {
                    when {
                        healthStatus == HealthStatus.NEEDS_PROVIDER_UPDATE -> {
                            prefillNotes = listOf("Update Health Connect from the Play Store first.")
                        }
                        else -> scope.launch {
                            if (source.hasPermissions()) readAndFill()
                            else permissionLauncher.launch(HealthPermissions.READ)
                        }
                    }
                }
            )
            Spacer(Modifier.height(14.dp))
        }

        // Sleep
        CardBox {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sleep", color = Palette.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text(
                        if (sleepMin == null) "not set" else Format.minutes(sleepMin),
                        color = if (sleepMin == null) Palette.TextDim else Palette.Text,
                        fontSize = 22.sp, fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepButton(Icons.Filled.Remove) {
                        sleepMin = (((sleepMin ?: 420) - 15).coerceIn(120, 900))
                    }
                    Spacer(Modifier.width(10.dp))
                    StepButton(Icons.Filled.Add) {
                        sleepMin = (((sleepMin ?: 420) + 15).coerceIn(120, 900))
                    }
                    Spacer(Modifier.weight(1f))
                    listOf(6.0, 7.0, 7.5, 8.0).forEach { preset ->
                        val minutes = (preset * 60).toInt()
                        Spacer(Modifier.width(6.dp))
                        PresetChip(
                            label = if (preset % 1.0 == 0.0) "${preset.toInt()}h" else "${preset}h",
                            selected = sleepMin == minutes
                        ) { sleepMin = minutes }
                    }
                }
            }
        }

        SectionLabel("How it felt")
        ScaleRow("Sleep quality", sleepQuality) { sleepQuality = it }
        ScaleRow("Energy", energy) { energy = it }
        ScaleRow("Mood", mood) { mood = it }
        ScaleRow("Stress", stress) { stress = it }

        SectionLabel("Inputs")
        AlcoholRow(alcohol) { alcohol = it }
        Spacer(Modifier.height(10.dp))
        TrainingRow(training) { training = it }

        if (!showLibido) {
            TextButton(onClick = { showLibido = true }) {
                Text("+ Add libido (hidden by default)", color = Palette.TextDim, fontSize = 12.sp)
            }
        } else {
            ScaleRow("Libido", libido) { libido = it }
        }

        SectionLabel("Anything unusual")
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Optional. One line is enough.", color = Palette.TextDim, fontSize = 14.sp) },
            minLines = 2,
            shape = RoundedCornerShape(14.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Palette.Surface,
                unfocusedContainerColor = Palette.Surface,
                focusedTextColor = Palette.Text,
                unfocusedTextColor = Palette.Text,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Spacer(Modifier.height(22.dp))
        Button(
            onClick = {
                onSave(
                    Checkin(
                        date = date,
                        sleepMin = sleepMin,
                        sleepQuality = sleepQuality,
                        energy = energy,
                        mood = mood,
                        stress = stress,
                        alcoholUnits = alcohol,
                        training = training,
                        libido = if (showLibido) libido else null,
                        note = note.trim().ifEmpty { null },
                        source = if (prefilled) "health-connect" else "typed"
                    )
                )
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Palette.Accent,
                contentColor = Color(0xFF06231A)
            )
        ) { Text("Save", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }

        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                "Self-monitoring is the mechanism. A missed day costs nothing.",
                color = Palette.TextDim.copy(alpha = 0.9f), fontSize = 11.sp
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StepButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(Palette.SurfaceAlt, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Palette.Text, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun PresetChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .widthIn(min = Spacing.touchTarget)
            .heightIn(min = Spacing.touchTarget)
            .background(if (selected) Palette.AccentDim else Palette.SurfaceAlt, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) Palette.Accent else Palette.TextDim,
            fontSize = 12.sp, fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ScaleRow(label: String, value: Int?, onPick: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Palette.TextDim, fontSize = 14.sp, modifier = Modifier.width(96.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            (1..5).forEach { i ->
                val selected = value == i
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .background(
                            if (selected) Palette.AccentDim else Palette.Surface,
                            RoundedCornerShape(10.dp)
                        )
                        .border(
                            1.dp,
                            if (selected) Palette.Accent.copy(alpha = 0.6f) else Palette.Line,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onPick(i) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$i",
                        color = if (selected) Palette.Accent else Palette.TextDim,
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun AlcoholRow(value: Double, onPick: (Double) -> Unit) {
    val labels = listOf("0", "1", "2", "3", "4", "5+")
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Alcohol", color = Palette.TextDim, fontSize = 14.sp, modifier = Modifier.width(96.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            labels.forEachIndexed { i, label ->
                val selected = value.toInt() == i
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .background(
                            if (selected) Palette.AccentDim else Palette.Surface,
                            RoundedCornerShape(10.dp)
                        )
                        .border(
                            1.dp,
                            if (selected) Palette.Accent.copy(alpha = 0.6f) else Palette.Line,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onPick(if (i == 5) 5.0 else i.toDouble()) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (selected) Palette.Accent else Palette.TextDim,
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun TrainingRow(value: Training, onPick: (Training) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Training", color = Palette.TextDim, fontSize = 14.sp, modifier = Modifier.width(96.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            Training.entries.forEach { t ->
                val selected = value == t
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .background(
                            if (selected) Palette.AccentDim else Palette.Surface,
                            RoundedCornerShape(10.dp)
                        )
                        .border(
                            1.dp,
                            if (selected) Palette.Accent.copy(alpha = 0.6f) else Palette.Line,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onPick(t) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        t.label,
                        color = if (selected) Palette.Accent else Palette.TextDim,
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun PrefillCard(
    status: HealthStatus,
    busy: Boolean,
    prefilled: Boolean,
    notes: List<String>,
    onFill: () -> Unit
) {
    CardBox(borderColor = if (prefilled) Palette.Accent.copy(alpha = 0.45f) else Palette.Line) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PillarTag(if (prefilled) "Filled from Health Connect" else "Health Connect")
                Spacer(Modifier.weight(1f))
                if (status == HealthStatus.NEEDS_PROVIDER_UPDATE) {
                    Text("update needed", color = Palette.Warn, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.height(10.dp))

            if (notes.isEmpty()) {
                Text(
                    "Fill sleep and training from your watch, then check them before saving.",
                    color = Palette.TextDim, fontSize = 12.5.sp, lineHeight = 18.sp
                )
            } else {
                notes.forEach { note ->
                    Text(
                        "· $note",
                        color = Palette.TextDim,
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .border(1.dp, Palette.AccentDim, RoundedCornerShape(12.dp))
                    .clickable(enabled = !busy) { onFill() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when {
                        busy -> "Reading…"
                        prefilled -> "Re-read from watch"
                        else -> "Fill from watch"
                    },
                    color = if (busy) Palette.TextDim else Palette.Accent,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Read-only, and only when you tap this. Nothing is saved until you press Save.",
                color = Palette.TextDim.copy(alpha = 0.8f), fontSize = 10.5.sp, lineHeight = 15.sp
            )
        }
    }
}
