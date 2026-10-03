package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CueMarkerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CueDao {
    @Query("SELECT * FROM cue_markers WHERE trackId = :trackId ORDER BY timestampMs ASC")
    fun getCuesForTrack(trackId: Long): Flow<List<CueMarkerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCue(cue: CueMarkerEntity): Long

    @Update
    suspend fun updateCue(cue: CueMarkerEntity)

    @Query("DELETE FROM cue_markers WHERE id = :id")
    suspend fun deleteCue(id: Long)

    @Query("DELETE FROM cue_markers WHERE trackId = :trackId")
    suspend fun deleteCuesForTrack(trackId: Long)
}
