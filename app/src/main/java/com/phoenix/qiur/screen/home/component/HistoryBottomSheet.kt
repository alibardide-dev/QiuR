package com.phoenix.qiur.screen.home.component

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phoenix.qiur.R
import com.phoenix.qiur.data.model.History
import com.phoenix.qiur.ui.theme.firstListShape
import com.phoenix.qiur.ui.theme.loneListShape
import kotlinx.coroutines.launch

data class HistoryActions(
    val open: (History) -> Unit,
    val delete: (History) -> Unit,
    val deleteAll: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryBottomSheet(
    setShowSheet: (Boolean) -> Unit,
    entries: List<History>,
    actions: HistoryActions,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberBottomSheetState(SheetValue.Hidden)
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = { setShowSheet(false) },
        sheetState = sheetState,
        modifier = modifier
    ) {
        LazyColumn(
            Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "History",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = { actions.deleteAll() },
                        shapes = ButtonDefaults.shapes(),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = null
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Delete All")
                    }
                }
            }
            if (entries.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "No entry saved",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "It's all clean and nice",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
            items(entries.reversed(), key = { it.uuid }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .padding(vertical = 4.dp, horizontal = 16.dp)
                        .clip(loneListShape)
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
                        .clickable {
                            actions.open(it)
                            coroutineScope.launch {
                                sheetState.hide()
                            }
                        }
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "${it.format} - ${it.type}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = it.raw,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable { actions.delete(it) }
                    )
                }
            }
        }
    }
}