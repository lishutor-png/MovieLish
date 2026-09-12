package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.MediaItemEntity
import com.example.data.model.MediaType
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items ORDER BY dateAdded DESC")
    fun getAllMediaItems(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE mediaType = :mediaType ORDER BY title ASC")
    fun getMediaByType(mediaType: MediaType): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isOfflineAvailable = 1 ORDER BY dateAdded DESC")
    fun getOfflineMedia(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE cloudProvider IS NOT NULL ORDER BY dateAdded DESC")
    fun getCloudMedia(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE id = :id LIMIT 1")
    suspend fun getMediaById(id: String): MediaItemEntity?

    @Query("SELECT DISTINCT seriesName FROM media_items WHERE mediaType = 'TV_SERIES' AND seriesName IS NOT NULL")
    fun getAllSeriesNames(): Flow<List<String>>

    @Query("SELECT * FROM media_items WHERE seriesName = :seriesName ORDER BY seasonNumber ASC, episodeNumber ASC")
    fun getEpisodesForSeries(seriesName: String): Flow<List<MediaItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(item: MediaItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaList(items: List<MediaItemEntity>)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteMediaById(id: String)

    @Query("DELETE FROM media_items WHERE id IN (:ids)")
    suspend fun deleteMediaByIds(ids: List<String>)

    @Query("UPDATE media_items SET isOfflineAvailable = :isOffline WHERE id = :id")
    suspend fun updateOfflineStatus(id: String, isOffline: Boolean)

    @Query("UPDATE media_items SET title = :newTitle WHERE id = :id")
    suspend fun updateTitle(id: String, newTitle: String)

    @Query("DELETE FROM media_items WHERE id LIKE 'demo_%'")
    suspend fun deleteDemoMedia()

    @Query("SELECT COUNT(*) FROM media_items")
    suspend fun getMediaCount(): Int
}
