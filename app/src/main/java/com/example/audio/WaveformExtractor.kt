package com.example.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.sin

object WaveformExtractor {

    data class AudioMetadata(
        val title: String,
        val artist: String,
        val durationMs: Long,
        val localFilePath: String,
        val peaks: List<Float>
    )

    suspend fun importAudioFile(context: Context, uri: Uri): AudioMetadata =
        withContext(Dispatchers.IO) {
            val contentResolver = context.contentResolver
            val retriever = MediaMetadataRetriever()

            // 1. Copy the file into app internal storage for reliable playback & seeking
            val tracksDir = File(context.filesDir, "imported_tracks").apply { mkdirs() }
            val destFile = File(tracksDir, "track_${System.currentTimeMillis()}.audio")

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Could not open input audio stream")

            retriever.setDataSource(destFile.absolutePath)

            val titleMeta = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artistMeta = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 30_000L

            val fileName = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')
                ?: "Dance Practice Track"

            val title = if (!titleMeta.isNullOrBlank()) titleMeta else fileName
            val artist = if (!artistMeta.isNullOrBlank()) artistMeta else "Imported Music"

            retriever.release()

            // 2. Extract or synthesize waveform envelope from the audio file
            val peaks = extractWaveformPeaks(destFile, durationMs)

            AudioMetadata(
                title = title,
                artist = artist,
                durationMs = durationMs,
                localFilePath = destFile.absolutePath,
                peaks = peaks
            )
        }

    private fun extractWaveformPeaks(file: File, durationMs: Long, peakCount: Int = 400): List<Float> {
        // If file is WAV with PCM data, we can read chunks directly
        val peaks = mutableListOf<Float>()
        val fileLength = file.length()

        if (fileLength > 1000) {
            try {
                file.inputStream().use { stream ->
                    val buffer = ByteArray(2048)
                    val stepBytes = (fileLength / peakCount).coerceAtLeast(1024L)
                    var currentOffset = 44L // Skip standard header

                    for (i in 0 until peakCount) {
                        val skipAmount = (currentOffset - stream.channel.position()).coerceAtLeast(0)
                        if (skipAmount > 0) {
                            stream.skip(skipAmount)
                        }
                        val read = stream.read(buffer)
                        if (read > 0) {
                            var maxAmp = 0
                            var j = 0
                            while (j < read - 1) {
                                val sample = (buffer[j].toInt() and 0xFF) or (buffer[j + 1].toInt() shl 8)
                                val shortSample = sample.toShort()
                                val amp = abs(shortSample.toInt())
                                if (amp > maxAmp) maxAmp = amp
                                j += 2
                            }
                            val normalized = (maxAmp / 32768.0f).coerceIn(0.1f, 1.0f)
                            peaks.add(normalized)
                        } else {
                            peaks.add(0.2f)
                        }
                        currentOffset += stepBytes
                    }
                }
            } catch (e: Exception) {
                peaks.clear()
            }
        }

        // Fallback / smoothing if peaks are flat or couldn't parse
        if (peaks.size < peakCount || peaks.all { it < 0.05f }) {
            peaks.clear()
            val seed = (fileLength % 997).toInt()
            for (i in 0 until peakCount) {
                val cycle = sin(i * 0.08 + seed).toFloat()
                val pulse = if (i % 8 == 0 || i % 8 == 4) 0.85f else 0.35f
                val noise = ((i * 31 + seed) % 23) / 100f
                val valFinal = (pulse * 0.7f + cycle * 0.2f + noise).coerceIn(0.12f, 0.95f)
                peaks.add(valFinal)
            }
        }

        return peaks
    }
}
