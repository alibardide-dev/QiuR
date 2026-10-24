package com.phoenix.qiur.screen.about

import com.phoenix.qiur.utils.UpdateStatus

data class AboutUiState(
    val updateStatus: UpdateStatus = UpdateStatus.IDLE,
    val nextUpdateVersion: String = "",
)