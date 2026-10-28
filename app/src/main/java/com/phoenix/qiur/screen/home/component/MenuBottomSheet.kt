package com.phoenix.qiur.screen.home.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.phoenix.qiur.BuildConfig
import com.phoenix.qiur.R
import com.phoenix.qiur.ui.theme.firstListShape
import com.phoenix.qiur.ui.theme.lastListShape
import com.phoenix.qiur.ui.theme.loneListShape
import kotlinx.coroutines.launch

data class MenuActions(
    val setKeepHistory: (Boolean) -> Unit,
    val openHistory: () -> Unit,
    val openAbout: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuBottomSheet(
    setShowSheet: (Boolean) -> Unit,
    actions: MenuActions,
    isHistoryEnabled: Boolean,
    isUpdateAvailable: Boolean,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberBottomSheetState(SheetValue.Hidden)
    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    ModalBottomSheet(
        onDismissRequest = { setShowSheet(false) },
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(firstListShape)
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
                    .clickable { actions.openHistory() }
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "View scan history",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "A history of scanned codes",
                        style = MaterialTheme.typography.bodyMedium,
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
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(lastListShape)
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
                    .clickable { actions.setKeepHistory(!isHistoryEnabled) }
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Keep scan history",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Box {
                        AnimatedContent(
                            targetState = isHistoryEnabled,
                        ) { hasHistory ->
                            if (hasHistory)
                                Text(
                                    text = "QiuR will save your history",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            else
                                Text(
                                    text = "History won't be saved",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = isHistoryEnabled,
                    onCheckedChange = { actions.setKeepHistory(it) },
                    thumbContent = {
                        if (isHistoryEnabled)
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                modifier = Modifier.padding(4.dp)
                            )
                        else
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = null,
                                modifier = Modifier.padding(4.dp)
                            )
                    }
                )
            }
            Spacer(Modifier.height(8.dp))
            AnimatedVisibility(isUpdateAvailable) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(32.dp))
                        .clickable {
                            uriHandler.openUri("https://github.com/alibardide-dev/QiuR/releases/latest")
                        }
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "New Update available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(R.drawable.ic_keyboard_arrow_forward),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(loneListShape)
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
                    .clickable {
                        coroutineScope.launch {
                            sheetState.hide()
                            setShowSheet(false)
                        }
                        actions.openAbout()
                    }
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "QiuR ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium,
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
        }
    }
}