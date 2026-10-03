package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class AudioPlayerController(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var mediaPlayer: MediaPlayer? = null
    private var tickerJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(1L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _isLoopingCue = MutableStateFlow(false)
    val isLoopingCue: StateFlow<Boolean> = _isLoopingCue.asStateFlow()

    private var loopStartMs: Long = 0L
    private var loopEndMs: Long = Long.MAX_VALUE

    private var currentLoadedPath: String? = null

    fun loadTrack(pathOrUri: String, initialPosition: Long = 0L, onPrepared: (() -> Unit)? = null) {
        if (currentLoadedPath == pathOrUri && mediaPlayer != null) {
            seekTo(initialPosition)
            return
        }

        stop()
        mediaPlayer?.release()
        mediaPlayer = null
        currentLoadedPath = pathOrUri

        try {
            val player = MediaPlayer()
            if (pathOrUri.startsWith("content://") || pathOrUri.startsWith("android.resource://")) {
                player.setDataSource(context, Uri.parse(pathOrUri))
            } else {
                val file = File(pathOrUri)
                if (file.exists()) {
                    player.setDataSource(file.absolutePath)
                } else {
                    player.setDataSource(pathOrUri)
                }
            }

            player.setOnPreparedListener { mp ->
                val dur = mp.duration.toLong().coerceAtLeast(1000L)
                _durationMs.value = dur
                applySpeed(_playbackSpeed.value)
                if (initialPosition > 0) {
                    seekTo(initialPosition)
                } else {
                    _currentPositionMs.value = 0L
                }
                onPrepared?.invoke()
            }

            player.setOnCompletionListener {
                if (_isLoopingCue.value && loopEndMs > loopStartMs) {
                    seekTo(loopStartMs)
                    play()
                } else {
                    _isPlaying.value = false
                    stopTicker()
                    _currentPositionMs.value = _durationMs.value
                }
            }

            player.setOnErrorListener { _, what, extra ->
                Log.e("AudioPlayerController", "MediaPlayer error: what=$what extra=$extra")
                _isPlaying.value = false
                stopTicker()
                true
            }

            player.prepareAsync()
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e("AudioPlayerController", "Error preparing media player", e)
        }
    }

    fun play() {
        val player = mediaPlayer ?: return
        try {
            applySpeed(_playbackSpeed.value)
            player.start()
            _isPlaying.value = true
            startTicker()
        } catch (e: Exception) {
            Log.e("AudioPlayerController", "Error starting playback", e)
        }
    }

    fun pause() {
        val player = mediaPlayer ?: return
        try {
            if (player.isPlaying) {
                player.pause()
            }
            _isPlaying.value = false
            stopTicker()
            _currentPositionMs.value = player.currentPosition.toLong()
        } catch (e: Exception) {
            Log.e("AudioPlayerController", "Error pausing playback", e)
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(positionMs: Long) {
        val target = positionMs.coerceIn(0L, _durationMs.value)
        _currentPositionMs.value = target
        try {
            mediaPlayer?.seekTo(target.toInt())
        } catch (e: Exception) {
            Log.e("AudioPlayerController", "Error seeking", e)
        }
    }

    fun seekRelative(deltaMs: Long) {
        val current = _currentPositionMs.value
        seekTo(current + deltaMs)
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        applySpeed(speed)
    }

    private fun applySpeed(speed: Float) {
        val player = mediaPlayer ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val params = player.playbackParams ?: PlaybackParams()
                params.speed = speed
                player.playbackParams = params
            } catch (e: Exception) {
                Log.e("AudioPlayerController", "Error setting playback params", e)
            }
        }
    }

    fun setLoopRange(startMs: Long, endMs: Long, enable: Boolean = true) {
        loopStartMs = startMs.coerceAtLeast(0L)
        loopEndMs = endMs.coerceAtLeast(loopStartMs + 500L)
        _isLoopingCue.value = enable
    }

    fun toggleLoop(enable: Boolean? = null) {
        _isLoopingCue.value = enable ?: !_isLoopingCue.value
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch(Dispatchers.Main) {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let { player ->
                    try {
                        if (player.isPlaying) {
                            val pos = player.currentPosition.toLong()
                            _currentPositionMs.value = pos

                            // Check loop boundary
                            if (_isLoopingCue.value && pos >= loopEndMs) {
                                seekTo(loopStartMs)
                            }
                        }
                    } catch (e: Exception) {
                        // ignore state errors
                    }
                }
                delay(25) // ~40 fps ticker for smooth playhead movement
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    fun stop() {
        pause()
        try {
            mediaPlayer?.stop()
        } catch (e: Exception) {
            // ignore
        }
    }

    fun release() {
        stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
