package com.example.phaze2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.example.phaze2.data.local.entity.PlaylistEntity
import com.example.phaze2.data.local.entity.PlaylistSongCrossRef
import com.example.phaze2.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

/**
 * Playlist storage plus the playlist → songs join.
 *
 * Member tracks are resolved with a JOIN against the songs table ordered by
 * cross-ref position. `clearSongs` must be called by the repository before
 * re-inserting a playlist's members (no FK cascade, see [PlaylistSongCrossRef]).
 */
@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun observeById(id: String): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getById(id: String): PlaylistEntity?

    @Upsert
    suspend fun upsert(playlist: PlaylistEntity)

    @Upsert
    suspend fun upsertAll(playlists: List<PlaylistEntity>)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM playlists")
    suspend fun clear()

    // ---- Playlist ↔ Song relations ----

    @Query(
        """
        SELECT songs.* FROM songs
        INNER JOIN playlist_song_cross_ref ON songs.id = playlist_song_cross_ref.songId
        WHERE playlist_song_cross_ref.playlistId = :playlistId
        ORDER BY playlist_song_cross_ref.position
        """
    )
    fun observeSongs(playlistId: String): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(crossRefs: List<PlaylistSongCrossRef>)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId")
    suspend fun clearSongs(playlistId: String)

    @Query("DELETE FROM playlist_song_cross_ref")
    suspend fun clearAllCrossRefs()
}
