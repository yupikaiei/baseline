package com.baseline.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baseline.app.data.Grade
import com.baseline.app.logic.Band
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Layout constants, so spacing is consistent rather than per-screen guesswork. */
object Spacing {
    /** Screen gutter on every edge. */
    val gutter = 16.dp
    /** Gap between tappable rows — enough to avoid hitting the neighbour. */
    val betweenTargets = 10.dp
    /** Material's minimum, and the floor this app holds everywhere. */
    val touchTarget = 48.dp
}

/**
 * Wraps a small visual (icon, tick, chevron) in a full-size tap area without
 * changing how it looks. Used wherever the thing you see is smaller than the
 * thing you have to hit.
 */
@Composable
fun TouchTarget(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = Spacing.touchTarget,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
fun GradeChip(grade: Grade, compact: Boolean = false) {
    val c = gradeColor(grade)
    Box(
        modifier = Modifier
            .background(c.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .border(1.dp, c.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
            .padding(horizontal = if (compact) 6.dp else 8.dp, vertical = if (compact) 2.dp else 3.dp)
    ) {
        Text(
            "GRADE ${grade.label}",
            color = c,
            fontSize = if (compact) 9.sp else 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp
        )
    }
}

@Composable
fun PillarTag(label: String) {
    Box(
        modifier = Modifier
            .background(Palette.SurfaceAlt, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            label.uppercase(),
            color = Palette.TextDim,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp
        )
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = Palette.TextDim,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp, start = 2.dp)
    )
}

@Composable
fun CardBox(
    modifier: Modifier = Modifier,
    borderColor: androidx.compose.ui.graphics.Color = Palette.Line,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Palette.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) { content() }
}

/**
 * The dominant home element: one value, with the person's own 90-day range
 * drawn as the arc it is compared against.
 */
@Composable
fun BaselineRing(value: Double?, band: Band, formatted: String, label: String) {
    Box(modifier = Modifier.fillMaxWidth().height(210.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(210.dp)) {
            val stroke = 10.dp.toPx()
            val radius = size.minDimension / 2 - stroke
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2, radius * 2)
            val startAngle = 135f
            val sweepAngle = 270f

            drawArc(
                color = Palette.Line,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            if (band.n < 3) return@Canvas

            val loAngle = startAngle + sweepAngle * 0.06f
            val hiAngle = startAngle + sweepAngle * 0.94f
            drawArc(
                color = Palette.AccentDim,
                startAngle = loAngle,
                sweepAngle = hiAngle - loAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            if (value == null) return@Canvas
            val span = if (band.high - band.low == 0.0) 1.0 else band.high - band.low
            val t = ((value - band.low) / span).coerceIn(0.0, 1.0)
            val angle = loAngle + (hiAngle - loAngle) * t.toFloat()
            val rad = angle * PI.toFloat() / 180f
            val p = Offset(center.x + radius * cos(rad), center.y + radius * sin(rad))
            val inside = value >= band.low && value <= band.high
            val markerColor = if (inside) Palette.Accent else Palette.Warn
            drawCircle(color = markerColor, radius = 6.5.dp.toPx(), center = p)
            drawCircle(
                color = markerColor.copy(alpha = 0.35f),
                radius = 11.dp.toPx(),
                center = p,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (value == null) "—" else formatted,
                color = Palette.Text,
                fontSize = 40.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-1.2).sp
            )
            Text(
                label.uppercase(),
                color = Palette.TextDim,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.4.sp
            )
        }
    }
}

@Composable
fun Sparkline(values: List<Double>, band: Band?, height: Int = 34) {
    if (values.size < 2) {
        Text(
            "not enough data yet",
            color = Palette.TextDim.copy(alpha = 0.8f),
            fontSize = 11.sp,
            modifier = Modifier.height(height.dp)
        )
        return
    }
    Canvas(modifier = Modifier.fillMaxWidth().height(height.dp)) {
        var lo = values.min()
        var hi = values.max()
        if (band != null && band.n > 0) {
            lo = min(lo, band.low)
            hi = maxOf(hi, band.high)
        }
        val span = if (hi - lo == 0.0) 1.0 else hi - lo
        fun yOf(v: Double) = (size.height - ((v - lo) / span) * size.height).toFloat()

        if (band != null && band.n > 0) {
            drawRect(
                color = Palette.AccentDim.copy(alpha = 0.18f),
                topLeft = Offset(0f, yOf(band.high)),
                size = Size(size.width, (yOf(band.low) - yOf(band.high)).coerceAtLeast(1f))
            )
        }

        val dx = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * dx
            val y = yOf(v)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = Palette.Accent,
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

/** Completion, not streaks. A missed day must cost nothing. */
@Composable
fun DotStrip(days: List<Boolean>) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
        days.forEach { logged ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .background(
                        if (logged) Palette.Accent else Palette.Line,
                        RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}
