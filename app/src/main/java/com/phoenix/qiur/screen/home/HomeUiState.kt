package com.phoenix.qiur.screen.home

import com.phoenix.qiur.data.model.InputSource

data class HomeUiState(
    val askCamera: Boolean = false,
    val inputSource: InputSource = InputSource.STANDBY
)

