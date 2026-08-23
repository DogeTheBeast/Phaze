package com.example.phaze.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.phaze.data.local.entity.ArtistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtistDao {

    @Query("SELECT * FROM artists ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<ArtistEntity>>

    @Query("SELECT * FROM artists WHERE id = :id")
    fun observeById(id: String): Flow<ArtistEntity?>

    @Query("SELECT * FROM artists WHERE id = :id")
    suspend fun getById(id: String): ArtistEntity?

    @Query("UPDATE artists SET starred = :starred WHERE id = :id")
    suspend fun setStarred(id: String, starred: Boolean)

    /** Case-insensitive search over artist name. */
    @Query("SELECT * FROM artists WHERE name LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE LIMIT :limit")
    suspend fun search(query: String, limit: Int): List<ArtistEntity>

    @Upsert
    suspend fun upsert(artist: ArtistEntity)

    @Upsert
    suspend fun upsertAll(artists: List<ArtistEntity>)

    @Query("DELETE FROM artists")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM artists")
    suspend fun count(): Int
}
