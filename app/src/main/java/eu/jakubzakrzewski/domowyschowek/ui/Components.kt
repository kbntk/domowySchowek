package eu.jakubzakrzewski.domowyschowek.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import eu.jakubzakrzewski.domowyschowek.util.Photos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun rememberBitmap(path: String?): State<Bitmap?> = produceState<Bitmap?>(null, path) {
    value = if (path == null) null else withContext(Dispatchers.IO) { BitmapFactory.decodeFile(path) }
}

/** Zdjęcia o tej szerokości (w px) i większe są rozciągane na pełną szerokość ekranu. */
private const val FULL_WIDTH_PX = 640

/**
 * Zdjęcie w widoku. Zdjęcia węższe niż 640 px nie są powiększane: wyświetlane są w rozmiarze,
 * w jakim zostały zapisane (1 px obrazu = 1 px gęstości), nie szerzej niż dostępne miejsce.
 * Wysokość wynika z proporcji zdjęcia.
 */
@Composable
fun FullPhoto(path: String, modifier: Modifier = Modifier) {
    val bitmap by rememberBitmap(path)
    val density = LocalDensity.current.density
    bitmap?.let { bmp ->
        val ratio = bmp.width / bmp.height.toFloat()
        val natural = (bmp.width / density).dp
        BoxWithConstraints(modifier.fillMaxWidth()) {
            val shownWidth = if (bmp.width < FULL_WIDTH_PX) minOf(natural, maxWidth) else maxWidth
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                // Zdjęcia węższe niż 640 px są pokazywane 1:1, więc bez filtrowania zachowują ostrość pikseli.
                filterQuality = if (bmp.width < FULL_WIDTH_PX) FilterQuality.None else FilterQuality.Low,
                modifier = Modifier
                    .width(shownWidth)
                    .aspectRatio(ratio)
                    .clip(RoundedCornerShape(8.dp)),
            )
        }
    }
}

/** Miniatura kwadratowa, używana przy elementach listy. */
@Composable
fun Thumbnail(path: String, size: Int = 56, modifier: Modifier = Modifier) {
    val bitmap by rememberBitmap(path)
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size.dp).clip(RoundedCornerShape(8.dp)),
        )
    }
}

/**
 * Zwraca funkcję uruchamiającą aparat. Po zrobieniu zdjęcia jest ono skalowane do 640 px
 * i przekazywane do [onCaptured] jako ścieżka pliku.
 */
@Composable
fun rememberPhotoCapture(onCaptured: (String) -> Unit, onReturn: () -> Unit = {}): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingPath by rememberSaveable { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        onReturn()
        val file = pendingPath?.let(::File)
        pendingPath = null
        if (success && file != null) {
            scope.launch {
                val saved = Photos.importScaled(context, file)
                file.delete()
                if (saved != null) onCaptured(saved)
            }
        } else {
            file?.delete()
        }
    }
    return {
        val file = Photos.newCaptureFile(context)
        pendingPath = file.absolutePath
        launcher.launch(Photos.uriFor(context, file))
    }
}

/**
 * Okno dodawania / edycji nazwy i zdjęcia (grupy lub elementu).
 * [onDeleteRequest] – jeśli podane, pokazywany jest przycisk usunięcia.
 */
@Composable
fun NameAndPhotoDialog(
    title: String,
    nameLabel: String,
    initialName: String,
    initialPhoto: String?,
    onDismiss: () -> Unit,
    onSave: (name: String, photoPath: String?) -> Unit,
    onDeleteRequest: (() -> Unit)? = null,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var photo by rememberSaveable { mutableStateOf(initialPhoto) }

    // Zdjęcie zrobione w tym oknie, a nie zapisane, trzeba usunąć przy anulowaniu.
    fun discardNewPhoto() {
        if (photo != initialPhoto) Photos.delete(photo)
    }

    // Po powrocie z aparatu system przywraca fokus na pole nazwy i wraca klawiatura.
    // Zdejmujemy fokus z pola i chowamy klawiaturę, także po zapisaniu zdjęcia.
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    // Przy otwarciu okna od razu ustawiamy fokus w polu nazwy, żeby pojawiła się klawiatura.
    val nameFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        nameFocus.requestFocus()
    }
    fun dropNameFocus() {
        focusManager.clearFocus(force = true)
        keyboard?.hide()
    }
    val capture = rememberPhotoCapture(
        onCaptured = {
            photo = it
            dropNameFocus()
        },
        onReturn = { dropNameFocus() },
    )

    AlertDialog(
        onDismissRequest = { discardNewPhoto(); onDismiss() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(nameLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(nameFocus),
                )
                photo?.let { FullPhoto(it) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = capture) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (photo == null) "Zrób zdjęcie" else "Zmień zdjęcie")
                    }
                    if (photo != null) {
                        TextButton(onClick = { photo = null }) { Text("Usuń zdjęcie") }
                    }
                }
                if (onDeleteRequest != null) {
                    TextButton(onClick = { discardNewPhoto(); onDeleteRequest() }) {
                        Text("Usuń", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), photo) },
                enabled = name.isNotBlank(),
            ) { Text("Zapisz") }
        },
        dismissButton = {
            TextButton(onClick = { discardNewPhoto(); onDismiss() }) { Text("Anuluj") }
        },
    )
}

@Composable
fun PhotoViewerDialog(title: String, photoPath: String?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (photoPath != null) {
                FullPhoto(photoPath)
            } else {
                Text("Ta grupa nie ma zdjęcia.")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Zamknij") } },
    )
}

@Composable
fun ConfirmDialog(text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Usuń", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
    )
}
