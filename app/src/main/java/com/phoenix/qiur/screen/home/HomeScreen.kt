package com.phoenix.qiur.screen.home

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.SurfaceRequest
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import coil3.compose.rememberAsyncImagePainter
import com.phoenix.qiur.R
import com.phoenix.qiur.data.model.InputSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import zxingcpp.BarcodeReader

@Composable
fun HomeScreen(
    modifier: Modifier,
    snackbar: (String) -> Unit,
    inputSource: InputSource,
    onChangeInput: (InputSource) -> Unit,
    surfaceRequest: SurfaceRequest?,
    scanCode: (Bitmap) -> Unit,
    resultPhoto: Bitmap?,
    result:  List<BarcodeReader.Result>?,
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
                    val bitmap = context.contentResolver.openInputStream(uri).use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }
                    scanCode(bitmap)
                }
            }
        }

    BackHandler(resultPhoto != null) {
        clearResult()
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(resultPhoto != null) { hasPicture ->
            if (hasPicture) {
                ResultScreen(
                    bitmap = resultPhoto,
                    onClickClose = { clearResult() }
                )
            } else {
                Crossfade(inputSource) { source ->
                    Box(Modifier.fillMaxSize()) {
                        HomeFloatingToolbar(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = -ScreenOffset)
                                .zIndex(1f),
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
                            InputSource.STANDBY -> {}

                            InputSource.CAMERA ->
                                surfaceRequest?.let { request ->
                                    CameraXViewfinder(
                                        surfaceRequest = request,
                                        modifier = Modifier.fillMaxSize()
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
                                        PhotoPickerWidget(
                                            pickMedia = {
//                                    pickMedia.launch(
//                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
//                                    )
                                                openDocument.launch(arrayOf("image/*"))
                                            }
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
private fun HomeFloatingToolbar(
    modifier: Modifier,
    onClickCamera: () -> Unit,
    onClickPicture: () -> Unit,
) {
    HorizontalFloatingToolbar(
        modifier = modifier,
        expanded = true
    ) {
        IconButton(onClick = { onClickCamera() }) {
            Icon(
                painter = painterResource(R.drawable.ic_camera),
                contentDescription = "Switch to camera"
            )
        }
        IconButton(onClick = { onClickPicture() }) {
            Icon(
                painter = painterResource(R.drawable.ic_photo),
                contentDescription = "Switch to picture"
            )
        }
    }
}

@Composable
private fun PhotoPickerWidget(
    pickMedia: () -> Unit
) {
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

@Composable
private fun ResultScreen(
    bitmap: Bitmap?,
    onClickClose: () -> Unit
) {
    Box {
        Image(
            painter = rememberAsyncImagePainter(bitmap),
            contentDescription = "Selected Photo",
            modifier = Modifier.fillMaxSize()
        )
        IconButton(
            onClick = { onClickClose() },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            ),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = "Close popup",
                tint = MaterialTheme.colorScheme.onSurface,
            )
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

