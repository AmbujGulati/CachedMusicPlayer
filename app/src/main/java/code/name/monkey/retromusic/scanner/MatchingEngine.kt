package code.name.monkey.retromusic.scanner

import code.name.monkey.retromusic.spotify.db.SpotifyDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MatchingEngine(private val dao: SpotifyDao) {

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Matches [localFiles] against all Spotify tracks stored for [playlistId].
     * Updates the DB for every match found (AVAILABLE) and every miss (MISSING).
     * Must be called from a coroutine; DB writes happen on [Dispatchers.IO].
     */
    suspend fun matchAll(
        localFiles: List<LocalAudioFile>,
        playlistId: String
    ) = withContext(Dispatchers.IO) {
        val tracks = dao.getTracksForPlaylistSync(playlistId)

        for (track in tracks) {
            val spotifyTitle  = normalize(track.title)
            val spotifyArtist = normalize(track.artist)

            val match = localFiles.firstOrNull { file ->
                matchesTrack(file, spotifyTitle, spotifyArtist)
            }

            if (match != null) {
                dao.updateMatchStatus(
                    spotifyUrl    = track.spotifyUrl,
                    localFilePath = match.path,
                    matchStatus   = "AVAILABLE"
                )
            } else {
                dao.updateMatchStatus(
                    spotifyUrl    = track.spotifyUrl,
                    localFilePath = null,
                    matchStatus   = "MISSING"
                )
            }
        }
    }

    // ── Matching logic ────────────────────────────────────────────────────

    private fun matchesTrack(
        file: LocalAudioFile,
        spotifyTitle: String,
        spotifyArtist: String
    ): Boolean {
        // Match 1: ID3 title + artist
        val id3Title  = normalize(file.id3Title ?: "")
        val id3Artist = normalize(file.id3Artist ?: "")
        if (id3Title.isNotEmpty() && titlesMatch(id3Title, spotifyTitle) &&
            artistsOverlap(id3Artist, spotifyArtist)) return true

        // Match 2: parsed filename title + artist
        val parsedTitle  = normalize(file.parsedTitle)
        val parsedArtist = normalize(file.parsedArtist)
        if (titlesMatch(parsedTitle, spotifyTitle) &&
            artistsOverlap(parsedArtist, spotifyArtist)) return true

        // Match 3: title-only (guard against short / common words)
        if (spotifyTitle.length > 4) {
            if (titlesMatch(id3Title, spotifyTitle) && id3Title.isNotEmpty()) return true
            if (titlesMatch(parsedTitle, spotifyTitle)) return true
        }

        return false
    }

    private fun titlesMatch(a: String, b: String) = a == b

    /**
     * Returns true if any individual artist from [fileArtist] overlaps with
     * any individual artist in [spotifyArtist].
     * Splits on `,`, `/`, `&` to handle multi-artist strings.
     */
    private fun artistsOverlap(fileArtist: String, spotifyArtist: String): Boolean {
        if (fileArtist.isEmpty() || spotifyArtist.isEmpty()) return true
        val fileArtists    = fileArtist.split(Regex("[,/&]")).map { it.trim() }
        val spotifyArtists = spotifyArtist.split(Regex("[,/&]")).map { it.trim() }
        return fileArtists.any { fa -> spotifyArtists.any { sa -> fa == sa } }
    }

    // ── Normalisation ─────────────────────────────────────────────────────

    /**
     * Normalises a string for comparison:
     * - lowercase
     * - strip feat. clauses
     * - strip version/remaster/original parentheticals
     * - strip all punctuation
     * - collapse whitespace
     */
    internal fun normalize(input: String): String {
        var s = input.lowercase()
        // Strip feat. clauses
        s = s.replace(Regex("""\s*[\(\[]feat\..*?[\)\]]"""), "")
        s = s.replace(Regex("""\s*feat\..+$"""), "")
        // Strip version / remaster / original parentheticals
        s = s.replace(Regex("""\s*[\(\[](orig.*?version|remaster.*?|.*?version)[\)\]]""",
            RegexOption.IGNORE_CASE), "")
        // Strip punctuation
        s = s.replace(Regex("""[^\w\s]"""), "")
        // Collapse whitespace
        s = s.trim().replace(Regex("""\s+"""), " ")
        return s
    }
}
