package com.abc.qrscannerdev.data.repository

import com.abc.qrscannerdev.data.dao.ScanResultDao
import com.abc.qrscannerdev.data.model.ScanResult
import kotlinx.coroutines.flow.Flow

class ScanResultRepository(private val scanResultDao: ScanResultDao) {
    fun getAllScanResults(): Flow<List<ScanResult>> = scanResultDao.getAllScanResults()

    fun getFavoriteScanResults(): Flow<List<ScanResult>> = scanResultDao.getFavoriteScanResults()

    suspend fun getScanResultById(id: Long): ScanResult? = scanResultDao.getScanResultById(id)

    suspend fun insert(scanResult: ScanResult): Long = scanResultDao.insert(scanResult)

    suspend fun update(scanResult: ScanResult) = scanResultDao.update(scanResult)

    suspend fun delete(scanResult: ScanResult) = scanResultDao.delete(scanResult)

    suspend fun deleteAll() = scanResultDao.deleteAll()

    fun searchScanResults(query: String): Flow<List<ScanResult>> = scanResultDao.searchScanResults(query)
} 