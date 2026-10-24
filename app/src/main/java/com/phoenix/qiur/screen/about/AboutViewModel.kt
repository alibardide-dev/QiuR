package com.phoenix.qiur.screen.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoenix.qiur.data.DataStoreManager
import com.phoenix.qiur.utils.UpdateStateHolder
import com.phoenix.qiur.utils.UpdateStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AboutViewModel(
    private val updateStateHolder: UpdateStateHolder,
): ViewModel() {

    private val _uiState = MutableStateFlow(AboutUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            updateStateHolder.collectLastData()
            updateStateHolder.updateState.collect { state ->
                _uiState.update { it.copy(
                    updateStatus = state.updateStatus,
                    nextUpdateVersion = state.nextUpdateVersion
                ) }
            }
        }
    }

    fun onAction(action: AboutUiAction) {
        when (action) {
            AboutUiAction.CheckForUpdates -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(updateStatus = UpdateStatus.CHECKING) }
                    updateStateHolder.checkForUpdates()
                    updateStateHolder.updateState.collect { state ->
                        _uiState.update {
                            it.copy(
                                updateStatus = state.updateStatus,
                                nextUpdateVersion = state.nextUpdateVersion
                            )
                        }
                    }
                }
            }
        }
    }

}