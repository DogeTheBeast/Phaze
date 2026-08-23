package com.example.phaze.data.remote

import com.example.phaze.data.remote.dto.AlbumList2
import com.example.phaze.data.remote.dto.Artists
import com.example.phaze.data.remote.dto.SearchResults
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.Retrofit

/**
 * Exercises [SubsonicConverterFactory] against realistic OpenSubsonic JSON
 * (Navidrome-shaped envelopes) without a live server.
 */
class SubsonicApiTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    private val factory = SubsonicConverterFactory(json)
    private val retrofit = Retrofit.Builder().baseUrl("https://example.invalid/").build()

    private fun decode(payloadType: Class<*>, body: String): SubsonicResponse<*> {
        val type = object : ParameterizedType {
            override fun getActualTypeArguments(): Array<Type> = arrayOf(payloadType)
            override fun getRawType(): Type = SubsonicResponse::class.java
            override fun getOwnerType(): Type? = null
        }
        val converter = factory.responseBodyConverter(type, emptyArray(), retrofit)
            ?: error("no converter registered for ${payloadType.simpleName}")
        return converter.convert(body.toResponseBody("application/json".toMediaType())) as SubsonicResponse<*>
    }

    @Test
    fun `getAlbumList2 decodes envelope and payload`() {
        val body = """
            {
              "subsonic-response": {
                "status": "ok",
                "version": "1.16.1",
                "type": "Navidrome",
                "serverVersion": "0.51.0",
                "openSubsonic": true,
                "albumList2": {
                  "album": [
                    {
                      "id": "1",
                      "name": "A Night at the Opera",
                      "artist": "Queen",
                      "artistId": "10",
                      "coverArt": "al-1",
                      "songCount": 12,
                      "duration": 2154,
                      "year": 1975,
                      "created": "2024-01-02T03:04:05Z"
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val response = decode(AlbumList2::class.java, body)
        assertTrue(response.isOk)
        assertEquals("Navidrome", response.serverType)
        assertEquals("0.51.0", response.serverVersion)
        assertEquals(true, response.openSubsonic)

        val album = (response.data as AlbumList2).albumsList.albums.single()
        assertEquals("1", album.id)
        assertEquals("A Night at the Opera", album.name)
        assertEquals("10", album.artistId)
        assertEquals(12, album.songCount)
        assertEquals(1975, album.year)
    }

    @Test
    fun `numeric ids are coerced to strings`() {
        val body = """
            {
              "subsonic-response": {
                "status": "ok",
                "version": "1.16.1",
                "albumList2": {
                  "album": [
                    { "id": 42, "name": "Numeric", "artist": "X", "artistId": 7 }
                  ]
                }
              }
            }
        """.trimIndent()

        val response = decode(AlbumList2::class.java, body)
        val album = (response.data as AlbumList2).albumsList.albums.single()
        assertEquals("42", album.id)
        assertEquals("7", album.artistId)
    }

    @Test
    fun `getArtists decodes index structure`() {
        val body = """
            {
              "subsonic-response": {
                "status": "ok",
                "version": "1.16.1",
                "artists": {
                  "ignoredArticles": "The El La Los Las",
                  "index": [
                    { "name": "A", "artist": [ { "id": "a1", "name": "Adele", "albumCount": 4, "coverArt": "ar-a1" } ] },
                    { "name": "Q", "artist": [ { "id": "a2", "name": "Queen", "albumCount": 15 } ] }
                  ]
                }
              }
            }
        """.trimIndent()

        val response = decode(Artists::class.java, body)
        val artists = (response.data as Artists).artists
        assertEquals("The El La Los Las", artists.ignoredArticles)
        assertEquals(2, artists.index.size)
        assertEquals("Adele", artists.index[0].artist.single().name)
        assertEquals(15, artists.index[1].artist.single().albumCount)
    }

    @Test
    fun `search3 decodes grouped results`() {
        val body = """
            {
              "subsonic-response": {
                "status": "ok",
                "version": "1.16.1",
                "searchResult3": {
                  "artist": [ { "id": "a1", "name": "Artist One" } ],
                  "album": [ { "id": "al1", "name": "Album One", "artist": "Artist One" } ],
                  "song": [
                    {
                      "id": "s1",
                      "title": "Song One",
                      "artist": "Artist One",
                      "album": "Album One",
                      "parent": "al1",
                      "track": 1,
                      "duration": 180,
                      "size": 1000000,
                      "suffix": "mp3",
                      "contentType": "audio/mpeg",
                      "bitRate": 320
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val response = decode(SearchResults::class.java, body)
        val result = (response.data as SearchResults).result
        assertEquals(1, result.artist.size)
        assertEquals(1, result.album.size)
        val song = result.song.single()
        assertEquals("s1", song.id)
        assertEquals(180, song.duration)
        assertEquals("mp3", song.suffix)
        assertEquals("audio/mpeg", song.contentType)
        assertEquals(320, song.bitRate)
    }

    @Test
    fun `failed status throws SubsonicException with server error`() {
        val body = """
            {
              "subsonic-response": {
                "status": "failed",
                "version": "1.16.1",
                "error": { "code": 40, "message": "Wrong username or password" }
              }
            }
        """.trimIndent()

        try {
            decode(AlbumList2::class.java, body)
            fail("expected SubsonicException")
        } catch (e: SubsonicException) {
            assertEquals(40, e.code)
            assertEquals("Wrong username or password", e.message)
        }
    }

    @Test
    fun `ping-style empty payload yields null data`() {
        val body = """{"subsonic-response":{"status":"ok","version":"1.16.1"}}"""
        val response = decode(Unit::class.java, body)
        assertTrue(response.isOk)
        assertNull(response.data)
    }

    @Test
    fun `non-json body throws parse error`() {
        val html = "<html>502 Bad Gateway</html>"
        try {
            decode(AlbumList2::class.java, html)
            fail("expected SubsonicException")
        } catch (e: SubsonicException) {
            assertEquals(SubsonicException.CODE_PARSE_ERROR, e.code)
        }
    }

    @Test
    fun `unknown envelope keys are tolerated`() {
        val body = """
            {
              "subsonic-response": {
                "status": "ok",
                "version": "1.16.1",
                "someFutureOpenSubsonicField": { "x": 1 },
                "albumList2": { "album": [ { "id": "1", "name": "N", "artist": "A" } ] }
              }
            }
        """.trimIndent()

        val response = decode(AlbumList2::class.java, body)
        assertEquals(1, (response.data as AlbumList2).albumsList.albums.size)
    }
}
