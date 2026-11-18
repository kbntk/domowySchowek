package eu.jakubzakrzewski.domowyschowek.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

object Photos {
    /** Zdjęcia są zapisywane z szerokością 640 px, a wysokość wynika z proporcji oryginału. */
    const val TARGET_WIDTH = 640
    private const val JPEG_QUALITY = 90

    /** Plik tymczasowy, w którym aparat zapisze zdjęcie. */
    fun newCaptureFile(context: Context): File {
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        return File(dir, "${UUID.randomUUID()}.jpg")
    }

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /**
     * Wczytuje zdjęcie z aparatu (z uwzględnieniem orientacji EXIF), skaluje do 640 px szerokości
     * z zachowaniem proporcji i zapisuje jako JPEG w pamięci wewnętrznej aplikacji.
     * Zwraca ścieżkę do zapisanego pliku lub null w razie błędu.
     */
    suspend fun importScaled(context: Context, captured: File): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(captured)) { decoder, info, _ ->
                val width = info.size.width
                val height = info.size.height
                val targetHeight = max(1, (height.toDouble() * TARGET_WIDTH / width).roundToInt())
                decoder.setTargetSize(TARGET_WIDTH, targetHeight)
            }
            val dir = File(context.filesDir, "photos").apply { mkdirs() }
            val out = File(dir, "${UUID.randomUUID()}.jpg")
            FileOutputStream(out).use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            bitmap.recycle()
            out.absolutePath
        }.getOrNull()
    }

    /** Kopiuje plik z dowolnego URI (np. z importowanego katalogu) do pamięci aplikacji. */
    fun copyFromUri(context: Context, uri: Uri): String? = runCatching {
        val dir = File(context.filesDir, "photos").apply { mkdirs() }
        val out = File(dir, "${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(uri)!!.use { input ->
            out.outputStream().use { input.copyTo(it) }
        }
        out.absolutePath
    }.getOrNull()

    fun delete(path: String?) {
        if (path != null) File(path).delete()
    }
}
