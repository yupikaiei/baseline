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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baseline.app.data.AgeBand
import com.baseline.app.data.Problem
import com.baseline.app.data.Profile

/**
 * Three steps, under a minute, and every one of them skippable. Personalisation
 * that cannot be declined is not personalisation, it is a gate.
 */
@Composable
fun OnboardingScreen(
    onDone: (Profile) -> Unit,
    onSkip: () -> Unit,
    initial: Profile = Profile()
) {
    var step by remember { mutableStateOf(0) }
    var birthYear by remember { mutableStateOf<Int?>(initial.birthYear) }
    val selected = remember { mutableStateListOf<Problem>().apply { addAll(initial.problems) } }

    val profile = Profile(birthYear = birthYear, problems = selected.toSet())
    val offered = if (birthYear == null) Problem.entries else profile.offeredProblems()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Spacer(Modifier.height(24.dp))

        // progress
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            repeat(2) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(3.dp)
                        .background(
                            if (i <= step) Palette.Accent else Palette.Line,
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }
        Spacer(Modifier.height(28.dp))

        when (step) {
            0 -> {
                Text(
                    "How old are you?",
                    color = Palette.Text, fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = (-0.6).sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Age changes what actually matters. The leading causes of death for men shift " +
                        "by decade, and so does which lever has the most headroom. Year of birth is " +
                        "enough — the app only needs the band.",
                    color = Palette.TextDim, fontSize = 13.sp, lineHeight = 19.sp
                )
                Spacer(Modifier.height(20.dp))
                AgeBand.entries.forEach { band ->
                    val on = birthYear?.let { AgeBand.forAge((java.time.Year.now().value - it).coerceIn(18, 120)) } == band
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .background(
                                if (on) Palette.AccentDim.copy(alpha = 0.4f) else Palette.Surface,
                                RoundedCornerShape(14.dp)
                            )
                            .border(
                                1.dp,
                                if (on) Palette.Accent.copy(alpha = 0.6f) else Palette.Line,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                // store the band's midpoint so the band round-trips exactly
                                val mid = (band.minAge + band.maxAge) / 2
                                birthYear = java.time.Year.now().value - mid
                            }
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(
                                band.label,
                                color = if (on) Palette.Accent else Palette.Text,
                                fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(band.note, color = Palette.TextDim, fontSize = 11.5.sp, lineHeight = 16.sp)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { step = 1 },
                    enabled = birthYear != null,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Palette.Accent,
                        contentColor = Color(0xFF06231A),
                        disabledContainerColor = Palette.SurfaceAlt,
                        disabledContentColor = Palette.TextDim
                    )
                ) { Text("Next", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            }

            else -> {
                Text(
                    "What do you want to work on?",
                    color = Palette.Text, fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = (-0.6).sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Pick up to three. This decides what the daily card shows first — but your own " +
                        "logged data always outranks what you say here, and one day in seven is " +
                        "reserved for things you did not pick.",
                    color = Palette.TextDim, fontSize = 13.sp, lineHeight = 19.sp
                )
                Spacer(Modifier.height(18.dp))
                offered.forEach { problem ->
                    val on = selected.contains(problem)
                    val atLimit = selected.size >= 3 && !on
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .background(
                                if (on) Palette.AccentDim.copy(alpha = 0.4f) else Palette.Surface,
                                RoundedCornerShape(13.dp)
                            )
                            .border(
                                1.dp,
                                if (on) Palette.Accent.copy(alpha = 0.6f) else Palette.Line,
                                RoundedCornerShape(13.dp)
                            )
                            .clickable(enabled = !atLimit) {
                                if (on) selected.remove(problem) else selected.add(problem)
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                problem.label,
                                color = if (on) Palette.Accent else if (atLimit) Palette.TextDim.copy(alpha = 0.5f) else Palette.Text,
                                fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                problem.question,
                                color = Palette.TextDim.copy(alpha = if (atLimit) 0.5f else 0.9f),
                                fontSize = 11.5.sp, lineHeight = 16.sp
                            )
                        }
                        Box(
                            Modifier
                                .size(20.dp)
                                .background(
                                    if (on) Palette.Accent else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .border(1.dp, if (on) Palette.Accent else Palette.Line, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (on) Text("✓", color = Color(0xFF06231A), fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onDone(profile) },
                    enabled = selected.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Palette.Accent,
                        contentColor = Color(0xFF06231A),
                        disabledContainerColor = Palette.SurfaceAlt,
                        disabledContentColor = Palette.TextDim
                    )
                ) { Text("Start", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            if (step == 1) {
                TextButton(onClick = { step = 0 }) {
                    Text("Back", color = Palette.TextDim, fontSize = 12.sp)
                }
            }
            TextButton(onClick = onSkip) {
                Text("Skip — just show me the app", color = Palette.TextDim, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
