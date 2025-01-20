package com.abc.qrscannerdev.ui.generate

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GeneratorViewModel(application: Application) : AndroidViewModel(application) {
    private val _barcodeImage = MutableLiveData<Bitmap?>()
    val barcodeImage: LiveData<Bitmap?> = _barcodeImage

    private val _generationState = MutableLiveData<GenerationState>()
    val generationState: LiveData<GenerationState> = _generationState

    private var currentContent: String = ""
    private var currentFormat: BarcodeFormat = BarcodeFormat.QR_CODE
    private var currentSize: Int = 500

    fun generateBarcode(content: String, format: BarcodeFormat = BarcodeFormat.QR_CODE, size: Int = 500) {
        if (content.isBlank()) {
            _generationState.value = GenerationState.Error("Content cannot be empty")
            return
        }

        currentContent = content
        currentFormat = format
        currentSize = size

        viewModelScope.launch {
            try {
                _generationState.value = GenerationState.Generating
                val bitmap = withContext(Dispatchers.Default) {
                    generateBarcodeImage(content, format, size)
                }
                _barcodeImage.value = bitmap
                _generationState.value = GenerationState.Success
            } catch (e: Exception) {
                _generationState.value = GenerationState.Error(e.message ?: "Failed to generate barcode")
            }
        }
    }

    private fun generateBarcodeImage(content: String, format: BarcodeFormat, size: Int): Bitmap {
        val writer = MultiFormatWriter()
        val bitMatrix: BitMatrix = writer.encode(content, format, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }

        return bitmap
    }

    fun getCurrentBarcode(): Bitmap? = _barcodeImage.value

    fun clearBarcode() {
        _barcodeImage.value = null
        _generationState.value = GenerationState.Idle
    }
}

sealed class GenerationState {
    object Idle : GenerationState()
    object Generating : GenerationState()
    object Success : GenerationState()
    data class Error(val message: String) : GenerationState()
} 