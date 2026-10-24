package com.reayt.youtube.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val channelTitle: String,
    val channelId: String = "",
    val thumbnailUrl: String,
    val durationMs: Long = 0L,
    val lastPositionMs: Long = 0L,
    val watchedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "liked_videos")
data class LikedVideoEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val channelTitle: String,
    val channelId: String = "",
    val thumbnailUrl: String,
    val durationMs: Long = 0L,
    val likedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "watch_later")
data class WatchLaterEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val channelTitle: String,
    val channelId: String = "",
    val thumbnailUrl: String,
    val durationMs: Long = 0L,
    val sortOrder: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey val channelId: String,
    val channelTitle: String,
    val avatarUrl: String = "",
    val subscriberCount: String = "",
    val subscribedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val durationMs: Long = 0L,
    val localFilePath: String = "",
    val quality: String = "Auto",
    val isAudioOnly: Boolean = false,
    val status: String = "COMPLETED", // DOWNLOADING, COMPLETED, FAILED
    val progress: Float = 1.0f,
    val downloadedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_history ORDER BY watchedAt DESC")
    fun getAllHistory(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history ORDER BY watchedAt DESC LIMIT :limit")
    suspend fun getRecentHistorySync(limit: Int): List<WatchHistoryEntity>

    @Query("SELECT * FROM watch_history WHERE videoId = :videoId LIMIT 1")
    suspend fun getHistory(videoId: String): WatchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE videoId = :videoId")
    suspend fun delete(videoId: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearAll()
}

@Dao
interface LikedVideoDao {
    @Query("SELECT * FROM liked_videos ORDER BY likedAt DESC")
    fun getLikedVideos(): Flow<List<LikedVideoEntity>>

    @Query("SELECT * FROM liked_videos ORDER BY likedAt DESC LIMIT :limit")
    suspend fun getRecentLikedSync(limit: Int): List<LikedVideoEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM liked_videos WHERE videoId = :videoId)")
    fun isLiked(videoId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: LikedVideoEntity)

    @Query("DELETE FROM liked_videos WHERE videoId = :videoId")
    suspend fun delete(videoId: String)
}

@Dao
interface WatchLaterDao {
    @Query("SELECT * FROM watch_later ORDER BY sortOrder ASC, addedAt DESC")
    fun getWatchLaterVideos(): Flow<List<WatchLaterEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM watch_later WHERE videoId = :videoId)")
    fun isInWatchLater(videoId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchLaterEntity)

    @Query("DELETE FROM watch_later WHERE videoId = :videoId")
    suspend fun delete(videoId: String)

    @Query("DELETE FROM watch_later")
    suspend fun clearAll()
}

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY subscribedAt DESC")
    fun getSubscriptions(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions ORDER BY subscribedAt DESC")
    suspend fun getSubscriptionsSync(): List<SubscriptionEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM subscriptions WHERE channelId = :channelId)")
    fun isSubscribed(channelId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SubscriptionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SubscriptionEntity>)

    @Query("DELETE FROM subscriptions WHERE channelId = :channelId")
    suspend fun delete(channelId: String)
}

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 20")
    fun getRecentSearches(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE `query` = :query")
    suspend fun delete(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearAll()
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY downloadedAt DESC")
    fun getDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE videoId = :videoId LIMIT 1")
    suspend fun getDownload(videoId: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: DownloadEntity)

    @Query("DELETE FROM downloads WHERE videoId = :videoId")
    suspend fun delete(videoId: String)
}

@Dao
interface AppSettingsDao {
    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getValue(key: String): String?

    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    fun observeValue(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setValue(item: AppSettingsEntity)
}

@Database(
    entities = [
        WatchHistoryEntity::class,
        LikedVideoEntity::class,
        WatchLaterEntity::class,
        SubscriptionEntity::class,
        SearchHistoryEntity::class,
        DownloadEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun likedVideoDao(): LikedVideoDao
    abstract fun watchLaterDao(): WatchLaterDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun downloadDao(): DownloadDao
    abstract fun appSettingsDao(): AppSettingsDao
}
