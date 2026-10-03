package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.CueMarkerEntity
import com.example.ui.components.AddCueDialog
import com.example.ui.components.CueList
import com.example.ui.components.DawWaveform
import com.example.ui.components.EditCueDialog
import com.example.ui.components.TimeDisplay
import com.example.ui.components.TrackSelectDialog
import com.example.ui.components.TransportControls
import com.example.ui.components.WaveformMinimap
import com.example.ui.theme.CueMarkerGreen
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WaveformPlayed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CueMasterScreen(
    viewModel: CueMasterViewModel,
    modifier: Modifier = Modifier
) {
    val tracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val cues by viewModel.cues.collectAsStateWithLifecycle()
    val peaks by viewModel.peaks.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPos by viewModel.currentPositionMs.collectAsStateWithLifecycle()
    val duration by viewModel.durationMs.collectAsStateWithLifecycle()
    val zoomFactor by viewModel.zoomFactor.collectAsStateWithLifecycle()
    val speed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val isLooping by viewModel.isLooping.collectAsStateWithLifecycle()
    val loopStartMs by viewModel.loopStartMs.collectAsStateWithLifecycle()
    val loopEndMs by viewModel.loopEndMs.collectAsStateWithLifecycle()
    val loopingCueId by viewModel.loopingCueId.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    var showAddCueDialog by remember { mutableStateOf(false) }
    var cueToEdit by remember { mutableStateOf<CueMarkerEntity?>(null) }
    var showTrackSelectDialog by remember { mutableStateOf(false) }

    // Audio file picker (MP3 or WAV)
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadAudioFromUri(it) }
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("cue_master_screen"),
        containerColor = StudioDarkBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CueMarkerGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "CueMaster",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentTrack?.title ?: "Dance Practice Player",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    // Track Library Button
                    IconButton(
                        onClick = { showTrackSelectDialog = true },
                        modifier = Modifier.testTag("top_library_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = "Library",
                            tint = WaveformPlayed
                        )
                    }

                    // Add Music Button
                    FilledTonalButton(
                        onClick = { audioPickerLauncher.launch("audio/*") },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = WaveformPlayed,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .height(34.dp)
                            .testTag("top_add_music_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.add_music),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StudioDarkBg
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Loading indicator banner
            AnimatedVisibility(visible = isLoading, enter = fadeIn(), exit = fadeOut()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudioCardElevated)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = CueMarkerGreen,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Processing audio track...",
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                }
            }

            // 1. Time Display Card
            TimeDisplay(
                currentPositionMs = currentPos,
                durationMs = duration,
                currentCue = viewModel.getCurrentActiveCue(),
                playbackSpeed = speed,
                bpm = currentTrack?.bpm ?: 120,
                isLooping = isLooping
            )

            // 2. DAW Waveform Visualizer & Minimap
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioCardBg)
                    .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Header of Waveform: Zoom percentage & Cue Count
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DAW TIMELINE WAVEFORM",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Zoom ${(zoomFactor * 100).toInt()}% • Drag or Tap to Seek",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }

                // Interactive Waveform
                DawWaveform(
                    peaks = peaks,
                    currentPositionMs = currentPos,
                    durationMs = duration,
                    zoomFactor = zoomFactor,
                    cues = cues,
                    isPlaying = isPlaying,
                    isLooping = isLooping,
                    loopStartMs = loopStartMs,
                    loopEndMs = loopEndMs,
                    onSeek = { viewModel.seekTo(it) },
                    onCueClick = { viewModel.jumpToCue(it) },
                    modifier = Modifier.height(160.dp)
                )

                // Full-track Minimap Strip
                WaveformMinimap(
                    peaks = peaks,
                    currentPositionMs = currentPos,
                    durationMs = duration,
                    zoomFactor = zoomFactor,
                    cues = cues,
                    onSeek = { viewModel.seekTo(it) }
                )
            }

            // 3. Transport & Practice Controls (Play/Pause, -5s, +5s, Prev/Next Cue, Add Cue, Speed, Zoom)
            TransportControls(
                isPlaying = isPlaying,
                onPlayPause = { viewModel.togglePlayPause() },
                onRewind5s = { viewModel.seekRelative(-5000L) },
                onForward5s = { viewModel.seekRelative(5000L) },
                onPrevCue = { viewModel.jumpToPrevCue() },
                onNextCue = { viewModel.jumpToNextCue() },
                onAddCue = { showAddCueDialog = true },
                isLooping = isLooping,
                onToggleLoop = { viewModel.toggleGeneralLoop() },
                playbackSpeed = speed,
                onSpeedChange = { viewModel.setPlaybackSpeed(it) },
                zoomFactor = zoomFactor,
                onZoomChange = { viewModel.setZoomFactor(it) }
            )

            // 4. Cue Markers List (Choreography 8-count points, labels, loop, jump)
            CueList(
                cues = cues,
                activeCueId = viewModel.getCurrentActiveCue()?.id,
                isLooping = isLooping,
                loopingCueId = loopingCueId,
                onJumpToCue = { viewModel.jumpToCue(it) },
                onLoopCueSection = { viewModel.toggleLoopCueSection(it) },
                onEditCue = { cueToEdit = it },
                onDeleteCue = { viewModel.deleteCue(it) },
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }

    // Add Cue Dialog
    if (showAddCueDialog) {
        AddCueDialog(
            timestampMs = currentPos,
            defaultIndex = cues.size + 1,
            onConfirm = { label ->
                viewModel.addCueAtCurrentPosition(label)
                showAddCueDialog = false
            },
            onDismiss = { showAddCueDialog = false }
        )
    }

    // Edit Cue Dialog
    cueToEdit?.let { cue ->
        EditCueDialog(
            cue = cue,
            onConfirm = { newLabel ->
                viewModel.updateCue(cue, newLabel)
                cueToEdit = null
            },
            onDismiss = { cueToEdit = null }
        )
    }

    // Track Library Dialog
    if (showTrackSelectDialog) {
        TrackSelectDialog(
            tracks = tracks,
            currentTrackId = currentTrack?.id,
            onSelectTrack = { track ->
                viewModel.selectTrack(track)
                showTrackSelectDialog = false
            },
            onAddNewMusic = {
                showTrackSelectDialog = false
                audioPickerLauncher.launch("audio/*")
            },
            onDeleteTrack = { track ->
                viewModel.deleteTrack(track)
            },
            onDismiss = { showTrackSelectDialog = false }
        )
    }
}
