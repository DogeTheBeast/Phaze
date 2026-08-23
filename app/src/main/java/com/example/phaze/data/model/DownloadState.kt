package com.example.phaze.data.model

/**
 * Lifecycle of an item (song or album) with respect to offline storage.
 *
 * Mirrors the state machine in PLAN.md §8:
 *   NONE ──enqueue──► IN_PROGRESS ──finish──► DOWNLOADED
 *   IN_PROGRESS ──cancel/fail──► NONE
 */
enum class DownloadState {
    /** Not downloaded and not queued. */
    NONE,

    /** Currently downloading (or queued behind an active download). */
    IN_PROGRESS,

    /** File exists locally and is available offline. */
    DOWNLOADED,
}
