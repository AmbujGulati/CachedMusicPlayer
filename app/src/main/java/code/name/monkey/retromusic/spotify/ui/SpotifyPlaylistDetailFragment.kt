package code.name.monkey.retromusic.spotify.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.SearchView as AppSearchView
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.spotify.db.SpotifyDao
import code.name.monkey.retromusic.spotify.db.SpotifyDatabase
import code.name.monkey.retromusic.spotify.db.SpotifyTrackEntity
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.repository.RealSongRepository
import code.name.monkey.retromusic.views.TopAppBarLayout
import org.koin.android.ext.android.get

class SpotifyPlaylistDetailFragment : Fragment() {

    private val args: SpotifyPlaylistDetailFragmentArgs by navArgs()
    private val viewModel: SpotifyPlaylistsViewModel by activityViewModels()

    private lateinit var dao: SpotifyDao
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TrackAdapter

    private var allTracks: List<SpotifyTrackEntity> = emptyList()

    // ── Lifecycle ─────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_spotify_playlist_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dao = SpotifyDatabase.getInstance(requireContext()).spotifyDao()

        recyclerView = view.findViewById(R.id.recyclerViewTracks)
        adapter      = TrackAdapter(emptyList())

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter       = adapter

        // Toolbar — playlist name as title, search wired up
        val appBarLayout = view.findViewById<TopAppBarLayout>(R.id.appBarLayout)
        appBarLayout.title = args.playlistName
        setupToolbarSearch(appBarLayout)

        // Observe live track list for this playlist
        dao.getTracksForPlaylist(args.playlistId).observe(viewLifecycleOwner) { tracks ->
            allTracks = tracks
            adapter.update(tracks)
        }
    }

    // ── Toolbar / Search ──────────────────────────────────────────────────

    private fun setupToolbarSearch(appBarLayout: TopAppBarLayout) {
        val toolbar = appBarLayout.toolbar
        toolbar.navigationIcon = null
        toolbar.menu.clear()
        toolbar.inflateMenu(R.menu.menu_spotify_playlists)

        val searchItem = toolbar.menu.findItem(R.id.action_search_spotify)
        val searchView = searchItem?.actionView as? AppSearchView
        searchView?.queryHint = "Search tracks"
        searchView?.setOnQueryTextListener(object : AppSearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = true
            override fun onQueryTextChange(newText: String?): Boolean {
                val filtered = if (newText.isNullOrBlank()) {
                    allTracks
                } else {
                    allTracks.filter {
                        it.title.contains(newText, ignoreCase = true) ||
                                it.artist.contains(newText, ignoreCase = true)
                    }
                }
                adapter.update(filtered)
                return true
            }
        })
    }

    // ── Adapter ───────────────────────────────────────────────────────────

    private inner class TrackAdapter(
        private var items: List<SpotifyTrackEntity>
    ) : RecyclerView.Adapter<TrackAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val title : TextView = view.findViewById(R.id.textTrackTitle)
            val artist: TextView = view.findViewById(R.id.textTrackArtist)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_spotify_track, parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val track = items[position]
            val isAvailable = track.matchStatus == "AVAILABLE"

            holder.title.text  = track.title
            holder.artist.text = track.artist

            val alpha = if (isAvailable) 1.0f else 0.38f
            holder.title.alpha  = alpha
            holder.artist.alpha = alpha

            holder.itemView.setOnClickListener {
                if (isAvailable) {
                    playLocal(track)
                } else {
                    MissingTrackBottomSheet.newInstance(track.title, track.artist, track.spotifyUrl)
                        .show(parentFragmentManager, "missing_track")
                }
            }
        }

        fun update(newItems: List<SpotifyTrackEntity>) {
            items = newItems
            notifyDataSetChanged()
        }

        private fun playLocal(track: SpotifyTrackEntity) {
            val path = track.localFilePath ?: return
            val songRepository: RealSongRepository = get()
            val allSongs = songRepository.songs(songRepository.makeSongCursor(null, null))

            // Build queue from all AVAILABLE tracks in current playlist order
            val queue = allTracks.filter { it.matchStatus == "AVAILABLE" }
                .mapNotNull { t -> allSongs.firstOrNull { it.data == t.localFilePath } }

            if (queue.isEmpty()) return

            val position = queue.indexOfFirst { it.data == path }.coerceAtLeast(0)
            MusicPlayerRemote.openQueue(queue, position, true)
        }
    }
}