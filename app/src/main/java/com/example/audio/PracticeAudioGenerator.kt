package com.example.audio

import android.content.Context
import com.example.data.model.CueMarkerEntity
import com.example.data.model.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object PracticeAudioGenerator {

    suspend fun generatePresetTracksIfMissing(context: Context): List<Pair<TrackEntity, List<CueMarkerEntity>>> =
        withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, "preset_tracks").apply { mkdirs() }
            val results = mutableListOf<Pair<TrackEntity, List<CueMarkerEntity>>>()

            // Preset 1: Urban Hip-Hop 8-Count (95 BPM)
            val file1 = File(dir, "hiphop_95bpm.wav")
            if (!file1.exists() || file1.length() < 1000) {
                generateBeatTrack(
                    file = file1,
                    bpm = 95,
                    bars = 16, // 16 bars of 4/4 = 64 beats = 40.42 seconds
                    sampleRate = 22050,
                    style = TrackStyle.HIP_HOP
                )
            }
            val duration1Ms = 40421L
            val peaks1 = generateWaveformPeaks(95, 16, duration1Ms, TrackStyle.HIP_HOP)
            val track1 = TrackEntity(
                title = "Urban Hip-Hop Choreo",
                artist = "CueMaster Studio • 95 BPM",
                uri = file1.absolutePath,
                durationMs = duration1Ms,
                waveformPointsCsv = peaks1.joinToString(",") { "%.3f".format(it) },
                isPreset = true,
                bpm = 95
            )
            // 8-count = 8 beats = 5052 ms
            val beatMs1 = (60_000.0 / 95).toLong()
            val cues1 = listOf(
                CueMarkerEntity(0, 0, 0L, "Intro 8-Count", "#00E676", 0),
                CueMarkerEntity(0, 0, beatMs1 * 8, "Set 1: Bounce & Groove", "#00E676", 1),
                CueMarkerEntity(0, 0, beatMs1 * 16, "Set 2: Footwork Combo", "#00E676", 2),
                CueMarkerEntity(0, 0, beatMs1 * 24, "Beat Drop / Solo Break", "#00E676", 3),
                CueMarkerEntity(0, 0, beatMs1 * 32, "Set 3: Formation Switch", "#00E676", 4),
                CueMarkerEntity(0, 0, beatMs1 * 48, "Ending Freeze Pose", "#00E676", 5)
            )
            results.add(Pair(track1, cues1))

            // Preset 2: Street Funk & Breakbeat (112 BPM)
            val file2 = File(dir, "breakbeat_112bpm.wav")
            if (!file2.exists() || file2.length() < 1000) {
                generateBeatTrack(
                    file = file2,
                    bpm = 112,
                    bars = 16, // 16 bars of 4/4 = 64 beats = 34.28 seconds
                    sampleRate = 22050,
                    style = TrackStyle.BREAKBEAT
                )
            }
            val duration2Ms = 34285L
            val peaks2 = generateWaveformPeaks(112, 16, duration2Ms, TrackStyle.BREAKBEAT)
            val track2 = TrackEntity(
                title = "Street Funk & Breakbeat",
                artist = "CueMaster Studio • 112 BPM",
                uri = file2.absolutePath,
                durationMs = duration2Ms,
                waveformPointsCsv = peaks2.joinToString(",") { "%.3f".format(it) },
                isPreset = true,
                bpm = 112
            )
            val beatMs2 = (60_000.0 / 112).toLong()
            val cues2 = listOf(
                CueMarkerEntity(0, 0, 0L, "Ready / Count In (5,6,7,8)", "#00E676", 0),
                CueMarkerEntity(0, 0, beatMs2 * 8, "Verse: Top Rock", "#00E676", 1),
                CueMarkerEntity(0, 0, beatMs2 * 16, "Chorus: Floor Lock & Pop", "#00E676", 2),
                CueMarkerEntity(0, 0, beatMs2 * 24, "Speed Choreo Drill", "#00E676", 3),
                CueMarkerEntity(0, 0, beatMs2 * 32, "Outro 8-Count", "#00E676", 4)
            )
            results.add(Pair(track2, cues2))

            results
        }

    enum class TrackStyle { HIP_HOP, BREAKBEAT }

    private fun generateBeatTrack(
        file: File,
        bpm: Int,
        bars: Int,
        sampleRate: Int,
        style: TrackStyle
    ) {
        val beatsPerBar = 4
        val totalBeats = bars * beatsPerBar
        val secondsPerBeat = 60.0 / bpm
        val totalSeconds = totalBeats * secondsPerBeat
        val totalSamples = (totalSeconds * sampleRate).toInt()

        val pcm = ShortArray(totalSamples)

        val samplesPerBeat = (secondsPerBeat * sampleRate).toInt()
        val random = java.util.Random(42)

        for (sampleIdx in 0 until totalSamples) {
            val t = sampleIdx.toDouble() / sampleRate
            val beatIndex = (sampleIdx / samplesPerBeat)
            val beatFraction = (sampleIdx % samplesPerBeat).toDouble() / samplesPerBeat
            val timeInBeat = beatFraction * secondsPerBeat
            val barIndex = beatIndex / 4
            val beatInBar = beatIndex % 4 // 0, 1, 2, 3

            var mixedSample = 0.0

            // 1. Kick Drum (Beats 0 and 2.5 for Hip-hop; 0 and 2 for Breakbeat)
            val isKickBeat = when (style) {
                TrackStyle.HIP_HOP -> beatInBar == 0 || (beatInBar == 2 && beatFraction < 0.6)
                TrackStyle.BREAKBEAT -> beatInBar == 0 || beatInBar == 2 || (beatInBar == 1 && beatFraction >= 0.5)
            }
            if (isKickBeat && timeInBeat < 0.28) {
                val decay = exp(-timeInBeat * 16.0)
                val freq = 130.0 * exp(-timeInBeat * 28.0) + 45.0
                mixedSample += sin(2.0 * PI * freq * timeInBeat) * decay * 0.85
            }

            // 2. Snare Drum (Crisp snappy on Beats 1 and 3)
            val isSnareBeat = beatInBar == 1 || beatInBar == 3
            if (isSnareBeat && timeInBeat < 0.22) {
                val decay = exp(-timeInBeat * 20.0)
                val noise = (random.nextDouble() * 2.0 - 1.0) * 0.6
                val tone = sin(2.0 * PI * 185.0 * timeInBeat) * 0.4
                mixedSample += (noise + tone) * decay * 0.75
            }

            // 3. Hi-Hat (Every 8th note)
            val eighthFraction = (timeInBeat % (secondsPerBeat / 2.0))
            if (eighthFraction < 0.06) {
                val decay = exp(-eighthFraction * 70.0)
                val noise = (random.nextDouble() * 2.0 - 1.0) * 0.35
                mixedSample += noise * decay
            }

            // 4. Synth Bass / Chords
            val rootFreq = if (barIndex % 2 == 0) 65.41 else 55.0 // C2 or A1
            val bassAmp = 0.35 * exp(-(timeInBeat % (secondsPerBeat * 2)) * 1.5)
            mixedSample += (sin(2.0 * PI * rootFreq * t) + 0.3 * sin(2.0 * PI * rootFreq * 2.0 * t)) * bassAmp

            // 5. Melodic Accent on Bar Start
            if (beatInBar == 0 && timeInBeat < 0.4) {
                val chordFreq = if (barIndex % 4 < 2) 261.63 else 220.0 // C4 or A3
                val chordDecay = exp(-timeInBeat * 4.0)
                mixedSample += sin(2.0 * PI * chordFreq * t) * chordDecay * 0.2
            }

            // Clamp and convert to 16-bit PCM
            val clamped = mixedSample.coerceIn(-1.0, 1.0)
            pcm[sampleIdx] = (clamped * 32767.0).toInt().toShort()
        }

        writeWavFile(file, pcm, sampleRate, channels = 1)
    }

    private fun writeWavFile(file: File, pcmData: ShortArray, sampleRate: Int, channels: Int) {
        val totalAudioLen = pcmData.size * 2L
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * 2

        val header = ByteBuffer.allocate(44).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            put("RIFF".toByteArray())
            putInt(totalDataLen.toInt())
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16) // Subchunk1Size for PCM
            putShort(1.toShort()) // AudioFormat (1 = PCM)
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort((channels * 2).toShort()) // BlockAlign
            putShort(16.toShort()) // BitsPerSample
            put("data".toByteArray())
            putInt(totalAudioLen.toInt())
        }

        FileOutputStream(file).use { out ->
            out.write(header.array())
            val buffer = ByteBuffer.allocate(4096).apply { order(ByteOrder.LITTLE_ENDIAN) }
            for (sample in pcmData) {
                if (buffer.remaining() < 2) {
                    out.write(buffer.array(), 0, buffer.position())
                    buffer.clear()
                }
                buffer.putShort(sample)
            }
            if (buffer.position() > 0) {
                out.write(buffer.array(), 0, buffer.position())
            }
        }
    }

    fun generateWaveformPeaks(
        bpm: Int,
        bars: Int,
        durationMs: Long,
        style: TrackStyle,
        peakCount: Int = 400
    ): FloatArray {
        val peaks = FloatArray(peakCount)
        val msPerPeak = durationMs.toDouble() / peakCount
        val beatMs = 60_000.0 / bpm

        for (i in 0 until peakCount) {
            val timeMs = i * msPerPeak
            val beatNumber = (timeMs / beatMs)
            val beatFraction = (beatNumber % 1.0)
            val beatInBar = (beatNumber.toInt()) % 4

            // Kick or Snare produces prominent peak
            val isKick = beatInBar == 0 || (beatInBar == 2 && beatFraction < 0.5)
            val isSnare = beatInBar == 1 || beatInBar == 3

            var amp = 0.15f
            if (isKick && beatFraction < 0.25) {
                amp += (1.0f - (beatFraction.toFloat() / 0.25f)) * 0.75f
            } else if (isSnare && beatFraction < 0.3) {
                amp += (1.0f - (beatFraction.toFloat() / 0.3f)) * 0.65f
            } else {
                amp += 0.2f * sin(i * 0.2).toFloat().coerceAtLeast(0f)
            }
            // Add subtle noise
            val jitter = ((i * 37) % 19) / 100f
            peaks[i] = (amp + jitter).coerceIn(0.12f, 0.98f)
        }
        return peaks
    }
}
