package com.phoenix.qiur.screen.home

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.SurfaceRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
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
import androidx.core.content.ContextCompat
import coil3.compose.rememberAsyncImagePainter
import com.phoenix.qiur.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    modifier: Modifier,
    snackbar: (String) -> Unit,
    surfaceRequest: SurfaceRequest?,
    scanCode: (Bitmap) -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showCameraPopup by remember { mutableStateOf(true) }
    var hasCameraAccess by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var photoUri: Uri? by remember { mutableStateOf(null) }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                showCameraPopup = false
                hasCameraAccess = true
                snackbar("We got live camera")
            } else
                snackbar("No camera it is.")
        }

//    val pickMedia =
//        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
//            uri?.let { uri ->
//                photoUri = uri
//                coroutineScope.launch(Dispatchers.IO) {
//                    val bitmap = context.contentResolver.openInputStream(uri).use { inputStream ->
//                        BitmapFactory.decodeStream(inputStream)
//                    }
//                    scanCode(bitmap)
//                }
//            }
//        }
    val openDocument =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { uri ->
                photoUri = uri
                coroutineScope.launch(Dispatchers.IO) {
                    val bitmap = context.contentResolver.openInputStream(uri).use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }
                    scanCode(bitmap)
                }
            }
        }

    BackHandler(photoUri != null) {
        photoUri = null
    }


    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = photoUri != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ResultScreen(
                photoUri = photoUri,
                onClickClose = { photoUri = null }
            )
        }
        Crossfade(hasCameraAccess) { hasAccess ->
            Box(Modifier.fillMaxSize()) {
                if (hasAccess) {
                    surfaceRequest?.let { request ->
                        CameraXViewfinder(
                            surfaceRequest = request,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        AnimatedVisibility(
                            visible = photoUri == null,
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
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            exit = shrinkVertically()
                        ) {
                            CameraPermissionPopup(
                                onClickDismiss = { showCameraPopup = false },
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
    photoUri: Uri?,
    onClickClose: () -> Unit
) {
    Box {
        Image(
            painter = rememberAsyncImagePainter(photoUri),
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

