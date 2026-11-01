package com.baseline.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baseline.app.BaselineViewModel
import com.baseline.app.UiState
import com.baseline.app.logic.CoachReply
import com.baseline.app.data.evidenceLibrary
import kotlinx.coroutines.launch

data class ChatTurn(val role: String, val text: String, val reply: CoachReply? = null)

@Composable
fun CoachScreen(
    state: UiState,
    vm: BaselineViewModel,
    onOpenEvidence: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val turns = remember { mutableStateListOf<ChatTurn>() }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun send() {
        val q = input.trim()
        if (q.isEmpty() || busy) return
        input = ""
        turns.add(ChatTurn("user", q))
        busy = true
        scope.launch {
            val reply = vm.ask(q, allowRemote = state.coachConfigured)
            turns.add(ChatTurn("coach", reply.text, reply))
            busy = false
        }
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Coach", color = Palette.Text, fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = (-0.6).sp
                )
                Text(
                    when {
                        !state.coachConfigured -> "Offline library · answers cite graded evidence"
                        state.coachModel.isNotBlank() -> "Model: ${state.coachModel} · grounded in the library"
                        else -> "Model connected · grounded in the library"
                    },
                    color = Palette.TextDim, fontSize = 11.5.sp
                )
            }
            TextButton(onClick = onOpenSettings) {
                Text("Settings", color = Palette.Accent, fontSize = 12.sp)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 4.dp, 16.dp, 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (turns.isEmpty()) {
                item {
                    CardBox {
                        Column {
                            Text(
                                "Ask about a behaviour, not a diagnosis",
                                color = Palette.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "e.g. \"how much sleep do I actually need\", \"do testosterone boosters work\", " +
                                    "\"is two strength sessions enough\". Every answer names the grade and the source.",
                                color = Palette.TextDim, fontSize = 12.5.sp, lineHeight = 18.sp
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "It will not diagnose or dose. Emergencies and medicine questions are handled " +
                                    "locally and never sent anywhere.",
                                color = Palette.TextDim.copy(alpha = 0.85f), fontSize = 11.sp, lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
            items(turns) { turn ->
                if (turn.role == "user") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Box(
                            Modifier
                                .fillMaxWidth(0.85f)
                                .background(Palette.AccentDim.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Text(turn.text, color = Palette.Text, fontSize = 13.5.sp, lineHeight = 19.sp)
                        }
                    }
                } else {
                    val reply = turn.reply
                    val accent = when {
                        reply?.isEmergency == true -> Palette.Bad
                        reply?.isRefusal == true -> Palette.Warn
                        else -> Palette.Line
                    }
                    CardBox(borderColor = accent) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PillarTag(
                                    when {
                                        reply?.isEmergency == true -> "Safety"
                                        reply?.isRefusal == true -> "Out of scope"
                                        reply?.source == "model" -> "Model"
                                        else -> "Library"
                                    }
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(turn.text, color = Palette.Text, fontSize = 13.5.sp, lineHeight = 20.sp)
                            val ids = reply?.evidenceIds?.takeIf { it.isNotEmpty() } ?: emptyList()
                            if (ids.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "SOURCES", color = Palette.TextDim, fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold, letterSpacing = 0.9.sp
                                )
                                Spacer(Modifier.height(6.dp))
                                ids.forEach { id ->
                                    val item = evidenceLibrary.firstOrNull { it.id == id }
                                    if (item != null) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            GradeChip(item.grade, compact = true)
                                            Spacer(Modifier.size(7.dp))
                                            Text(
                                                item.title,
                                                color = Palette.Accent, fontSize = 11.5.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            TextButton(
                                                onClick = { onOpenEvidence(id) },
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                                            ) { Text("Open", color = Palette.TextDim, fontSize = 11.sp) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (busy) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = Palette.Accent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.size(10.dp))
                        Text("Thinking…", color = Palette.TextDim, fontSize = 12.sp)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Palette.Surface)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask about sleep, alcohol, strength…", color = Palette.TextDim, fontSize = 13.sp) },
                maxLines = 3,
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Palette.SurfaceAlt,
                    unfocusedContainerColor = Palette.SurfaceAlt,
                    focusedTextColor = Palette.Text,
                    unfocusedTextColor = Palette.Text,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            Spacer(Modifier.size(8.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (input.isBlank() || busy) Palette.SurfaceAlt else Palette.Accent,
                        RoundedCornerShape(14.dp)
                    )
                    .padding(0.dp),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = { send() }, enabled = input.isNotBlank() && !busy) {
                    Text(
                        "↑",
                        color = if (input.isBlank() || busy) Palette.TextDim else Color(0xFF06231A),
                        fontSize = 20.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
