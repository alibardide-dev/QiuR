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
import com.phoenix.qiur.data.dao.HistoryDao
import com.phoenix.qiur.data.model.History
import com.phoenix.qiur.utils.CodeResult
import com.phoenix.qiur.utils.UpdateStateHolder
import com.phoenix.qiur.utils.UpdateStatus
import com.phoenix.qiur.utils.applyExif
import com.phoenix.qiur.utils.barcodeEncoder
import com.phoenix.qiur.utils.downscale
import com.phoenix.qiur.utils.matrixEncoder
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
import java.util.Date
import java.util.concurrent.Executors
import com.phoenix.qiur.utils.render1D
import com.phoenix.qiur.utils.renderMatrix
import com.phoenix.qiur.utils.withLabel

class HomeViewModel(
    private val dataStoreManager: DataStoreManager,
    private val updateStateHolder: UpdateStateHolder,
    private val historyDao: HistoryDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    val surfaceRequest = _surfaceRequest.asStateFlow()
    private val _scanResults = MutableStateFlow<List<CodeResult>>(emptyList())
    val scanResults = _scanResults.asStateFlow()
    private val _history = MutableStateFlow<List<History>>(emptyList())
    val history = _history.asStateFlow()
    private val barcodeReader = BarcodeReader().apply {
        options = BarcodeReader.Options(
            tryHarder = true,
            tryDownscale = true,
        )
    }

    init {
        viewModelScope.launch {
            dataStoreManager.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        showCameraPopup = settings.showCameraPopup,
                        inputSource = settings.source,
                        isHistoryEnabled = settings.isHistoryEnabled
                    )
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            updateStateHolder.checkForUpdates()
        }
        viewModelScope.launch {
            _history.update { historyDao.getAllEntries() }
            updateStateHolder.updateState.collect { state ->
                _uiState.update { it.copy(isUpdateAvailable = state.updateStatus == UpdateStatus.AVAILABLE) }
            }
        }
    }

    private val analysisExecutor = Executors.newSingleThreadExecutor()

    private val previewUseCase = Preview.Builder().build().apply {
        setSurfaceProvider { request -> _surfaceRequest.value = request }
    }

    @Volatile
    private var pausedUntil = 0L
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
                        val result = bitmap.scanForCode()
                        if (result.isNotEmpty()) {
                            _uiState.update { it.copy(photoResult = bitmap.downscale()) }
                            updateScanResults(
                                result.map { CodeResult(it.format, it.text?.toCodeContent()) }
                            )
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
                    val orientation =
                        action.context.contentResolver.openInputStream(action.uri).use {
                            ExifInterface(it!!).getAttributeInt(
                                ExifInterface.TAG_ORIENTATION,
                                ExifInterface.ORIENTATION_NORMAL
                            )
                        }
                    val bitmap = action.uri
                        .toBitmap(action.context)
                        .applyExif(orientation)

                    _uiState.update { it.copy(photoResult = bitmap.downscale()) }
                    updateScanResults(
                        bitmap.scanForCode()
                            .map { CodeResult(it.format, it.text?.toCodeContent()) }
                    )

                    _uiState.update { it.copy(isProcessing = false) }
                }
            }

            HomeUiAction.CleanResult -> {
                _uiState.update { it.copy(photoResult = null) }
                _scanResults.update { emptyList() }
            }

            HomeUiAction.DismissCameraRequest -> {
                _uiState.update { it.copy(showCameraPopup = false) }
                viewModelScope.launch {
                    dataStoreManager.dismissCameraPopup()
                }
            }

            is HomeUiAction.SetHistoryEnabled -> {
                _uiState.update { it.copy(isHistoryEnabled = action.isEnabled) }
                viewModelScope.launch {
                    dataStoreManager.setHistoryEnabled(action.isEnabled)
                }
            }

            is HomeUiAction.GenerateQRCode -> {
                viewModelScope.launch {
                    val bitmap = generateBitmapCode(action.raw, action.format)
                    _uiState.update { it.copy(photoResult = bitmap) }
                    updateScanResults(
                        listOf(CodeResult(action.format, action.raw.toCodeContent()))
                    )
                }
            }

            is HomeUiAction.DeleteHistoryEntry -> {
                viewModelScope.launch {
                    historyDao.deleteEntry(action.history)
                    _history.update { historyDao.getAllEntries() }
                }
            }

            HomeUiAction.CleanHistory -> {
                viewModelScope.launch {
                    historyDao.deleteAll()
                    _history.update { emptyList() }
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

    private fun Bitmap.scanForCode(): List<BarcodeReader.Result> {
        try {
            return barcodeReader.read(this)
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    private fun generateBitmapCode(
        content: String,
        format: BarcodeReader.Format,
    ): Bitmap? {
        val barcodeEncoder = barcodeEncoder(format)
        val matrixEncoder = matrixEncoder(format)
        if (barcodeEncoder == null && matrixEncoder == null)
            throw Exception("This is not a code stupid, you're no supposed to reach here")

        barcodeEncoder?.let {
            return render1D(it.encode(content)).withLabel(content)
        }
        matrixEncoder?.let {
            val code = it.encode(content)
            return renderMatrix(code.width, { x, y -> code[x, y] })
        }

        return null
    }

    private fun updateScanResults(codeResults: List<CodeResult>) {
        _scanResults.update { codeResults }
        if (uiState.value.isHistoryEnabled) {
            viewModelScope.launch {
                addHistory(codeResults)
                _history.update { historyDao.getAllEntries() }
            }
        }
    }

    private suspend fun addHistory(codeContents: List<CodeResult>) {
        if (uiState.value.isHistoryEnabled) {
            codeContents.forEach { code ->
                historyDao.insertEntry(
                    History(
                        raw = code.content!!.raw,
                        format = code.format.toString(),
                        type = code.content.type.toString(),
                        createdAt = Date(System.currentTimeMillis()),
                        updatedAt = Date(System.currentTimeMillis())
                    )
                )
            }
        }
    }

    override fun onCleared() {
        analysisUseCase.clearAnalyzer()
        analysisExecutor.shutdown()
    }

}