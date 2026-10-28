package com.phoenix.qiur.screen.home

import android.graphics.Bitmap
import com.phoenix.qiur.data.model.InputSource

data class HomeUiState(
    val isProcessing: Boolean = false,
    val isUpdateAvailable: Boolean = false,
    val showCameraPopup: Boolean = false,
    val inputSource: InputSource = InputSource.STANDBY,
    val photoResult: Bitmap? = null,
    val isHistoryEnabled: Boolean = true,
)


