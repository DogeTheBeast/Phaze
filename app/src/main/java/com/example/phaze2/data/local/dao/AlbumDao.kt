package com.example.phaze2.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.phaze2.data.local.entity.AlbumEntity
import kotlinx.coroutines.flow.Flow

/**
 * Album storage. The rail queries back the four home rails from PLAN.md §4
 * (`newest` / `frequent` / `recent` / `random`), all served from the local DB.
 */
@Dao
interface AlbumDao {

    @Query("SELECT * FROM albums ORDER BY created DESC")
    fun observeAll(): Flow<List<AlbumEntity>>

    /** "Recently added" rail. */
    @Query("SELECT * FROM albums ORDER BY created DESC LIMIT :limit")
    fun observeRecentlyAdded(limit: Int): Flow<List<AlbumEntity>>

    /** "Most played" rail (fed by getAlbumList2 type=frequent). */
    @Query("SELECT * FROM albums ORDER BY playCount DESC LIMIT :limit")
    fun observeMostPlayed(limit: Int): Flow<List<AlbumEntity>>

    /** "Recently played" rail (fed by getAlbumList2 type=recent). */
    @Query("SELECT * FROM albums ORDER BY lastPlayed DESC LIMIT :limit")
    fun observeRecentlyPlayed(limit: Int): Flow<List<AlbumEntity>>

    /** "Random" rail. */
    @Query("SELECT * FROM albums ORDER BY RANDOM() LIMIT :limit")
    fun observeRandom(limit: Int): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums WHERE id = :id")
    fun observeById(id: String): Flow<AlbumEntity?>

    @Query("SELECT * FROM albums WHERE id = :id")
    suspend fun getById(id: String): AlbumEntity?

    @Query("SELECT * FROM albums WHERE artistId = :artistId ORDER BY year")
    fun observeByArtist(artistId: String): Flow<List<AlbumEntity>>

    /** Offline mode: albums fully downloaded. */
    @Query("SELECT * FROM albums WHERE downloadState = 'DOWNLOADED' ORDER BY name COLLATE NOCASE")
    fun observeDownloaded(): Flow<List<AlbumEntity>>

    /** Case-insensitive search over album or artist name. */
    @Query(
        """
        SELECT * FROM albums
        WHERE name LIKE '%' || :query || '%'
           OR artistName LIKE '%' || :query || '%'
        ORDER BY name COLLATE NOCASE LIMIT :limit
        """
    )
    suspend fun search(query: String, limit: Int): List<AlbumEntity>

    @Upsert
    suspend fun upsert(album: AlbumEntity)

    @Upsert
    suspend fun upsertAll(albums: List<AlbumEntity>)

    @Query("UPDATE albums SET starred = :starred WHERE id = :id")
    suspend fun setStarred(id: String, starred: Boolean)

    @Query("DELETE FROM albums")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM albums")
    suspend fun count(): Int
}
