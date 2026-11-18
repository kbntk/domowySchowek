package eu.jakubzakrzewski.domowyschowek.util

import android.content.Context
import eu.jakubzakrzewski.domowyschowek.data.ItemSort

/** Ustawienia aplikacji zapisywane lokalnie na urządzeniu. */
object Prefs {
    private const val FILE = "settings"
    private const val KEY_PHOTO_WIDTH = "photo_width"
    private const val KEY_ITEM_SORT = "item_sort"

    val PHOTO_WIDTHS = listOf(320, 480, 640)
    const val DEFAULT_PHOTO_WIDTH = 640

    fun photoWidth(context: Context): Int {
        val width = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getInt(KEY_PHOTO_WIDTH, DEFAULT_PHOTO_WIDTH)
        return if (width in PHOTO_WIDTHS) width else DEFAULT_PHOTO_WIDTH
    }

    fun itemSort(context: Context): ItemSort =
        ItemSort.fromKey(context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_ITEM_SORT, null))

    fun setItemSort(context: Context, sort: ItemSort) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ITEM_SORT, sort.name)
            .apply()
    }

    fun setPhotoWidth(context: Context, width: Int) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_PHOTO_WIDTH, width)
            .apply()
    }
}
