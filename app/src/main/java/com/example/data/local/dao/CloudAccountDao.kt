package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.CloudAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CloudAccountDao {
    @Query("SELECT * FROM cloud_accounts")
    fun getAllAccounts(): Flow<List<CloudAccountEntity>>

    @Query("SELECT * FROM cloud_accounts WHERE isConnected = 1")
    fun getConnectedAccounts(): Flow<List<CloudAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(account: CloudAccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<CloudAccountEntity>)

    @Query("UPDATE cloud_accounts SET lastSyncTimestamp = :timestamp WHERE providerId = :providerId")
    suspend fun updateSyncTimestamp(providerId: String, timestamp: Long)

    @Query("UPDATE cloud_accounts SET isConnected = :connected, userEmail = :email WHERE providerId = :providerId")
    suspend fun updateConnection(providerId: String, connected: Boolean, email: String?)
}
