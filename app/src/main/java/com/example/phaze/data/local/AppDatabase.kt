package com.example.phaze.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.phaze.data.local.dao.AlbumDao
import com.example.phaze.data.local.dao.ArtistDao
import com.example.phaze.data.local.dao.DownloadDao
import com.example.phaze.data.local.dao.PlaylistDao
import com.example.phaze.data.local.dao.ServerDao
import com.example.phaze.data.local.dao.SongDao
import com.example.phaze.data.local.entity.AlbumEntity
import com.example.phaze.data.local.entity.ArtistEntity
import com.example.phaze.data.local.entity.DownloadJobEntity
import com.example.phaze.data.local.entity.PlaylistEntity
import com.example.phaze.data.local.entity.PlaylistSongCrossRef
import com.example.phaze.data.local.entity.ServerEntity
import com.example.phaze.data.local.entity.SongEntity

@Database(
    entities = [
        ServerEntity::class,
        ArtistEntity::class,
        AlbumEntity::class,
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        DownloadJobEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun serverDao(): ServerDao
    abstract fun artistDao(): ArtistDao
    abstract fun albumDao(): AlbumDao
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        /** v1 → v2: artists gain a `starred` column. */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE artists ADD COLUMN starred INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
