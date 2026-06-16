package com.zestyysports.app.ui.home

import androidx.lifecycle.*
import com.zestyysports.app.data.model.Channel
import com.zestyysports.app.data.model.PlaylistRegion
import com.zestyysports.app.data.repository.ChannelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val channels: List<Channel>, val allChannels: List<Channel>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(private val repo: ChannelRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState

    private val _region = MutableLiveData(PlaylistRegion.INDIA)
    val region: LiveData<PlaylistRegion> = _region

    private var indiaChannels: List<Channel> = emptyList()
    private var usaChannels: List<Channel> = emptyList()

    // Live favorites set from Room
    val favoriteIds: LiveData<Set<String>> = repo.getFavoritesFlow()
        .asLiveData()
        .map { list -> list.map { it.channelId }.toSet() }

    init {
        loadAll()
    }

    private fun loadAll() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val (india, usa) = repo.loadAllRegions()
                indiaChannels = india
                usaChannels = usa
                emitForCurrentRegion()
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error("Failed to load channels")
            }
        }
    }

    fun setRegion(r: PlaylistRegion) {
        _region.value = r
        emitForCurrentRegion()
    }

    private fun emitForCurrentRegion() {
        val main = if (_region.value == PlaylistRegion.INDIA) indiaChannels else usaChannels
        val all  = indiaChannels + usaChannels
        _uiState.value = HomeUiState.Success(main, all)
    }

    fun toggleFavorite(channelId: String) {
        viewModelScope.launch { repo.toggleFavorite(channelId) }
    }

    fun retry() = loadAll()
}

class HomeViewModelFactory(private val repo: ChannelRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HomeViewModel(repo) as T
    }
}
