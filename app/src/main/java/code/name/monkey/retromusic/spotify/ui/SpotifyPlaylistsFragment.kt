package code.name.monkey.retromusic.spotify.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.spotify.db.PlaylistSummary
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.appcompat.widget.SearchView as AppSearchView
import code.name.monkey.retromusic.views.TopAppBarLayout

class SpotifyPlaylistsFragment : Fragment() {

    private var allPlaylists: List<PlaylistSummary> = emptyList()
    private val selectedIds = mutableSetOf<String>()
    private var inSelectionMode = false

    private val viewModel: SpotifyPlaylistsViewModel by activityViewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var fab: FloatingActionButton
    private lateinit var adapter: PlaylistAdapter
    private lateinit var appBarLayout: TopAppBarLayout

    // ── Lifecycle ─────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_spotify_playlists, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewPlaylists)
        fab          = view.findViewById(R.id.fabAddPlaylist)
        appBarLayout = view.findViewById(R.id.appBarLayout)

        appBarLayout.title = getString(R.string.onfile_tab)
        setupToolbarNormal()
        setupRecyclerView()
        observeViewModel()
        setupFab()
    }

    // ── Toolbar modes ─────────────────────────────────────────────────────

    private fun setupToolbarNormal() {
        val toolbar = appBarLayout.toolbar
        toolbar.navigationIcon = null
        toolbar.menu.clear()
        toolbar.inflateMenu(R.menu.menu_spotify_playlists)

        val searchItem = toolbar.menu.findItem(R.id.action_search_spotify)
        val searchView = searchItem?.actionView as? AppSearchView
        searchView?.queryHint = "Search playlists"
        searchView?.setOnQueryTextListener(object : AppSearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = true
            override fun onQueryTextChange(newText: String?): Boolean {
                val filtered = if (newText.isNullOrBlank()) allPlaylists
                else allPlaylists.filter {
                    it.playlistName.contains(newText, ignoreCase = true)
                }
                adapter.update(filtered)
                return true
            }
        })

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_resync_all -> {
                    allPlaylists.forEach { viewModel.resyncPlaylist(it.playlistId) }
                    true
                }
                else -> false
            }
        }
    }

    private fun setupToolbarSelection() {
        val toolbar = appBarLayout.toolbar
        toolbar.menu.clear()
        toolbar.inflateMenu(R.menu.menu_spotify_selection)
        appBarLayout.title = "${selectedIds.size} selected"

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_resync_selected -> {
                    selectedIds.forEach { viewModel.resyncPlaylist(it) }
                    exitSelectionMode()
                    true
                }
                R.id.action_delete_selected -> {
                    selectedIds.forEach { viewModel.deletePlaylist(it) }
                    exitSelectionMode()
                    true
                }
                else -> false
            }
        }

        // Back arrow exits selection mode
        toolbar.setNavigationIcon(R.drawable.ic_keyboard_arrow_left)
        toolbar.setNavigationOnClickListener { exitSelectionMode() }
    }

    // ── Selection mode ────────────────────────────────────────────────────

    private fun enterSelectionMode(playlistId: String) {
        inSelectionMode = true
        selectedIds.clear()
        selectedIds.add(playlistId)
        fab.hide()
        setupToolbarSelection()
        adapter.notifyDataSetChanged()
    }

    private fun exitSelectionMode() {
        inSelectionMode = false
        selectedIds.clear()
        fab.show()
        appBarLayout.title = getString(R.string.onfile_tab)
        setupToolbarNormal()
        adapter.notifyDataSetChanged()
    }

    private fun toggleSelection(playlistId: String) {
        if (selectedIds.contains(playlistId)) {
            selectedIds.remove(playlistId)
            if (selectedIds.isEmpty()) {
                exitSelectionMode()
                return
            }
        } else {
            selectedIds.add(playlistId)
        }
        appBarLayout.title = "${selectedIds.size} selected"
        adapter.notifyDataSetChanged()
    }

    // ── Setup ─────────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = PlaylistAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun observeViewModel() {
        val emptyView = view?.findViewById<View>(android.R.id.empty)

        viewModel.playlists.observe(viewLifecycleOwner) { summaries ->
            allPlaylists = summaries
            adapter.update(summaries)
            emptyView?.visibility = if (summaries.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.syncState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SyncState.Idle    -> { }
                is SyncState.Syncing -> { }
                is SyncState.Success -> {
                    Toast.makeText(requireContext(),
                        "Synced: ${state.matched}/${state.total} tracks matched",
                        Toast.LENGTH_SHORT).show()
                    viewModel.resetSyncState()
                }
                is SyncState.Error -> {
                    Toast.makeText(requireContext(),
                        "Sync failed: ${state.message}",
                        Toast.LENGTH_LONG).show()
                    viewModel.resetSyncState()
                }
            }
        }
    }

    private fun setupFab() {
        fab.setOnClickListener {
            AddPlaylistBottomSheet().show(parentFragmentManager, "add_playlist")
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────

    private fun navigateToDetail(summary: PlaylistSummary) {
        val action = SpotifyPlaylistsFragmentDirections
            .actionSpotifyToPlaylistDetail(
                playlistId   = summary.playlistId,
                playlistName = summary.playlistName
            )
        findNavController().navigate(action)
    }

    // ── Adapter ───────────────────────────────────────────────────────────

    private inner class PlaylistAdapter(
        private var items: List<PlaylistSummary>
    ) : RecyclerView.Adapter<PlaylistAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val name      : android.widget.TextView = view.findViewById(R.id.textPlaylistName)
            val trackCount: android.widget.TextView = view.findViewById(R.id.textTrackCount)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_spotify_playlist, parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val summary = items[position]
            val isSelected = selectedIds.contains(summary.playlistId)

            holder.name.text       = summary.playlistName
            holder.trackCount.text = "${summary.availableTracks}/${summary.totalTracks} available"

            // Highlight selected rows
            holder.itemView.isActivated = isSelected
            holder.itemView.alpha = if (inSelectionMode && !isSelected) 0.5f else 1.0f

            holder.itemView.setOnClickListener {
                if (inSelectionMode) toggleSelection(summary.playlistId)
                else navigateToDetail(summary)
            }

            holder.itemView.setOnLongClickListener {
                if (!inSelectionMode) enterSelectionMode(summary.playlistId)
                else toggleSelection(summary.playlistId)
                true
            }
        }

        fun update(newItems: List<PlaylistSummary>) {
            items = newItems
            notifyDataSetChanged()
        }
    }
}