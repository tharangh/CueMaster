package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artist: String = "Dance Track",
    val uri: String,
    val durationMs: Long,
    val waveformPointsCsv: String = "",
    val isPreset: Boolean = false,
    val bpm: Int = 120,
    val lastPositionMs: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)
