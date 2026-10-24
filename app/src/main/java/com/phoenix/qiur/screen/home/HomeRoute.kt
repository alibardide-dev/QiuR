package com.phoenix.qiur.screen.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeRoute(
    navigateToAbout: () -> Unit,
    navigateToSetting: () -> Unit,
) {
    val homeViewModel: HomeViewModel = koinViewModel()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val surfaceRequest by homeViewModel.surfaceRequest.collectAsStateWithLifecycle()
    val results by homeViewModel.scanResults.collectAsStateWithLifecycle()
    val photoResult by homeViewModel.photoResult.collectAsStateWithLifecycle()
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    fun snackbar(message: String) {
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = message, withDismissAction = true, duration = SnackbarDuration.Indefinite
            )
        }
    }
    LaunchedEffect(hasCameraPermission, lifecycleOwner) {
        homeViewModel.bindToCamera(context, lifecycleOwner)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        HomeScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            onPermissionGranted = { hasCameraPermission = true },
            onClickMenu = { navigateToAbout() },
            isUpdateAvailable = uiState.isUpdateAvailable,
            inputSource = uiState.inputSource,
            onChangeInput = { homeViewModel.onAction(HomeUiAction.ChangeInput(it)) },
            snackbar = { snackbar(it) },
            surfaceRequest = surfaceRequest,
            scanCode = { homeViewModel.onAction(HomeUiAction.ScanCodeFromUri(context, it)) },
            isProcessing = uiState.isProcessing,
            resultPhoto = photoResult,
            results = results,
            clearResult = { homeViewModel.onAction(HomeUiAction.CleanResult) },
            showCameraPopup = uiState.askCamera,
            dismissCameraRequest = { homeViewModel.onAction(HomeUiAction.DismissCameraRequest) },
        )
    }
}