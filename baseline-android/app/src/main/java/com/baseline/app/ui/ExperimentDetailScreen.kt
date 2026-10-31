package com.baseline.app.ui

import androidx.compose.foundation.background
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
import com.baseline.app.data.Arm
import com.baseline.app.data.Experiment
import com.baseline.app.data.ExperimentStatus
import com.baseline.app.data.Metric
import com.baseline.app.logic.ExperimentAnalysis
import com.baseline.app.logic.ExperimentEngine
import com.baseline.app.logic.Format
import com.baseline.app.logic.Stats
import java.time.Instant
import java.time.ZoneId

@Composable
fun ExperimentDetailScreen(
    state: UiState,
    experimentId: String,
    onStop: (String) -> Unit,
    onBack: () -> Unit
) {
    val experiment = state.experiments.firstOrNull { it.id == experimentId }
    if (experiment == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Experiment not found", color = Palette.TextDim)
        }
        return
    }
    val a = ExperimentEngine.analyse(experiment, state.checkins)
    var showStop by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 4.dp, 16.dp, 36.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        experiment.title, color = Palette.Text, fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp
                    )
                    TextButton(onClick = onBack, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                        Text("← Back", color = Palette.TextDim, fontSize = 12.sp)
                    }
                }
                if (experiment.status == ExperimentStatus.RUNNING) {
                    TextButton(onClick = { showStop = true }) {
                        Text("Stop", color = Palette.TextDim, fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            DesignStrip(experiment)
            Spacer(Modifier.height(16.dp))
            ResultCard(a)
            Spacer(Modifier.height(18.dp))
            SectionLabel("Arms")
        }
        item {
            ArmRow("Baseline", a.baseline.size, a.baseline.takeIf { it.isNotEmpty() }?.let { Stats.mean(it) }, experiment.metric, Palette.Info)
            ArmRow("Intervention", a.intervention.size, a.intervention.takeIf { it.isNotEmpty() }?.let { Stats.mean(it) }, experiment.metric, Palette.Accent)
            if (experiment.washoutDays > 0) {
                ArmRow("Washout", a.washout.size, a.washout.takeIf { it.isNotEmpty() }?.let { Stats.mean(it) }, experiment.metric, Palette.TextDim)
            }
            if (experiment.repeatDays > 0) {
                ArmRow("Repeat", a.repeat.size, a.repeat.takeIf { it.isNotEmpty() }?.let { Stats.mean(it) }, experiment.metric, Palette.Accent)
            }
            a.primary?.takeIf { it.enoughData }?.let {
                Spacer(Modifier.height(8.dp))
                CiBar(it.low, it.high)
            }
            Spacer(Modifier.height(18.dp))
            SectionLabel("Pre-registration")
            CardBox {
                Column {
                    Kv("Declared", formatDate(experiment.preregisteredAt))
                    Kv("Outcome", experiment.metric.label)
                    Kv("Expected", if (experiment.expectedDirection > 0) "higher than baseline" else "lower than baseline")
                    Kv("Confounders", experiment.confounders.ifEmpty { listOf("none") }.joinToString(", "))
                    Kv("Logged days", "${a.totalLogged} · ${a.missingDays} missed")
                    if (a.confoundedDays > 0) {
                        Kv("Flagged days", "${a.confoundedDays} (alcohol > 2u or hard training)")
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "This declaration was locked when you started. It is stored unedited so the " +
                            "outcome cannot be chosen after the fact.",
                        color = Palette.TextDim.copy(alpha = 0.9f), fontSize = 11.sp, lineHeight = 16.sp
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Not medical advice. One person, one trial — a personal result is not a clinical finding.",
                color = Palette.TextDim.copy(alpha = 0.8f), fontSize = 11.sp
            )
        }
    }

    if (showStop) {
        AlertDialog(
            onDismissRequest = { showStop = false },
            containerColor = Palette.SurfaceAlt,
            title = { Text("Stop this experiment?", color = Palette.Text) },
            text = {
                Text(
                    "Abandoned experiments are kept, not deleted — an abandoned trial is data too. " +
                        "You will not get a result for it.",
                    color = Palette.TextDim, fontSize = 13.sp, lineHeight = 19.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showStop = false
                    onStop(experiment.id)
                    onBack()
                }) { Text("Stop", color = Palette.Bad) }
            },
            dismissButton = {
                TextButton(onClick = { showStop = false }) {
                    Text("Keep running", color = Palette.TextDim)
                }
            }
        )
    }
}

private fun formatDate(millis: Long): String {
    val d = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    return "${d.dayOfMonth.toString().padStart(2, '0')}/" +
        "${d.monthValue.toString().padStart(2, '0')}/${d.year}"
}

@Composable
private fun DesignStrip(e: Experiment) {
    val phases = buildList {
        add("Baseline" to e.baselineDays)
        add("Change" to e.interventionDays)
        if (e.washoutDays > 0) add("Washout" to e.washoutDays)
        if (e.repeatDays > 0) add("Repeat" to e.repeatDays)
    }
    val colors = listOf(Palette.Info, Palette.Accent, Palette.TextDim, Palette.Accent)
    val currentIndex = when (e.currentArm()) {
        Arm.BASELINE -> 0
        Arm.INTERVENTION -> 1
        Arm.WASHOUT -> 2
        Arm.REPEAT -> if (e.repeatDays > 0) 3 else 2
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "day ${e.dayNumber()} of ${e.totalDays}",
                color = Palette.TextDim, fontSize = 12.sp
            )
            Spacer(Modifier.weight(1f))
            Text(e.currentArm().label, color = Palette.Accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            phases.forEachIndexed { i, (_, days) ->
                Box(
                    modifier = Modifier
                        .weight(days.toFloat())
                        .height(8.dp)
                        .background(
                            colors[i.coerceAtMost(colors.lastIndex)]
                                .copy(alpha = if (i == currentIndex) 0.95f else 0.28f),
                            RoundedCornerShape(4.dp)
                        )
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            phases.forEachIndexed { i, (label, days) ->
                Text(
                    label,
                    color = if (i == currentIndex) Palette.Text else Palette.TextDim.copy(alpha = 0.7f),
                    fontSize = 9.5.sp,
                    modifier = Modifier.weight(days.toFloat())
                )
            }
        }
    }
}

@Composable
private fun ResultCard(a: ExperimentAnalysis) {
    val s = a.primary
    val color = when {
        s == null || !s.enoughData -> Palette.TextDim
        s.isLikelyReal -> Palette.Accent
        else -> Palette.Warn
    }
    CardBox(borderColor = color.copy(alpha = 0.4f)) {
        Column {
            Text(
                a.verdictTag, color = color, fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(a.verdict, color = Palette.Text, fontSize = 14.5.sp, lineHeight = 21.sp)
            if (s != null && s.enoughData) {
                Spacer(Modifier.height(12.dp))
                Row {
                    Stat("difference", Format.signedDelta(s.diff, a.experiment.metric))
                    Spacer(Modifier.size(18.dp))
                    Stat("95% CI", "${Format.value(a.experiment.metric, s.low)} to ${Format.value(a.experiment.metric, s.high)}")
                    Spacer(Modifier.size(18.dp))
                    Stat("p", String.format("%.2f", s.p))
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(
            label.uppercase(), color = Palette.TextDim.copy(alpha = 0.9f),
            fontSize = 8.5.sp, letterSpacing = 0.7.sp
        )
        Spacer(Modifier.height(3.dp))
        Text(value, color = Palette.Text, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ArmRow(label: String, n: Int, mean: Double?, metric: Metric, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).background(color, RoundedCornerShape(4.dp)))
        Spacer(Modifier.size(9.dp))
        Text(label, color = Palette.Text, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text("$n d", color = Palette.TextDim.copy(alpha = 0.9f), fontSize = 11.5.sp)
        Spacer(Modifier.size(12.dp))
        Text(
            if (mean == null) "—" else Format.value(metric, mean),
            color = Palette.Text, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold
        )
    }
}

/** Where the confidence interval sits relative to zero. Drawn on a canvas so the
 *  bar's geometry is exact rather than approximated with layout weights. */
@Composable
private fun CiBar(low: Double, high: Double) {
    val span = kotlin.math.abs(high - low).let { if (it == 0.0) 1.0 else it }
    val pad = span * 0.25
    val min = (if (low < 0) low else 0.0) - pad
    val max = (if (high > 0) high else 0.0) + pad
    val total = max - min
    val excludesZero = low > 0 || high < 0
    val barColor = (if (excludesZero) Palette.Accent else Palette.Warn).copy(alpha = 0.35f)

    fun frac(v: Double) = (((v - min) / total).toFloat()).coerceIn(0f, 1f)

    Column(Modifier.fillMaxWidth()) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(28.dp)) {
            val y = size.height / 2
            drawLine(
                color = Palette.Line,
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
            val x0 = frac(low) * size.width
            val x1 = frac(high) * size.width
            drawRect(
                color = barColor,
                topLeft = androidx.compose.ui.geometry.Offset(x0, y - 4.5.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(
                    width = (x1 - x0).coerceAtLeast(2.dp.toPx()),
                    height = 9.dp.toPx()
                )
            )
            val zx = frac(0.0) * size.width
            drawLine(
                color = Palette.TextDim.copy(alpha = 0.7f),
                start = androidx.compose.ui.geometry.Offset(zx, y - 11.dp.toPx()),
                end = androidx.compose.ui.geometry.Offset(zx, y + 11.dp.toPx()),
                strokeWidth = 1.5.dp.toPx()
            )
        }
        Text(
            "confidence interval vs zero",
            color = Palette.TextDim.copy(alpha = 0.8f), fontSize = 10.sp
        )
    }
}

@Composable
private fun Kv(key: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Text(
            key, color = Palette.TextDim.copy(alpha = 0.9f), fontSize = 11.5.sp,
            modifier = Modifier.size(width = 88.dp, height = 18.dp)
        )
        Text(value, color = Palette.Text, fontSize = 12.5.sp, lineHeight = 17.sp)
    }
}
