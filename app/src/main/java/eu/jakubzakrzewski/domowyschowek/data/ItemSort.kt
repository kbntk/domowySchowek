package eu.jakubzakrzewski.domowyschowek.data

import java.text.Collator
import java.util.Locale

/** Kolejność elementów w grupie. Ustawienie obowiązuje we wszystkich grupach. */
enum class ItemSort(val label: String) {
    NAME_ASC("Alfabetycznie A–Z"),
    NAME_DESC("Alfabetycznie Z–A"),
    NEWEST("Najnowsze – najstarsze"),
    OLDEST("Najstarsze – najnowsze");

    companion object {
        val DEFAULT = NAME_ASC

        fun fromKey(key: String?): ItemSort = entries.firstOrNull { it.name == key } ?: DEFAULT
    }
}

private val polishCollator: Collator =
    Collator.getInstance(Locale("pl", "PL")).apply { strength = Collator.PRIMARY }

fun List<Item>.sortedItems(sort: ItemSort): List<Item> = when (sort) {
    ItemSort.NAME_ASC -> sortedWith(Comparator<Item> { a, b -> polishCollator.compare(a.name, b.name) }.thenBy { it.id })
    ItemSort.NAME_DESC -> sortedWith(Comparator<Item> { a, b -> polishCollator.compare(b.name, a.name) }.thenBy { it.id })
    ItemSort.NEWEST -> sortedWith(compareByDescending<Item> { it.createdAt }.thenByDescending { it.id })
    ItemSort.OLDEST -> sortedWith(compareBy<Item> { it.createdAt }.thenBy { it.id })
}
