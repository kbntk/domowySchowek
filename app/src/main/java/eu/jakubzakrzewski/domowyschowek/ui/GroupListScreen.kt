package eu.jakubzakrzewski.domowyschowek.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
fun GroupListScreen(
    groups: List<StorageGroup>,
    allItems: List<Item>,
    onOpen: (StorageGroup, query: String) -> Unit,
    onAdd: (name: String, photoPath: String?) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var adding by rememberSaveable { mutableStateOf(false) }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Domowy Schowek") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Ustawienia")
                    }
                },
            )
        },
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
}
