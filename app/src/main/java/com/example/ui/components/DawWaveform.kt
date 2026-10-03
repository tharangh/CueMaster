package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CueMarkerEntity
import com.example.ui.theme.CueMarkerGreen
import com.example.ui.theme.LoopBorderColor
import com.example.ui.theme.LoopRegionColor
import com.example.ui.theme.PlayheadColor
import com.example.ui.theme.PlayheadGlow
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WaveformGrid
import com.example.ui.theme.WaveformPlayed
import com.example.ui.theme.WaveformUnplayed
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun DawWaveform(
    peaks: List<Float>,
    currentPositionMs: Long,
    durationMs: Long,
    zoomFactor: Float, // 1.0f to 5.0f
    cues: List<CueMarkerEntity>,
    isPlaying: Boolean,
    isLooping: Boolean,
    loopStartMs: Long,
    loopEndMs: Long,
    onSeek: (Long) -> Unit,
    onCueClick: (CueMarkerEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var isUserScrubbing by remember { mutableStateOf(false) }
    var scrubTimeMs by remember { mutableFloatStateOf(0f) }
    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StudioCardElevated)
            .testTag("daw_waveform_container")
    ) {
        val containerWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(100f)
        val containerHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(100f)
        val totalWaveformWidthPx = containerWidthPx * zoomFactor

        // Auto-follow playhead during playback when zoomed in and user is not scrubbing
        LaunchedEffect(currentPositionMs, zoomFactor, isPlaying) {
            if (isPlaying && !isUserScrubbing && zoomFactor > 1.05f && durationMs > 0) {
                val progress = (currentPositionMs.toFloat() / durationMs).coerceIn(0f, 1f)
                val playheadPx = progress * totalWaveformWidthPx
                val targetScroll = (playheadPx - (containerWidthPx / 2f)).coerceIn(
                    0f,
                    (totalWaveformWidthPx - containerWidthPx).coerceAtLeast(0f)
                )
                scrollState.scrollTo(targetScroll.toInt())
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
        ) {
            Canvas(
                modifier = Modifier
                    .width((this@BoxWithConstraints.maxWidth * zoomFactor))
                    .fillMaxSize()
                    .testTag("waveform_canvas")
                    .pointerInput(totalWaveformWidthPx, durationMs, cues) {
                        detectTapGestures { offset ->
                            val progress = (offset.x / totalWaveformWidthPx).coerceIn(0f, 1f)
                            val targetMs = (progress * durationMs).toLong()

                            // Check if tapped near an existing cue marker
                            val tappedCue = cues.firstOrNull { cue ->
                                val cueProgress = (cue.timestampMs.toFloat() / durationMs).coerceIn(0f, 1f)
                                val cueX = cueProgress * totalWaveformWidthPx
                                abs(cueX - offset.x) < 32f // 32px tolerance
                            }

                            if (tappedCue != null) {
                                onCueClick(tappedCue)
                            } else {
                                onSeek(targetMs)
                            }
                        }
                    }
                    .pointerInput(totalWaveformWidthPx, durationMs) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                isUserScrubbing = true
                                val progress = (offset.x / totalWaveformWidthPx).coerceIn(0f, 1f)
                                val targetMs = (progress * durationMs).toLong()
                                scrubTimeMs = targetMs.toFloat()
                                onSeek(targetMs)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val progress = (change.position.x / totalWaveformWidthPx).coerceIn(0f, 1f)
                                val targetMs = (progress * durationMs).toLong()
                                scrubTimeMs = targetMs.toFloat()
                                onSeek(targetMs)
                            },
                            onDragEnd = {
                                isUserScrubbing = false
                            },
                            onDragCancel = {
                                isUserScrubbing = false
                            }
                        )
                    }
            ) {
                val width = size.width
                val height = size.height
                val safeDuration = durationMs.coerceAtLeast(1L)

                // 1. Draw DAW Grid Background & Ruler Lines
                drawDawGrid(
                    width = width,
                    height = height,
                    durationMs = safeDuration,
                    zoomFactor = zoomFactor,
                    textMeasurer = textMeasurer
                )

                // 2. Draw Active Loop Region Highlight
                if (isLooping && loopEndMs > loopStartMs) {
                    val startX = (loopStartMs.toFloat() / safeDuration) * width
                    val endX = (loopEndMs.toFloat() / safeDuration) * width
                    val loopWidth = (endX - startX).coerceAtLeast(4f)

                    drawRect(
                        color = LoopRegionColor,
                        topLeft = Offset(startX, 0f),
                        size = Size(loopWidth, height)
                    )
                    // Loop boundary lines
                    drawLine(
                        color = LoopBorderColor,
                        start = Offset(startX, 0f),
                        end = Offset(startX, height),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawLine(
                        color = LoopBorderColor,
                        start = Offset(endX, 0f),
                        end = Offset(endX, height),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                // 3. Draw DAW Vertical Waveform Bars
                val playheadProgress = (currentPositionMs.toFloat() / safeDuration).coerceIn(0f, 1f)
                val playheadX = playheadProgress * width

                drawDawWaveformBars(
                    peaks = peaks,
                    width = width,
                    height = height,
                    playheadX = playheadX
                )

                // 4. Draw Green Cue Markers
                drawCueMarkers(
                    cues = cues,
                    width = width,
                    height = height,
                    durationMs = safeDuration,
                    textMeasurer = textMeasurer
                )

                // 5. Draw Playhead (Vertical Needle with glowing cap)
                drawPlayhead(
                    playheadX = playheadX,
                    height = height
                )
            }
        }

        // Floating Scrub Time Badge when user is actively dragging
        if (isUserScrubbing) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .background(PlayheadColor, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = TimeFormatter.formatMs(scrubTimeMs.toLong()),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

private fun DrawScope.drawDawGrid(
    width: Float,
    height: Float,
    durationMs: Long,
    zoomFactor: Float,
    textMeasurer: TextMeasurer
) {
    val rulerHeight = 24.dp.toPx()
    val centerLineY = (height + rulerHeight) / 2f

    // Horizontal Center Baseline
    drawLine(
        color = WaveformGrid,
        start = Offset(0f, centerLineY),
        end = Offset(width, centerLineY),
        strokeWidth = 1.dp.toPx()
    )

    // Ruler bottom divider
    drawLine(
        color = StudioBorder,
        start = Offset(0f, rulerHeight),
        end = Offset(width, rulerHeight),
        strokeWidth = 1.dp.toPx()
    )

    // Dynamic Timecode Intervals based on zoom
    // Zoom 1x: tick every 5s; Zoom 2x: every 2s; Zoom 5x: every 1s or 500ms
    val intervalMs = when {
        zoomFactor >= 4.0f -> 500L
        zoomFactor >= 2.5f -> 1000L
        zoomFactor >= 1.8f -> 2000L
        else -> 5000L
    }

    var time = 0L
    while (time <= durationMs) {
        val x = (time.toFloat() / durationMs) * width

        // Vertical beat/second line
        val isMajor = time % 5000L == 0L
        val lineColor = if (isMajor) WaveformGrid.copy(alpha = 0.25f) else WaveformGrid.copy(alpha = 0.10f)

        drawLine(
            color = lineColor,
            start = Offset(x, rulerHeight),
            end = Offset(x, height),
            strokeWidth = if (isMajor) 1.5.dp.toPx() else 1.dp.toPx()
        )

        // Ruler Tick mark
        val tickHeight = if (isMajor) 12.dp.toPx() else 6.dp.toPx()
        drawLine(
            color = if (isMajor) Color(0xFF8899B5) else Color(0xFF556677),
            start = Offset(x, rulerHeight - tickHeight),
            end = Offset(x, rulerHeight),
            strokeWidth = 1.5.dp.toPx()
        )

        // Timecode text for major ticks
        if (isMajor || (zoomFactor >= 3f && time % 1000L == 0L)) {
            val label = TimeFormatter.formatDuration(time)
            val textLayout = textMeasurer.measure(
                text = label,
                style = TextStyle(
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            )
            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(x + 4f, 4f)
            )
        }

        time += intervalMs
    }
}

private fun DrawScope.drawDawWaveformBars(
    peaks: List<Float>,
    width: Float,
    height: Float,
    playheadX: Float
) {
    if (peaks.isEmpty()) return

    val rulerHeight = 24.dp.toPx()
    val availableHeight = height - rulerHeight
    val centerY = rulerHeight + (availableHeight / 2f)
    val maxBarHalfHeight = (availableHeight / 2f) * 0.92f

    val barCount = peaks.size
    val step = width / barCount.toFloat()
    val barWidth = (step * 0.72f).coerceIn(1.5f, 10f)

    for (i in 0 until barCount) {
        val x = i * step + (step / 2f)
        val peak = peaks[i].coerceIn(0.08f, 1.0f)
        val barHalfHeight = peak * maxBarHalfHeight

        val isPlayed = x <= playheadX
        val barColor = if (isPlayed) WaveformPlayed else WaveformUnplayed

        // Draw symmetrical vertical DAW audio bar
        drawLine(
            color = barColor,
            start = Offset(x, centerY - barHalfHeight),
            end = Offset(x, centerY + barHalfHeight),
            strokeWidth = barWidth,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawCueMarkers(
    cues: List<CueMarkerEntity>,
    width: Float,
    height: Float,
    durationMs: Long,
    textMeasurer: TextMeasurer
) {
    val rulerHeight = 24.dp.toPx()

    cues.forEachIndexed { index, cue ->
        val progress = (cue.timestampMs.toFloat() / durationMs).coerceIn(0f, 1f)
        val cueX = progress * width

        // Signature Green Cue Mark Line
        val cueColor = CueMarkerGreen

        // Subtle glow aura behind cue line
        drawLine(
            color = cueColor.copy(alpha = 0.35f),
            start = Offset(cueX, rulerHeight),
            end = Offset(cueX, height),
            strokeWidth = 4.dp.toPx()
        )

        // Solid green vertical line
        drawLine(
            color = cueColor,
            start = Offset(cueX, rulerHeight),
            end = Offset(cueX, height),
            strokeWidth = 2.dp.toPx()
        )

        // Cue Flag Banner at the top
        val flagHeight = 20.dp.toPx()
        val flagWidth = 32.dp.toPx()

        val flagPath = Path().apply {
            moveTo(cueX, rulerHeight)
            lineTo(cueX + flagWidth, rulerHeight)
            lineTo(cueX + flagWidth - 6.dp.toPx(), rulerHeight + (flagHeight / 2f))
            lineTo(cueX + flagWidth, rulerHeight + flagHeight)
            lineTo(cueX, rulerHeight + flagHeight)
            close()
        }
        drawPath(
            path = flagPath,
            color = cueColor
        )

        // Cue Number Text on flag (e.g. C1, C2)
        val cueLabel = "C${index + 1}"
        val textLayout = textMeasurer.measure(
            text = cueLabel,
            style = TextStyle(
                color = Color.Black,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        )
        drawText(
            textLayoutResult = textLayout,
            topLeft = Offset(cueX + 3.dp.toPx(), rulerHeight + 3.dp.toPx())
        )

        // Bottom anchor dot
        drawCircle(
            color = cueColor,
            radius = 3.dp.toPx(),
            center = Offset(cueX, height - 4.dp.toPx())
        )
    }
}

private fun DrawScope.drawPlayhead(
    playheadX: Float,
    height: Float
) {
    val rulerHeight = 24.dp.toPx()

    // Playhead Glow Aura
    drawLine(
        color = PlayheadGlow,
        start = Offset(playheadX, 0f),
        end = Offset(playheadX, height),
        strokeWidth = 5.dp.toPx()
    )

    // Needle Line
    drawLine(
        color = PlayheadColor,
        start = Offset(playheadX, 0f),
        end = Offset(playheadX, height),
        strokeWidth = 2.dp.toPx()
    )

    // Top Playhead Diamond / Head
    val diamondSize = 7.dp.toPx()
    val headPath = Path().apply {
        moveTo(playheadX, 0f)
        lineTo(playheadX - diamondSize, rulerHeight * 0.7f)
        lineTo(playheadX, rulerHeight)
        lineTo(playheadX + diamondSize, rulerHeight * 0.7f)
        close()
    }
    drawPath(
        path = headPath,
        color = PlayheadColor
    )
}
