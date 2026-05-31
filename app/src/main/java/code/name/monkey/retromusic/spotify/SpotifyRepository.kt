package code.name.monkey.retromusic.spotify

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import code.name.monkey.retromusic.spotify.db.SpotifyDatabase
import code.name.monkey.retromusic.spotify.db.SpotifyTrackEntity

data class SpotifyTrack(
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val spotifyUrl: String,
    val albumArtUrl: String
)

data class SpotifyPlaylist(
    val name: String,
    val description: String,
    val tracks: List<SpotifyTrack>
)

class SpotifyRepository(private val context: Context) {

    private val client = OkHttpClient()
    private val db by lazy { SpotifyDatabase.getInstance(context) }

    suspend fun fetchAndSave(playlistUrl: String): Result<SpotifyPlaylist> =
        withContext(Dispatchers.IO) {
            val result = fetchPlaylist(playlistUrl)
            result.onSuccess { playlist ->
                val playlistId = extractPlaylistId(playlistUrl) ?: return@onSuccess
                val entities = playlist.tracks.map { track ->
                    SpotifyTrackEntity(
                        spotifyUrl = track.spotifyUrl,
                        title = track.title,
                        artist = track.artist,
                        album = track.album,
                        durationMs = track.durationMs,
                        playlistId = playlistId,
                        playlistName = playlist.name,
                        playlistUrl = playlistUrl          // ← add this
                    )
                }
                db.spotifyDao().upsertTracks(entities)
                android.util.Log.d("SpotifyTest", "Saved ${entities.size} tracks to DB for playlist: ${playlist.name}")
            }
            result
        }

    suspend fun fetchPlaylist(playlistUrl: String): Result<SpotifyPlaylist> =
        withContext(Dispatchers.IO) {
            try {
                val playlistId = extractPlaylistId(playlistUrl)
                    ?: return@withContext Result.failure(Exception("Invalid Spotify playlist URL"))

                val embedUrl = "https://open.spotify.com/embed/playlist/$playlistId"

                val request = Request.Builder()
                    .url(embedUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .build()

                val response = client.newCall(request).execute()
                val html = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response"))

                val playlist = parseNextData(html)
                Result.success(playlist)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    internal fun extractPlaylistId(url: String): String? {
        val urlRegex = Regex("playlist/([A-Za-z0-9]+)")
        val uriRegex = Regex("spotify:playlist:([A-Za-z0-9]+)")
        return urlRegex.find(url)?.groupValues?.get(1)
            ?: uriRegex.find(url)?.groupValues?.get(1)
    }

    private fun parseNextData(html: String): SpotifyPlaylist {
        val start = html.indexOf("<script id=\"__NEXT_DATA__\"")
        if (start == -1) throw Exception("No __NEXT_DATA__ found")

        val jsonStart = html.indexOf(">", start) + 1
        val jsonEnd = html.indexOf("</script>", jsonStart)
        val json = html.substring(jsonStart, jsonEnd)

        val root = JSONObject(json)
        val entity = root
            .getJSONObject("props")
            .getJSONObject("pageProps")
            .getJSONObject("state")
            .getJSONObject("data")
            .getJSONObject("entity")

        val name = entity.getString("name")
        val description = entity.optString("subtitle", "")
        val trackListArray = entity.getJSONArray("trackList")
        val tracks = mutableListOf<SpotifyTrack>()

        for (i in 0 until trackListArray.length()) {
            val item = trackListArray.getJSONObject(i)
            val title = item.optString("title", "")
            val artist = item.optString("subtitle", "")
            val durationMs = item.optLong("duration", 0)
            val uri = item.optString("uri", "")
            val spotifyUrl = uri.replace("spotify:track:", "https://open.spotify.com/track/")

            if (title.isNotEmpty()) {
                tracks.add(SpotifyTrack(title, artist, "", durationMs, spotifyUrl, ""))
            }
        }

        return SpotifyPlaylist(name, description, tracks)
    }
}