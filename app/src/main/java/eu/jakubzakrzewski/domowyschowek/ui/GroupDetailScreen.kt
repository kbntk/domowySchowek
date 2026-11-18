package eu.jakubzakrzewski.domowyschowek.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.input.pointer.pointerInput
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.lerp
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.jakubzakrzewski.domowyschowek.data.Item
import eu.jakubzakrzewski.domowyschowek.data.ItemSort
import eu.jakubzakrzewski.domowyschowek.data.StorageGroup
import eu.jakubzakrzewski.domowyschowek.data.sortedItems
import eu.jakubzakrzewski.domowyschowek.util.Prefs
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    group: StorageGroup,
    items: List<Item>,
    onBack: () -> Unit,
    highlight: String,
    onUpdateGroup: (name: String, photoPath: String?) -> Unit,
    onDeleteGroup: () -> Unit,
    onSaveItem: (existing: Item?, name: String, photoPath: String?) -> Unit,
    onDeleteItem: (Item) -> Unit,
) {
    var showPhoto by rememberSaveable { mutableStateOf(false) }
    var editingGroup by rememberSaveable { mutableStateOf(false) }
    var confirmDeleteGroup by rememberSaveable { mutableStateOf(false) }
    var addingItem by rememberSaveable { mutableStateOf(false) }
    var viewingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingItem by remember { mutableStateOf<Item?>(null) }
    var itemToDelete by remember { mutableStateOf<Item?>(null) }

    BackHandler(onBack = onBack)

    val sort: ItemSort = Prefs.itemSort(LocalContext.current)
    val sortedItems = remember(items, sort) { items.sortedItems(sort) }

    // Fraza z wyszukiwania: pasujące elementy migają przez 2 sekundy.
    val query = highlight.trim()
    fun matches(item: Item) = query.isNotEmpty() && item.name.contains(query, ignoreCase = true)
    var blinking by rememberSaveable { mutableStateOf(query.isNotEmpty()) }
    LaunchedEffect(query) {
        if (query.isNotEmpty()) {
            blinking = true
            delay(1_000)
            blinking = false
        }
    }
    val pulse by rememberInfiniteTransition(label = "blink").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse),
        label = "pulse",
    )
    val listState = rememberLazyListState()
    LaunchedEffect(query, sortedItems) {
        val first = sortedItems.indexOfFirst { matches(it) }
        if (query.isNotEmpty() && first >= 0) listState.animateScrollToItem(first)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                },
                actions = {
                    IconButton(onClick = { showPhoto = true }) {
                        Icon(Icons.Default.Photo, contentDescription = "Zdjęcie grupy")
                    }
                    IconButton(onClick = { editingGroup = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edytuj grupę")
                    }
                    IconButton(onClick = { confirmDeleteGroup = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Usuń grupę")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { addingItem = true }) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj element")
            }
        },
    ) { padding ->
        if (items.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Brak elementów w tej grupie.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(sortedItems, key = { it.id }) { item ->
                    val highlighted = blinking && matches(item)
                    val container = if (highlighted) {
                        lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.tertiaryContainer, pulse)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                    Card(
                        onClick = { viewingId = item.id },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = container),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            item.photoPath?.let { Thumbnail(it, modifier = Modifier.padding(end = 12.dp)) }
                            Text(item.name, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }

    if (showPhoto) {
        PhotoViewerDialog(title = group.name, photoPath = group.photoPath, onDismiss = { showPhoto = false })
    }

    if (editingGroup) {
        NameAndPhotoDialog(
            title = "Edytuj grupę",
            nameLabel = "Nazwa grupy",
            initialName = group.name,
            initialPhoto = group.photoPath,
            onDismiss = { editingGroup = false },
            onSave = { name, photo ->
                onUpdateGroup(name, photo)
                editingGroup = false
            },
        )
    }

    if (confirmDeleteGroup) {
        ConfirmDialog(
            text = "Usunąć grupę \"${group.name}\" wraz z elementami?",
            onConfirm = { confirmDeleteGroup = false; onDeleteGroup() },
            onDismiss = { confirmDeleteGroup = false },
        )
    }

    if (addingItem) {
        NameAndPhotoDialog(
            title = "Nowy element",
            nameLabel = "Nazwa elementu",
            initialName = "",
            initialPhoto = null,
            onDismiss = { addingItem = false },
            onSave = { name, photo ->
                onSaveItem(null, name, photo)
                addingItem = false
            },
        )
    }

    viewingId?.let { id ->
        val index = sortedItems.indexOfFirst { it.id == id }
        if (index >= 0) {
            ItemViewDialog(
                item = sortedItems[index],
                hasPrevious = index > 0,
                hasNext = index < sortedItems.lastIndex,
                onPrevious = { viewingId = sortedItems[index - 1].id },
                onNext = { viewingId = sortedItems[index + 1].id },
                onEdit = {
                    val item = sortedItems[index]
                    viewingId = null
                    editingItem = item
                },
                onDismiss = { viewingId = null },
            )
        }
    }

    editingItem?.let { item ->
        NameAndPhotoDialog(
            title = "Edytuj element",
            nameLabel = "Nazwa elementu",
            initialName = item.name,
            initialPhoto = item.photoPath,
            onDismiss = { editingItem = null },
            onSave = { name, photo ->
                onSaveItem(item, name, photo)
                editingItem = null
            },
            onDeleteRequest = {
                itemToDelete = item
                editingItem = null
            },
        )
    }

    itemToDelete?.let { item ->
        ConfirmDialog(
            text = "Usunąć element \"${item.name}\"?",
            onConfirm = { onDeleteItem(item); itemToDelete = null },
            onDismiss = { itemToDelete = null },
        )
    }
}

private const val SWIPE_THRESHOLD_PX = 100f
private val addedDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

private fun formatAdded(millis: Long): String =
    addedDateFormat.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

/**
 * Podgląd elementu: nazwa i zdjęcie bez możliwości edycji. Przesunięcie palcem po zdjęciu w lewo
 * przechodzi do następnego elementu, w prawo do poprzedniego. Pod zdjęciem data dodania i przycisk edycji.
 */
@Composable
private fun ItemViewDialog(
    item: Item,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(item.id, hasPrevious, hasNext) {
                            var total = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { total = 0f },
                                onDragCancel = { total = 0f },
                                onDragEnd = {
                                    when {
                                        total < -SWIPE_THRESHOLD_PX && hasNext -> onNext()
                                        total > SWIPE_THRESHOLD_PX && hasPrevious -> onPrevious()
                                    }
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    total += dragAmount
                                },
                            )
                        },
                ) {
                    if (item.photoPath != null) {
                        FullPhoto(item.photoPath)
                    } else {
                        Text("Ten element nie ma zdjęcia.")
                    }
                }
                Text(
                    "Dodano: ${formatAdded(item.createdAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Edytuj element")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Zamknij") } },
    )
}
