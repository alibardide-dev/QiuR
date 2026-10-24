package com.phoenix.qiur.screen.setting

data class SettingUiState(
    val isUpdateAvailable: Boolean = false,
    val nextUpdateVersion: String = "",
)