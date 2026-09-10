package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cloud_accounts")
data class CloudAccountEntity(
    @PrimaryKey val providerId: String, // "gdrive", "dropbox", "onedrive", "webdav"
    val providerName: String, // "Google Drive", "Dropbox", "Microsoft OneDrive", "Nextcloud / WebDAV"
    val isConnected: Boolean = false,
    val userEmail: String? = null,
    val storageUsedGb: Double = 0.0,
    val storageTotalGb: Double = 15.0,
    val lastSyncTimestamp: Long = 0L,
    val autoSyncEnabled: Boolean = true
)
