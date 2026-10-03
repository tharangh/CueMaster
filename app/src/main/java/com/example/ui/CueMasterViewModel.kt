package com.example.ui

import android.app.Application
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayerController
import com.example.audio.PracticeAudioGenerator
import com.example.audio.WaveformExtractor
import com.example.data.db.CueMasterDatabase
import com.example.data.model.CueMarkerEntity
import com.example.data.model.TrackEntity
import com.example.data.repository.TrackRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CueMasterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TrackRepository
    val playerController: AudioPlayerController = AudioPlayerController(application, viewModelScope)

    val allTracks: StateFlow<List<TrackEntity>>

    private val _currentTrack = MutableStateFlow<TrackEntity?>(null)
    val currentTrack: StateFlow<TrackEntity?> = _currentTrack.asStateFlow()

    private val _cues = MutableStateFlow<List<CueMarkerEntity>>(emptyList())
    val cues: StateFlow<List<CueMarkerEntity>> = _cues.asStateFlow()

    private val _peaks = MutableStateFlow<List<Float>>(emptyList())
    val peaks: StateFlow<List<Float>> = _peaks.asStateFlow()

    private val _zoomFactor = MutableStateFlow(1.0f) // 1.0f to 5.0f
    val zoomFactor: StateFlow<Float> = _zoomFactor.asStateFlow()

    private val _loopingCueId = MutableStateFlow<Long?>(null)
    val loopingCueId: StateFlow<Long?> = _loopingCueId.asStateFlow()

    private val _loopStartMs = MutableStateFlow(0L)
    val loopStartMs: StateFlow<Long> = _loopStartMs.asStateFlow()

    private val _loopEndMs = MutableStateFlow(Long.MAX_VALUE)
    val loopEndMs: StateFlow<Long> = _loopEndMs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Player delegations
    val isPlaying: StateFlow<Boolean> = playerController.isPlaying
    val currentPositionMs: StateFlow<Long> = playerController.currentPositionMs
    val durationMs: StateFlow<Long> = playerController.durationMs
    val playbackSpeed: StateFlow<Float> = playerController.playbackSpeed
    val isLooping: StateFlow<Boolean> = playerController.isLoopingCue

    private var cuesObserverJob: Job? = null

    init {
        val db = CueMasterDatabase.getDatabase(application)
        repository = TrackRepository(db.trackDao(), db.cueDao())

        allTracks = repository.allTracks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch {
            // Initialize default practice dance tracks if DB is empty
            initializePresets()
        }
    }

    private suspend fun initializePresets() {
        val tracks = repository.allTracks
        var hasLoaded = false

        viewModelScope.launch {
            repository.allTracks.collectLatest { list ->
                if (list.isEmpty() && !hasLoaded) {
                    hasLoaded = true
                    _isLoading.value = true
                    try {
                        val presets = PracticeAudioGenerator.generatePresetTracksIfMissing(getApplication())
                        presets.forEach { (track, cueList) ->
                            val trackId = repository.insertTrack(track)
                            cueList.forEach { cue ->
                                repository.insertCue(cue.copy(trackId = trackId))
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        _isLoading.value = false
                    }
                } else if (list.isNotEmpty() && _currentTrack.value == null) {
                    selectTrack(list.first())
                }
            }
        }
    }

    fun selectTrack(track: TrackEntity) {
        _currentTrack.value = track
        _peaks.value = parsePeaks(track.waveformPointsCsv)

        // Load cues for this track
        cuesObserverJob?.cancel()
        cuesObserverJob = viewModelScope.launch {
            repository.getCuesForTrack(track.id).collectLatest { cueList ->
                _cues.value = cueList
            }
        }

        playerController.loadTrack(track.uri, initialPosition = track.lastPositionMs)
    }

    fun loadAudioFromUri(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Importing music & analyzing waveform..."
            try {
                val metadata = WaveformExtractor.importAudioFile(getApplication(), uri)
                val newTrack = TrackEntity(
                    title = metadata.title,
                    artist = metadata.artist,
                    uri = metadata.localFilePath,
                    durationMs = metadata.durationMs,
                    waveformPointsCsv = metadata.peaks.joinToString(",") { "%.3f".format(it) },
                    isPreset = false,
                    bpm = 120
                )
                val trackId = repository.insertTrack(newTrack)
                // Add initial 8-count Cue 1
                repository.insertCue(
                    CueMarkerEntity(
                        trackId = trackId,
                        timestampMs = 0L,
                        label = "Intro 8-Count",
                        colorHex = "#00E676",
                        orderIndex = 0
                    )
                )

                val inserted = newTrack.copy(id = trackId)
                selectTrack(inserted)
                vibrateFeedback()
                _statusMessage.value = "Loaded: ${newTrack.title}"
            } catch (e: Exception) {
                _statusMessage.value = "Failed to load audio: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun togglePlayPause() {
        playerController.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        playerController.seekTo(positionMs)
        _currentTrack.value?.let { track ->
            viewModelScope.launch {
                repository.updateLastPosition(track.id, positionMs)
            }
        }
    }

    fun seekRelative(deltaMs: Long) {
        playerController.seekRelative(deltaMs)
        vibrateFeedback(30)
    }

    fun jumpToCue(cue: CueMarkerEntity) {
        seekTo(cue.timestampMs)
        vibrateFeedback(40)
    }

    fun jumpToPrevCue() {
        val currentPos = currentPositionMs.value
        val cueList = _cues.value
        if (cueList.isEmpty()) {
            seekTo(0L)
            return
        }

        // Find the cue immediately preceding current position (with 500ms threshold)
        val prevCue = cueList.lastOrNull { it.timestampMs < currentPos - 500L }
            ?: cueList.firstOrNull()

        if (prevCue != null) {
            seekTo(prevCue.timestampMs)
            vibrateFeedback(40)
        } else {
            seekTo(0L)
        }
    }

    fun jumpToNextCue() {
        val currentPos = currentPositionMs.value
        val cueList = _cues.value
        if (cueList.isEmpty()) return

        val nextCue = cueList.firstOrNull { it.timestampMs > currentPos + 200L }
        if (nextCue != null) {
            seekTo(nextCue.timestampMs)
            vibrateFeedback(40)
        }
    }

    fun addCueAtCurrentPosition(label: String) {
        val track = _currentTrack.value ?: return
        val currentPos = currentPositionMs.value

        viewModelScope.launch {
            val cue = CueMarkerEntity(
                trackId = track.id,
                timestampMs = currentPos,
                label = label,
                colorHex = "#00E676",
                orderIndex = _cues.value.size
            )
            repository.insertCue(cue)
            vibrateFeedback(60)
            _statusMessage.value = "Added Cue: $label at ${com.example.ui.components.TimeFormatter.formatMs(currentPos)}"
        }
    }

    fun updateCue(cue: CueMarkerEntity, newLabel: String) {
        viewModelScope.launch {
            repository.updateCue(cue.copy(label = newLabel))
            vibrateFeedback(30)
        }
    }

    fun deleteCue(cue: CueMarkerEntity) {
        viewModelScope.launch {
            repository.deleteCue(cue.id)
            if (_loopingCueId.value == cue.id) {
                playerController.toggleLoop(false)
                _loopingCueId.value = null
            }
        }
    }

    fun toggleLoopCueSection(cue: CueMarkerEntity) {
        if (_loopingCueId.value == cue.id && isLooping.value) {
            // Disable loop
            playerController.toggleLoop(false)
            _loopingCueId.value = null
            return
        }

        val cueList = _cues.value
        val cueIndex = cueList.indexOfFirst { it.id == cue.id }
        val startMs = cue.timestampMs
        val nextCue = if (cueIndex >= 0 && cueIndex < cueList.size - 1) cueList[cueIndex + 1] else null
        val endMs = nextCue?.timestampMs ?: durationMs.value

        _loopStartMs.value = startMs
        _loopEndMs.value = endMs
        _loopingCueId.value = cue.id

        playerController.setLoopRange(startMs, endMs, enable = true)
        seekTo(startMs)
        if (!isPlaying.value) {
            playerController.play()
        }
        vibrateFeedback(50)
    }

    fun toggleGeneralLoop() {
        if (isLooping.value) {
            playerController.toggleLoop(false)
            _loopingCueId.value = null
        } else {
            // Find current cue and loop its section
            val currentPos = currentPositionMs.value
            val active = getCurrentActiveCue()
            if (active != null) {
                toggleLoopCueSection(active)
            } else {
                val start = (currentPos - 2500L).coerceAtLeast(0L)
                val end = (currentPos + 2500L).coerceAtMost(durationMs.value)
                _loopStartMs.value = start
                _loopEndMs.value = end
                playerController.setLoopRange(start, end, enable = true)
            }
        }
    }

    fun getCurrentActiveCue(): CueMarkerEntity? {
        val currentPos = currentPositionMs.value
        val cueList = _cues.value
        return cueList.lastOrNull { it.timestampMs <= currentPos + 100L }
    }

    fun setZoomFactor(zoom: Float) {
        _zoomFactor.value = zoom.coerceIn(1.0f, 5.0f)
    }

    fun setPlaybackSpeed(speed: Float) {
        playerController.setSpeed(speed)
    }

    fun deleteTrack(track: TrackEntity) {
        viewModelScope.launch {
            if (_currentTrack.value?.id == track.id) {
                playerController.stop()
                _currentTrack.value = null
            }
            repository.deleteTrack(track.id)
            val remaining = repository.allTracks
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    private fun parsePeaks(csv: String): List<Float> {
        if (csv.isBlank()) return emptyList()
        return try {
            csv.split(',').mapNotNull { it.trim().toFloatOrNull() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun vibrateFeedback(durationMs: Long = 40) {
        try {
            val ctx = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = ctx.getSystemService(VibratorManager::class.java)
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = ctx.getSystemService(Vibrator::class.java)
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (e: Exception) {
            // Ignore if vibration fails
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerController.release()
    }
}
