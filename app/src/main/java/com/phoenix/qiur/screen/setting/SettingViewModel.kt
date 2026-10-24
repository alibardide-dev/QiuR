package com.phoenix.qiur.screen.setting

import androidx.lifecycle.ViewModel
import com.phoenix.qiur.data.DataStoreManager
import com.phoenix.qiur.utils.UpdateStateHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingViewModel(
    private val dataStoreManager: DataStoreManager,
    private val updateStateHolder: UpdateStateHolder,
): ViewModel() {

    private val _uiState = MutableStateFlow<SettingUiState>(SettingUiState())
    val uiState = _uiState.asStateFlow()

}