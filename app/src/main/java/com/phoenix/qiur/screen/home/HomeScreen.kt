package com.phoenix.qiur.screen.home

import android.media.Image
import android.net.Uri
import android.widget.Button
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultBlendMode
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import com.phoenix.qiur.R

@Composable
fun HomeScreen() {
    var showCameraPopup by remember { mutableStateOf(true) }
    var photoUri: Uri? by remember { mutableStateOf(null) }

    val pickMedia =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { photoUri = it }
        }

    BackHandler(photoUri != null) {
        photoUri = null
    }

    Scaffold { innerPadding ->
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
                AnimatedVisibility(showCameraPopup) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                            .clip(RoundedCornerShape(8))
                            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Want to use camera?",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = { showCameraPopup = false }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_close),
                                    contentDescription = "Close popup",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "QuiR respects your privacy, so by default I won't ask for camera access. You can give camera permission for a seamless experience, or pick images yourself."
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = {}, Modifier.align(Alignment.CenterHorizontally)) {
                            Text("Allow Camera Permission")
                        }
                    }
                }
            }
        }

    }
}