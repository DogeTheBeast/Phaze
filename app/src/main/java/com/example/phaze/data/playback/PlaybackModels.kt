package com.example.phaze.data.playback

/** A track in the playback queue (lightweight view of a MediaItem). */
data class QueueTrack(
    val id: String,
    val title: String,
    val artist: String = "",
    val durationSec: Long = 0,
    val coverArtUrl: String? = null,
)

/** Reactive snapshot of the active playback session (PLAN.md §7). */
data class PlaybackUiState(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val coverArtUrl: String? = null,
    val isPlaying: Boolean = false,
    val isShuffle: Boolean = false,
    val isRepeat: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val currentIndex: Int = -1,
    val hasCurrent: Boolean = false,
    val queue: List<QueueTrack> = emptyList(),
)
