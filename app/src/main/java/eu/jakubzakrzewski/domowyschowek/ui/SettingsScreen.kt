package eu.jakubzakrzewski.domowyschowek.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import eu.jakubzakrzewski.domowyschowek.data.ItemSort
import eu.jakubzakrzewski.domowyschowek.data.StorageGroup
import eu.jakubzakrzewski.domowyschowek.util.Prefs
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    groups: List<StorageGroup>,
    messages: Flow<String>,
    onBack: () -> Unit,
    onExport: (groupIds: Set<Long>, folder: Uri) -> Unit,
    onImport: (folder: Uri) -> Unit,
) {
    val context = LocalContext.current
    var photoWidth by remember { mutableStateOf(Prefs.photoWidth(context)) }
    var itemSort by remember { mutableStateOf(Prefs.itemSort(context)) }
    var exporting by rememberSaveable { mutableStateOf(false) }
    var pendingExport by rememberSaveable { mutableStateOf<LongArray?>(null) }
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
                title = { Text("Ustawienia") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text("Zdjęcia", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Szerokość zapisywanych zdjęć, wysokość zależy od proporcji. Dotyczy nowo zrobionych zdjęć.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            Prefs.PHOTO_WIDTHS.forEach { width ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = photoWidth == width,
                            onClick = {
                                photoWidth = width
                                Prefs.setPhotoWidth(context, width)
                            },
                            role = Role.RadioButton,
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = photoWidth == width, onClick = null)
                    Spacer(Modifier.width(8.dp))
                    val suffix = if (width == Prefs.DEFAULT_PHOTO_WIDTH) " (domyślnie)" else ""
                    Text("$width px$suffix")
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            Text("Sortowanie elementów", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text("Dotyczy wszystkich grup.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            ItemSort.entries.forEach { sort ->
                SortRow(
                    label = sort.label,
                    selected = itemSort == sort,
                    onClick = {
                        itemSort = sort
                        Prefs.setItemSort(context, sort)
                    },
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            Text("Kopia zapasowa", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Eksport zapisuje wybrane grupy wraz ze zdjęciami do katalogu, a import wczytuje je z powrotem.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { exporting = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Upload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Eksportuj")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { importLauncher.launch(null) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Importuj")
            }
        }
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
private fun SortRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
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

/** Pytanie o nazwę przy imporcie grupy, której nazwa już istnieje. */
@Composable
fun ImportConflictDialog(name: String, onChoice: (ImportChoice) -> Unit) {
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
