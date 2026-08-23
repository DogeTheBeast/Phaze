package com.example.phaze.playback

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.phaze.data.remote.SubsonicApiProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Background playback as a MediaSessionService (PLAN.md §7). Hosting a
 * [MediaSession] on the player makes the system recognize Phaze as a media/audio
 * app: the notification gets proper play/pause + next/previous actions, and
 * playback appears on the lock screen / media carousel. The app controls it
 * through a MediaController (see PlaybackController).
 *
 * Streams via the shared authenticated OkHttpClient, so `stream` requests carry
 * the Subsonic auth params automatically.
 */
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var apiProvider: SubsonicApiProvider

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(
                    OkHttpDataSource.Factory(apiProvider.httpClient) as DataSource.Factory
                )
            )
            .build()
            .apply {
                // Keep CPU/network awake while streaming, play nicely with focus.
                setWakeMode(C.WAKE_MODE_NETWORK)
                setHandleAudioBecomingNoisy(true)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    true,
                )
            }
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player ?: return
        if (!player.playWhenReady || player.playbackState == Player.STATE_ENDED) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
