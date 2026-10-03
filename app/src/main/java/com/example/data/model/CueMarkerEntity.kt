package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cue_markers")
data class CueMarkerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackId: Long,
    val timestampMs: Long,
    val label: String,
    val colorHex: String = "#00E676", // Green cue marks as specified
    val orderIndex: Int = 0
)
