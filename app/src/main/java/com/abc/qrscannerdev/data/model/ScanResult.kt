package com.abc.qrscannerdev.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "scan_results")
data class ScanResult(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val format: String,
    val timestamp: Date,
    val isFavorite: Boolean = false,
    val thumbnailPath: String? = null
) 