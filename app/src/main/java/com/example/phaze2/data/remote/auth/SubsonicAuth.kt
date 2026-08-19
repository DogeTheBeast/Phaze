package com.example.phaze2.data.remote.auth

import java.security.MessageDigest
import kotlin.random.Random

/**
 * Subsonic authentication helpers (PLAN.md §4):
 *
 * ```
 * salt  = random string
 * token = MD5(password + salt), lowercase hex
 * ```
 *
 * The client sends `u=<username>&t=<token>&s=<salt>` on every request; the server
 * recomputes MD5(storedPassword + salt) and compares. Because only the derived
 * token and salt are ever persisted, the raw password never touches disk.
 */
object SubsonicAuth {

    /** API version we speak. Bumped to 1.16.1 (OpenSubsonic) — older servers ignore it. */
    const val API_VERSION = "1.16.1"

    /** Stable client identifier reported to the server (`c=` param). */
    const val CLIENT_NAME = "phaze2"

    /** Generates a fresh 16-char hex salt. */
    fun generateSalt(): String = buildString {
        repeat(16) { append(HEX[Random.nextInt(HEX.length)]) }
    }

    /** token = MD5(password + salt), lowercase hex. */
    fun computeToken(password: String, salt: String): String = md5Hex(password + salt)

    fun md5Hex(input: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    private const val HEX = "0123456789abcdef"
}
