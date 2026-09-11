package com.example.data.repository

import android.content.Context
import android.net.Uri
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

    suspend fun deleteMediaItems(items: List<MediaItemEntity>, context: Context) = withContext(Dispatchers.IO) {
        val ids = items.map { it.id }
        items.forEach { item ->
            try {
                if (item.fileUri.startsWith("file://")) {
                    val file = java.io.File(android.net.Uri.parse(item.fileUri).path ?: "")
                    if (file.exists()) file.delete()
                } else if (item.fileUri.startsWith("content://")) {
                    context.contentResolver.delete(android.net.Uri.parse(item.fileUri), null, null)
                }
            } catch (_: Exception) {
                // Ignore physical file delete failure if read-only or restricted
            }
            watchDao.clearWatchPosition(item.id)
        }
        mediaDao.deleteMediaByIds(ids)
    }

    suspend fun deleteSingleMedia(item: MediaItemEntity, context: Context) = withContext(Dispatchers.IO) {
        deleteMediaItems(listOf(item), context)
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

    suspend fun importPickedVideo(context: Context, uri: Uri): MediaItemEntity? = withContext(Dispatchers.IO) {
        val item = SmartMediaScanner.createMediaItemFromUri(context, uri)
        if (item != null) {
            mediaDao.insertMedia(item)
        }
        item
    }

    suspend fun initializeCuratedMediaIfNeeded(context: Context? = null) = withContext(Dispatchers.IO) {
        // Hapus semua sample / demo video bawaan agar aplikasi kosongan
        mediaDao.deleteDemoMedia()

        // Pindai otomatis media lokal dari memori HP jika context tersedia
        if (context != null) {
            try {
                val scanned = SmartMediaScanner.scanDeviceMediaStore(context)
                if (scanned.isNotEmpty()) {
                    mediaDao.insertMediaList(scanned)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Siapkan akun cloud jika belum ada
        val accounts = cloudDao.getAllAccounts()
        // Cek jika akun cloud belum diinisialisasi
        val initialAccounts = listOf(
            CloudAccountEntity(
                providerId = "gdrive",
                providerName = "Google Drive",
                isConnected = false,
                userEmail = null,
                storageUsedGb = 0.0,
                storageTotalGb = 15.0,
                lastSyncTimestamp = 0L,
                autoSyncEnabled = false
            ),
            CloudAccountEntity(
                providerId = "dropbox",
                providerName = "Dropbox",
                isConnected = false,
                userEmail = null,
                storageUsedGb = 0.0,
                storageTotalGb = 5.0,
                lastSyncTimestamp = 0L,
                autoSyncEnabled = false
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
        try {
            cloudDao.insertAll(initialAccounts)
        } catch (_: Exception) {}
    }
}
