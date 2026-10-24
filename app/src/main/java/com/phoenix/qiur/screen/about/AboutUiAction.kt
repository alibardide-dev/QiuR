package com.phoenix.qiur.screen.about

sealed interface AboutUiAction {
    data object CheckForUpdates: AboutUiAction
}