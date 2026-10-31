package com.baseline.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baseline.app.UiState
import com.baseline.app.data.Experiment
import com.baseline.app.data.ExperimentStatus
import com.baseline.app.logic.ExperimentEngine
import com.baseline.app.logic.Stats
import com.baseline.app.logic.Template
import com.baseline.app.logic.templates

@Composable
fun ExperimentsScreen(
    state: UiState,
    onNew: () -> Unit,
    onOpen: (String) -> Unit
) {
    val running = state.experiments.filter { it.status == ExperimentStatus.RUNNING }
    val done = state.experiments.filter { it.status != ExperimentStatus.RUNNING }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 8.dp, 16.dp, 100.dp)
    ) {
        item {
            Text("Experiments", color = Palette.Text, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.6).sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "Group averages are not your answer. Pre-register the question, run the arms, and let " +
                    "the app tell you if the effect is real — including when it is not.",
                color = Palette.TextDim, fontSize = 12.5.sp, lineHeight = 18.sp
            )
            Spacer(Modifier.height(18.dp))
        }
        if (state.experiments.isEmpty()) {
            item {
                CardBox {
                    Column {
                        Text("Nothing running", color = Palette.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "A personal experiment has four parts: a baseline, the change, a washout, and " +
                                "a repeat. You declare the outcome before you start — that is what makes " +
                                "the answer trustworthy.",
                            color = Palette.TextDim, fontSize = 13.sp, lineHeight = 19.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onNew,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Palette.Accent, contentColor = Color(0xFF06231A)
                            )
                        ) { Text("Start one", fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        }
        if (running.isNotEmpty()) {
            item { SectionLabel("Running") }
            running.forEach { e ->
                item {
                    ExperimentCard(state, e) { onOpen(e.id) }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
        if (done.isNotEmpty()) {
            item { SectionLabel("Finished") }
            done.forEach { e ->
                item {
                    ExperimentCard(state, e) { onOpen(e.id) }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
        if (state.experiments.isNotEmpty()) {
            item {
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .border(1.dp, Palette.AccentDim, RoundedCornerShape(14.dp))
                        .clickable { onNew() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("New experiment", color = Palette.Accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ExperimentCard(state: UiState, e: Experiment, onOpen: () -> Unit) {
    val analysis = ExperimentEngine.analyse(e, state.checkins)
    CardBox(modifier = Modifier.clickable { onOpen() }) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    e.title, color = Palette.Text, fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)
                )
                Text(
                    if (analysis.finished) "done" else "day ${e.dayNumber()}/${e.totalDays}",
                    color = Palette.TextDim, fontSize = 11.5.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { e.progressPct() / 100f },
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = Palette.Accent,
                trackColor = Palette.Line
            )
            Spacer(Modifier.height(9.dp))
            Text(e.currentArm().label, color = Palette.TextDim, fontSize = 11.sp)
        }
    }
}

@Composable
fun NewExperimentScreen(
    onStart: (Experiment) -> Unit,
    onCancel: () -> Unit
) {
    var selected by remember { mutableStateOf(templates.first()) }
    var direction by remember { mutableStateOf(templates.first().expectedDirection) }
    var confounders by remember { mutableStateOf(templates.first().confounders.toSet()) }
    var showLockDialog by remember { mutableStateOf(false) }

    fun select(t: Template) {
        selected = t
        direction = t.expectedDirection
        confounders = t.confounders.toSet()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 4.dp, 16.dp, 32.dp)
    ) {
        item {
            Text("New experiment", color = Palette.Text, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            SectionLabel("What do you want to test?")
        }
        templates.forEach { t ->
            item {
                val isSelected = t.id == selected.id
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) Palette.AccentDim.copy(alpha = 0.35f) else Palette.Surface,
                            RoundedCornerShape(14.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) Palette.Accent.copy(alpha = 0.6f) else Palette.Line,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { select(t) }
                        .padding(13.dp)
                ) {
                    Column {
                        Text(t.title, color = Palette.Text, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "${t.variable} · ${t.totalDays} days",
                            color = Palette.TextDim, fontSize = 11.5.sp
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        item {
            SectionLabel("Primary outcome")
            CardBox {
                Column {
                    Text(
                        "${selected.metric.label} — logged every day already",
                        color = Palette.Text, fontSize = 13.5.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("Expected direction", color = Palette.TextDim, fontSize = 11.5.sp)
                    Spacer(Modifier.height(8.dp))
                    Row {
                        DirChip("Higher", direction > 0) { direction = 1 }
                        Spacer(Modifier.size(8.dp))
                        DirChip("Lower", direction < 0) { direction = -1 }
                    }
                }
            }
            SectionLabel("Confounders to flag")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("alcohol", "late training", "travel").forEach { c ->
                    val on = confounders.contains(c)
                    Box(
                        modifier = Modifier
                            .background(if (on) Palette.AccentDim else Palette.Surface, RoundedCornerShape(9.dp))
                            .border(1.dp, Palette.Line, RoundedCornerShape(9.dp))
                            .clickable {
                                confounders = if (on) confounders - c else confounders + c
                            }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Text(c, color = if (on) Palette.Accent else Palette.TextDim, fontSize = 11.5.sp)
                    }
                }
            }
            SectionLabel("Design")
            DesignRow("Baseline", selected.baselineDays, "no change")
            DesignRow("Intervention", selected.interventionDays, selected.variable)
            if (selected.washoutDays > 0) {
                DesignRow("Washout", selected.washoutDays, "revert — catches placebo and season")
            }
            if (selected.repeatDays > 0) {
                DesignRow("Repeat", selected.repeatDays, "confirmation")
            }
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Palette.SurfaceAlt, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    "Minimum 10 logged days per arm before any result is shown. Underpowered results " +
                        "are labelled inconclusive, not significant.",
                    color = Palette.TextDim, fontSize = 11.5.sp, lineHeight = 17.sp
                )
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { showLockDialog = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Palette.Accent, contentColor = Color(0xFF06231A)
                )
            ) { Text("Pre-register and start", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
        }
    }

    if (showLockDialog) {
        AlertDialog(
            onDismissRequest = { showLockDialog = false },
            containerColor = Palette.SurfaceAlt,
            title = { Text("Pre-register this experiment", color = Palette.Text) },
            text = {
                Column {
                    Text(selected.title, color = Palette.Text, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "You are declaring the outcome and the expected direction now. This cannot be " +
                            "edited afterwards — that is what stops you from moving the target later.",
                        color = Palette.TextDim, fontSize = 13.sp, lineHeight = 19.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "${selected.baselineDays}d baseline · ${selected.interventionDays}d intervention" +
                            (if (selected.washoutDays > 0) " · ${selected.washoutDays}d washout" else "") +
                            (if (selected.repeatDays > 0) " · ${selected.repeatDays}d repeat" else ""),
                        color = Palette.Text, fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showLockDialog = false
                    onStart(
                        Experiment(
                            id = "exp-${System.currentTimeMillis()}",
                            title = selected.title,
                            metric = selected.metric,
                            expectedDirection = direction,
                            confounders = confounders.toList(),
                            baselineDays = selected.baselineDays,
                            interventionDays = selected.interventionDays,
                            washoutDays = selected.washoutDays,
                            repeatDays = selected.repeatDays,
                            preregisteredAt = System.currentTimeMillis()
                        )
                    )
                }) { Text("Lock it in", color = Palette.Accent, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showLockDialog = false }) {
                    Text("Cancel", color = Palette.TextDim)
                }
            }
        )
    }
}

@Composable
private fun DirChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) Palette.AccentDim else Palette.SurfaceAlt, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            label,
            color = if (selected) Palette.Accent else Palette.TextDim,
            fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DesignRow(label: String, days: Int, note: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Palette.TextDim, fontSize = 13.sp, modifier = Modifier.size(width = 92.dp, height = 18.dp))
        Text("$days d", color = Palette.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.size(10.dp))
        Text(
            note, color = Palette.TextDim.copy(alpha = 0.85f), fontSize = 11.5.sp,
            maxLines = 1, modifier = Modifier.weight(1f)
        )
    }
}
