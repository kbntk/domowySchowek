package eu.jakubzakrzewski.domowyschowek.util

import android.content.Context

/** Ustawienia aplikacji zapisywane lokalnie na urządzeniu. */
object Prefs {
    private const val FILE = "settings"
    private const val KEY_PHOTO_WIDTH = "photo_width"

    val PHOTO_WIDTHS = listOf(320, 480, 640)
    const val DEFAULT_PHOTO_WIDTH = 640

    fun photoWidth(context: Context): Int {
        val width = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getInt(KEY_PHOTO_WIDTH, DEFAULT_PHOTO_WIDTH)
        return if (width in PHOTO_WIDTHS) width else DEFAULT_PHOTO_WIDTH
    }

    fun setPhotoWidth(context: Context, width: Int) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_PHOTO_WIDTH, width)
            .apply()
    }
}
