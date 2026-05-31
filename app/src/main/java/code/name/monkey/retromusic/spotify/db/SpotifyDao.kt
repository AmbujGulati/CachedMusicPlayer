package code.name.monkey.retromusic.spotify.db

import androidx.lifecycle.LiveData
import androidx.room.*

// ── Projection data classes ────────────────────────────────────────────────

/** Full grouped summary — used by the playlists-list screen. */
data class PlaylistSummary(
    val playlistId: String,
    val playlistName: String,
    val totalTracks: Int,
    val availableTracks: Int
)

/** Lightweight 2-column projection — used by getAllPlaylists(). */
data class PlaylistIdName(
    val playlistId: String,
    val playlistName: String
)

// ── DAO ───────────────────────────────────────────────────────────────────

@Dao
interface SpotifyDao {

    // ── Write ──────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrack(track: SpotifyTrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTracks(tracks: List<SpotifyTrackEntity>)

    @Query(
        """UPDATE spotify_tracks
           SET localFilePath = :localFilePath, matchStatus = :matchStatus
           WHERE spotifyUrl = :spotifyUrl"""
    )
    suspend fun updateMatchStatus(
        spotifyUrl: String,
        localFilePath: String?,
        matchStatus: String
    )

    // ── Playlist-list queries ──────────────────────────────────────────────

    /** All playlists as lightweight id+name pairs (no counts). */
    @Query(
        """SELECT DISTINCT playlistId, playlistName
           FROM spotify_tracks
           ORDER BY playlistName ASC"""
    )
    fun getAllPlaylists(): LiveData<List<PlaylistIdName>>

    /**
     * Grouped summary with track counts — used by SpotifyPlaylistsFragment
     * to show "X / Y available" per playlist.
     */
    @Query(
        """SELECT
               playlistId,
               playlistName,
               COUNT(*) AS totalTracks,
               SUM(CASE WHEN matchStatus = 'AVAILABLE' THEN 1 ELSE 0 END) AS availableTracks
           FROM spotify_tracks
           GROUP BY playlistId
           ORDER BY playlistName ASC"""
    )
    fun getAllPlaylistSummaries(): LiveData<List<PlaylistSummary>>

    // ── Track queries ──────────────────────────────────────────────────────

    /** LiveData track list — observed by SpotifyPlaylistDetailFragment. */
    @Query(
        """SELECT * FROM spotify_tracks
           WHERE playlistId = :playlistId
           ORDER BY title ASC"""
    )
    fun getTracksForPlaylist(playlistId: String): LiveData<List<SpotifyTrackEntity>>

    /** Suspend plain list — used by MatchingEngine and post-sync counting. */
    @Query(
        """SELECT * FROM spotify_tracks
           WHERE playlistId = :playlistId"""
    )
    suspend fun getTracksForPlaylistSync(playlistId: String): List<SpotifyTrackEntity>

    /** Non-suspend plain list — called from Dispatchers.IO inside MatchingEngine. */
    @Query("SELECT * FROM spotify_tracks")
    fun getAllTracksSync(): List<SpotifyTrackEntity>

    // ── Deletion ───────────────────────────────────────────────────────────

    @Query("DELETE FROM spotify_tracks WHERE playlistId = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Query("SELECT playlistUrl FROM spotify_tracks WHERE playlistId = :playlistId LIMIT 1")
    suspend fun getPlaylistUrl(playlistId: String): String?
}
