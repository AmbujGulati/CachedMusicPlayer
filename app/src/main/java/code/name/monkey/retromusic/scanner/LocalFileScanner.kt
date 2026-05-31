package code.name.monkey.retromusic.scanner

import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LocalAudioFile(
    val path: String,
    val fileName: String,           // e.g. "Pav Dharia - Na Ja.mp3"
    val parsedTitle: String,        // from filename: "Na Ja"
    val parsedArtist: String,       // from filename: "Pav Dharia"
    val id3Title: String,           // from MediaStore ID3 tag
    val id3Artist: String           // from MediaStore ID3 tag
)

class LocalFileScanner(private val context: Context) {

    suspend fun scanDevice(): List<LocalAudioFile> = withContext(Dispatchers.IO) {
        val results = mutableListOf<LocalAudioFile>()

        val projection = arrayOf(
            MediaStore.Audio.Media.DATA,            // file path
            MediaStore.Audio.Media.DISPLAY_NAME,    // filename
            MediaStore.Audio.Media.TITLE,           // ID3 title
            MediaStore.Audio.Media.ARTIST           // ID3 artist
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            null
        )

        cursor?.use {
            val pathCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val nameCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)

            while (it.moveToNext()) {
                val path = it.getString(pathCol) ?: continue
                val fileName = it.getString(nameCol) ?: ""
                val id3Title = it.getString(titleCol) ?: ""
                val id3Artist = it.getString(artistCol) ?: ""

                val (parsedArtist, parsedTitle) = parseFileName(fileName)

                results.add(
                    LocalAudioFile(
                        path = path,
                        fileName = fileName,
                        parsedTitle = parsedTitle,
                        parsedArtist = parsedArtist,
                        id3Title = id3Title,
                        id3Artist = id3Artist
                    )
                )
            }
        }

        android.util.Log.d("SpotifyTest", "Scanner found ${results.size} audio files")
        results
    }

    private fun parseFileName(fileName: String): Pair<String, String> {
        // spotdl default format: "Artist - Title.mp3"
        val nameWithoutExt = fileName.substringBeforeLast(".")
        val dashIndex = nameWithoutExt.indexOf(" - ")
        return if (dashIndex != -1) {
            val artist = nameWithoutExt.substring(0, dashIndex).trim()
            val title = nameWithoutExt.substring(dashIndex + 3).trim()
            Pair(artist, title)
        } else {
            Pair("", nameWithoutExt.trim())
        }
    }
}