package com.phoenix.qiur.screen.home

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import coil3.compose.rememberAsyncImagePainter
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.phoenix.qiur.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCameraPopup by remember { mutableStateOf(true) }
    var hasCameraAccess by remember { mutableStateOf(false) }
    var photoUri: Uri? by remember { mutableStateOf(null) }

    fun snackbar(message: String) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message = message, withDismissAction = true
            )
        }
    }

    val askPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                showCameraPopup = false
                hasCameraAccess = true
                snackbar("We got live camera")
            } else
                snackbar("No camera it is.")
        }

    val pickMedia =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let {
                photoUri = it

                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val image = InputImage.fromFilePath(context, uri)
                        val scanner = BarcodeScanning.getClient()
                        val result = scanner.process(image)
                            .addOnSuccessListener { barcodes ->
//                                if (barcodes.isEmpty())
//                                    snackbar("You Sure it's the right one?")
//                                else
                                    snackbar(barcodes.map { it.rawValue }.toString())
//                                for (barcode in barcodes) {
//                                    snackbar(barcode.rawValue.toString())
//                                }
                            }
                            .addOnFailureListener {
                                snackbar("Houston, we have a problem")
                            }
                    } catch (e: Exception) {
                        Toast.makeText(context, e.message.toString(), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

    BackHandler(photoUri != null) {
        photoUri = null
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            AnimatedVisibility(
                visible = photoUri != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box {
                    Image(
                        painter = rememberAsyncImagePainter(photoUri),
                        contentDescription = "Selected Photo",
                        modifier = Modifier.fillMaxSize()
                    )
                    IconButton(
                        onClick = { photoUri = null },
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
                            onClick = {
                                pickMedia.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
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
                CameraPermissionPopup(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    showCameraPopup = showCameraPopup,
                    onClickDismiss = { showCameraPopup = false },
                    onClickAllow = {
                        askPermission.launch(Manifest.permission.CAMERA)
                    }
                )
            }
        }

    }
}

@Composable
private fun CameraPermissionPopup(
    modifier: Modifier,
    showCameraPopup: Boolean,
    onClickDismiss: () -> Unit,
    onClickAllow: () -> Unit
) {
    AnimatedVisibility(showCameraPopup, modifier) {
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
                text = "QiuR respects your privacy, so by default I won't ask for camera access. You can give camera permission for a seamless experience, or pick images yourself.",
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
}