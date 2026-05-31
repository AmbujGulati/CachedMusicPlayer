package code.name.monkey.retromusic.spotify.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import code.name.monkey.retromusic.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MissingTrackBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_TITLE       = "title"
        private const val ARG_ARTIST      = "artist"
        private const val ARG_SPOTIFY_URL = "spotifyUrl"

        fun newInstance(title: String, artist: String, spotifyUrl: String) =
            MissingTrackBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE,       title)
                    putString(ARG_ARTIST,      artist)
                    putString(ARG_SPOTIFY_URL, spotifyUrl)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_missing_track, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val title      = arguments?.getString(ARG_TITLE)      ?: ""
        val artist     = arguments?.getString(ARG_ARTIST)     ?: ""
        val spotifyUrl = arguments?.getString(ARG_SPOTIFY_URL) ?: ""

        view.findViewById<TextView>(R.id.textMissingTitle).text  = title
        view.findViewById<TextView>(R.id.textMissingArtist).text = artist

        view.findViewById<Button>(R.id.buttonOpenSpotify).setOnClickListener {
            openInSpotify(spotifyUrl)
        }
    }

    private fun openInSpotify(url: String) {
        // Try Spotify app first; fall back to browser
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage("com.spotify.music")
        }
        try {
            startActivity(appIntent)
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
        dismiss()
    }
}
