package code.name.monkey.retromusic.spotify.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spotify_tracks")
data class SpotifyTrackEntity(
    @PrimaryKey
    val spotifyUrl: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val playlistId: String,
    val playlistName: String,
    val localFilePath: String? = null,      // filled in by matching engine later
    val matchStatus: String = "MISSING",     // "AVAILABLE" or "MISSING"
    val playlistUrl: String = ""            // original playlist URL, used for resync

)