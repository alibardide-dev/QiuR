package com.phoenix.qiur.data.model

data class UserSettings(
    val showCameraPopup: Boolean,
    val source: InputSource,
    val isHistoryEnabled: Boolean,
)

enum class InputSource {
    STANDBY, CAMERA, PICTURE
}