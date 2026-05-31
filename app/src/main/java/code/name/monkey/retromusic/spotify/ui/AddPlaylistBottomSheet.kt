package code.name.monkey.retromusic.spotify.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.activityViewModels
import code.name.monkey.retromusic.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddPlaylistBottomSheet : BottomSheetDialogFragment() {

    private val viewModel: SpotifyPlaylistsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_add_playlist, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val editUrl   = view.findViewById<EditText>(R.id.editPlaylistUrl)
        val btnImport = view.findViewById<Button>(R.id.buttonImportPlaylist)

        btnImport.setOnClickListener {
            val url = editUrl.text.toString().trim()
            if (url.isNotEmpty()) {
                viewModel.syncPlaylist(url)
                dismiss()
            } else {
                editUrl.error = "Please paste a Spotify playlist URL"
            }
        }
    }
}
