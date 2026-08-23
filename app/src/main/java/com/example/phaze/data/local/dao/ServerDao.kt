package com.example.phaze.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.example.phaze.data.local.entity.ServerEntity
import kotlinx.coroutines.flow.Flow

/**
 * Persists the single active server configuration.
 *
 * Reads are exposed both as cold [Flow]s (for reactive UI like the Setup and
 * Settings screens) and one-shot suspending functions (for workers/usecases).
 */
@Dao
interface ServerDao {

    @Query("SELECT * FROM servers LIMIT 1")
    fun observeActive(): Flow<ServerEntity?>

    @Query("SELECT * FROM servers LIMIT 1")
    suspend fun getActive(): ServerEntity?

    @Query("SELECT * FROM servers")
    fun observeAll(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM servers")
    suspend fun getAll(): List<ServerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(server: ServerEntity)

    @Query("DELETE FROM servers WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM servers")
    suspend fun clear()
}
