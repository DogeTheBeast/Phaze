package com.example.phaze2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The active Subsonic/OpenSubsonic server configuration.
 *
 * Only one row is expected (`id = 0`); connecting to a different server replaces it.
 *
 * For **token auth** the raw password is deliberately not persisted — only the
 * derived `token = MD5(password + salt)` and its [salt] are stored (PLAN.md §4).
 * For **legacy auth** (older servers without token support) the password must be
 * retained to authenticate; it is stored here in plaintext until the optional
 * Android Keystore encryption from PLAN.md §13 is implemented.
 */
@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey val id: Long = 0,
    val url: String,
    val username: String,
    val token: String? = null,
    val salt: String? = null,
    val useLegacyAuth: Boolean = false,
    val legacyPassword: String? = null,
    /** Server-reported type, e.g. "Navidrome", "Airsonic", "Gonic". */
    val serverType: String? = null,
)
