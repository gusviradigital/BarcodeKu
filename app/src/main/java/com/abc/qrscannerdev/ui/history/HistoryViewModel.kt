package com.abc.qrscannerdev.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asLiveData
import com.abc.qrscannerdev.data.AppDatabase
import com.abc.qrscannerdev.data.model.ScanResult
import com.abc.qrscannerdev.data.repository.ScanResultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ScanResultRepository
    private val searchQuery = MutableStateFlow("")
    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: LiveData<Boolean> = _showFavoritesOnly.asLiveData()
    private val _scanResults = MutableLiveData<List<ScanResult>>()
    
    init {
        val dao = AppDatabase.getDatabase(application).scanResultDao()
        repository = ScanResultRepository(dao)
        loadScanResults()
    }

    val scanResults: LiveData<List<ScanResult>> = searchQuery.flatMapLatest { query ->
        if (query.isEmpty()) {
            _showFavoritesOnly.flatMapLatest { showFavorites ->
                if (showFavorites) {
                    repository.getFavoriteScanResults().map { it }
                } else {
                    repository.getAllScanResults().map { it }
                }
            }
        } else {
            repository.searchScanResults(query).map { it }
        }
    }.asLiveData()

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun toggleShowFavoritesOnly() {
        _showFavoritesOnly.value = !_showFavoritesOnly.value
    }

    fun toggleFavorite(scanResult: ScanResult) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.update(scanResult.copy(isFavorite = !scanResult.isFavorite))
            loadScanResults()
        }
    }

    fun delete(scanResult: ScanResult) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(scanResult)
            loadScanResults()
        }
    }

    fun deleteAll() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAll()
            loadScanResults()
        }
    }

    private fun loadScanResults() {
        viewModelScope.launch {
            val results = withContext(Dispatchers.IO) {
                repository.getAllScanResults().map { it }.asLiveData().value ?: emptyList()
            }
            _scanResults.value = results
        }
    }

    fun getAllScans(callback: (List<ScanResult>) -> Unit) {
        viewModelScope.launch {
            val scans = withContext(Dispatchers.IO) {
                repository.getAllScanResults().map { it }.asLiveData().value ?: emptyList()
            }
            callback(scans)
        }
    }
} 