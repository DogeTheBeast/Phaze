package com.example.phaze.data.remote

import android.util.Log
import com.example.phaze.data.remote.dto.AlbumDetail
import com.example.phaze.data.remote.dto.AlbumList2
import com.example.phaze.data.remote.dto.ArtistDetail
import com.example.phaze.data.remote.dto.Artists
import com.example.phaze.data.remote.dto.License
import com.example.phaze.data.remote.dto.PlaylistDetail
import com.example.phaze.data.remote.dto.Playlists
import com.example.phaze.data.remote.dto.SearchResults
import com.example.phaze.data.remote.dto.Songs
import com.example.phaze.data.remote.dto.StarredItems
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit

/**
 * Converter for `SubsonicResponse<T>` return types.
 *
 * The Subsonic envelope is `{ "subsonic-response": { "status": ..., "version": ...,
 * <endpoint payload keys> } }`. Payload keys differ per endpoint (`artists`,
 * `albumList2`, `searchResult3`, ...), so we strip the known envelope keys and
 * decode whatever remains as the typed payload [T].
 *
 * - `status != "ok"` → throws [SubsonicException] carrying the server [SubsonicError].
 * - non-JSON body (proxy error pages etc.) → [SubsonicException] with [SubsonicException.CODE_PARSE_ERROR].
 * - empty payload (ping, star, scrobble...) → `data = null`.
 */
class SubsonicConverterFactory(
    private val json: Json,
) : Converter.Factory() {

    private val envelopeKeys =
        setOf("status", "version", "type", "serverVersion", "openSubsonic", "error")

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<ResponseBody, *>? {
        val parameterized = type as? ParameterizedType ?: return null
        if (parameterized.rawType != SubsonicResponse::class.java) return null
        val payloadType = parameterized.actualTypeArguments.singleOrNull() ?: return null
        val payloadSerializer = serializerFor(payloadType) ?: return null
        Log.d(TAG, "registered converter for $payloadType (${payloadSerializer.descriptor.serialName})")

        return Converter<ResponseBody, SubsonicResponse<*>> { body ->
            val raw = body.string()
            val envelope = try {
                json.parseToJsonElement(raw).jsonObject["subsonic-response"]?.jsonObject
            } catch (e: SerializationException) {
                Log.w(TAG, "non-JSON response for $payloadType: '${raw.take(200)}'")
                throw SubsonicException(
                    code = SubsonicException.CODE_PARSE_ERROR,
                    message = "Server returned a non-JSON response: ${e.message}",
                    cause = e,
                )
            } ?: run {
                Log.w(TAG, "malformed response for $payloadType: missing 'subsonic-response': '${raw.take(200)}'")
                throw SubsonicException(
                    code = SubsonicException.CODE_PARSE_ERROR,
                    message = "Malformed Subsonic response: missing 'subsonic-response'",
                )
            }

            val status = envelope["status"]?.jsonPrimitive?.contentOrNull ?: "failed"
            val version = envelope["version"]?.jsonPrimitive?.contentOrNull ?: ""
            val serverType = envelope["type"]?.jsonPrimitive?.contentOrNull
            val serverVersion = envelope["serverVersion"]?.jsonPrimitive?.contentOrNull
            val openSubsonic = envelope["openSubsonic"]?.jsonPrimitive?.booleanOrNull
            val error = envelope["error"]?.let { json.decodeFromJsonElement(SubsonicError.serializer(), it) }
            Log.d(
                TAG,
                "response $payloadType: status=$status version=$version type=$serverType serverVersion=$serverVersion openSubsonic=$openSubsonic",
            )

            if (status != "ok") {
                Log.w(TAG, "server error for $payloadType: code=${error?.code} msg='${error?.message}'")
                throw SubsonicException(
                    code = error?.code ?: SubsonicException.CODE_GENERIC,
                    message = error?.message ?: "Subsonic request failed with status '$status'",
                    serverVersion = serverVersion,
                )
            }

            val payload = envelope.filterKeys { it !in envelopeKeys }
            val payloadJson = JsonObject(payload)
            Log.d(
                TAG,
                "Payload: ${payloadJson.toString()}",
            )
            val data: Any? = if (payload.isEmpty()) {
                null
            } else {
                json.decodeFromJsonElement(payloadSerializer, payloadJson)
            }
            Log.d(
                TAG,
                "decoded $payloadType: ${payloadJson.toString().take(1500)}",
            )

            SubsonicResponse(
                status = status,
                version = version,
                serverType = serverType,
                serverVersion = serverVersion,
                openSubsonic = openSubsonic,
                error = error,
                data = data,
            )
        }
    }

    private fun serializerFor(type: Type): KSerializer<*>? = when (type) {
        Unit::class.java -> Unit.serializer()
        License::class.java -> License.serializer()
        Artists::class.java -> Artists.serializer()
        AlbumList2::class.java -> AlbumList2.serializer()
        AlbumDetail::class.java -> AlbumDetail.serializer()
        ArtistDetail::class.java -> ArtistDetail.serializer()
        Songs::class.java -> Songs.serializer()
        Playlists::class.java -> Playlists.serializer()
        PlaylistDetail::class.java -> PlaylistDetail.serializer()
        SearchResults::class.java -> SearchResults.serializer()
        StarredItems::class.java -> StarredItems.serializer()
        else -> null
    }
}

private const val TAG = "SubsonicConverter"
