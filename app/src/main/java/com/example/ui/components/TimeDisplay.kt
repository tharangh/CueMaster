package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CueMarkerEntity
import com.example.ui.theme.AccentYellow
import com.example.ui.theme.CueMarkerGreen
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WaveformPlayed

@Composable
fun TimeDisplay(
    currentPositionMs: Long,
    durationMs: Long,
    currentCue: CueMarkerEntity?,
    playbackSpeed: Float,
    bpm: Int,
    isLooping: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StudioCardBg)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("time_display_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large High-contrast Time Counter
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.testTag("time_counter_row")
                ) {
                    Text(
                        text = TimeFormatter.formatMs(currentPositionMs, includeMillis = true),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "/ ${TimeFormatter.formatMs(durationMs, includeMillis = true)}",
                        color = TextMuted,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }

                // Badges: BPM & Speed
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isLooping) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF8E24AA), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "LOOP ON",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (playbackSpeed != 1.0f) {
                        Box(
                            modifier = Modifier
                                .background(AccentYellow.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${playbackSpeed}x",
                                color = AccentYellow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(StudioCardBg.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "$bpm BPM",
                            color = WaveformPlayed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Current Cue Section Label
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = CueMarkerGreen,
                    modifier = Modifier.width(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (currentCue != null) "Current: ${currentCue.label}" else "Section: Intro / Unmarked",
                    color = if (currentCue != null) CueMarkerGreen else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}
