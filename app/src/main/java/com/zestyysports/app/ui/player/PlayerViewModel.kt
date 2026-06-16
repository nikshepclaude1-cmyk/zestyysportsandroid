package com.zestyysports.app.ui.player

import androidx.lifecycle.*
import com.zestyysports.app.data.model.Channel
import com.zestyysports.app.data.repository.ChannelRepository
import kotlinx.coroutines.launch

class PlayerViewModel(private val repo: ChannelRepository) : ViewModel() {

    val favoriteIds: LiveData<Set<String>> = repo.getFavoritesFlow()
        .asLiveData()
        .map { list -> list.map { it.channelId }.toSet() }

    fun toggleFavorite(channelId: String) {
        viewModelScope.launch { repo.toggleFavorite(channelId) }
    }
}

class PlayerViewModelFactory(private val repo: ChannelRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return PlayerViewModel(repo) as T
    }
}
