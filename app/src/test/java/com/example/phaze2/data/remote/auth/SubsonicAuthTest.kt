package com.example.phaze2.data.remote.auth

import com.example.phaze2.data.local.entity.SongEntity
import com.example.phaze2.data.mapper.toEntity
import com.example.phaze2.data.model.DownloadState
import com.example.phaze2.data.remote.dto.Child
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubsonicAuthTest {

    @Test
    fun `token is md5 of password plus salt`() {
        // Known vector: MD5("sesame" + "c19c56ebf2a1c46e") — verifies both the
        // concatenation order and the lowercase-hex formatting.
        assertEquals(
            "a1f0115d88383455bdd79df9d4b60500",
            SubsonicAuth.computeToken("sesame", "c19c56ebf2a1c46e"),
        )
    }

    @Test
    fun `salt is 16 lowercase hex chars`() {
        repeat(50) {
            val salt = SubsonicAuth.generateSalt()
            assertEquals(16, salt.length)
            assertTrue(salt.all { it in "0123456789abcdef" })
        }
    }

    @Test
    fun `token auth config and legacy config differ`() {
        val tokenAuth = ServerAuthConfig.tokenAuth("https://x.example", "u", "tok", "salt")
        assertFalse(tokenAuth.useLegacyAuth)
        assertEquals("tok", tokenAuth.token)
        assertEquals("salt", tokenAuth.salt)

        val legacy = ServerAuthConfig.legacy("https://x.example", "u", "pw")
        assertTrue(legacy.useLegacyAuth)
        assertEquals("pw", legacy.legacyPassword)
        assertEquals(null, legacy.token)
    }
}

class SubsonicMappersTest {

    @Test
    fun `child maps to song and keeps local download state`() {
        val child = Child(
            id = "s1",
            title = "Bohemian Rhapsody",
            artist = "Queen",
            suffix = "mp3",
            duration = 354,
            size = 8_000_000,
            track = 1,
        )
        val existing = SongEntity(
            id = "s1",
            title = "stale",
            albumId = "al-1",
            artistId = "ar-1",
            artistName = "stale",
            downloadState = DownloadState.DOWNLOADED,
            localPath = "/data/user/0/.../files/downloads/s1.mp3",
            starred = true,
        )

        val song = child.toEntity(albumId = "al-1", artistId = "ar-1", existing = existing)

        assertEquals("s1", song.id)
        assertEquals("Bohemian Rhapsody", song.title)
        assertEquals("al-1", song.albumId)
        assertEquals("ar-1", song.artistId)
        assertEquals(354, song.duration)
        assertEquals("mp3", song.format)
        // Local-only state survives a re-sync:
        assertEquals(DownloadState.DOWNLOADED, song.downloadState)
        assertEquals(existing.localPath, song.localPath)
        assertTrue(song.starred)
    }

    @Test
    fun `child without existing row defaults to not downloaded`() {
        val song = Child(id = "s2", title = "T", artist = "A").toEntity(albumId = "al-2")
        assertEquals(DownloadState.NONE, song.downloadState)
        assertEquals(null, song.localPath)
        assertFalse(song.starred)
        assertEquals("al-2", song.albumId)
    }
}
