package com.phoenix.qiur.screen.about

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phoenix.qiur.BuildConfig
import com.phoenix.qiur.R
import com.phoenix.qiur.screen.about.component.LicenseBottomSheet
import com.phoenix.qiur.ui.theme.firstListShape
import com.phoenix.qiur.ui.theme.lastListShape
import com.phoenix.qiur.ui.theme.loneListShape
import com.phoenix.qiur.utils.UpdateStatus

data class AboutActions(
    val navigateBack: () -> Unit,
    val update: () -> Unit,
)

@Composable
fun AboutScreen(
    uiState: AboutUiState,
    actions: AboutActions,
) {
    val uriHandler = LocalUriHandler.current
    var showLicense by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = "About", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(
                        onClick = { actions.navigateBack() },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back to home",
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .fillMaxSize()
                .padding(vertical = 16.dp, horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(firstListShape)
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(32.dp))
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "QiuR",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                FilledTonalIconButton(
                    onClick = {
                        uriHandler.openUri("https://GitHub.com/alibardide-dev/QiuR")
                    },
                    shapes = IconButtonDefaults.shapes()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_github),
                        contentDescription = "Open GitHub",
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(lastListShape)
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
                    .clickable {
                        if (uiState.updateStatus != UpdateStatus.CHECKING) {
                            if (uiState.updateStatus == UpdateStatus.AVAILABLE) {
                                uriHandler.openUri("https://github.com/alibardide-dev/QiuR/releases/latest")
                            } else {
                                actions.update()
                            }
                        }
                    }
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BadgedBox(
                    badge = {
                        if (uiState.updateStatus == UpdateStatus.AVAILABLE)
                            Badge()
                    }
                ) {
                    Crossfade(uiState.updateStatus == UpdateStatus.CHECKING) { target ->
                        if (target) {
                            CircularProgressIndicator(Modifier.size(24.dp))
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_sync),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text =
                            if (uiState.updateStatus == UpdateStatus.AVAILABLE)
                                "New Version Available: ${uiState.nextUpdateVersion}"
                            else if (BuildConfig.VERSION_NAME >= uiState.nextUpdateVersion)
                                "You're up to date"
                            else if (uiState.updateStatus == UpdateStatus.FAILED)
                                "Failed to check updates"
                            else
                                "Check for Updates",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_keyboard_arrow_forward),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(loneListShape)
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
                    .padding(vertical = 12.dp, horizontal = 16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(loneListShape)
                        .background(MaterialTheme.colorScheme.onTertiaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "A",
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        fontSize = 32.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Ali Bardide",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Developer",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalIconButton(
                            onClick = { uriHandler.openUri("https://github.com/alibardide-dev")},
                            shapes = IconButtonDefaults.shapes(),
                            modifier = Modifier.width(52.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_github),
                                contentDescription = "GitHub"
                            )
                        }
                        FilledTonalIconButton(
                            onClick = { uriHandler.openUri("https://linktr.ee/alibardide")},
                            shapes = IconButtonDefaults.shapes(),
                            modifier = Modifier.width(52.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_web),
                                contentDescription = "GitHub"
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(loneListShape)
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
                    .clickable { showLicense = true }
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_gavel),
                    contentDescription = null,
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "License",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "GNU General Public License Version 3",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    if (showLicense) {
        LicenseBottomSheet({ showLicense = false })
    }
}