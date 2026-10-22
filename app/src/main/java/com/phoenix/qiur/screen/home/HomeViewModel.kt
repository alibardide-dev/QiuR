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
import androidx.lifecycle.viewModelScope
import com.phoenix.qiur.data.PreferencesStore
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import zxingcpp.BarcodeReader
import java.util.concurrent.Executors

class HomeViewModel(
    private val preferencesStore: PreferencesStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    val surfaceRequest = _surfaceRequest.asStateFlow()
    private val _scanResult = MutableStateFlow<List<BarcodeReader.Result>?>(null)
    val scanResult = _scanResult.asStateFlow()
    private val _photoResult = MutableStateFlow<Bitmap?>(null)
    val photoResult = _photoResult.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesStore.getSettings().collect { settings ->
                _uiState.update {
                    it.copy(askCamera = settings.askCamera, inputSource = settings.source)
                }
            }
        }
    }

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
                    imageProxy.imageInfo.rotationDegrees
                    val bitmap = imageProxy.toBitmap()
                    val result = scanCode(bitmap)
                    if (!result.isNullOrEmpty()) {
                        _scanResult.update { result }
                        _photoResult.update { bitmap }
                    }
                }
            }
        }

    fun onAction(action: HomeUiAction) {
        when (action) {
            is HomeUiAction.ChangeInput -> {
                _uiState.update { it.copy(inputSource = action.inputSource) }
                viewModelScope.launch {
                    preferencesStore.changeInputSource(action.inputSource)
                }
            }

            is HomeUiAction.ScanCodeFromPhoto -> {
                _photoResult.update { action.bitmap }
                viewModelScope.launch {
                    _scanResult.update { scanCode(action.bitmap) }
                }
            }

            HomeUiAction.CleanResult -> {
                _scanResult.update { null }
                _photoResult.update { null }
            }

            HomeUiAction.DismissCameraRequest -> {
                _uiState.update { it.copy(askCamera = false) }
                viewModelScope.launch {
                    preferencesStore.dismissCameraPopup()
                }
            }
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
                    tryInvert = true,
                    tryRotate = true,
                    tryDownscale = true,
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