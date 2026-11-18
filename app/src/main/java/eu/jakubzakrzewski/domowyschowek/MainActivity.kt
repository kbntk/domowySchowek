package eu.jakubzakrzewski.domowyschowek

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import eu.jakubzakrzewski.domowyschowek.ui.GroupDetailScreen
import eu.jakubzakrzewski.domowyschowek.ui.GroupListScreen
import eu.jakubzakrzewski.domowyschowek.ui.ImportConflictDialog
import eu.jakubzakrzewski.domowyschowek.ui.SchowekViewModel
import eu.jakubzakrzewski.domowyschowek.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                SchowekApp()
            }
        }
    }
}

@Composable
private fun SchowekApp(vm: SchowekViewModel = viewModel()) {
    val groups by vm.groups.collectAsState()
    val allItems by vm.allItems.collectAsState()
    val conflict by vm.importConflict.collectAsState()
    var openGroupId by rememberSaveable { mutableStateOf<Long?>(null) }
    var highlight by rememberSaveable { mutableStateOf("") }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = showSettings) { showSettings = false }

    val groupId = openGroupId
    when {
        showSettings -> SettingsScreen(
            groups = groups,
            messages = vm.messages,
            onBack = { showSettings = false },
            onExport = { ids, folder -> vm.exportGroups(folder, ids) },
            onImport = { folder -> vm.startImport(folder) },
        )

        groupId == null -> GroupListScreen(
            groups = groups,
            allItems = allItems,
            onOpen = { group, query ->
                highlight = query
                openGroupId = group.id
            },
            onAdd = { name, photo -> vm.saveGroup(null, name, photo) },
            onOpenSettings = { showSettings = true },
        )

        else -> {
            val group by remember(groupId) { vm.observeGroup(groupId) }.collectAsState(initial = null)
            val items by remember(groupId) { vm.observeItems(groupId) }.collectAsState(initial = emptyList())
            val current = group
            if (current != null) {
                GroupDetailScreen(
                    group = current,
                    items = items,
                    onBack = { openGroupId = null },
                    highlight = highlight,
                    onUpdateGroup = { name, photo -> vm.saveGroup(current, name, photo) },
                    onDeleteGroup = {
                        openGroupId = null
                        vm.deleteGroup(current)
                    },
                    onSaveItem = { existing, name, photo -> vm.saveItem(groupId, existing, name, photo) },
                    onDeleteItem = { vm.deleteItem(it) },
                )
            }
        }
    }

    conflict?.let { c ->
        ImportConflictDialog(name = c.group.name, onChoice = { choice -> vm.resolveImportConflict(choice) })
    }
}
