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
    private const val JPEG_QUALITY = 90

    /** Plik tymczasowy, w którym aparat zapisze zdjęcie. */
    fun newCaptureFile(context: Context): File {
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        return File(dir, "${UUID.randomUUID()}.jpg")
    }

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /**
     * Wczytuje zdjęcie z aparatu (z uwzględnieniem orientacji EXIF), skaluje do szerokości
     * wybranej w ustawieniach (320, 480 lub 640 px) z zachowaniem proporcji i zapisuje jako JPEG
     * w pamięci wewnętrznej aplikacji. Zwraca ścieżkę do pliku lub null w razie błędu.
     */
    suspend fun importScaled(context: Context, captured: File): String? = withContext(Dispatchers.IO) {
        val targetWidth = Prefs.photoWidth(context)
        runCatching {
            // Dekodujemy najpierw w ~2x docelowej szerokości, a potem zmniejszamy filtrem dwuliniowym.
            // Jednokrokowe skalowanie dużego zdjęcia z aparatu do 320 px daje wyraźnie miększy obraz.
            val decoded = ImageDecoder.decodeBitmap(ImageDecoder.createSource(captured)) { decoder, info, _ ->
                val width = info.size.width
                val height = info.size.height
                if (width > targetWidth * 2) {
                    val intermediateWidth = targetWidth * 2
                    val intermediateHeight = max(1, (height.toDouble() * intermediateWidth / width).roundToInt())
                    decoder.setTargetSize(intermediateWidth, intermediateHeight)
                }
            }
            val finalHeight = max(1, (decoded.height.toDouble() * targetWidth / decoded.width).roundToInt())
            val bitmap = Bitmap.createScaledBitmap(decoded, targetWidth, finalHeight, true)
            if (bitmap !== decoded) decoded.recycle()
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
