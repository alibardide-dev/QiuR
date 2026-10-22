package com.phoenix.qiur.screen.home

import android.graphics.Bitmap
import com.phoenix.qiur.data.model.InputSource

sealed interface HomeUiAction {
    data class ChangeInput(val inputSource: InputSource): HomeUiAction
    data class ScanCodeFromPhoto(val bitmap: Bitmap): HomeUiAction
    data object CleanResult: HomeUiAction
    data object DismissCameraRequest: HomeUiAction
}