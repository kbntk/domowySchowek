package eu.jakubzakrzewski.domowyschowek.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import eu.jakubzakrzewski.domowyschowek.data.ImportGroup
import eu.jakubzakrzewski.domowyschowek.data.Item
import eu.jakubzakrzewski.domowyschowek.data.SchowekDatabase
import eu.jakubzakrzewski.domowyschowek.data.SchowekDao
import eu.jakubzakrzewski.domowyschowek.data.StorageGroup
import eu.jakubzakrzewski.domowyschowek.data.Transfer
import eu.jakubzakrzewski.domowyschowek.util.Photos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Grupa z importu, której nazwa koliduje z istniejącą grupą. */
data class ImportConflict(val group: ImportGroup, val existing: StorageGroup)

sealed interface ImportChoice {
    data object Overwrite : ImportChoice
    data object Skip : ImportChoice
    data class Rename(val newName: String) : ImportChoice
}

class SchowekViewModel(application: Application) : AndroidViewModel(application) {
    private val dao: SchowekDao = SchowekDatabase.create(application).dao()

    val groups: StateFlow<List<StorageGroup>> = dao.observeGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Wszystkie elementy ze wszystkich grup – potrzebne do wyszukiwania. */
    val allItems: StateFlow<List<Item>> = dao.observeAllItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun observeGroup(id: Long): Flow<StorageGroup?> = dao.observeGroup(id)

    fun observeItems(groupId: Long): Flow<List<Item>> = dao.observeItems(groupId)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    /** Konflikt nazw podczas importu; null gdy nie ma pytania do użytkownika. */
    val importConflict = MutableStateFlow<ImportConflict?>(null)
    private val importQueue = ArrayDeque<ImportGroup>()
    private var importedCount = 0

    /** [existing] == null -> nowa grupa. Zmieniane zdjęcie powoduje usunięcie poprzedniego pliku. */
    fun saveGroup(existing: StorageGroup?, name: String, photoPath: String?) = viewModelScope.launch(Dispatchers.IO) {
        if (existing == null) {
            dao.insertGroup(StorageGroup(name = name, photoPath = photoPath))
        } else {
            if (existing.photoPath != photoPath) Photos.delete(existing.photoPath)
            dao.updateGroup(existing.copy(name = name, photoPath = photoPath))
        }
    }

    fun deleteGroup(group: StorageGroup) = viewModelScope.launch(Dispatchers.IO) { removeGroup(group) }

    private suspend fun removeGroup(group: StorageGroup) {
        dao.itemsOf(group.id).forEach { Photos.delete(it.photoPath) }
        Photos.delete(group.photoPath)
        dao.deleteGroup(group) // elementy usuwane kaskadowo
    }

    fun saveItem(groupId: Long, existing: Item?, name: String, photoPath: String?) = viewModelScope.launch(Dispatchers.IO) {
        if (existing == null) {
            dao.insertItem(Item(groupId = groupId, name = name, photoPath = photoPath, createdAt = System.currentTimeMillis()))
        } else {
            if (existing.photoPath != photoPath) Photos.delete(existing.photoPath)
            dao.updateItem(existing.copy(name = name, photoPath = photoPath))
        }
    }

    fun deleteItem(item: Item) = viewModelScope.launch(Dispatchers.IO) {
        Photos.delete(item.photoPath)
        dao.deleteItem(item)
    }

    fun exportGroups(treeUri: Uri, groupIds: Set<Long>) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val count = Transfer.export(getApplication(), dao, treeUri, groupIds)
            _messages.emit("Wyeksportowano grupy: $count")
        } catch (e: Exception) {
            _messages.emit("Eksport nieudany: ${e.message}")
        }
    }

    fun startImport(treeUri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val groups = Transfer.readPackage(getApplication(), treeUri)
            importQueue.clear()
            importQueue.addAll(groups)
            importedCount = 0
            processImportQueue()
        } catch (e: Exception) {
            importConflict.value = null
            _messages.emit("Import nieudany: ${e.message}")
        }
    }

    fun resolveImportConflict(choice: ImportChoice) = viewModelScope.launch(Dispatchers.IO) {
        val conflict = importConflict.value ?: return@launch
        importConflict.value = null
        val head = importQueue.removeFirst()
        try {
            when (choice) {
                ImportChoice.Overwrite -> {
                    removeGroup(conflict.existing)
                    Transfer.insert(getApplication(), dao, head, head.name)
                    importedCount++
                }
                ImportChoice.Skip -> Unit
                is ImportChoice.Rename -> importQueue.addFirst(head.copy(name = choice.newName))
            }
            processImportQueue()
        } catch (e: Exception) {
            _messages.emit("Import nieudany: ${e.message}")
        }
    }

    /** Importuje kolejne grupy; zatrzymuje się, gdy nazwa koliduje z istniejącą grupą. */
    private suspend fun processImportQueue() {
        while (importQueue.isNotEmpty()) {
            val head = importQueue.first()
            val existing = dao.findGroupByName(head.name)
            if (existing != null) {
                importConflict.value = ImportConflict(head, existing)
                return
            }
            Transfer.insert(getApplication(), dao, head, head.name)
            importQueue.removeFirst()
            importedCount++
        }
        _messages.emit("Zaimportowano grupy: $importedCount")
    }
}
