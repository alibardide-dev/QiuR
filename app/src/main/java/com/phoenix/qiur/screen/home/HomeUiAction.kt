package com.phoenix.qiur.screen.home

import android.graphics.Bitmap

sealed interface HomeUiAction {
    data class ScanCode(val bitmap: Bitmap): HomeUiAction
}