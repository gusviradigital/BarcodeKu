package com.abc.qrscannerdev.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.abc.qrscannerdev.data.model.ScanResult
import com.abc.qrscannerdev.databinding.ItemScanHistoryBinding
import java.text.SimpleDateFormat
import java.util.Locale

class ScanResultAdapter(
    private val onItemClick: (ScanResult) -> Unit,
    private val onFavoriteClick: (ScanResult) -> Unit,
    private val onDeleteClick: (ScanResult) -> Unit
) : ListAdapter<ScanResult, ScanResultAdapter.ViewHolder>(ScanResultDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScanHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemScanHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }

            binding.moreButton.setOnClickListener {
                // Show popup menu with favorite and delete options
                showPopupMenu(binding.moreButton, getItem(adapterPosition))
            }
        }

        fun bind(scanResult: ScanResult) {
            binding.apply {
                barcodeContent.text = scanResult.content
                barcodeType.text = scanResult.format
                scanDate.text = formatDate(scanResult.timestamp)
                // Set favorite icon state
                // Load thumbnail if available
            }
        }

        private fun formatDate(date: java.util.Date): String {
            return SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(date)
        }

        private fun showPopupMenu(view: android.view.View, scanResult: ScanResult) {
            android.widget.PopupMenu(view.context, view).apply {
                menuInflater.inflate(com.abc.qrscannerdev.R.menu.scan_result_menu, menu)
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        com.abc.qrscannerdev.R.id.action_favorite -> {
                            onFavoriteClick(scanResult)
                            true
                        }
                        com.abc.qrscannerdev.R.id.action_delete -> {
                            onDeleteClick(scanResult)
                            true
                        }
                        else -> false
                    }
                }
                show()
            }
        }
    }
}

private class ScanResultDiffCallback : DiffUtil.ItemCallback<ScanResult>() {
    override fun areItemsTheSame(oldItem: ScanResult, newItem: ScanResult): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ScanResult, newItem: ScanResult): Boolean {
        return oldItem == newItem
    }
} 