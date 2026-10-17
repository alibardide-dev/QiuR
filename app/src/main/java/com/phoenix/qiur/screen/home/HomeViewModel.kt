package com.phoenix.qiur.screen.home

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import zxingcpp.BarcodeReader
import java.util.concurrent.Executors

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    val surfaceRequest = _surfaceRequest.asStateFlow()
    private val _scanResult = MutableStateFlow<List<BarcodeReader.Result>?>(null)
    val scanResult = _scanResult.asStateFlow()

    private val analysisExecutor = Executors.newSingleThreadExecutor()

    private val previewUseCase = Preview.Builder().build().apply {
        setSurfaceProvider { request -> _surfaceRequest.value = request }
    }

    private val analysisUseCase = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .apply {
            setAnalyzer(analysisExecutor) { imageProxy ->
                imageProxy.use { imageProxy ->
                    val bitmap = imageProxy.toBitmap()
                    val result = scanCode(bitmap)
                    if (result != null && result.map { it.text } != _scanResult.value?.map { it.text }) {
                        _scanResult.update { result }
                    }
                }
            }
        }

    fun onAction(action: HomeUiAction) {
        when (action) {
            is HomeUiAction.ScanCode ->
                _scanResult.update { scanCode(action.bitmap) }
        }
    }

    suspend fun bindToCamera(context: Context, lifecycleOwner: LifecycleOwner) {
        val provider = ProcessCameraProvider.awaitInstance(context)
        provider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            previewUseCase,
            analysisUseCase
        )
        try {
            awaitCancellation()
        } finally {
            provider.unbindAll()
        }
    }

    private fun scanCode(
        bitmap: Bitmap,
    ): List<BarcodeReader.Result>? {
        try {
            val barcodeReader = BarcodeReader().apply {
                options = BarcodeReader.Options(
                    tryHarder = true,
                    tryDenoise = true,
                    tryDownscale = true,
                    returnErrors = true
                )
            }
            return barcodeReader.read(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    override fun onCleared() {
        analysisUseCase.clearAnalyzer()
        analysisExecutor.shutdown()
    }

}