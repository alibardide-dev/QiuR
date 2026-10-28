package com.phoenix.qiur.screen.home

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.SurfaceRequest
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.phoenix.qiur.R
import com.phoenix.qiur.data.model.History
import com.phoenix.qiur.data.model.InputSource
import com.phoenix.qiur.screen.home.component.HistoryActions
import com.phoenix.qiur.screen.home.component.HistoryBottomSheet
import com.phoenix.qiur.screen.home.component.MenuActions
import com.phoenix.qiur.screen.home.component.MenuBottomSheet
import com.phoenix.qiur.screen.home.component.ResultScreen
import com.phoenix.qiur.utils.CodeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import zxingcpp.BarcodeReader

data class HomeActions(
    val onPermissionGranted: () -> Unit,
    val setKeepHistory: (Boolean) -> Unit,
    val setInputSource: (InputSource) -> Unit,
    val scanCodeFromUri: (Uri) -> Unit,
    val generateQRCode: (String, BarcodeReader.Format) -> Unit,
    val clearResult: () -> Unit,
    val dismissCameraRequest: () -> Unit,
    val openAbout: () -> Unit,
    val deleteHistoryEntry: (History) -> Unit,
    val cleanHistory: () -> Unit,
)

@Composable
fun HomeScreen(
    modifier: Modifier,
    snackbar: (String) -> Unit,
    uiState: HomeUiState,
    actions: HomeActions,
    surfaceRequest: SurfaceRequest?,
    results: List<CodeResult>,
    historyEntries: List<History>,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
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
                actions.dismissCameraRequest()
                actions.onPermissionGranted()
                hasCameraAccess = true
                actions.setInputSource(InputSource.CAMERA)
                snackbar("We got live camera")
            } else {
                snackbar("No camera it is.")
            }
        }
    val openDocument =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { uri ->
                coroutineScope.launch(Dispatchers.IO) {
                    actions.scanCodeFromUri(uri)
                }
            }
        }

    BackHandler(uiState.photoResult != null) {
        actions.clearResult()
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(uiState.isProcessing) {
            LinearProgressIndicator()
        }
        AnimatedContent(uiState.photoResult != null) { hasPicture ->
            if (hasPicture) {
                ResultScreen(
                    bitmap = uiState.photoResult,
                    results = results,
                    onClickClose = actions.clearResult,
                    snackbar = snackbar
                )
            } else {
                Crossfade(uiState.inputSource) { source ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        HomeToolbar(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = -ScreenOffset)
                                .zIndex(1f),
                            isUpdateAvailable = uiState.isUpdateAvailable,
                            inputSource = uiState.inputSource,
                            onClickMenu = { showMenu = true },
                            onClickCamera = {
                                if (hasCameraAccess)
                                    actions.setInputSource(InputSource.CAMERA)
                                else
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            onClickPicture = {
                                actions.setInputSource(InputSource.PICTURE)
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
                                        visible = uiState.photoResult == null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                    ) {
                                        PhotoPickerScreen(
                                            pickMedia = { openDocument.launch(arrayOf("image/*")) }
                                        )
                                    }
                                    AnimatedVisibility(
                                        visible = uiState.showCameraPopup,
                                        modifier = Modifier
                                            .align(Alignment.CenterHorizontally)
                                            .offset(y = -ScreenOffset - 56.dp),
                                        exit = shrinkVertically()
                                    ) {
                                        CameraPermissionPopup(
                                            onClickDismiss = {
                                                actions.dismissCameraRequest()
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

    if (showMenu) {
        MenuBottomSheet(
            onDismiss = { showMenu = false },
            actions = MenuActions(
                setKeepHistory = { actions.setKeepHistory(it) },
                openHistory = { showHistory = true },
                openAbout = { actions.openAbout() }
            ),
            isHistoryEnabled = uiState.isHistoryEnabled,
            isUpdateAvailable = uiState.isUpdateAvailable,
        )
    }
    if (showHistory) {
        HistoryBottomSheet(
            onDismiss = { showHistory = false },
            entries = historyEntries,
            actions = HistoryActions(
                open = {
                    actions.generateQRCode(it.raw, BarcodeReader.Format.valueOf(it.format))
                    showMenu = false
                       },
                delete = { actions.deleteHistoryEntry(it) },
                deleteAll = { actions.cleanHistory() },
            ),
        )
    }
}

@Composable
private fun HomeToolbar(
    modifier: Modifier,
    isUpdateAvailable: Boolean,
    inputSource: InputSource,
    onClickMenu: () -> Unit,
    onClickCamera: () -> Unit,
    onClickPicture: () -> Unit,
) {
    HorizontalFloatingToolbar(
        modifier = modifier,
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(
            toolbarContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
            toolbarContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        expanded = true
    ) {
        BadgedBox(
            badge = {
                if (isUpdateAvailable) Badge()
            }
        ) {
            IconButton(
                onClick = { onClickMenu() },
                shapes = IconButtonShapes(
                    CircleShape,
                    CircleShape,
                ),
                modifier = Modifier.height(56.dp),
            ) {
                Icon(painterResource(R.drawable.ic_qr_code), contentDescription = null)
            }
        }
        Spacer(Modifier.width(8.dp))
        VerticalDivider(
            modifier = Modifier.height(56.dp),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(Modifier.width(8.dp))
        ToggleButton(
            checked = inputSource == InputSource.CAMERA,
            onCheckedChange = { onClickCamera() },
            colors = ToggleButtonDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            shapes = ToggleButtonShapes(
                CircleShape,
                CircleShape,
                CircleShape
            ),
            modifier = Modifier.height(56.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_camera),
                    contentDescription = "Camera"
                )
                AnimatedVisibility(
                    visible = inputSource == InputSource.CAMERA,
                    enter = expandHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec()),
                    exit = shrinkHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec())
                ) {
                    Text(
                        text = "Camera",
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.padding(start = ButtonDefaults.IconSpacing)
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        ToggleButton(
            checked = inputSource == InputSource.PICTURE,
            onCheckedChange = { onClickPicture() },
            colors = ToggleButtonDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            shapes = ToggleButtonShapes(
                CircleShape,
                CircleShape,
                CircleShape
            ),
            modifier = Modifier.height(56.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_photo),
                    contentDescription = "Picture"
                )
                AnimatedVisibility(
                    visible = inputSource == InputSource.PICTURE,
                    enter = expandHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec()),
                    exit = shrinkHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec())
                ) {
                    Text(
                        text = "Picture",
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.padding(start = ButtonDefaults.IconSpacing)
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionPopup(
    onClickDismiss: () -> Unit,
    onClickAllow: () -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(min = 280.dp, max = 560.dp)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .clip(RoundedCornerShape(12))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(24.dp)
    ) {
        Text(
            text = "Want to use camera?",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "QiuR respects your privacy. You can give camera permission for a seamless experience.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Row(Modifier.align(Alignment.End)) {
            TextButton(onClick = { onClickDismiss() }) {
                Text("Dismiss")
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { onClickAllow() }) {
                Text("Allow Camera")
            }
        }
    }
}

@Composable
private fun PhotoPickerScreen(pickMedia: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.home_blob),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                modifier = Modifier.size(144.dp)
            )
            Icon(
                painter = painterResource(R.drawable.ic_qr_code),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(96.dp)
            )
        }
        Spacer(Modifier.height(48.dp))
        FloatingActionButton(
            onClick = { pickMedia() }
        ) {
            Row(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_photo),
                    contentDescription = null,
                )
                Spacer(Modifier.width(8.dp))
                Text("Pick a Photo")
            }
        }
    }
}