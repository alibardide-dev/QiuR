package com.phoenix.qiur.screen.about

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AboutRoute(
    navigateBack: () -> Unit,
) {
    val aboutViewModel: AboutViewModel = koinViewModel()
    val uiState by aboutViewModel.uiState.collectAsStateWithLifecycle()

    AboutScreen(
        onClickBack = { navigateBack() },
        onClickUpdate = { aboutViewModel.onAction(AboutUiAction.CheckForUpdates) },
        updateStatus = uiState.updateStatus,
        nextUpdateVersion = uiState.nextUpdateVersion,
    )
}