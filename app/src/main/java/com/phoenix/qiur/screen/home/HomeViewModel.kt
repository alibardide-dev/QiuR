package com.phoenix.qiur.screen.home

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoenix.qiur.data.DataStoreManager
import com.phoenix.qiur.utils.CodeResult
import com.phoenix.qiur.utils.UpdateStateHolder
import com.phoenix.qiur.utils.UpdateStatus
import com.phoenix.qiur.utils.applyExif
import com.phoenix.qiur.utils.downscale
import com.phoenix.qiur.utils.rotate
import com.phoenix.qiur.utils.toBitmap
import com.phoenix.qiur.utils.toCodeContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import zxingcpp.BarcodeReader
import java.util.concurrent.Executors

class HomeViewModel(
    private val dataStoreManager: DataStoreManager,
    private val updateStateHolder: UpdateStateHolder,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    val surfaceRequest = _surfaceRequest.asStateFlow()
    private val _scanResults = MutableStateFlow<List<CodeResult>>(emptyList())
    val scanResults = _scanResults.asStateFlow()
    private val _photoResult = MutableStateFlow<Bitmap?>(null)
    val photoResult = _photoResult.asStateFlow()
    private val barcodeReader = BarcodeReader().apply {
        options = BarcodeReader.Options(
            tryHarder = true,
//            tryDenoise = true,
//            tryInvert = true,
//            tryRotate = true,
            tryDownscale = true,
        )
    }

    init {
        viewModelScope.launch {
            dataStoreManager.settings.collect { settings ->
                _uiState.update {
                    it.copy(askCamera = settings.askCamera, inputSource = settings.source)
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            updateStateHolder.checkForUpdates()
        }
        viewModelScope.launch {
            updateStateHolder.updateState.collect { state ->
                _uiState.update { it.copy(isUpdateAvailable = state.updateStatus == UpdateStatus.AVAILABLE) }
            }
        }
    }

    private val analysisExecutor = Executors.newSingleThreadExecutor()

    private val previewUseCase = Preview.Builder().build().apply {
        setSurfaceProvider { request -> _surfaceRequest.value = request }
    }

    @Volatile private var pausedUntil = 0L
    private val analysisUseCase = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .apply {
            setAnalyzer(analysisExecutor) { imageProxy ->
                viewModelScope.launch(Dispatchers.Default) {
                    if (SystemClock.elapsedRealtime() < pausedUntil) {
                        imageProxy.close()
                        return@launch
                    }
                    imageProxy.use { proxy ->
                        val bitmap = proxy.toBitmap().rotate(proxy.imageInfo.rotationDegrees)
                        val result = scanCode(bitmap)
                        if (result.isNotEmpty()) {
                            _photoResult.update { bitmap.downscale() }
                            _scanResults.update {
                                result.map { CodeResult(it.format, it.text?.toCodeContent()) }
                            }
                            pausedUntil = SystemClock.elapsedRealtime() + 1000
                        }
                    }
                }
            }
        }

    fun onAction(action: HomeUiAction) {
        when (action) {
            is HomeUiAction.ChangeInput -> {
                _uiState.update { it.copy(inputSource = action.inputSource) }
                viewModelScope.launch {
                    dataStoreManager.changeInputSource(action.inputSource)
                }
            }

            is HomeUiAction.ScanCodeFromUri -> {
                viewModelScope.launch(Dispatchers.Default) {
                    _uiState.update { it.copy(isProcessing = true) }
                    val orientation = action.context.contentResolver.openInputStream(action.uri).use {
                        ExifInterface(it!!).getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )
                    }
                    val bitmap = action.uri
                        .toBitmap(action.context)
                        .applyExif(orientation)

                    _photoResult.update { bitmap.downscale() }
                    _scanResults.update {
                        scanCode(bitmap)
                            .map { CodeResult(it.format, it.text?.toCodeContent()) }
                    }
                    _uiState.update { it.copy(isProcessing = false) }
                }
            }

            HomeUiAction.CleanResult -> {
                _photoResult.update { null }
                _scanResults.update { emptyList() }
            }

            HomeUiAction.DismissCameraRequest -> {
                _uiState.update { it.copy(askCamera = false) }
                viewModelScope.launch {
                    dataStoreManager.dismissCameraPopup()
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
    ): List<BarcodeReader.Result> {
        try {
            return barcodeReader.read(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    override fun onCleared() {
        analysisUseCase.clearAnalyzer()
        analysisExecutor.shutdown()
    }

}