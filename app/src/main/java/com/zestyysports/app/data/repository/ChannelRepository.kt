package com.zestyysports.app.data.repository

import com.zestyysports.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class ChannelRepository(private val favoriteDao: FavoriteDao) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()
            chain.proceed(request)
        }
        .build()

    suspend fun loadChannels(region: PlaylistRegion): List<Channel> = withContext(Dispatchers.IO) {
        val sources = if (region == PlaylistRegion.INDIA) Sources.INDIA else Sources.USA
        val MAX = 20000

        val deferred = sources.map { source ->
            async {
                try {
                    val request = Request.Builder().url(source.url).build()
                    val body = client.newCall(request).execute().use { it.body?.string() ?: "" }
                    var items = M3UParser.parse(body, source.id)

                    // Same filters as web app
                    if (source.id == "sonur") {
                        items = items.filter { it.name.lowercase().contains("star sports") }
                    }
                    if (source.id == "premium") {
                        items = items.filter { ch ->
                            val n = ch.name.lowercase(); val g = ch.group.lowercase()
                            n.contains("sports") || n.contains("cricket") ||
                            g.contains("sports") || g.contains("cricket")
                        }
                    }

                    items.filter { ch ->
                        ch.url.isNotBlank() &&
                        ch.group.lowercase() !in Sources.BLOCKED_GROUPS
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }

        val results = deferred.awaitAll().flatten()
        results.take(MAX)
    }

    suspend fun loadAllRegions(): Pair<List<Channel>, List<Channel>> = withContext(Dispatchers.IO) {
        val india = async { loadChannels(PlaylistRegion.INDIA) }
        val usa   = async { loadChannels(PlaylistRegion.USA) }
        Pair(india.await(), usa.await())
    }

    fun getFavoritesFlow() = favoriteDao.getAllFlow()

    suspend fun toggleFavorite(channelId: String) {
        val fav = FavoriteEntity(channelId)
        if (favoriteDao.isFavorite(channelId)) favoriteDao.delete(fav)
        else favoriteDao.insert(fav)
    }
}
