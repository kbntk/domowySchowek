package eu.jakubzakrzewski.domowyschowek.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import eu.jakubzakrzewski.domowyschowek.util.Photos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStream
import java.time.Instant

/**
 * Format eksportu (katalog wybrany przez użytkownika):
 *  schowek.json            – lista grup, linki względne do zdjęć
 *  <grupa>.jpg             – zdjęcie grupy (w tym samym katalogu co json)
 *  <grupa>/<element>.jpg   – zdjęcia elementów danej grupy
 */
const val EXPORT_JSON_NAME = "schowek.json"
private const val FORMAT = "domowy-schowek"

data class ImportItem(val name: String, val photo: Uri?, val createdAt: Long)
data class ImportGroup(val name: String, val photo: Uri?, val items: List<ImportItem>)

object Transfer {

    suspend fun export(context: Context, dao: SchowekDao, treeUri: Uri, groupIds: Set<Long>): Int =
        withContext(Dispatchers.IO) {
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: error("Brak dostępu do katalogu")
            val selected = dao.allGroups().filter { it.id in groupIds }
            val usedDirs = mutableSetOf<String>()
            val groupsJson = JSONArray()

            for (group in selected) {
                val dirName = uniqueName(sanitize(group.name), usedDirs)

                val photoRel = group.photoPath?.takeIf { File(it).exists() }?.let { path ->
                    val fileName = "$dirName.jpg"
                    writeDoc(context, root, fileName, "image/jpeg") { out -> File(path).inputStream().use { it.copyTo(out) } }
                    fileName
                }

                val usedFiles = mutableSetOf<String>()
                val itemsJson = JSONArray()
                for (item in dao.itemsOf(group.id).sortedBy { it.name.lowercase() }) {
                    val itemRel = item.photoPath?.takeIf { File(it).exists() }?.let { path ->
                        val base = uniqueName(sanitize(item.name), usedFiles)
                        val dir = findOrCreateDir(root, dirName)
                        writeDoc(context, dir, "$base.jpg", "image/jpeg") { out -> File(path).inputStream().use { it.copyTo(out) } }
                        "$dirName/$base.jpg"
                    }
                    itemsJson.put(
                        JSONObject().put("name", item.name).put("photo", itemRel ?: JSONObject.NULL).put("createdAt", item.createdAt)
                    )
                }

                groupsJson.put(
                    JSONObject()
                        .put("name", group.name)
                        .put("photo", photoRel ?: JSONObject.NULL)
                        .put("items", itemsJson)
                )
            }

            val json = JSONObject()
                .put("format", FORMAT)
                .put("version", 1)
                .put("exportedAt", Instant.now().toString())
                .put("groups", groupsJson)
            writeDoc(context, root, EXPORT_JSON_NAME, "application/json") { out ->
                out.write(json.toString(2).toByteArray(Charsets.UTF_8))
            }
            selected.size
        }

    suspend fun readPackage(context: Context, treeUri: Uri): List<ImportGroup> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: error("Brak dostępu do katalogu")
        val jsonFile = root.findFile(EXPORT_JSON_NAME)?.takeIf { it.isFile }
            ?: error("W wybranym katalogu nie ma pliku $EXPORT_JSON_NAME")
        val text = context.contentResolver.openInputStream(jsonFile.uri)?.bufferedReader()?.use { it.readText() }
            ?: error("Nie można odczytać $EXPORT_JSON_NAME")
        val groups = JSONObject(text).getJSONArray("groups")
        // Pliki z wcześniejszych wersji nie mają daty dodania – dostają datę importu.
        val importedAt = System.currentTimeMillis()
        List(groups.length()) { i ->
            val g = groups.getJSONObject(i)
            val items = g.optJSONArray("items") ?: JSONArray()
            ImportGroup(
                name = g.getString("name"),
                photo = resolve(root, g.optStr("photo")),
                items = List(items.length()) { j ->
                    val item = items.getJSONObject(j)
                    ImportItem(item.getString("name"), resolve(root, item.optStr("photo")), item.optLong("createdAt", importedAt))
                },
            )
        }
    }

    /** Zapisuje grupę wraz ze zdjęciami (kopiowanymi do pamięci aplikacji) pod podaną nazwą. */
    suspend fun insert(context: Context, dao: SchowekDao, group: ImportGroup, name: String) =
        withContext(Dispatchers.IO) {
            val groupId = dao.insertGroup(
                StorageGroup(name = name, photoPath = group.photo?.let { Photos.copyFromUri(context, it) })
            )
            for (item in group.items) {
                dao.insertItem(
                    Item(
                        groupId = groupId,
                        name = item.name,
                        photoPath = item.photo?.let { Photos.copyFromUri(context, it) },
                        createdAt = item.createdAt,
                    )
                )
            }
        }

    private fun JSONObject.optStr(key: String): String? = if (isNull(key)) null else getString(key)

    private fun resolve(root: DocumentFile, relPath: String?): Uri? {
        if (relPath == null) return null
        var current: DocumentFile? = root
        for (part in relPath.split('/')) current = current?.findFile(part)
        return current?.takeIf { it.isFile }?.uri
    }

    private fun findOrCreateDir(root: DocumentFile, name: String): DocumentFile {
        root.findFile(name)?.takeIf { it.isDirectory }?.let { return it }
        return root.createDirectory(name) ?: error("Nie udało się utworzyć katalogu $name")
    }

    /** Nadpisuje plik o nazwie [fileName]. SAF sam dokleja rozszerzenie na podstawie MIME, więc przekazujemy samą bazę. */
    private fun writeDoc(context: Context, dir: DocumentFile, fileName: String, mime: String, write: (OutputStream) -> Unit) {
        dir.findFile(fileName)?.delete()
        val doc = dir.createFile(mime, fileName.substringBeforeLast('.'))
            ?: error("Nie udało się utworzyć pliku $fileName")
        context.contentResolver.openOutputStream(doc.uri, "wt")?.use(write)
            ?: error("Nie udało się zapisać pliku $fileName")
    }

    private fun sanitize(name: String): String {
        val cleaned = name.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_").trim().trimEnd('.').take(60)
        return cleaned.ifBlank { "bez_nazwy" }
    }

    private fun uniqueName(base: String, used: MutableSet<String>): String {
        var candidate = base
        var n = 2
        while (!used.add(candidate.lowercase())) {
            candidate = "$base ($n)"
            n++
        }
        return candidate
    }
}
