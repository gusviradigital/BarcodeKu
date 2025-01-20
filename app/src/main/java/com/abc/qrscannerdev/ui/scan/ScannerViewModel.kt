package com.abc.qrscannerdev.ui.scan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.abc.qrscannerdev.data.AppDatabase
import com.abc.qrscannerdev.data.model.ScanResult
import com.abc.qrscannerdev.data.repository.ScanResultRepository
import kotlinx.coroutines.launch
import java.util.Date

class ScannerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ScanResultRepository
    private val _scanState = MutableLiveData<ScanState>()
    val scanState: LiveData<ScanState> = _scanState

    private val _isBatchMode = MutableLiveData(false)
    val isBatchMode: LiveData<Boolean> = _isBatchMode

    private val _batchResults = MutableLiveData<List<ScanResult>>(emptyList())
    val batchResults: LiveData<List<ScanResult>> = _batchResults

    init {
        val dao = AppDatabase.getDatabase(application).scanResultDao()
        repository = ScanResultRepository(dao)
    }

    fun toggleBatchMode() {
        _isBatchMode.value = !(_isBatchMode.value ?: false)
        if (!(_isBatchMode.value ?: false)) {
            _batchResults.value = emptyList()
        }
    }

    fun processScanResult(content: String, format: String) {
        if (_isBatchMode.value == true) {
            val newResult = ScanResult(
                content = content,
                format = format,
                timestamp = Date()
            )
            val currentList = _batchResults.value.orEmpty().toMutableList()
            currentList.add(newResult)
            _batchResults.value = currentList
        } else {
            saveScanResult(content, format)
        }
    }

    fun saveScanResult(content: String, format: String) {
        viewModelScope.launch {
            val scanResult = ScanResult(
                content = content,
                format = format,
                timestamp = Date()
            )
            repository.insert(scanResult)
        }
    }

    fun saveBatchResults() {
        viewModelScope.launch {
            _batchResults.value?.forEach { result ->
                repository.insert(result)
            }
            _batchResults.value = emptyList()
            _isBatchMode.value = false
        }
    }

    fun updateScanState(state: ScanState) {
        _scanState.value = state
    }
}

sealed class ScanState {
    object Idle : ScanState()
    object Scanning : ScanState()
    data class Success(val content: String, val format: String) : ScanState()
    data class Error(val message: String) : ScanState()
} 