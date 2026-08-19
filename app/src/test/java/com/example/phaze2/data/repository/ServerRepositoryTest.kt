package com.example.phaze2.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServerRepositoryTest {

    @Test
    fun `adds https scheme when missing`() {
        assertEquals(
            "https://music.example.com",
            ServerRepository.normalizeUrl("music.example.com"),
        )
    }

    @Test
    fun `trims whitespace and trailing slashes`() {
        assertEquals(
            "https://music.example.com",
            ServerRepository.normalizeUrl("  https://music.example.com/  "),
        )
    }

    @Test
    fun `keeps http with an explicit scheme and port`() {
        assertEquals(
            "http://192.168.1.5:4533",
            ServerRepository.normalizeUrl("http://192.168.1.5:4533"),
        )
    }

    @Test
    fun `strips trailing rest suffix`() {
        assertEquals(
            "https://music.example.com",
            ServerRepository.normalizeUrl("https://music.example.com/rest"),
        )
    }

    @Test
    fun `keeps subpath but strips trailing slash`() {
        assertEquals(
            "https://music.example.com/navidrome",
            ServerRepository.normalizeUrl("https://music.example.com/navidrome/"),
        )
    }

    @Test
    fun `drops query and fragment`() {
        assertEquals(
            "https://music.example.com",
            ServerRepository.normalizeUrl("https://music.example.com/?utm=1#frag"),
        )
    }

    @Test
    fun `rejects blank and malformed input`() {
        assertNull(ServerRepository.normalizeUrl(""))
        assertNull(ServerRepository.normalizeUrl("   "))
        assertNull(ServerRepository.normalizeUrl("not a url with spaces"))
        assertNull(ServerRepository.normalizeUrl("ftp://example.com"))
    }
}
