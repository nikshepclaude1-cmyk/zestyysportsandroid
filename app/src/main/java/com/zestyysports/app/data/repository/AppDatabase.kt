package com.zestyysports.app.data.repository

import android.content.Context
import androidx.room.*
import com.zestyysports.app.data.model.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites")
    fun getAllFlow(): Flow<List<FavoriteEntity>>

    @Query("SELECT channelId FROM favorites")
    suspend fun getAllIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(fav: FavoriteEntity)

    @Delete
    suspend fun delete(fav: FavoriteEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE channelId = :id)")
    suspend fun isFavorite(id: String): Boolean
}

@Database(entities = [FavoriteEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zestyy_db"
                ).build().also { INSTANCE = it }
            }
    }
}
