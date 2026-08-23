package com.example.phaze.data.remote.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/**
 * Subsonic IDs are strings on most servers (Navidrome) but integers on some
 * (Airsonic, Gonic with certain backends). This serializer coerces either form
 * to a [String] so the rest of the app can treat IDs uniformly.
 */
object SubsonicIdSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("SubsonicId", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val element = (decoder as? JsonDecoder)?.decodeJsonElement()
        return when (element) {
            is JsonPrimitive -> element.content
            is JsonNull -> ""
            null -> decoder.decodeString()
            else -> element.toString()
        }
    }

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }
}

// ---------------------------------------------------------------------------
// Core objects (shared by many endpoints)
// ---------------------------------------------------------------------------

/** Artist in ID3 form: `getArtists`, `search3`, `getStarred2`, artist albums. */
@Serializable
data class ArtistID3(
    @Serializable(with = SubsonicIdSerializer::class) val id: String,
    val name: String = "",
    @SerialName("coverArt") val coverArt: String? = null,
    @SerialName("albumCount") val albumCount: Int = 0,
    val starred: String? = null,
    @SerialName("artistImageUrl") val artistImageUrl: String? = null,
    @SerialName("userRating") val userRating: Int? = null,
    @SerialName("averageRating") val averageRating: Double? = null,
)

/** Album in ID3 form: `getAlbumList2`, `getArtist`, `search3`, `getStarred2`. */
@Serializable
data class AlbumID3(
    @Serializable(with = SubsonicIdSerializer::class) val id: String,
    val name: String = "",
    val artist: String = "",
    @Serializable(with = SubsonicIdSerializer::class)
    @SerialName("artistId") val artistId: String = "",
    @SerialName("coverArt") val coverArt: String? = null,
    @SerialName("songCount") val songCount: Int = 0,
    val duration: Int = 0,
    val year: Int? = null,
    val genre: String? = null,
    val created: String? = null,
    val starred: String? = null,
    @SerialName("playCount") val playCount: Int? = null,
    @SerialName("userRating") val userRating: Int? = null,
    @SerialName("averageRating") val averageRating: Double? = null,
)

/**
 * A track (or directory) — the OpenSubsonic `Child`. Served by `getAlbum`
 * (`song`), `getSongs` (`song`), `search3` (`song`) and `getPlaylist` (`entry`).
 */
@Serializable
data class Child(
    @Serializable(with = SubsonicIdSerializer::class) val id: String,
    /** Album id this track belongs to (present on album/playlist responses). */
    @Serializable(with = SubsonicIdSerializer::class) val parent: String? = null,
    @SerialName("isDir") val isDir: Boolean = false,
    val title: String? = null,
    val album: String? = null,
    val artist: String? = null,
    val track: Int? = null,
    val year: Int? = null,
    val genre: String? = null,
    @SerialName("coverArt") val coverArt: String? = null,
    val size: Long? = null,
    @SerialName("contentType") val contentType: String? = null,
    /** File extension: "mp3", "flac", "ogg", ... */
    val suffix: String? = null,
    val duration: Int? = null,
    @SerialName("bitRate") val bitRate: Int? = null,
    @SerialName("bitDepth") val bitDepth: Int? = null,
    @SerialName("samplingRate") val samplingRate: Int? = null,
    @SerialName("channelCount") val channelCount: Int? = null,
    @SerialName("isVideo") val isVideo: Boolean = false,
    val starred: String? = null,
    @Serializable(with = SubsonicIdSerializer::class)
    @SerialName("albumId") val albumId: String? = null,
    @Serializable(with = SubsonicIdSerializer::class)
    @SerialName("artistId") val artistId: String? = null,
    @SerialName("discNumber") val discNumber: Int? = null,
    val created: String? = null,
)

// ---------------------------------------------------------------------------
// Endpoint payloads. The Subsonic JSON mapping turns the response XML into
// `{ "<container>": ... }` under the envelope; each class below mirrors that key.
// ---------------------------------------------------------------------------

/** `getArtists` → `artists: { ignoredArticles, index: [{ name, artist }] }`. */
@Serializable
data class Artists(
    @SerialName("artists") val artists: ArtistIndexList = ArtistIndexList(),
)

@Serializable
data class ArtistIndexList(
    @SerialName("ignoredArticles") val ignoredArticles: String? = null,
    val index: List<ArtistIndex> = emptyList(),
)

@Serializable
data class ArtistIndex(
    val name: String = "",
    val artist: List<ArtistID3> = emptyList(),
)

/** `getAlbumList2` → `albumList2: [ AlbumID3 ]`. */
@Serializable
data class AlbumList2(
    @SerialName("albumList2") val albumsList: AlbumContainer = AlbumContainer(),
)

