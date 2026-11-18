package eu.jakubzakrzewski.domowyschowek.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.Entity
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/** Pojemnik / grupa, np. "Pojemnik 1 duży". [photoPath] – zdjęcie grupy (640 px szerokości). */
@Entity(tableName = "storage_groups")
data class StorageGroup(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val photoPath: String? = null,
)

/** Element w grupie. [photoPath] – opcjonalne zdjęcie elementu (640 px szerokości). */
@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = StorageGroup::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("groupId")],
)
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val name: String,
    val photoPath: String? = null,
    /** Data dodania w milisekundach od epoki. */
    @ColumnInfo(defaultValue = "0") val createdAt: Long = 0,
)

@Dao
interface SchowekDao {
    @Query("SELECT * FROM storage_groups ORDER BY name COLLATE NOCASE")
    fun observeGroups(): Flow<List<StorageGroup>>

    @Query("SELECT * FROM storage_groups WHERE id = :id")
    fun observeGroup(id: Long): Flow<StorageGroup?>

    @Query("SELECT * FROM items")
    fun observeAllItems(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE groupId = :groupId ORDER BY name COLLATE NOCASE")
    fun observeItems(groupId: Long): Flow<List<Item>>

    @Query("SELECT * FROM storage_groups ORDER BY name COLLATE NOCASE")
    suspend fun allGroups(): List<StorageGroup>

    @Query("SELECT * FROM storage_groups WHERE name = :name LIMIT 1")
    suspend fun findGroupByName(name: String): StorageGroup?

    @Query("SELECT * FROM items WHERE groupId = :groupId")
    suspend fun itemsOf(groupId: Long): List<Item>

    @Insert
    suspend fun insertGroup(group: StorageGroup): Long

    @Update
    suspend fun updateGroup(group: StorageGroup)

    @Delete
    suspend fun deleteGroup(group: StorageGroup)

    @Insert
    suspend fun insertItem(item: Item): Long

    @Update
    suspend fun updateItem(item: Item)

    @Delete
    suspend fun deleteItem(item: Item)
}

@Database(entities = [StorageGroup::class, Item::class], version = 2, exportSchema = false)
abstract class SchowekDatabase : RoomDatabase() {
    abstract fun dao(): SchowekDao

    companion object {
        /** Dodaje datę dodania elementów. Stare elementy dostają datę migracji; kolejność zachowuje id. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE items SET createdAt = ${System.currentTimeMillis()}")
            }
        }

        fun create(context: Context): SchowekDatabase =
            Room.databaseBuilder(context.applicationContext, SchowekDatabase::class.java, "domowy_schowek.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
