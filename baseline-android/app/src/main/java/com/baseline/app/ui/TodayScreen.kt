package com.baseline.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baseline.app.UiState
import com.baseline.app.data.Metric
import com.baseline.app.logic.DailyCard
import com.baseline.app.logic.ExperimentEngine
import com.baseline.app.logic.Format
import com.baseline.app.logic.Insights

@Composable
fun TodayScreen(
    state: UiState,
    onLog: () -> Unit,
    onOpenEvidence: (String) -> Unit
) {
    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Palette.Accent) }
        return
    }

    val focus = state.focus
    val snap = ExperimentEngine.snapshot(state.checkins, focus)
    val programStep = state.activeProgram?.day(state.programDay)
    val card = Insights.dailyCard(
        checkins = state.checkins,
        programStep = programStep,
        programDoneToday = state.programDone.contains(state.programDay),
        profile = state.profile
    )
    val todayValue = state.todayCheckin?.valueOf(focus)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 8.dp, 16.dp, 100.dp)
    ) {
        state.profile.band?.let { band ->
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val label = when {
                        state.profile.problems.isEmpty() -> band.label + " · general"
                        state.profile.problems.size == 1 ->
                            band.label + " · " + state.profile.problems.first().label
                        else -> band.label + " · " + state.profile.problems.first().label +
                            " +" + (state.profile.problems.size - 1)
                    }
                    PillarTag(label)
                }
            }
        }
        item {
            BaselineRing(
                value = todayValue,
                band = snap.band90,
                formatted = if (todayValue == null) "—" else Format.value(focus, todayValue),
                label = if (focus == Metric.SLEEP_MIN) "sleep last night" else focus.label
            )
        }
        item {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    if (snap.band90.n < 3) "Log a few days and this becomes your own range."
                    else "your 90-day range  ${Format.value(focus, snap.band90.low)} – ${Format.value(focus, snap.band90.high)}",
                    color = Palette.TextDim,
                    fontSize = 11.5.sp
                )
            }
            Spacer(Modifier.height(20.dp))
        }
        item {
            DailyCardView(card, onOpenEvidence)
            card.reason?.let { reason ->
                Spacer(Modifier.height(8.dp))
                Text(
                    reason,
                    color = Palette.TextDim.copy(alpha = 0.75f),
                    fontSize = 10.5.sp,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }
        item {
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Last 7 days", color = Palette.TextDim, fontSize = 11.sp, letterSpacing = 1.2.sp)
                Spacer(Modifier.weight(1f))
                Text(
                    "${state.loggedDates.size} days logged",
                    color = Palette.TextDim.copy(alpha = 0.7f), fontSize = 11.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            DotStrip(state.last7)
            if (state.streakEnabled) {
                Spacer(Modifier.height(8.dp))
                Text(
                    if (state.streak == 0) "No run going. Nothing lost."
                    else "${state.streak} day run · resets without penalty",
                    color = Palette.TextDim.copy(alpha = 0.85f), fontSize = 10.5.sp
                )
            }
            Spacer(Modifier.height(22.dp))
        }
        item {
            Button(
                onClick = onLog,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Palette.Accent,
                    contentColor = androidx.compose.ui.graphics.Color(0xFF06231A)
                )
            ) {
                Icon(
                    if (state.todayCheckin == null) Icons.Filled.Add else Icons.Filled.Edit,
                    contentDescription = null, modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    if (state.todayCheckin == null) "Log today" else "Edit today's log",
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "One card a day. No streaks, no reminders you did not ask for.",
                    color = Palette.TextDim.copy(alpha = 0.8f), fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun DailyCardView(card: DailyCard, onOpenEvidence: (String) -> Unit) {
    CardBox {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PillarTag(if (card.isDrift) "Drift · ${card.pillar}" else card.pillar)
                Spacer(Modifier.size(8.dp))
                GradeChip(card.grade)
                Spacer(Modifier.weight(1f))
                card.evidenceId?.let { id ->
                    Icon(
                        Icons.Filled.Info, contentDescription = "Evidence",
                        tint = Palette.TextDim, modifier = Modifier.size(17.dp).clickable { onOpenEvidence(id) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                card.title, color = Palette.Text, fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp
            )
            Spacer(Modifier.height(8.dp))
            Text(card.body, color = Palette.TextDim, fontSize = 13.5.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Palette.AccentDim.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Icon(Icons.Filled.ArrowForward, null, tint = Palette.Accent, modifier = Modifier.size(15.dp))
                Spacer(Modifier.size(8.dp))
                Text(card.action, color = Palette.Text, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
fun BaselineTab(state: UiState) {
    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Palette.Accent) }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 8.dp, 16.dp, 40.dp)
    ) {
        item {
            Text("Your baseline", color = Palette.Text, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.6).sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "Every number is compared to you 28 and 90 days ago. Not to other men.",
                color = Palette.TextDim, fontSize = 12.5.sp
            )
            Spacer(Modifier.height(16.dp))
        }
        val ordered = state.profile.focusMetrics + Insights.PILLARS.filter { it !in state.profile.focusMetrics }
        ordered.forEach { pillar ->
            item {
                PillarTile(state, pillar, highlighted = pillar in state.profile.focusMetrics)
                Spacer(Modifier.height(10.dp))
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            CardBox {
                Text(
                    "The shaded strip is your usual range (mean ± 1 SD, clamped to what you actually logged). " +
                        "A value inside it is noise. Outside it is worth a look.",
                    color = Palette.TextDim, fontSize = 12.sp, lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun PillarTile(state: UiState, metric: Metric, highlighted: Boolean = false) {
    val snap = ExperimentEngine.snapshot(state.checkins, metric)
    val values = ExperimentEngine.seriesFor(state.checkins, metric, 90)
    val border = when {
        snap.drift.detected -> Palette.Warn.copy(alpha = 0.45f)
        highlighted -> Palette.AccentDim
        else -> Palette.Line
    }
    CardBox(borderColor = border) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(metric.label, color = Palette.Text, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                if (highlighted && !snap.drift.detected) {
                    Spacer(Modifier.size(8.dp))
                    Text("your focus", color = Palette.Accent.copy(alpha = 0.8f), fontSize = 9.5.sp)
                }
                if (snap.drift.detected) {
                    Spacer(Modifier.size(8.dp))
                    Box(
                        Modifier
                            .background(Palette.Warn.copy(alpha = 0.14f), RoundedCornerShape(5.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "OUTSIDE RANGE", color = Palette.Warn, fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    if (snap.band90.n == 0) "—" else Format.value(metric, snap.band90.mean),
                    color = Palette.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                if (snap.band90.n == 0) "no data yet"
                else "usual range ${Format.value(metric, snap.band90.low)} – ${Format.value(metric, snap.band90.high)} · ${snap.band90.n} days",
                color = Palette.TextDim, fontSize = 11.sp
            )
            Spacer(Modifier.height(8.dp))
            Sparkline(values, snap.band90)
        }
    }
}
