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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baseline.app.UiState
import com.baseline.app.data.Program
import com.baseline.app.data.programs
import com.baseline.app.logic.Personalisation

@Composable
fun ProgramsScreen(
    state: UiState,
    onStart: (String) -> Unit,
    onMarkDay: (Int, Boolean) -> Unit,
    onLeave: () -> Unit
) {
    val active = state.activeProgram

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 8.dp, 16.dp, 100.dp)
    ) {
        item {
            Text(
                "Programs", color = Palette.Text, fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold, letterSpacing = (-0.6).sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Four weeks, one action a day, three minutes. Each step pairs logging with a plan — " +
                    "self-monitoring alone is not what changes behaviour.",
                color = Palette.TextDim, fontSize = 12.5.sp, lineHeight = 18.sp
            )
            Spacer(Modifier.height(16.dp))
        }

        if (active != null) {
            item {
                ActiveProgramCard(state, active, onMarkDay, onLeave)
                Spacer(Modifier.height(20.dp))
                SectionLabel("Catalogue")
            }
        }

        val recommendation = Personalisation.programRecommendation(state.profile)
        Personalisation.programsFor(state.profile)
            .filter { it.id != active?.id }
            .forEach { p ->
                item {
                    val isRec = recommendation?.program?.id == p.id
                    ProgramCard(
                        program = p,
                        isActive = false,
                        reason = if (isRec) "Recommended — ${recommendation?.reason}" else null
                    ) { onStart(p.id) }
                    Spacer(Modifier.height(10.dp))
                }
            }
    }
}

@Composable
private fun ActiveProgramCard(
    state: UiState,
    program: Program,
    onMarkDay: (Int, Boolean) -> Unit,
    onLeave: () -> Unit
) {
    val currentDay = state.programDay
    val done = state.programDone
    val todayStep = program.day(currentDay)
    val todayDone = done.contains(currentDay)
    val pct = (done.size.toFloat() / program.totalDays).coerceIn(0f, 1f)

    CardBox(borderColor = Palette.Accent.copy(alpha = 0.4f)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PillarTag("Active")
                Spacer(Modifier.size(8.dp))
                Text(
                    "day $currentDay of ${program.totalDays}",
                    color = Palette.TextDim, fontSize = 11.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                program.title, color = Palette.Text, fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp
            )
            Text(program.subtitle, color = Palette.TextDim, fontSize = 12.5.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(14.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(Palette.Line, RoundedCornerShape(3.dp))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(pct)
                        .height(5.dp)
                        .background(Palette.Accent, RoundedCornerShape(3.dp))
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "${done.size} of ${program.totalDays} days marked · week ${todayStep.week} — ${todayStep.theme}",
                color = Palette.TextDim, fontSize = 11.sp
            )

            Spacer(Modifier.height(16.dp))
            Text(
                "TODAY",
                color = Palette.TextDim, fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.0.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(todayStep.action, color = Palette.Text, fontSize = 14.5.sp, lineHeight = 21.sp)
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Palette.AccentDim.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    todayStep.ifThen,
                    color = Palette.Text, fontSize = 12.5.sp, lineHeight = 18.sp
                )
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { onMarkDay(currentDay, !todayDone) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (todayDone) Palette.SurfaceAlt else Palette.Accent,
                    contentColor = if (todayDone) Palette.TextDim else Color(0xFF06231A)
                )
            ) {
                Text(
                    if (todayDone) "Done today — tap to undo" else "Mark today done",
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                )
            }
            TextButton(onClick = onLeave) {
                Text("Leave program", color = Palette.TextDim, fontSize = 12.sp)
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    CardBox {
        Column {
            Text(
                "All days", color = Palette.Text, fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            (1..program.totalDays).forEach { d ->
                val step = program.day(d)
                val isDone = done.contains(d)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMarkDay(d, !isDone) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(16.dp)
                            .background(
                                if (isDone) Palette.Accent else Palette.SurfaceAlt,
                                RoundedCornerShape(5.dp)
                            )
                            .border(1.dp, if (isDone) Palette.Accent else Palette.Line, RoundedCornerShape(5.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDone) Text("✓", color = Color(0xFF06231A), fontSize = 10.sp)
                    }
                    Spacer(Modifier.size(10.dp))
                    Text(
                        "$d",
                        color = Palette.TextDim, fontSize = 11.sp,
                        modifier = Modifier.size(width = 22.dp, height = 16.dp)
                    )
                    Text(
                        step.action,
                        color = if (d == currentDay) Palette.Text else Palette.TextDim,
                        fontSize = 12.5.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgramCard(
    program: Program,
    isActive: Boolean,
    reason: String? = null,
    onStart: () -> Unit
) {
    CardBox(borderColor = if (reason != null) Palette.Accent.copy(alpha = 0.4f) else Palette.Line) {
        Column {
            if (reason != null) {
                Text(reason, color = Palette.Accent, fontSize = 10.5.sp, lineHeight = 15.sp)
                Spacer(Modifier.height(8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    program.title, color = Palette.Text, fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)
                )
                PillarTag(program.focus.label)
            }
            Spacer(Modifier.height(4.dp))
            Text(program.subtitle, color = Palette.TextDim, fontSize = 12.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                "${program.totalDays} days · ${program.weeklyThemes.size} weekly themes",
                color = Palette.TextDim.copy(alpha = 0.85f), fontSize = 11.sp
            )
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .border(1.dp, Palette.AccentDim, RoundedCornerShape(12.dp))
                    .clickable { onStart() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isActive) "Active" else "Start",
                    color = Palette.Accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
