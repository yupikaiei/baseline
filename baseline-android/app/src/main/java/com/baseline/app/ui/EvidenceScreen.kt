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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baseline.app.data.EvidenceItem
import com.baseline.app.data.Grade
import com.baseline.app.data.evidenceLibrary

@Composable
fun EvidenceScreen(onOpen: (EvidenceItem) -> Unit) {
    val pillars = listOf("All") + evidenceLibrary.map { it.pillar }.distinct()
    var selectedPillar = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("All") }
    val filtered = if (selectedPillar.value == "All") evidenceLibrary
    else evidenceLibrary.filter { it.pillar == selectedPillar.value }
    val gradeD = evidenceLibrary.count { it.grade == Grade.D }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp, 8.dp, 0.dp, 100.dp)
    ) {
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "Evidence", color = Palette.Text, fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = (-0.6).sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Every claim carries a grade and a source. ${evidenceLibrary.size} items, " +
                        "$gradeD of them saying something does not work.",
                    color = Palette.TextDim, fontSize = 12.5.sp, lineHeight = 18.sp
                )
                Spacer(Modifier.height(14.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pillars.take(5).forEach { p ->
                    val on = selectedPillar.value == p
                    Box(
                        modifier = Modifier
                            .background(
                                if (on) Palette.AccentDim else Palette.Surface,
                                RoundedCornerShape(9.dp)
                            )
                            .border(1.dp, if (on) Palette.AccentDim else Palette.Line, RoundedCornerShape(9.dp))
                            .clickable { selectedPillar.value = p }
                            .padding(horizontal = 11.dp, vertical = 7.dp)
                    ) {
                        Text(
                            p,
                            color = if (on) Palette.Accent else Palette.TextDim,
                            fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
        filtered.forEach { item ->
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp)
                        .background(Palette.Surface, RoundedCornerShape(14.dp))
                        .border(1.dp, Palette.Line, RoundedCornerShape(14.dp))
                        .clickable { onOpen(item) }
                        .padding(14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GradeChip(item.grade, compact = true)
                            Spacer(Modifier.size(7.dp))
                            Text(
                                item.pillar,
                                color = Palette.TextDim.copy(alpha = 0.9f),
                                fontSize = 10.5.sp, letterSpacing = 0.4.sp
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            item.title, color = Palette.Text, fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold, lineHeight = 19.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EvidenceDetailSheet(item: EvidenceItem, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Bg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Filled.Close,
                contentDescription = "Close",
                tint = Palette.TextDim,
                modifier = Modifier.size(22.dp).clickable { onClose() }
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            GradeChip(item.grade)
            Spacer(Modifier.size(8.dp))
            PillarTag(item.pillar)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            item.title, color = Palette.Text, fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp, lineHeight = 26.sp
        )
        Spacer(Modifier.height(14.dp))
        Text(item.claim, color = Palette.Text, fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(12.dp))
        Text(item.summary, color = Palette.TextDim, fontSize = 13.5.sp, lineHeight = 21.sp)
        Spacer(Modifier.height(20.dp))
        InfoBlock("What this grade means", item.grade.meaning, gradeColor(item.grade))
        Spacer(Modifier.height(10.dp))
        InfoBlock("What we don't know", item.dontKnow, Palette.Warn)
        Spacer(Modifier.height(10.dp))
        InfoBlock(
            "Source",
            item.citation + (item.pmid?.let { "\nPMID $it" } ?: ""),
            Palette.Info
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "Reviewed ${item.reviewedAt}",
            color = Palette.TextDim.copy(alpha = 0.8f), fontSize = 10.5.sp
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun InfoBlock(title: String, body: String, color: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.07f), RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
            .padding(13.dp)
    ) {
        Text(
            title.uppercase(), color = color, fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 0.9.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(body, color = Palette.TextDim, fontSize = 12.5.sp, lineHeight = 19.sp)
    }
}
