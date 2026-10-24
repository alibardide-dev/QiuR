package com.phoenix.qiur.screen.home

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.SurfaceRequest
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.phoenix.qiur.R
import com.phoenix.qiur.data.model.InputSource
import com.phoenix.qiur.screen.home.component.CameraPermissionPopup
import com.phoenix.qiur.screen.home.component.PhotoPickerScreen
import com.phoenix.qiur.screen.home.component.ResultScreen
import com.phoenix.qiur.utils.CodeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    modifier: Modifier,
    snackbar: (String) -> Unit,
    onPermissionGranted: () -> Unit,
    isUpdateAvailable: Boolean,
    inputSource: InputSource,
    onChangeInput: (InputSource) -> Unit,
    surfaceRequest: SurfaceRequest?,
    scanCode: (Uri) -> Unit,
    isProcessing: Boolean,
    resultPhoto: Bitmap?,
    results: List<CodeResult>,
    clearResult: () -> Unit,
    showCameraPopup: Boolean,
    dismissCameraRequest: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraAccess by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                dismissCameraRequest()
                onPermissionGranted()
                hasCameraAccess = true
                onChangeInput(InputSource.CAMERA)
                snackbar("We got live camera")
            } else {
                snackbar("No camera it is.")
            }
        }

    val openDocument =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { uri ->
                coroutineScope.launch(Dispatchers.IO) {
                    scanCode(uri)
                }
            }
        }

    BackHandler(resultPhoto != null) {
        clearResult()
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(isProcessing) {
            LinearProgressIndicator()
        }
        AnimatedContent(resultPhoto != null) { hasPicture ->
            if (hasPicture) {
                ResultScreen(
                    bitmap = resultPhoto,
                    results = results,
                    onClickClose = { clearResult() }
                )
            } else {
                Crossfade(inputSource) { source ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        HomeToolbar(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = -ScreenOffset)
                                .zIndex(1f),
                            isUpdateAvailable = isUpdateAvailable,
                            inputSource = inputSource,
                            onClickCamera = {
                                if (hasCameraAccess)
                                    onChangeInput(InputSource.CAMERA)
                                else
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            onClickPicture = {
                                onChangeInput(InputSource.PICTURE)
                            }
                        )
                        when (source) {
                            InputSource.STANDBY -> {
                                CircularWavyProgressIndicator()
                            }

                            InputSource.CAMERA -> {
                                surfaceRequest?.let { request ->
                                    CameraXViewfinder(
                                        surfaceRequest = request,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(
                                                bottom = ScreenOffset + 96.dp,
                                                top = 24.dp,
                                                start = 24.dp,
                                                end = 24.dp
                                            )
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .fillMaxWidth()
                                        .offset(y = (-56).dp)
                                        .padding(64.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .border(
                                            width = 2.dp,
                                            color = MaterialTheme.colorScheme.primaryFixed,
                                            shape = RoundedCornerShape(24.dp)
                                        )
                                )
                            }

                            InputSource.PICTURE ->
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    AnimatedVisibility(
                                        visible = resultPhoto == null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                    ) {
                                        PhotoPickerScreen(
                                            pickMedia = { openDocument.launch(arrayOf("image/*")) }
                                        )
                                    }
                                    AnimatedVisibility(
                                        visible = showCameraPopup,
                                        modifier = Modifier
                                            .align(Alignment.CenterHorizontally)
                                            .offset(y = -ScreenOffset - 56.dp),
                                        exit = shrinkVertically()
                                    ) {
                                        CameraPermissionPopup(
                                            onClickDismiss = {
                                                dismissCameraRequest()
                                            },
                                            onClickAllow = {
                                                permissionLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        )
                                    }
                                }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeToolbar(
    modifier: Modifier,
    isUpdateAvailable: Boolean,
    inputSource: InputSource,
    onClickCamera: () -> Unit,
    onClickPicture: () -> Unit,
) {
    HorizontalFloatingToolbar(
        modifier = modifier,
        expanded = true
    ) {
        BadgedBox(
            badge = {
                if (isUpdateAvailable) Badge()
            }
        ) {
            IconButton(onClick = {}) {
                Icon(painterResource(R.drawable.ic_qr_code), contentDescription = null)
            }
        }
        VerticalDivider(Modifier.height(48.dp))
        IconButton(
            onClick = { onClickCamera() },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor =
                    animateColorAsState(
                        if(inputSource == InputSource.CAMERA) MaterialTheme.colorScheme.secondaryContainer
                        else Color.Transparent
                    ).value
            )
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_camera),
                contentDescription = "Switch to camera"
            )
        }
        IconButton(
            onClick = { onClickPicture() },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor =
                    animateColorAsState(
                        if(inputSource == InputSource.PICTURE) MaterialTheme.colorScheme.secondaryContainer
                        else Color.Transparent
                    ).value
            )
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_photo),
                contentDescription = "Switch to picture"
            )
        }
    }
}





