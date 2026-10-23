package com.phoenix.qiur.screen.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun CameraPermissionPopup(
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
