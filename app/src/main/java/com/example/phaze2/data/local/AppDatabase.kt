package com.example.phaze2.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.phaze2.data.local.dao.AlbumDao
import com.example.phaze2.data.local.dao.ArtistDao
import com.example.phaze2.data.local.dao.DownloadDao
import com.example.phaze2.data.local.dao.PlaylistDao
import com.example.phaze2.data.local.dao.ServerDao
import com.example.phaze2.data.local.dao.SongDao
import com.example.phaze2.data.local.entity.AlbumEntity
import com.example.phaze2.data.local.entity.ArtistEntity
import com.example.phaze2.data.local.entity.DownloadJobEntity
import com.example.phaze2.data.local.entity.PlaylistEntity
import com.example.phaze2.data.local.entity.PlaylistSongCrossRef
import com.example.phaze2.data.local.entity.ServerEntity
import com.example.phaze2.data.local.entity.SongEntity

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
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun serverDao(): ServerDao
    abstract fun artistDao(): ArtistDao
    abstract fun albumDao(): AlbumDao
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun downloadDao(): DownloadDao
}
