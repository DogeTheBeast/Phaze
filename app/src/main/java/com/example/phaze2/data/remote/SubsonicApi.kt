package com.example.phaze2.data.remote

import com.example.phaze2.data.remote.dto.AlbumDetail
import com.example.phaze2.data.remote.dto.AlbumList2
import com.example.phaze2.data.remote.dto.ArtistDetail
import com.example.phaze2.data.remote.dto.Artists
import com.example.phaze2.data.remote.dto.License
import com.example.phaze2.data.remote.dto.PlaylistDetail
import com.example.phaze2.data.remote.dto.Playlists
import com.example.phaze2.data.remote.dto.SearchResults
import com.example.phaze2.data.remote.dto.Songs
import com.example.phaze2.data.remote.dto.StarredItems
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming

/**
 * Retrofit definition of the OpenSubsonic REST API (PLAN.md §4).
 *
 * The base URL is the server root (`https://host/`); paths are relative to the
 * `rest/` endpoint. Authentication parameters are injected by the
 * [com.example.phaze2.data.remote.auth.AuthInterceptor] — never specify `u/t/s/p`
 * here. All endpoints return a typed [SubsonicResponse] decoded by
 * [SubsonicConverterFactory]; binary endpoints return raw [ResponseBody].
 */
interface SubsonicApi {

    // ---- Connectivity & auth validation ----

    @GET("rest/ping")
    suspend fun ping(): SubsonicResponse<Unit>

    @GET("rest/getLicense")
    suspend fun getLicense(): SubsonicResponse<License>

    // ---- Library ----

    /** Library → Artists tab, grouped into alphabetical indexes. */
    @GET("rest/getArtists")
    suspend fun getArtists(): SubsonicResponse<Artists>

    /** Home rails: type ∈ newest | frequent | recent | random | starred | ... */
    @GET("rest/getAlbumList2")
    suspend fun getAlbumList2(
        @Query("type") type: String,
        @Query("size") size: Int? = null,
        @Query("offset") offset: Int? = null,
        @Query("fromYear") fromYear: Int? = null,
        @Query("toYear") toYear: Int? = null,
        @Query("genre") genre: String? = null,
    ): SubsonicResponse<AlbumList2>

    /** Album detail + track list. */
    @GET("rest/getAlbum")
    suspend fun getAlbum(@Query("id") id: String): SubsonicResponse<AlbumDetail>

    /** Artist detail + album list. */
    @GET("rest/getArtist")
    suspend fun getArtist(@Query("id") id: String): SubsonicResponse<ArtistDetail>

    /** Library → Songs tab. size/offset are OpenSubsonic/Navidrome extensions for pagination. */
    @GET("rest/getSongs")
    suspend fun getSongs(
        @Query("genre") genre: String? = null,
        @Query("fromYear") fromYear: Int? = null,
        @Query("toYear") toYear: Int? = null,
        @Query("size") size: Int? = null,
        @Query("offset") offset: Int? = null,
    ): SubsonicResponse<Songs>

    /** Library → Playlists tab. */
    @GET("rest/getPlaylists")
    suspend fun getPlaylists(): SubsonicResponse<Playlists>

    @GET("rest/getPlaylist")
    suspend fun getPlaylist(@Query("id") id: String): SubsonicResponse<PlaylistDetail>

    /** Search screen — grouped artists / albums / songs. */
    @GET("rest/search3")
    suspend fun search3(
        @Query("query") query: String,
        @Query("artistCount") artistCount: Int? = null,
        @Query("artistOffset") artistOffset: Int? = null,
        @Query("albumCount") albumCount: Int? = null,
        @Query("albumOffset") albumOffset: Int? = null,
        @Query("songCount") songCount: Int? = null,
        @Query("songOffset") songOffset: Int? = null,
    ): SubsonicResponse<SearchResults>

    /** Starred items — filters & auto-download rules. */
    @GET("rest/getStarred2")
    suspend fun getStarred2(): SubsonicResponse<StarredItems>

    // ---- Mutations ----

    /** Star songs/albums/artists (at least one of id/albumId/artistId). */
    @GET("rest/star")
    suspend fun star(
        @Query("id") id: String? = null,
        @Query("albumId") albumId: String? = null,
        @Query("artistId") artistId: String? = null,
    ): SubsonicResponse<Unit>

    @GET("rest/unstar")
    suspend fun unstar(
        @Query("id") id: String? = null,
        @Query("albumId") albumId: String? = null,
        @Query("artistId") artistId: String? = null,
    ): SubsonicResponse<Unit>

    /** Report playback to the server. `time` = epoch seconds; `submission=true` marks the final report. */
    @GET("rest/scrobble")
    suspend fun scrobble(
        @Query("id") id: String,
        @Query("time") time: Long? = null,
        @Query("submission") submission: Boolean? = null,
    ): SubsonicResponse<Unit>

    /** Create a playlist (name required for new playlists). */
    @GET("rest/createPlaylist")
    suspend fun createPlaylist(
        @Query("playlistId") playlistId: String? = null,
        @Query("name") name: String,
        @Query("songId") songIds: List<String> = emptyList(),
    ): SubsonicResponse<Unit>

    @GET("rest/updatePlaylist")
    suspend fun updatePlaylist(
        @Query("playlistId") playlistId: String,
        @Query("name") name: String? = null,
        @Query("comment") comment: String? = null,
        @Query("isPublic") isPublic: Boolean? = null,
        @Query("songId") songIds: List<String> = emptyList(),
        @Query("songIndexToRemove") songIndexesToRemove: List<Int> = emptyList(),
    ): SubsonicResponse<Unit>

    @GET("rest/deletePlaylist")
    suspend fun deletePlaylist(@Query("id") id: String): SubsonicResponse<Unit>

    // ---- Binary endpoints ----

    /** Album/artist artwork. */
    @GET("rest/getCoverArt")
    @Streaming
    suspend fun getCoverArt(
        @Query("id") id: String,
        @Query("size") size: Int? = null,
    ): Response<ResponseBody>

    /** Playback stream (also used for the offline download source). */
    @GET("rest/stream")
    @Streaming
    suspend fun stream(
        @Query("id") id: String,
        @Query("maxBitRate") maxBitRate: Int? = null,
        @Query("format") format: String? = null,
        @Query("estimateContentLength") estimateContentLength: Boolean? = null,
    ): Response<ResponseBody>

    /** File download — same bytes as [stream] but with a download disposition header. */
    @GET("rest/download")
    @Streaming
    suspend fun download(@Query("id") id: String): Response<ResponseBody>
}
