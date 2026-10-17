package com.phoenix.qiur.screen.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeRoute() {
    val homeViewModel: HomeViewModel = koinViewModel()
    val appContext = LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState = homeViewModel.uiState.collectAsStateWithLifecycle()
    val surfaceRequest = homeViewModel.surfaceRequest.collectAsStateWithLifecycle()
    val result by homeViewModel.scanResult.collectAsStateWithLifecycle()

    fun snackbar(message: String) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message = message, withDismissAction = true
            )
        }
    }

    LaunchedEffect(lifecycleOwner) {
        homeViewModel.bindToCamera(appContext, lifecycleOwner)
    }
    LaunchedEffect(result) {
        result?.let { snackbar(it.toString()) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        HomeScreen(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            snackbar = { snackbar(it) },
            surfaceRequest = surfaceRequest.value,
            scanCode = { homeViewModel.onAction(HomeUiAction.ScanCode(it)) }
        )
    }
}