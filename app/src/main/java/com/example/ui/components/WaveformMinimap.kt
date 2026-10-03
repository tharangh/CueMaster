package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.CueMarkerEntity
import com.example.ui.theme.CueMarkerGreen
import com.example.ui.theme.PlayheadColor
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.WaveformPlayed
import com.example.ui.theme.WaveformUnplayed

@Composable
fun WaveformMinimap(
    peaks: List<Float>,
    currentPositionMs: Long,
    durationMs: Long,
    zoomFactor: Float,
    cues: List<CueMarkerEntity>,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(StudioCardBg)
            .testTag("waveform_minimap")
            .pointerInput(durationMs) {
                detectTapGestures { offset ->
                    val progress = (offset.x / size.width).coerceIn(0f, 1f)
                    val targetMs = (progress * durationMs).toLong()
                    onSeek(targetMs)
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val safeDuration = durationMs.coerceAtLeast(1L)
        val centerY = height / 2f

        // Draw miniature peaks
        if (peaks.isNotEmpty()) {
            val step = width / peaks.size.toFloat()
            val playheadProgress = (currentPositionMs.toFloat() / safeDuration).coerceIn(0f, 1f)
            val playheadX = playheadProgress * width

            for (i in peaks.indices) {
                val x = i * step + (step / 2f)
                val peak = peaks[i].coerceIn(0.1f, 1.0f)
                val barHalf = peak * (height * 0.4f)
                val color = if (x <= playheadX) WaveformPlayed.copy(alpha = 0.6f) else WaveformUnplayed.copy(alpha = 0.5f)

                drawLine(
                    color = color,
                    start = Offset(x, centerY - barHalf),
                    end = Offset(x, centerY + barHalf),
                    strokeWidth = 1.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Draw cues on minimap
        cues.forEach { cue ->
            val cueProgress = (cue.timestampMs.toFloat() / safeDuration).coerceIn(0f, 1f)
            val cueX = cueProgress * width
            drawLine(
                color = CueMarkerGreen,
                start = Offset(cueX, 0f),
                end = Offset(cueX, height),
                strokeWidth = 1.5.dp.toPx()
            )
        }

        // Visible Viewport window if zoomed in
        if (zoomFactor > 1.05f) {
            val windowWidth = width / zoomFactor
            val centerProgress = (currentPositionMs.toFloat() / safeDuration).coerceIn(0f, 1f)
            val windowX = (centerProgress * width - (windowWidth / 2f)).coerceIn(0f, width - windowWidth)

            drawRect(
                color = Color.White.copy(alpha = 0.12f),
                topLeft = Offset(windowX, 0f),
                size = Size(windowWidth, height)
            )
            drawLine(
                color = StudioBorder,
                start = Offset(windowX, 0f),
                end = Offset(windowX, height),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = StudioBorder,
                start = Offset(windowX + windowWidth, 0f),
                end = Offset(windowX + windowWidth, height),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Playhead indicator
        val playheadProgress = (currentPositionMs.toFloat() / safeDuration).coerceIn(0f, 1f)
        val playheadX = playheadProgress * width
        drawLine(
            color = PlayheadColor,
            start = Offset(playheadX, 0f),
            end = Offset(playheadX, height),
            strokeWidth = 2.dp.toPx()
        )
    }
}
