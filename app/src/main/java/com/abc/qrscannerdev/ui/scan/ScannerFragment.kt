package com.abc.qrscannerdev.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.abc.qrscannerdev.R
import com.abc.qrscannerdev.databinding.FragmentScannerBinding
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@androidx.camera.core.ExperimentalGetImage
class ScannerFragment : Fragment() {
    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScannerViewModel by viewModels()
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner
    
    private var isAutoFocusEnabled = true
    private var lastScanTime = 0L
    private val scanCooldown = 1000L // 1 detik cooldown antara scan

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.scanner_permission_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCamera()
        setupUI()
        observeViewModel()
    }

    private fun setupCamera() {
        cameraExecutor = Executors.newSingleThreadExecutor()
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_AZTEC,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_PDF417,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E
            )
            .build()
        barcodeScanner = BarcodeScanning.getClient(options)

        if (hasPermissions()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun setupUI() {
        binding.apply {
            flashButton.setOnClickListener { toggleFlash() }
            focusButton.apply {
                isChecked = isAutoFocusEnabled
                setOnClickListener { toggleAutoFocus() }
            }
            batchModeButton.apply {
                isChecked = viewModel.isBatchMode.value == true
                setOnClickListener { 
                    viewModel.toggleBatchMode()
                    updateBatchModeUI(isChecked)
                }
            }
        }
    }

    private fun updateBatchModeUI(enabled: Boolean) {
        binding.batchModeButton.apply {
            isChecked = enabled
            icon = ContextCompat.getDrawable(requireContext(),
                if (enabled) R.drawable.ic_batch_mode_active
                else R.drawable.ic_batch_mode
            )
        }
        
        if (enabled) {
            Snackbar.make(
                binding.root,
                getString(R.string.scanner_batch_mode_enabled),
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun observeViewModel() {
        viewModel.scanState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ScanState.Success -> handleScanSuccess(state.content, state.format)
                is ScanState.Error -> showError(state.message)
                is ScanState.Scanning -> binding.scanningProgress.visibility = View.VISIBLE
                is ScanState.Idle -> binding.scanningProgress.visibility = View.GONE
            }
        }

        viewModel.isBatchMode.observe(viewLifecycleOwner) { isBatchMode ->
            updateBatchModeUI(isBatchMode)
        }

        viewModel.batchResults.observe(viewLifecycleOwner) { results ->
            if (results.isNotEmpty()) {
                showBatchResults(results.map { it.content })
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases() {
        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

        imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImage(imageProxy)
                }
            }

        try {
            cameraProvider?.unbindAll()
            camera = cameraProvider?.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalyzer
            )

            // Setup initial auto focus
            setupAutoFocus()
        } catch (e: Exception) {
            showError("Failed to bind camera use cases")
        }
    }

    private fun setupAutoFocus() {
        camera?.cameraControl?.let { control ->
            if (isAutoFocusEnabled) {
                val factory = SurfaceOrientedMeteringPointFactory(
                    binding.viewFinder.width.toFloat(),
                    binding.viewFinder.height.toFloat()
                )
                val centerPoint = factory.createPoint(0.5f, 0.5f)
                val action = FocusMeteringAction.Builder(centerPoint)
                    .setAutoCancelDuration(2, TimeUnit.SECONDS)
                    .build()
                control.startFocusAndMetering(action)
            }
        }
    }

    private fun processImage(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastScanTime < scanCooldown) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        val barcode = barcodes[0]
                        barcode.rawValue?.let { content ->
                            lastScanTime = currentTime
                            viewModel.processScanResult(content, barcode.format.toString())
                            vibrateOnScan()
                        }
                    }
                }
                .addOnFailureListener {
                    viewModel.updateScanState(ScanState.Error(it.message ?: "Failed to process image"))
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun handleScanSuccess(content: String, format: String) {
        if (viewModel.isBatchMode.value == true) {
            showBatchScanNotification(content)
        } else {
            showScanSuccessNotification()
            findNavController().navigate(
                ScannerFragmentDirections.actionScanToResult(content, format)
            )
        }
    }

    private fun showScanSuccessNotification() {
        Snackbar.make(
            binding.root,
            getString(R.string.scanner_success),
            Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun showBatchScanNotification(content: String) {
        Snackbar.make(
            binding.root,
            getString(R.string.scanner_batch_item_added, content),
            Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun showBatchResults(results: List<String>) {
        Snackbar.make(
            binding.root,
            getString(R.string.scanner_batch_complete, results.size),
            Snackbar.LENGTH_LONG
        ).setAction(getString(R.string.view)) {
            // Navigate to batch results
        }.show()
    }

    private fun toggleFlash() {
        camera?.cameraControl?.enableTorch(
            !(camera?.cameraInfo?.torchState?.value == TorchState.ON)
        )?.addListener({
            binding.flashButton.icon = ContextCompat.getDrawable(
                requireContext(),
                if (camera?.cameraInfo?.torchState?.value == TorchState.ON)
                    R.drawable.ic_flash_on
                else
                    R.drawable.ic_flash_off
            )
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun toggleAutoFocus() {
        isAutoFocusEnabled = !isAutoFocusEnabled
        binding.focusButton.isChecked = isAutoFocusEnabled
        
        if (isAutoFocusEnabled) {
            setupAutoFocus()
        } else {
            camera?.cameraControl?.cancelFocusAndMetering()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun vibrateOnScan() {
        val vibrator = ContextCompat.getSystemService(requireContext(), Vibrator::class.java)
        vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
} 