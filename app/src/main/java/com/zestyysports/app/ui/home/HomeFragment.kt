package com.zestyysports.app.ui.home

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.chip.Chip
import com.zestyysports.app.R
import com.zestyysports.app.data.model.Channel
import com.zestyysports.app.data.model.PlaylistRegion
import com.zestyysports.app.data.repository.AppDatabase
import com.zestyysports.app.data.repository.ChannelRepository
import com.zestyysports.app.databinding.FragmentHomeBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        val db = AppDatabase.getInstance(requireContext())
        HomeViewModelFactory(ChannelRepository(db.favoriteDao()))
    }

    private lateinit var adapter: ChannelAdapter
    private var allChannels: List<Channel> = emptyList()
    private var mainChannels: List<Channel> = emptyList()
    private var currentTab: Tab = Tab.HOME
    private var currentGroup: String = "All"
    private var searchQuery: String = ""

    private enum class Tab { HOME, FAVORITES, SEARCH }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupBottomNav()
        setupRegionToggle()
        setupSearch()
        observeState()
    }

    private fun setupRecyclerView() {
        adapter = ChannelAdapter(
            onPlay = { ch -> navigateToPlayer(ch) },
            onFavorite = { ch -> viewModel.toggleFavorite(ch.id) }
        )
        val spanCount = if (resources.configuration.screenWidthDp >= 600) 4 else 2
        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.setHasFixedSize(true)
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { switchTab(Tab.HOME); true }
                R.id.nav_favorites -> { switchTab(Tab.FAVORITES); true }
                R.id.nav_search -> { switchTab(Tab.SEARCH); true }
                else -> false
            }
        }
    }

    private fun setupRegionToggle() {
        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val region = if (checkedId == R.id.btn_india) PlaylistRegion.INDIA else PlaylistRegion.USA
            viewModel.setRegion(region)
        }
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = true
            override fun onQueryTextChange(newText: String?): Boolean {
                searchQuery = newText ?: ""
                applyFilters()
                return true
            }
        })
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is HomeUiState.Loading -> showLoading()
                    is HomeUiState.Success -> {
                        mainChannels = state.channels
                        allChannels = state.allChannels
                        buildGroupChips()
                        applyFilters()
                        showContent()
                    }
                    is HomeUiState.Error -> showError(state.message)
                }
            }
        }

        viewModel.favoriteIds.observe(viewLifecycleOwner) { ids ->
            adapter.updateFavorites(ids)
            if (currentTab == Tab.FAVORITES) applyFilters()
        }
    }

    private fun switchTab(tab: Tab) {
        currentTab = tab
        binding.searchView.isVisible = tab == Tab.SEARCH
        binding.scrollChips.isVisible = tab == Tab.HOME
        applyFilters()
    }

    private fun buildGroupChips() {
        binding.chipGroupGroups.removeAllViews()

        val groups = mutableListOf("All")
        val priority = listOf("FIFA World Cup 2026", "Cricket", "Football")
        val allGroups = mainChannels.mapNotNull { it.group.takeIf { g -> g.isNotBlank() } }
            .distinct().sortedWith(Comparator { a, b ->
                val ai = priority.indexOfFirst { it.equals(a, ignoreCase = true) }
                val bi = priority.indexOfFirst { it.equals(b, ignoreCase = true) }
                when {
                    ai >= 0 && bi >= 0 -> ai - bi
                    ai >= 0 -> -1
                    bi >= 0 -> 1
                    else -> a.compareTo(b)
                }
            })
        groups.addAll(allGroups)

        groups.forEach { g ->
            val chip = Chip(requireContext()).apply {
                text = g
                isCheckable = true
                isChecked = g == currentGroup
                setOnClickListener {
                    currentGroup = g
                    applyFilters()
                    updateChipSelection()
                }
            }
            binding.chipGroupGroups.addView(chip)
        }
    }

    private fun updateChipSelection() {
        for (i in 0 until binding.chipGroupGroups.childCount) {
            val chip = binding.chipGroupGroups.getChildAt(i) as? Chip ?: continue
            chip.isChecked = chip.text == currentGroup
        }
    }

    private fun applyFilters() {
        val favIds = viewModel.favoriteIds.value ?: emptySet()

        val result = when (currentTab) {
            Tab.FAVORITES -> allChannels.filter { it.id in favIds }
            Tab.SEARCH -> {
                val q = searchQuery.trim().lowercase()
                if (q.isBlank()) emptyList()
                else allChannels.filter { it.name.lowercase().contains(q) }
            }
            Tab.HOME -> {
                if (currentGroup == "All") mainChannels
                else mainChannels.filter { it.group.equals(currentGroup, ignoreCase = true) }
            }
        }

        adapter.submitList(result)
        binding.tvEmpty.isVisible = result.isEmpty() && currentTab != Tab.SEARCH
    }

    private fun navigateToPlayer(ch: Channel) {
        val action = HomeFragmentDirections.actionHomeToPlayer(
            channelId   = ch.id,
            channelName = ch.name,
            channelUrl  = ch.url,
            channelLogo = ch.logo,
            channelGroup= ch.group
        )
        findNavController().navigate(action)
    }

    private fun showLoading() {
        binding.progressBar.isVisible = true
        binding.recyclerView.isVisible = false
        binding.tvEmpty.isVisible = false
        binding.layoutError.isVisible = false
    }

    private fun showContent() {
        binding.progressBar.isVisible = false
        binding.recyclerView.isVisible = true
        binding.layoutError.isVisible = false
    }

    private fun showError(msg: String) {
        binding.progressBar.isVisible = false
        binding.recyclerView.isVisible = false
        binding.layoutError.isVisible = true
        binding.tvError.text = msg
        binding.btnRetry.setOnClickListener { viewModel.retry() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
