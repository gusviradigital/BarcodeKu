package com.abc.qrscannerdev.data.dao

import androidx.room.*
import com.abc.qrscannerdev.data.model.ScanResult
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanResultDao {
    @Query("SELECT * FROM scan_results ORDER BY timestamp DESC")
    fun getAllScanResults(): Flow<List<ScanResult>>

    @Query("SELECT * FROM scan_results WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteScanResults(): Flow<List<ScanResult>>

    @Query("SELECT * FROM scan_results WHERE id = :id")
    suspend fun getScanResultById(id: Long): ScanResult?

    @Insert
    suspend fun insert(scanResult: ScanResult): Long

    @Update
    suspend fun update(scanResult: ScanResult)

    @Delete
    suspend fun delete(scanResult: ScanResult)

    @Query("DELETE FROM scan_results")
    suspend fun deleteAll()

    @Query("SELECT * FROM scan_results WHERE content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchScanResults(query: String): Flow<List<ScanResult>>
} 