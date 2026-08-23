package com.example.phaze.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.phaze.data.model.DownloadStatus

/**
 * Bookkeeping record for one download job (one song), kept in sync with the
 * WorkManager work that actually streams the file (PLAN.md §8).
 */
@Entity(tableName = "download_jobs")
data class DownloadJobEntity(
    @PrimaryKey val songId: String,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val failureReason: String? = null,
)
