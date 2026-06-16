package com.zestyysports.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

data class Channel(
    val id: String,
    val name: String,
    val logo: String,
    val url: String,
    val group: String,
    val sourceId: String = ""
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val channelId: String
)

enum class PlaylistRegion { INDIA, USA }