@Serializable
data class AlbumContainer(
    @SerialName("album")
    val albums: List<AlbumID3> = emptyList()
)

/** `getAlbum` → `album: { ..., song: [ Child ] }`. */
@Serializable
data class AlbumDetail(
    @SerialName("album") val album: AlbumWithSongs = AlbumWithSongs(),
)

@Serializable
data class AlbumWithSongs(
    @Serializable(with = SubsonicIdSerializer::class) val id: String = "",
    val name: String = "",
    val artist: String? = null,
    @Serializable(with = SubsonicIdSerializer::class)
    @SerialName("artistId") val artistId: String? = null,
    @SerialName("coverArt") val coverArt: String? = null,
    @SerialName("songCount") val songCount: Int = 0,
    val duration: Int = 0,
    val year: Int? = null,
    val genre: String? = null,
    val created: String? = null,
    val starred: String? = null,
    @SerialName("playCount") val playCount: Int? = null,
    val song: List<Child> = emptyList(),
)

/** `getArtist` → `artist: { ..., album: [ AlbumID3 ] }`. */
@Serializable
data class ArtistDetail(
    @SerialName("artist") val artist: ArtistWithAlbums = ArtistWithAlbums(),
)

@Serializable
data class ArtistWithAlbums(
    @Serializable(with = SubsonicIdSerializer::class) val id: String = "",
    val name: String = "",
    @SerialName("coverArt") val coverArt: String? = null,
    @SerialName("albumCount") val albumCount: Int = 0,
    @SerialName("userRating") val userRating: Int? = null,
    @SerialName("averageRating") val averageRating: Double? = null,
    val album: List<AlbumID3> = emptyList(),
)

/** `getSongs` → `songs: { song: [ Child ] }`. */
@Serializable
data class Songs(
    @SerialName("songs") val songs: SongList = SongList(),
)

@Serializable
data class SongList(
    @SerialName("song") val song: List<Child> = emptyList(),
)

/** `getPlaylists` → `playlists: { playlist: [ PlaylistInfo ] }`. */
@Serializable
data class Playlists(
    @SerialName("playlists") val playlists: PlaylistList = PlaylistList(),
)

@Serializable
data class PlaylistList(
    @SerialName("playlist") val playlist: List<PlaylistInfo> = emptyList(),
)

@Serializable
data class PlaylistInfo(
    @Serializable(with = SubsonicIdSerializer::class) val id: String = "",
    val name: String? = null,
    val comment: String? = null,
    val owner: String? = null,
    @SerialName("public") val isPublic: Boolean = false,
    @SerialName("songCount") val songCount: Int = 0,
    val duration: Int = 0,
    val created: String? = null,
    val changed: String? = null,
    @SerialName("coverArt") val coverArt: String? = null,
)

/** `getPlaylist` → `playlist: { ..., entry: [ Child ] }`. */
@Serializable
data class PlaylistDetail(
    @SerialName("playlist") val playlist: PlaylistWithSongs = PlaylistWithSongs(),
)

@Serializable
data class PlaylistWithSongs(
    @Serializable(with = SubsonicIdSerializer::class) val id: String = "",
    val name: String? = null,
    val comment: String? = null,
    val owner: String? = null,
    @SerialName("public") val isPublic: Boolean = false,
    @SerialName("songCount") val songCount: Int = 0,
    val duration: Int = 0,
    val created: String? = null,
    val changed: String? = null,
    @SerialName("coverArt") val coverArt: String? = null,
    val entry: List<Child> = emptyList(),
)

/** `search3` → `searchResult3: { artist, album, song }`. */
@Serializable
data class SearchResults(
    @SerialName("searchResult3") val result: SearchResult3 = SearchResult3(),
)

@Serializable
data class SearchResult3(
    val artist: List<ArtistID3> = emptyList(),
    val album: List<AlbumID3> = emptyList(),
    val song: List<Child> = emptyList(),
)

/** `getStarred2` → `starred2: { artist, album, song }`. */
@Serializable
data class StarredItems(
    @SerialName("starred2") val starred: Starred2 = Starred2(),
)

@Serializable
data class Starred2(
    val artist: List<ArtistID3> = emptyList(),
    val album: List<AlbumID3> = emptyList(),
    val song: List<Child> = emptyList(),
)

/** `getLicense` → `license: { valid, email, ... }`. */
@Serializable
data class License(
    @SerialName("license") val license: LicenseInfo = LicenseInfo(),
)

@Serializable
data class LicenseInfo(
    val valid: Boolean = false,
    val email: String? = null,
    @SerialName("licenseExpires") val licenseExpires: String? = null,
    @SerialName("trialExpires") val trialExpires: String? = null,
)
