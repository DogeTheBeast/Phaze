package com.example.phaze.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.phaze.data.local.entity.AlbumEntity
import com.example.phaze.data.local.entity.DownloadJobEntity
import com.example.phaze.data.local.entity.SongEntity
import com.example.phaze.data.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

/**
 * Download job storage (PLAN.md §8). Progress is observed via [observeInProgress]
 * to drive the Downloads screen, and completed sizes feed the storage bar via
 * [observeDownloadedBytes].
 */
@Dao
interface DownloadDao {

    @Query("SELECT * FROM download_jobs WHERE status = 'PENDING' OR status = 'RUNNING' ORDER BY startedAt")
    fun observeInProgress(): Flow<List<DownloadJobEntity>>

    @Query("SELECT * FROM download_jobs WHERE status = 'PENDING' OR status = 'RUNNING' ORDER BY startedAt")
    suspend fun getInProgress(): List<DownloadJobEntity>

    @Query("SELECT * FROM download_jobs WHERE status = 'PENDING' ORDER BY startedAt LIMIT 1")
    suspend fun getNextPending(): DownloadJobEntity?

    @Query("SELECT * FROM download_jobs WHERE songId = :songId")
    suspend fun getBySongId(songId: String): DownloadJobEntity?

    @Query("SELECT * FROM download_jobs WHERE songId = :songId")
    fun observeBySongId(songId: String): Flow<DownloadJobEntity?>

    @Upsert
    suspend fun upsert(job: DownloadJobEntity)

    @Query(
        """
        UPDATE download_jobs SET status = :status, bytesDownloaded = :bytesDownloaded,
            completedAt = :completedAt, failureReason = :failureReason
        WHERE songId = :songId
        """
    )
    suspend fun updateStatus(
        songId: String,
        status: DownloadStatus,
        bytesDownloaded: Long,
        completedAt: Long?,
        failureReason: String?,
    )

    @Query("DELETE FROM download_jobs WHERE songId = :songId")
    suspend fun delete(songId: String)

    @Query("DELETE FROM download_jobs")
    suspend fun clear()

    // ---- Convenience views over the songs/albums tables (PLAN.md §5) ----

    @Query("SELECT * FROM songs WHERE downloadState = 'DOWNLOADED' ORDER BY title COLLATE NOCASE")
    fun observeDownloadedSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE downloadState = 'DOWNLOADED' ORDER BY title COLLATE NOCASE")
    suspend fun getDownloadedSongs(): List<SongEntity>

    @Query("SELECT * FROM albums WHERE downloadState = 'DOWNLOADED' ORDER BY name COLLATE NOCASE")
    fun observeDownloadedAlbums(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums WHERE downloadState = 'DOWNLOADED' ORDER BY name COLLATE NOCASE")
    suspend fun getDownloadedAlbums(): List<AlbumEntity>

    /** Sum of downloaded song sizes in bytes — drives the storage bar. */
    @Query("SELECT COALESCE(SUM(size), 0) FROM songs WHERE downloadState = 'DOWNLOADED'")
    fun observeDownloadedBytes(): Flow<Long>
}
