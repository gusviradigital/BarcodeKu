package com.abc.qrscannerdev.ui.generate

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.abc.qrscannerdev.R
import com.abc.qrscannerdev.databinding.FragmentGeneratorBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import java.io.File
import java.io.FileOutputStream

class GeneratorFragment : Fragment() {
    private var _binding: FragmentGeneratorBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GeneratorViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGeneratorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupFormatSpinner()
        setupInputListeners()
        setupButtons()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_clear -> {
                    clearInputs()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupFormatSpinner() {
        val formats = resources.getStringArray(R.array.barcode_format_entries)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, formats)
        binding.formatInput.setAdapter(adapter)
        binding.formatInput.setText(formats[0], false)
    }

    private fun setupInputListeners() {
        binding.apply {
            contentInput.doAfterTextChanged { generateBarcode() }
            formatInput.setOnItemClickListener { _, _, _, _ -> generateBarcode() }
            sizeInput.doAfterTextChanged { generateBarcode() }
        }
    }

    private fun setupButtons() {
        binding.apply {
            saveButton.setOnClickListener { saveBarcode() }
            shareButton.setOnClickListener { shareBarcode() }
        }
    }

    private fun observeViewModel() {
        viewModel.barcodeImage.observe(viewLifecycleOwner) { bitmap ->
            binding.barcodePreview.setImageBitmap(bitmap)
            binding.saveButton.isEnabled = bitmap != null
            binding.shareButton.isEnabled = bitmap != null
        }

        viewModel.generationState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is GenerationState.Error -> showError(state.message)
                is GenerationState.Generating -> showLoading()
                is GenerationState.Success -> hideLoading()
                is GenerationState.Idle -> hideLoading()
            }
        }
    }

    private fun generateBarcode() {
        val content = binding.contentInput.text?.toString() ?: ""
        val formatStr = binding.formatInput.text?.toString()
        val sizeStr = binding.sizeInput.text?.toString()

        if (content.isNotEmpty() && !formatStr.isNullOrEmpty() && !sizeStr.isNullOrEmpty()) {
            try {
                val format = getBarcodeFormat(formatStr)
                val size = sizeStr.toIntOrNull() ?: 512
                viewModel.generateBarcode(content, format, size)
            } catch (e: Exception) {
                showError(e.message ?: "Invalid input")
            }
        }
    }

    private fun getBarcodeFormat(formatStr: String): BarcodeFormat {
        val formatValues = resources.getStringArray(R.array.barcode_format_values)
        val formatEntries = resources.getStringArray(R.array.barcode_format_entries)
        val index = formatEntries.indexOf(formatStr)
        return if (index != -1) {
            BarcodeFormat.valueOf(formatValues[index])
        } else {
            BarcodeFormat.QR_CODE
        }
    }

    private fun saveBarcode() {
        viewModel.getCurrentBarcode()?.let { (content, format, _) ->
            viewModel.barcodeImage.value?.let { bitmap ->
                try {
                    val file = createImageFile(content, format)
                    saveBitmapToFile(bitmap, file)
                    showSuccessDialog(getString(R.string.generator_save_success))
                } catch (e: Exception) {
                    showError(getString(R.string.generator_save_error))
                }
            }
        }
    }

    private fun shareBarcode() {
        viewModel.getCurrentBarcode()?.let { (content, format, _) ->
            viewModel.barcodeImage.value?.let { bitmap ->
                try {
                    val file = createImageFile(content, format)
                    saveBitmapToFile(bitmap, file)
                    shareImage(file)
                } catch (e: Exception) {
                    showError(getString(R.string.generator_share_error))
                }
            }
        }
    }

    private fun createImageFile(content: String, format: BarcodeFormat): File {
        val fileName = "barcode_${System.currentTimeMillis()}.png"
        return File(requireContext().cacheDir, fileName)
    }

    private fun saveBitmapToFile(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    private fun shareImage(file: File) {
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.generator_share_via)))
    }

    private fun clearInputs() {
        binding.apply {
            contentInput.text?.clear()
            formatInput.setText(resources.getStringArray(R.array.barcode_format_entries)[0], false)
            sizeInput.setText("512")
        }
        viewModel.clearBarcode()
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showSuccessDialog(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.barcodePreview.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.barcodePreview.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onBarcodeGenerated(content: String, format: BarcodeFormat, size: Int) {
        showSuccessDialog(getString(R.string.generator_save_success))
        binding.barcodePreview.contentDescription = "Generated $format barcode for: $content ($size px)"
    }

    private fun onBarcodeGenerationError(content: String, format: BarcodeFormat, size: Int) {
        showError("Failed to generate $format barcode for: $content with size $size px")
    }

    private fun clearInput() {
        binding.contentInput.text?.clear()
        binding.formatInput.setText(resources.getStringArray(R.array.barcode_format_entries)[0], false)
        binding.sizeInput.setText("512")
        viewModel.clearBarcode()
    }
} 