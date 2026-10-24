package com.phoenix.qiur.screen.setting

import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingRoute() {
    val settingViewModel: SettingViewModel = koinViewModel()

    SettingScreen()
}