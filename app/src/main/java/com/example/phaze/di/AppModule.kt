package com.example.phaze.di

import android.content.Context
import androidx.room.Room
import com.example.phaze.data.local.AppDatabase
import com.example.phaze.data.local.dao.AlbumDao
import com.example.phaze.data.local.dao.ArtistDao
import com.example.phaze.data.local.dao.PlaylistDao
import com.example.phaze.data.local.dao.ServerDao
import com.example.phaze.data.local.dao.SongDao
import com.example.phaze.data.remote.SubsonicApiProvider
import com.example.phaze.data.remote.auth.MutableServerAuthProvider
import com.example.phaze.data.remote.auth.ServerAuthProvider
import coil.ImageLoader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "phaze.db"
        ).addMigrations(AppDatabase.MIGRATION_1_2).build()
    }

		@Provides
		fun provideServerDao(database: AppDatabase): ServerDao {
				return database.serverDao()
		}

    @Provides
    @Singleton
    fun provideAlbumDao(database: AppDatabase): AlbumDao {
        return database.albumDao()
    }

    @Provides
    @Singleton
    fun provideArtistDao(database: AppDatabase): ArtistDao {
        return database.artistDao()
    }

    @Provides
    @Singleton
    fun provideSongDao(database: AppDatabase): SongDao {
        return database.songDao()
    }

    @Provides
    @Singleton
    fun providePlaylistDao(database: AppDatabase): PlaylistDao {
        return database.playlistDao()
    }

    /**
     * Shared JSON configuration for the Subsonic API: tolerate unknown/extended
     * OpenSubsonic fields and coerce `null`/missing to declared defaults.
     */
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    /** Mutable holder the settings layer writes the active server auth into. */
    @Provides
    @Singleton
    fun provideMutableServerAuthProvider(): MutableServerAuthProvider = MutableServerAuthProvider()

    @Provides
    @Singleton
    fun provideServerAuthProvider(impl: MutableServerAuthProvider): ServerAuthProvider = impl

    /**
     * Coil image loader backed by the shared authenticated OkHttpClient, so
     * `getCoverArt` requests carry the Subsonic auth params automatically.
     */
    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        apiProvider: SubsonicApiProvider,
    ): ImageLoader = ImageLoader.Builder(context)
        .okHttpClient(apiProvider.httpClient)
        .crossfade(true)
        .build()
}
