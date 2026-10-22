package com.phoenix.qiur.data.model

data class UserSettings(
    val askCamera: Boolean,
    val source: InputSource,
)

enum class InputSource {
    STANDBY, CAMERA, PICTURE
}