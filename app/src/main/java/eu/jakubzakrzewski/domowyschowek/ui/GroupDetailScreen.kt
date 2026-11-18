package eu.jakubzakrzewski.domowyschowek.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import eu.jakubzakrzewski.domowyschowek.data.StorageGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    group: StorageGroup,
    items: List<Item>,
    onBack: () -> Unit,
    onUpdateGroup: (name: String, photoPath: String?) -> Unit,
    onDeleteGroup: () -> Unit,
    onSaveItem: (existing: Item?, name: String, photoPath: String?) -> Unit,
    onDeleteItem: (Item) -> Unit,
) {
    var showPhoto by rememberSaveable { mutableStateOf(false) }
    var editingGroup by rememberSaveable { mutableStateOf(false) }
    var confirmDeleteGroup by rememberSaveable { mutableStateOf(false) }
    var addingItem by rememberSaveable { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<Item?>(null) }
    var itemToDelete by remember { mutableStateOf<Item?>(null) }

    BackHandler(onBack = onBack)

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
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    Card(onClick = { editingItem = item }, modifier = Modifier.fillMaxWidth()) {
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
