package com.example.phaze2.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.phaze2.data.local.entity.SongEntity
import com.example.phaze2.data.model.DownloadState
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE")
    fun observeAll(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE albumId = :albumId ORDER BY track")
    fun observeByAlbum(albumId: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE albumId = :albumId ORDER BY track")
    suspend fun getSongsForAlbum(albumId: String): List<SongEntity>

    @Query("SELECT * FROM songs WHERE id = :id")
    fun observeById(id: String): Flow<SongEntity?>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getById(id: String): SongEntity?

    /** Case-insensitive search across title, artist and album name. */
    @Query(
        """
        SELECT * FROM songs
        WHERE title LIKE '%' || :query || '%'
           OR artistName LIKE '%' || :query || '%'
           OR albumId IN (SELECT id FROM albums WHERE name LIKE '%' || :query || '%')
        ORDER BY title COLLATE NOCASE
        """
    )
    fun search(query: String): Flow<List<SongEntity>>

    /** One-shot local song search (offline / instant path). */
    @Query(
        """
        SELECT * FROM songs
        WHERE title LIKE '%' || :query || '%'
           OR artistName LIKE '%' || :query || '%'
           OR albumId IN (SELECT id FROM albums WHERE name LIKE '%' || :query || '%')
        ORDER BY title COLLATE NOCASE LIMIT :limit
        """
    )
    suspend fun searchSongs(query: String, limit: Int): List<SongEntity>

    /** Offline mode / downloads screen. */
    @Query("SELECT * FROM songs WHERE downloadState = 'DOWNLOADED' ORDER BY title COLLATE NOCASE")
    fun observeDownloaded(): Flow<List<SongEntity>>

    @Upsert
    suspend fun upsert(song: SongEntity)

    @Upsert
    suspend fun upsertAll(songs: List<SongEntity>)

    @Query("UPDATE songs SET starred = :starred WHERE id = :id")
    suspend fun setStarred(id: String, starred: Boolean)

    @Query("UPDATE songs SET downloadState = :state, localPath = :localPath WHERE id = :id")
    suspend fun setDownloadState(id: String, state: DownloadState, localPath: String?)

    @Query("DELETE FROM songs")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun count(): Int
}
