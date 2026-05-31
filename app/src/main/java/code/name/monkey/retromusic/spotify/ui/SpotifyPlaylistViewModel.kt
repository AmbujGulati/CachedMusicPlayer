package code.name.monkey.retromusic.spotify.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import code.name.monkey.retromusic.scanner.LocalFileScanner
import code.name.monkey.retromusic.scanner.MatchingEngine
import code.name.monkey.retromusic.spotify.SpotifyRepository
import code.name.monkey.retromusic.spotify.db.PlaylistSummary
import code.name.monkey.retromusic.spotify.db.SpotifyDao
import code.name.monkey.retromusic.spotify.db.SpotifyDatabase
import kotlinx.coroutines.launch

// ── SyncState ─────────────────────────────────────────────────────────────

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val matched: Int, val total: Int) : SyncState()
    data class Error(val message: String) : SyncState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────

class SpotifyPlaylistsViewModel(app: Application) : AndroidViewModel(app) {

    private val dao: SpotifyDao = SpotifyDatabase.getInstance(app).spotifyDao()
    private val repository      = SpotifyRepository(app)
    private val scanner         = LocalFileScanner(app)
    private val matcher         = MatchingEngine(dao)

    /** Observed by SpotifyPlaylistsFragment — list of all playlists with counts. */
    val playlists: LiveData<List<PlaylistSummary>> = dao.getAllPlaylistSummaries()

    private val _syncState = MutableLiveData<SyncState>(SyncState.Idle)
    val syncState: LiveData<SyncState> = _syncState

    // ── Public actions ────────────────────────────────────────────────────

    fun syncPlaylist(url: String) {
        _syncState.value = SyncState.Syncing
        viewModelScope.launch {
            try {
                // 1. Extract playlistId first (needed for matching + counting)
                val playlistId = repository.extractPlaylistId(url)
                    ?: throw Exception("Invalid Spotify playlist URL")

                // 2. Fetch + save — unwrap Result, throw on failure
                repository.fetchAndSave(url).getOrThrow()

                // 3. Scan device
                val localFiles = scanner.scanDevice()

                // 4. Match
                matcher.matchAll(localFiles, playlistId)

                // 5. Count results
                val tracks  = dao.getTracksForPlaylistSync(playlistId)
                val matched = tracks.count { it.matchStatus == "AVAILABLE" }
                val total   = tracks.size

                _syncState.value = SyncState.Success(matched, total)
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Unknown error")
            }
        }
    }
    fun resyncPlaylist(playlistId: String) {
        viewModelScope.launch {
            try {
                val url = dao.getPlaylistUrl(playlistId)
                    ?: throw Exception("No URL stored for playlist $playlistId")
                syncPlaylist(url)
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetSyncState() {
        _syncState.value = SyncState.Idle
    }
    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            try {
                dao.deletePlaylist(playlistId)
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Delete failed")
            }
        }
    }
}
