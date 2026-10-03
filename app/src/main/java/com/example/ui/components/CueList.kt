package com.example.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.theme.CueMarkerGreen
import com.example.ui.theme.LoopBorderColor
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WaveformPlayed

@Composable
fun CueList(
    cues: List<CueMarkerEntity>,
    activeCueId: Long?,
    isLooping: Boolean,
    loopingCueId: Long?,
    onJumpToCue: (CueMarkerEntity) -> Unit,
    onLoopCueSection: (CueMarkerEntity) -> Unit,
    onEditCue: (CueMarkerEntity) -> Unit,
    onDeleteCue: (CueMarkerEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StudioCardBg)
            .padding(12.dp)
            .testTag("cue_list_container")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = CueMarkerGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "CHOREOGRAPHY CUE MARKS",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(CueMarkerGreen.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${cues.size} Cues",
                    color = CueMarkerGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (cues.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioCardElevated)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No cue marks yet",
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Tap '+ ADD CUE' during music to mark 8-counts & drops!",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cue_list_items"),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                cues.forEachIndexed { index, cue ->
                    val isActive = cue.id == activeCueId
                    val isSectionLooping = isLooping && (cue.id == loopingCueId)

                    CueItemRow(
                        index = index + 1,
                        cue = cue,
                        isActive = isActive,
                        isSectionLooping = isSectionLooping,
                        onJump = { onJumpToCue(cue) },
                        onLoop = { onLoopCueSection(cue) },
                        onEdit = { onEditCue(cue) },
                        onDelete = { onDeleteCue(cue) }
                    )
                }
            }
        }
    }
}

@Composable
fun CueItemRow(
    index: Int,
    cue: CueMarkerEntity,
    isActive: Boolean,
    isSectionLooping: Boolean,
    onJump: () -> Unit,
    onLoop: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (isActive || isSectionLooping) 1.5.dp else 1.dp,
                color = when {
                    isSectionLooping -> LoopBorderColor
                    isActive -> CueMarkerGreen
                    else -> StudioBorder
                },
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onJump() }
            .testTag("cue_item_$index"),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) CueMarkerGreen.copy(alpha = 0.08f) else StudioCardElevated
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Cue Number badge + Timestamp + Label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Green Cue Badge
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(CueMarkerGreen)
                        .clickable { onJump() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "C$index",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = cue.label,
                        color = if (isActive) CueMarkerGreen else TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = TimeFormatter.formatMs(cue.timestampMs),
                        color = if (isActive) CueMarkerGreen.copy(alpha = 0.8f) else TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Right: Actions (Loop this section, Edit label, Delete cue)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Loop Section Button
                IconButton(
                    onClick = onLoop,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("cue_loop_button_$index")
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Loop Section",
                        tint = if (isSectionLooping) LoopBorderColor else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Edit Button
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("cue_edit_button_$index")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Cue",
                        tint = TextSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("cue_delete_button_$index")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Cue",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}
