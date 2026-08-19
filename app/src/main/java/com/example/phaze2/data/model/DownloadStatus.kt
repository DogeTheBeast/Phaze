package com.example.phaze2.data.model

/**
 * Lifecycle of a download *job* tracked by the download manager (WorkManager).
 *
 * Distinct from [DownloadState]: [DownloadState] lives on the song/album row and
 * reflects the offline availability of the item, while [DownloadStatus] tracks the
 * job record in `download_jobs` (progress, timing, failure reason).
 */
enum class DownloadStatus {
    /** Queued, waiting for a worker slot. */
    PENDING,

    /** A worker is actively streaming bytes. */
    RUNNING,

    /** File fully written and renamed into place. */
    COMPLETED,

    /** Worker failed (network, storage, auth...). See `failureReason`. */
    FAILED,

    /** User cancelled; partial temp file must be cleaned up. */
    CANCELLED,
}
