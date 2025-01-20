package com.abc.qrscannerdev.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.abc.qrscannerdev.data.AppDatabase
import com.abc.qrscannerdev.data.model.ScanResult
import com.abc.qrscannerdev.data.repository.ScanResultRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ScanResultRepository
    private val searchQuery = MutableStateFlow("")
    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: LiveData<Boolean> = _showFavoritesOnly.asLiveData()

    init {
        val dao = AppDatabase.getDatabase(application).scanResultDao()
        repository = ScanResultRepository(dao)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val scanResults: LiveData<List<ScanResult>> = searchQuery.flatMapLatest { query ->
        if (query.isEmpty()) {
            _showFavoritesOnly.flatMapLatest { showFavorites ->
                if (showFavorites) {
                    repository.getFavoriteScanResults()
                } else {
                    repository.getAllScanResults()
                }
            }
        } else {
            repository.searchScanResults(query)
        }
    }.asLiveData()

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun toggleShowFavoritesOnly() {
        _showFavoritesOnly.value = !_showFavoritesOnly.value
    }

    fun toggleFavorite(scanResult: ScanResult) {
        viewModelScope.launch {
            repository.update(scanResult.copy(isFavorite = !scanResult.isFavorite))
        }
    }

    fun delete(scanResult: ScanResult) {
        viewModelScope.launch {
            repository.delete(scanResult)
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }
} 