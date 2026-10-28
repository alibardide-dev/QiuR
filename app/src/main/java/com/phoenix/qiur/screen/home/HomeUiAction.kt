package com.phoenix.qiur.screen.home

import android.content.Context
import android.net.Uri
import com.phoenix.qiur.data.model.History
import com.phoenix.qiur.data.model.InputSource
import zxingcpp.BarcodeReader

sealed interface HomeUiAction {
    data class ChangeInput(val inputSource: InputSource): HomeUiAction
    data class ScanCodeFromUri(val context: Context, val uri: Uri): HomeUiAction
    data class GenerateQRCode(val raw: String, val format: BarcodeReader.Format): HomeUiAction
    data object CleanResult: HomeUiAction
    data object DismissCameraRequest: HomeUiAction
    data class SetHistoryEnabled(val isEnabled: Boolean): HomeUiAction
    data class DeleteHistoryEntry(val history: History): HomeUiAction
    data object CleanHistory: HomeUiAction
}