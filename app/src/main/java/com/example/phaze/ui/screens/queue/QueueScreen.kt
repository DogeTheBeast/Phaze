package com.example.phaze.ui.screens.queue

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.phaze.data.playback.QueueTrack
import com.example.phaze.ui.components.AlbumArt
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun QueueScreen(
    navController: NavHostController,
    viewModel: QueueViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        QueueTopBar(
            count = state.items.size,
            onBack = { navController.popBackStack() },
        )

        if (state.isEmpty) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Queue is empty",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            QueueList(
                items = state.items,
                currentIndex = state.currentIndex,
                onPlay = viewModel::playAt,
                onRemove = viewModel::removeAt,
                onMove = viewModel::move,
            )
        }

        QueueBottomBar(
            isEmpty = state.isEmpty,
            onSaveAsPlaylist = viewModel::saveAsPlaylist,
            onClear = viewModel::clear,
        )
    }
}

@Composable
private fun QueueTopBar(count: Int, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
        }
        Text("Up next", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Text(
            text = if (count > 0) "$count songs" else "",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = { /* TODO: menu */ }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More")
        }
    }
}

@Composable
private fun QueueList(
    items: List<QueueTrack>,
    currentIndex: Int,
    onPlay: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val edgePx = with(density) { 96.dp.toPx() }
    val maxScrollPx = with(density) { 14.dp.toPx() }
    var viewportHeight by remember { mutableIntStateOf(0) }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var overIndex by remember { mutableIntStateOf(-1) }
    var draggingOffset by remember { mutableStateOf(0f) }
    var draggingSize by remember { mutableIntStateOf(0) }
    var autoScrollJob by remember { mutableStateOf<Job?>(null) }

    fun shiftY(index: Int): Int {
        val d = draggingIndex ?: return 0
        if (index == d) return draggingOffset.roundToInt()
        if (overIndex == -1 || draggingSize == 0) return 0
        val step = draggingSize
        return when {
            d < overIndex && index > d && index <= overIndex -> -step
            d > overIndex && index < d && index >= overIndex -> step
            else -> 0
        }
    }

    /** Auto-scrolls the list while the dragged row is near a viewport edge. */
    fun startAutoScroll() {
        autoScrollJob?.cancel()
        autoScrollJob = scope.launch {
            while (isActive) {
                val d = draggingIndex
                val visible = listState.layoutInfo.visibleItemsInfo
                val info = d?.let { dragged -> visible.find { it.index == dragged } }
                if (info != null && viewportHeight > 0 && visible.isNotEmpty()) {
                    val viewTop = visible.first().offset
                    val y = info.offset + draggingOffset - viewTop
                    val itemBottom = y + info.size
                    val scrollDelta = when {
                        y < edgePx -> -((edgePx - y).coerceAtMost(maxScrollPx))
                        itemBottom > viewportHeight - edgePx -> (itemBottom - (viewportHeight - edgePx)).coerceAtMost(maxScrollPx)
                        else -> 0f
                    }
                    if (scrollDelta != 0f) {
                        // Keep the dragged row pinned under the finger as the content scrolls.
                        listState.scroll { scrollBy(scrollDelta) }
                        draggingOffset += scrollDelta
                    }
                }
                delay(16)
            }
        }
    }

    fun stopAutoScroll() {
        autoScrollJob?.cancel()
        autoScrollJob = null
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { viewportHeight = it.height },
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        itemsIndexed(items, key = { _, item -> item.id }) { index, _ ->
            val item = items[index]
            val isDragging = index == draggingIndex
            QueueRow(
                item = item,
                isCurrent = index == currentIndex,
                onClick = { if (index != draggingIndex) onPlay(index) },
                onRemove = { onRemove(index) },
                modifier = Modifier
                    .offset { IntOffset(0, shiftY(index)) }
                    .zIndex(if (isDragging) 1f else 0f)
                    .pointerInput(index) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingIndex = index
                                overIndex = index
                                draggingOffset = 0f
                                draggingSize = listState.layoutInfo.visibleItemsInfo.find { it.index == index }?.size ?: 0
                                startAutoScroll()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                draggingOffset += dragAmount.y
                                val d = draggingIndex
                                if (d != null) {
                                    val info = listState.layoutInfo.visibleItemsInfo.find { it.index == d }
                                    if (info != null) {
                                        val top = info.offset + draggingOffset
                                        val bottom = top + info.size
                                        val center = (top + bottom) / 2f
                                        overIndex = listState.layoutInfo.visibleItemsInfo
                                            .filter { it.index != d }
                                            .firstOrNull { center >= it.offset && center <= it.offset + it.size }
                                            ?.index ?: d
                                    }
                                }
                            },
                            onDragEnd = {
                                stopAutoScroll()
                                val from = draggingIndex
                                val to = overIndex
                                if (from != null && to != -1 && from != to) onMove(from, to)
                                draggingIndex = null; overIndex = -1; draggingOffset = 0f; draggingSize = 0
                            },
                            onDragCancel = {
                                stopAutoScroll()
                                draggingIndex = null; overIndex = -1; draggingOffset = 0f; draggingSize = 0
                            },
                        )
                    },
            )
        }
    }
}

@Composable
private fun QueueRow(
    item: QueueTrack,
    isCurrent: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isCurrent) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isCurrent) {
            Equalizer()
            Spacer(Modifier.width(10.dp))
        } else {
            Icon(
                imageVector = Icons.Filled.DragHandle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
        }
        AlbumArt(coverUrl = item.coverArtUrl, name = item.title, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.artist.isNotEmpty()) {
                Text(
                    text = item.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

/** Three animated bars marking the currently-playing track (mockup .eq). */
@Composable
private fun Equalizer(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "eq")
    Row(
        modifier = modifier
            .height(16.dp)
            .width(18.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        repeat(3) { i ->
            val barHeight by transition.animateFloat(
                initialValue = 5f,
                targetValue = 16f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 400, delayMillis = i * 150),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "eq-bar$i",
            )
            Box(
                Modifier
                    .width(3.dp)
                    .height(barHeight.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp)),
            )
        }
    }
}

@Composable
private fun QueueBottomBar(isEmpty: Boolean, onSaveAsPlaylist: () -> Unit, onClear: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = onSaveAsPlaylist,
            enabled = !isEmpty,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 10.dp),
        ) {
            Text("Save as playlist")
        }
        FilledTonalButton(
            onClick = onClear,
            enabled = !isEmpty,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 10.dp),
        ) {
            Text("Clear queue")
        }
    }
}

private fun formatDuration(seconds: Int): String =
    String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60)
