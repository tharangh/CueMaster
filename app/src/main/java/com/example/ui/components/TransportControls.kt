package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AccentYellow
import com.example.ui.theme.CueMarkerGreen
import com.example.ui.theme.LoopBorderColor
import com.example.ui.theme.PlayheadColor
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WaveformPlayed

@Composable
fun TransportControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onRewind5s: () -> Unit,
    onForward5s: () -> Unit,
    onPrevCue: () -> Unit,
    onNextCue: () -> Unit,
    onAddCue: () -> Unit,
    isLooping: Boolean,
    onToggleLoop: () -> Unit,
    playbackSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    zoomFactor: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSpeedDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StudioCardBg)
            .padding(12.dp)
            .testTag("transport_controls_panel"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: DAW Zoom Slider Bar (100% to 500%)
        DawZoomBar(
            zoomFactor = zoomFactor,
            onZoomChange = onZoomChange
        )

        // Row 2: Main Playback & Cue Jump Deck (Large touch targets for dancers)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Cue Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onPrevCue() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("prev_cue_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = stringResource(R.string.prev_cue),
                    tint = CueMarkerGreen,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "PREV CUE",
                    color = CueMarkerGreen,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // -5s Rewind Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onRewind5s() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("rewind_5s_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FastRewind,
                    contentDescription = stringResource(R.string.rewind_5s),
                    tint = TextPrimary,
                    modifier = Modifier.size(30.dp)
                )
                Text(
                    text = "-5s",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Central Play / Pause Button (Hero Stage Control - 64dp)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) PlayheadColor else WaveformPlayed)
                    .clickable { onPlayPause() }
                    .testTag("play_pause_button")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                    tint = Color.Black,
                    modifier = Modifier.size(36.dp)
                )
            }

            // +5s Forward Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onForward5s() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("forward_5s_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FastForward,
                    contentDescription = stringResource(R.string.forward_5s),
                    tint = TextPrimary,
                    modifier = Modifier.size(30.dp)
                )
                Text(
                    text = "+5s",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Next Cue Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onNextCue() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("next_cue_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = stringResource(R.string.next_cue),
                    tint = CueMarkerGreen,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "NEXT CUE",
                    color = CueMarkerGreen,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Row 3: Action Buttons: [+ Add Cue], [Loop Section], [Speed: 1.0x]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Add Cue Marker (Signature Green Action)
            FilledTonalButton(
                onClick = onAddCue,
                modifier = Modifier
                    .weight(1.3f)
                    .height(44.dp)
                    .testTag("add_cue_button"),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = CueMarkerGreen,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "+ ADD CUE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            // Loop Section Toggle
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isLooping) LoopBorderColor else StudioCardElevated)
                    .border(
                        width = 1.dp,
                        color = if (isLooping) LoopBorderColor else StudioBorder,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onToggleLoop() }
                    .testTag("loop_section_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = stringResource(R.string.loop_section),
                        tint = if (isLooping) Color.White else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isLooping) "LOOPING" else "LOOP",
                        color = if (isLooping) Color.White else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            // Playback Speed Selector (0.5x, 0.75x, 0.85x, 1.0x, 1.15x, 1.25x)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioCardElevated)
                    .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                    .clickable { showSpeedDialog = true }
                    .testTag("speed_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = stringResource(R.string.speed),
                        tint = if (playbackSpeed != 1.0f) AccentYellow else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${playbackSpeed}x",
                        color = if (playbackSpeed != 1.0f) AccentYellow else TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }

    if (showSpeedDialog) {
        SpeedSelectorDialog(
            currentSpeed = playbackSpeed,
            onSpeedSelected = {
                onSpeedChange(it)
                showSpeedDialog = false
            },
            onDismiss = { showSpeedDialog = false }
        )
    }
}

@Composable
fun DawZoomBar(
    zoomFactor: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(StudioCardElevated)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag("zoom_control_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = { onZoomChange((zoomFactor - 0.5f).coerceAtLeast(1.0f)) },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ZoomOut,
                contentDescription = "Zoom Out",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        Slider(
            value = zoomFactor,
            onValueChange = onZoomChange,
            valueRange = 1.0f..5.0f,
            steps = 7, // 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0
            modifier = Modifier
                .weight(1f)
                .height(28.dp)
                .testTag("zoom_slider"),
            colors = SliderDefaults.colors(
                thumbColor = WaveformPlayed,
                activeTrackColor = WaveformPlayed,
                inactiveTrackColor = StudioBorder
            )
        )

        IconButton(
            onClick = { onZoomChange((zoomFactor + 0.5f).coerceAtMost(5.0f)) },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ZoomIn,
                contentDescription = "Zoom In",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        // Quick Preset Chips (100%, 250%, 500%)
        val zoomPercent = (zoomFactor * 100).toInt()
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(WaveformPlayed.copy(alpha = 0.15f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "${zoomPercent}%",
                color = WaveformPlayed,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SpeedSelectorDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.5f, 0.75f, 0.85f, 1.0f, 1.15f, 1.25f, 1.5f)

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Practice Playback Speed",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Slow down to learn fast routines or drill counts:",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                speeds.forEach { speed ->
                    val isSelected = kotlin.math.abs(currentSpeed - speed) < 0.01f
                    val label = when (speed) {
                        0.5f -> "0.50x — Half Speed (Super Slow)"
                        0.75f -> "0.75x — Slow Drill"
                        0.85f -> "0.85x — Choreo Rehearsal"
                        1.0f -> "1.00x — Normal Speed"
                        1.15f -> "1.15x — Fast Drill"
                        1.25f -> "1.25x — High Energy Drill"
                        1.5f -> "1.50x — Double Time"
                        else -> "${speed}x"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) WaveformPlayed.copy(alpha = 0.2f) else StudioCardElevated)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) WaveformPlayed else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onSpeedSelected(speed) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) WaveformPlayed else TextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = WaveformPlayed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        },
        containerColor = StudioCardBg
    )
}
