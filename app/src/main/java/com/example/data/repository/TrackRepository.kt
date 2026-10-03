package com.example.data.repository

import com.example.data.db.CueDao
import com.example.data.db.TrackDao
import com.example.data.model.CueMarkerEntity
import com.example.data.model.TrackEntity
import kotlinx.coroutines.flow.Flow

class TrackRepository(
    private val trackDao: TrackDao,
    private val cueDao: CueDao
) {
    val allTracks: Flow<List<TrackEntity>> = trackDao.getAllTracks()

    fun getTrackById(id: Long): Flow<TrackEntity?> = trackDao.getTrackById(id)

    suspend fun getTrackByIdOnce(id: Long): TrackEntity? = trackDao.getTrackByIdOnce(id)

    fun getCuesForTrack(trackId: Long): Flow<List<CueMarkerEntity>> =
        cueDao.getCuesForTrack(trackId)

    suspend fun insertTrack(track: TrackEntity): Long = trackDao.insertTrack(track)

    suspend fun updateTrack(track: TrackEntity) = trackDao.updateTrack(track)

    suspend fun updateLastPosition(trackId: Long, positionMs: Long) =
        trackDao.updateLastPosition(trackId, positionMs)

    suspend fun deleteTrack(trackId: Long) {
        cueDao.deleteCuesForTrack(trackId)
        trackDao.deleteTrack(trackId)
    }

    suspend fun insertCue(cue: CueMarkerEntity): Long = cueDao.insertCue(cue)

    suspend fun updateCue(cue: CueMarkerEntity) = cueDao.updateCue(cue)

    suspend fun deleteCue(cueId: Long) = cueDao.deleteCue(cueId)
}
