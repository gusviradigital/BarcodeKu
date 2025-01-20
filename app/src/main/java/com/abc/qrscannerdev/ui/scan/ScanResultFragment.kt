package com.abc.qrscannerdev.ui.scan

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.abc.qrscannerdev.R
import com.abc.qrscannerdev.databinding.FragmentScanResultBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ScanResultFragment : Fragment() {
    private var _binding: FragmentScanResultBinding? = null
    private val binding get() = _binding!!
    private val args: ScanResultFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupActions()
    }

    private fun setupUI() {
        binding.apply {
            resultContent.text = args.scanResult
            resultFormat.text = args.barcodeFormat
            // Generate and display barcode image
        }
    }

    private fun setupActions() {
        binding.apply {
            copyButton.setOnClickListener { copyToClipboard() }
            shareButton.setOnClickListener { shareContent() }
            actionButton.setOnClickListener { handleAction() }
        }
    }

    private fun copyToClipboard() {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Scan Result", args.scanResult)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), R.string.result_copied, Toast.LENGTH_SHORT).show()
    }

    private fun shareContent() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, args.scanResult)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.result_share_via)))
    }

    private fun handleAction() {
        when {
            args.scanResult.startsWith("http://") || args.scanResult.startsWith("https://") -> {
                showUrlDialog()
            }
            args.scanResult.startsWith("tel:") -> {
                showPhoneDialog()
            }
            args.scanResult.startsWith("mailto:") -> {
                showEmailDialog()
            }
            // Add more content type handlers
        }
    }

    private fun showUrlDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.result_open_url)
            .setMessage(args.scanResult)
            .setPositiveButton(R.string.ok) { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse(args.scanResult)
                }
                startActivity(intent)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showPhoneDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.result_call_number)
            .setMessage(args.scanResult.substring(4))
            .setPositiveButton(R.string.ok) { _, _ ->
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = android.net.Uri.parse(args.scanResult)
                }
                startActivity(intent)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showEmailDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.result_send_email)
            .setMessage(args.scanResult.substring(7))
            .setPositiveButton(R.string.ok) { _, _ ->
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = android.net.Uri.parse(args.scanResult)
                }
                startActivity(intent)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 