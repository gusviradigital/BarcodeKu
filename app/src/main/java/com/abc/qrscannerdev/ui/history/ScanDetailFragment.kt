package com.abc.qrscannerdev.ui.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.abc.qrscannerdev.R
import com.abc.qrscannerdev.data.model.ScanResult
import com.abc.qrscannerdev.databinding.FragmentScanDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Locale

class ScanDetailFragment : Fragment() {
    private var _binding: FragmentScanDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HistoryViewModel by viewModels()
    private val args: ScanDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupContent()
        setupActions()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    private fun setupContent() {
        viewModel.scanResults.observe(viewLifecycleOwner) { results ->
            val scan = results.find { it.id == args.scanId }
            scan?.let { updateUI(it) }
        }
    }

    private fun updateUI(scan: ScanResult) {
        binding.apply {
            contentText.text = scan.content
            formatText.text = scan.format
            timestampText.text = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault())
                .format(scan.timestamp)
            favoriteButton.setImageResource(
                if (scan.isFavorite) R.drawable.ic_favorite_filled
                else R.drawable.ic_favorite
            )
        }
    }

    private fun setupActions() {
        binding.apply {
            copyButton.setOnClickListener { copyToClipboard() }
            shareButton.setOnClickListener { shareContent() }
            favoriteButton.setOnClickListener { toggleFavorite() }
            actionButton.setOnClickListener { handleAction() }
        }
    }

    private fun copyToClipboard() {
        val content = binding.contentText.text.toString()
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Scan Result", content)
        clipboard.setPrimaryClip(clip)
        showMessage(getString(R.string.result_copied))
    }

    private fun shareContent() {
        val content = binding.contentText.text.toString()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, content)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.result_share_via)))
    }

    private fun toggleFavorite() {
        viewModel.scanResults.value?.find { it.id == args.scanId }?.let {
            viewModel.toggleFavorite(it)
        }
    }

    private fun handleAction() {
        val content = binding.contentText.text.toString()
        when {
            content.startsWith("http://") || content.startsWith("https://") -> {
                openUrl(content)
            }
            content.startsWith("tel:") -> {
                dialNumber(content.substring(4))
            }
            content.startsWith("mailto:") -> {
                sendEmail(content.substring(7))
            }
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            showError(getString(R.string.error_generic))
        }
    }

    private fun dialNumber(number: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
            startActivity(intent)
        } catch (e: Exception) {
            showError(getString(R.string.error_generic))
        }
    }

    private fun sendEmail(email: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
            startActivity(intent)
        } catch (e: Exception) {
            showError(getString(R.string.error_generic))
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 