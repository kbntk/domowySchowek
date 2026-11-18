package eu.jakubzakrzewski.domowyschowek.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(
    groups: List<StorageGroup>,
    allItems: List<Item>,
    messages: Flow<String>,
    importConflict: ImportConflict?,
    onOpen: (StorageGroup, query: String) -> Unit,
    onAdd: (name: String, photoPath: String?) -> Unit,
    onExport: (groupIds: Set<Long>, folder: Uri) -> Unit,
    onImport: (folder: Uri) -> Unit,
    onResolveConflict: (ImportChoice) -> Unit,
) {
    var adding by rememberSaveable { mutableStateOf(false) }
    var exporting by rememberSaveable { mutableStateOf(false) }
    var pendingExport by rememberSaveable { mutableStateOf<LongArray?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val q = query.trim()

    // Elementy pasujące do frazy, pogrupowane po id grupy.
    val matchingItems = remember(allItems, q) {
        if (q.isEmpty()) emptyMap() else allItems
            .filter { it.name.contains(q, ignoreCase = true) }
            .groupBy { it.groupId }
    }
    // Grupa widoczna, gdy pasuje jej nazwa albo któryś z elementów.
    val visibleGroups = remember(groups, q, matchingItems) {
        if (q.isEmpty()) groups
        else groups.filter { it.name.contains(q, ignoreCase = true) || matchingItems.containsKey(it.id) }
    }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(messages) {
        messages.collect { snackbar.showSnackbar(it) }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val ids = pendingExport
        pendingExport = null
        if (uri != null && ids != null) onExport(ids.toSet(), uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) onImport(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Domowy Schowek") },
                actions = {
                    IconButton(onClick = { exporting = true }) {
                        Icon(Icons.Default.Upload, contentDescription = "Eksportuj")
                    }
                    IconButton(onClick = { importLauncher.launch(null) }) {
                        Icon(Icons.Default.Download, contentDescription = "Importuj")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(onClick = { adding = true }) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj grupę")
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Szukaj") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Wyczyść")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp),
            )

            if (groups.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Brak grup. Dodaj pierwszą grupę przyciskiem +.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else if (visibleGroups.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Brak wyników dla \"$q\".",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(visibleGroups, key = { it.id }) { group ->
                        Card(onClick = { onOpen(group, q) }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(group.name, style = MaterialTheme.typography.titleLarge)
                                group.photoPath?.let { path ->
                                    FullPhoto(path, Modifier.padding(top = 8.dp))
                                }
                                matchingItems[group.id]?.let { found ->
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Pasujące elementy:",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    found.sortedBy { it.name.lowercase() }.forEach { item ->
                                        Text("• ${item.name}", style = MaterialTheme.typography.bodyLarge)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (adding) {
        NameAndPhotoDialog(
            title = "Nowa grupa",
            nameLabel = "Nazwa grupy",
            initialName = "",
            initialPhoto = null,
            onDismiss = { adding = false },
            onSave = { name, photo ->
                onAdd(name, photo)
                adding = false
            },
        )
    }

    if (exporting) {
        ExportDialog(
            groups = groups,
            onDismiss = { exporting = false },
            onConfirm = { ids ->
                exporting = false
                pendingExport = ids
                exportLauncher.launch(null)
            },
        )
    }

    importConflict?.let { conflict ->
        ImportConflictDialog(name = conflict.group.name, onChoice = onResolveConflict)
    }
}

@Composable
private fun ExportDialog(
    groups: List<StorageGroup>,
    onDismiss: () -> Unit,
    onConfirm: (LongArray) -> Unit,
) {
    var selected by remember { mutableStateOf(groups.map { it.id }.toSet()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eksport") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                SelectRow(
                    label = "Wszystkie grupy",
                    checked = groups.isNotEmpty() && selected.size == groups.size,
                    onChange = { all -> selected = if (all) groups.map { it.id }.toSet() else emptySet() },
                )
                groups.forEach { group ->
                    SelectRow(
                        label = group.name,
                        checked = group.id in selected,
                        onChange = { on -> selected = if (on) selected + group.id else selected - group.id },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = selected.isNotEmpty(), onClick = { onConfirm(selected.toLongArray()) }) {
                Text("Wybierz katalog")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
    )
}

@Composable
private fun SelectRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
private fun ImportConflictDialog(name: String, onChoice: (ImportChoice) -> Unit) {
    var newName by remember(name) { mutableStateOf("$name (2)") }
    AlertDialog(
        onDismissRequest = { onChoice(ImportChoice.Skip) },
        title = { Text("Grupa już istnieje") },
        text = {
            Column {
                Text("Grupa \"$name\" już jest w schowku. Co zrobić z importowaną grupą?")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nowa nazwa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = { onChoice(ImportChoice.Overwrite) }) {
                    Text("Nadpisz", color = MaterialTheme.colorScheme.error)
                }
                TextButton(
                    enabled = newName.trim().isNotEmpty() && newName.trim() != name,
                    onClick = { onChoice(ImportChoice.Rename(newName.trim())) },
                ) { Text("Zmień nazwę") }
            }
        },
        dismissButton = { TextButton(onClick = { onChoice(ImportChoice.Skip) }) { Text("Pomiń") } },
    )
}
