package com.phoenix.qiur.screen.home

import android.content.Context
import android.net.Uri
import com.phoenix.qiur.data.model.InputSource

sealed interface HomeUiAction {
    data class ChangeInput(val inputSource: InputSource): HomeUiAction
    data class ScanCodeFromUri(val context: Context, val uri: Uri): HomeUiAction
    data object CleanResult: HomeUiAction
    data object DismissCameraRequest: HomeUiAction
}