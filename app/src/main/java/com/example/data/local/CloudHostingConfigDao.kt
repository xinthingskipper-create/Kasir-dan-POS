package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CloudHostingConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface CloudHostingConfigDao {

    @Query("SELECT * FROM cloud_hosting_config WHERE id = 1 LIMIT 1")
    fun getConfigFlow(): Flow<CloudHostingConfig?>

    @Query("SELECT * FROM cloud_hosting_config WHERE id = 1 LIMIT 1")
    suspend fun getConfigSync(): CloudHostingConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConfig(config: CloudHostingConfig)

    @Query("UPDATE cloud_hosting_config SET lastBackupTimestamp = :timestamp WHERE id = 1")
    suspend fun updateLastBackupTimestamp(timestamp: Long)
}
