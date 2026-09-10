package com.example.data.repository

import android.content.Context
import com.example.data.local.MoviLishDatabase
import com.example.data.local.entity.CloudAccountEntity
import com.example.data.local.entity.MediaItemEntity
import com.example.data.local.entity.WatchPositionEntity
import com.example.data.model.MediaSource
import com.example.data.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class MediaRepository(private val database: MoviLishDatabase) {
    private val mediaDao = database.mediaDao()
    private val watchDao = database.watchPositionDao()
    private val cloudDao = database.cloudAccountDao()

    val allMedia: Flow<List<MediaItemEntity>> = mediaDao.getAllMediaItems()
    val allMovies: Flow<List<MediaItemEntity>> = mediaDao.getMediaByType(MediaType.MOVIE)
    val allTvShows: Flow<List<MediaItemEntity>> = mediaDao.getMediaByType(MediaType.TV_SERIES)
    val offlineMedia: Flow<List<MediaItemEntity>> = mediaDao.getOfflineMedia()
    val cloudMedia: Flow<List<MediaItemEntity>> = mediaDao.getCloudMedia()
    val watchPositions: Flow<List<WatchPositionEntity>> = watchDao.getAllWatchPositions()
    val cloudAccounts: Flow<List<CloudAccountEntity>> = cloudDao.getAllAccounts()

    suspend fun getMediaById(id: String): MediaItemEntity? = withContext(Dispatchers.IO) {
        mediaDao.getMediaById(id)
    }

    suspend fun getWatchPosition(mediaId: String): WatchPositionEntity? = withContext(Dispatchers.IO) {
        watchDao.getPositionForMedia(mediaId)
    }

    fun observeWatchPosition(mediaId: String): Flow<WatchPositionEntity?> =
        watchDao.observePositionForMedia(mediaId)

    suspend fun saveWatchPosition(
        mediaId: String,
        positionMs: Long,
        durationMs: Long,
        isCompleted: Boolean = false,
        selectedSubtitleId: String? = null,
        subtitleVerticalOffsetDp: Int = 36,
        subtitleTimingOffsetMs: Long = 0L
    ) = withContext(Dispatchers.IO) {
        watchDao.saveWatchPosition(
            WatchPositionEntity(
                mediaId = mediaId,
                positionMs = positionMs,
                durationMs = durationMs,
                lastWatchedTimestamp = System.currentTimeMillis(),
                isCompleted = isCompleted,
                selectedSubtitleId = selectedSubtitleId,
                subtitleVerticalOffsetDp = subtitleVerticalOffsetDp,
                subtitleTimingOffsetMs = subtitleTimingOffsetMs
            )
        )
    }

    suspend fun insertMedia(item: MediaItemEntity) = withContext(Dispatchers.IO) {
        mediaDao.insertMedia(item)
    }

    suspend fun updateOfflineStatus(id: String, isOffline: Boolean) = withContext(Dispatchers.IO) {
        mediaDao.updateOfflineStatus(id, isOffline)
    }

    suspend fun deleteMedia(id: String) = withContext(Dispatchers.IO) {
        mediaDao.deleteMediaById(id)
        watchDao.clearWatchPosition(id)
    }

    suspend fun scanDeviceLibrary(context: Context): Int = withContext(Dispatchers.IO) {
        val scanned = SmartMediaScanner.scanDeviceMediaStore(context)
        if (scanned.isNotEmpty()) {
            mediaDao.insertMediaList(scanned)
        }
        scanned.size
    }

    suspend fun syncCloudAccount(providerId: String) = withContext(Dispatchers.IO) {
        cloudDao.updateSyncTimestamp(providerId, System.currentTimeMillis())
    }

    suspend fun toggleCloudConnection(providerId: String, isConnected: Boolean, email: String? = null) = withContext(Dispatchers.IO) {
        cloudDao.updateConnection(providerId, isConnected, email)
        if (isConnected) {
            cloudDao.updateSyncTimestamp(providerId, System.currentTimeMillis())
        }
    }

    suspend fun initializeCuratedMediaIfNeeded() = withContext(Dispatchers.IO) {
        val count = mediaDao.getMediaCount()
        if (count == 0) {
            val initialList = getPreloadedCuratedMedia()
            mediaDao.insertMediaList(initialList)

            // Also seed sample watch positions so "Lanjutkan Menonton" has immediate demo value
            watchDao.saveWatchPosition(
                WatchPositionEntity(
                    mediaId = "demo_movie_1",
                    positionMs = 184000L,
                    durationMs = 596000L,
                    lastWatchedTimestamp = System.currentTimeMillis() - 3600000L,
                    isCompleted = false
                )
            )
            watchDao.saveWatchPosition(
                WatchPositionEntity(
                    mediaId = "demo_tv_1_1",
                    positionMs = 310000L,
                    durationMs = 653000L,
                    lastWatchedTimestamp = System.currentTimeMillis() - 7200000L,
                    isCompleted = false
                )
            )

            // Seed cloud accounts
            val initialAccounts = listOf(
                CloudAccountEntity(
                    providerId = "gdrive",
                    providerName = "Google Drive",
                    isConnected = true,
                    userEmail = "movilish.user@gmail.com",
                    storageUsedGb = 4.2,
                    storageTotalGb = 15.0,
                    lastSyncTimestamp = System.currentTimeMillis() - 180000L,
                    autoSyncEnabled = true
                ),
                CloudAccountEntity(
                    providerId = "dropbox",
                    providerName = "Dropbox",
                    isConnected = true,
                    userEmail = "cinemafan@dropbox.com",
                    storageUsedGb = 1.8,
                    storageTotalGb = 5.0,
                    lastSyncTimestamp = System.currentTimeMillis() - 840000L,
                    autoSyncEnabled = true
                ),
                CloudAccountEntity(
                    providerId = "onedrive",
                    providerName = "Microsoft OneDrive",
                    isConnected = false,
                    userEmail = null,
                    storageUsedGb = 0.0,
                    storageTotalGb = 5.0,
                    lastSyncTimestamp = 0L,
                    autoSyncEnabled = false
                ),
                CloudAccountEntity(
                    providerId = "webdav",
                    providerName = "Nextcloud / WebDAV Server",
                    isConnected = false,
                    userEmail = null,
                    storageUsedGb = 0.0,
                    storageTotalGb = 50.0,
                    lastSyncTimestamp = 0L,
                    autoSyncEnabled = false
                )
            )
            cloudDao.insertAll(initialAccounts)
        }
    }

    private fun getPreloadedCuratedMedia(): List<MediaItemEntity> {
        return listOf(
            // Movies
            MediaItemEntity(
                id = "demo_movie_1",
                title = "Big Buck Bunny",
                mediaType = MediaType.MOVIE,
                fileUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                format = "MP4",
                durationMs = 596000L,
                thumbnailUri = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=800",
                mediaSource = MediaSource.ONLINE_STREAM,
                isOfflineAvailable = true,
                releaseYear = 2008,
                fileSizeFormatted = "158 MB"
            ),
            MediaItemEntity(
                id = "demo_movie_2",
                title = "Sintel (4K HDR Cinema)",
                mediaType = MediaType.MOVIE,
                fileUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                format = "MKV",
                durationMs = 888000L,
                thumbnailUri = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=800",
                mediaSource = MediaSource.ONLINE_STREAM,
                isOfflineAvailable = true,
                releaseYear = 2010,
                fileSizeFormatted = "240 MB"
            ),
            MediaItemEntity(
                id = "demo_movie_3",
                title = "Tears of Steel (Sci-Fi Edition)",
                mediaType = MediaType.MOVIE,
                fileUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                format = "WebM",
                durationMs = 734000L,
                thumbnailUri = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800",
                mediaSource = MediaSource.ONLINE_STREAM,
                isOfflineAvailable = false,
                releaseYear = 2012,
                fileSizeFormatted = "192 MB"
            ),
            MediaItemEntity(
                id = "demo_movie_4",
                title = "Cosmic Nomad Express",
                mediaType = MediaType.MOVIE,
                fileUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4",
                format = "MP4",
                durationMs = 300000L,
                thumbnailUri = "https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=800",
                mediaSource = MediaSource.CLOUD,
                isOfflineAvailable = false,
                cloudProvider = "Google Drive",
                releaseYear = 2021,
                fileSizeFormatted = "95 MB"
            ),

            // TV Series: Elephants Dream (Season 1)
            MediaItemEntity(
                id = "demo_tv_1_1",
                title = "Elephants Dream S01E01",
                mediaType = MediaType.TV_SERIES,
                seriesName = "Elephants Dream",
                seasonNumber = 1,
                episodeNumber = 1,
                episodeTitle = "The Machine Architecture",
                fileUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                format = "MP4",
                durationMs = 653000L,
                thumbnailUri = "https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?w=800",
                mediaSource = MediaSource.ONLINE_STREAM,
                isOfflineAvailable = true,
                releaseYear = 2006,
                fileSizeFormatted = "180 MB"
            ),
            MediaItemEntity(
                id = "demo_tv_1_2",
                title = "Elephants Dream S01E02",
                mediaType = MediaType.TV_SERIES,
                seriesName = "Elephants Dream",
                seasonNumber = 1,
                episodeNumber = 2,
                episodeTitle = "The Eternal Core",
                fileUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                format = "MP4",
                durationMs = 450000L,
                thumbnailUri = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800",
                mediaSource = MediaSource.ONLINE_STREAM,
                isOfflineAvailable = true,
                releaseYear = 2006,
                fileSizeFormatted = "120 MB"
            ),
            MediaItemEntity(
                id = "demo_tv_1_3",
                title = "Elephants Dream S01E03",
                mediaType = MediaType.TV_SERIES,
                seriesName = "Elephants Dream",
                seasonNumber = 1,
                episodeNumber = 3,
                episodeTitle = "Awakening of the Giants",
                fileUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                format = "MKV",
                durationMs = 490000L,
                thumbnailUri = "https://images.unsplash.com/photo-1511447333015-45b65e60f6d5?w=800",
                mediaSource = MediaSource.ONLINE_STREAM,
                isOfflineAvailable = false,
                releaseYear = 2006,
                fileSizeFormatted = "135 MB"
            ),

            // TV Series: Cyber City Chronicles (Season 2)
            MediaItemEntity(
                id = "demo_tv_2_1",
                title = "Cyber City S02E01",
                mediaType = MediaType.TV_SERIES,
                seriesName = "Cyber City",
                seasonNumber = 2,
                episodeNumber = 1,
                episodeTitle = "Neon Genesis Protocol",
                fileUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                format = "MP4",
                durationMs = 380000L,
                thumbnailUri = "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?w=800",
                mediaSource = MediaSource.CLOUD,
                isOfflineAvailable = true,
                cloudProvider = "Dropbox",
                releaseYear = 2023,
                fileSizeFormatted = "110 MB"
            ),
            MediaItemEntity(
                id = "demo_tv_2_2",
                title = "Cyber City S02E02",
                mediaType = MediaType.TV_SERIES,
                seriesName = "Cyber City",
                seasonNumber = 2,
                episodeNumber = 2,
                episodeTitle = "Shadows in the Grid",
                fileUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
                format = "MP4",
                durationMs = 420000L,
                thumbnailUri = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800",
                mediaSource = MediaSource.CLOUD,
                isOfflineAvailable = false,
                cloudProvider = "Dropbox",
                releaseYear = 2023,
                fileSizeFormatted = "125 MB"
            )
        )
    }
}
