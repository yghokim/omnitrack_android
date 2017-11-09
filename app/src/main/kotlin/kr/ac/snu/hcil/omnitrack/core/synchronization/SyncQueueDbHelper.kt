package kr.ac.snu.hcil.omnitrack.core.synchronization

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.db.*

/**
 * Created by younghokim on 2017. 11. 4..
 */
class SyncQueueDbHelper(context: Context, dbName: String): ManagedSQLiteOpenHelper(context, dbName, null, 1) {
    companion object {
        const val TABLE_SYNC_ENTRY = "SyncEntry"
        const val COLUMN_ID = "id"
        const val COLUMN_TYPE = "type"
        const val COLUMN_DIRECTION = "direction"
        const val COLUMN_TIMESTAMP = "timestamp"

        private val rowParser: RowParser<SyncQueueEntry> by lazy {
            rowParser{id: Int, typeText: String, direction: Short, timestamp: Long ->
                SyncQueueEntry(id, ESyncDataType.valueOf(typeText.toUpperCase()), SyncDirection.fromCode(direction), timestamp)
            }
        }
    }

    private data class SyncQueueEntry(val id: Int? = null, val type: ESyncDataType, val direction: SyncDirection, val timestamp: Long)

    data class AggregatedSyncQueue(val ids: IntArray, val data: Array<Pair<ESyncDataType, SyncDirection>>)

    override fun onCreate(db: SQLiteDatabase) {
        /*
        db.createTable(TABLE_SYNC_ENTRY,
                true,
                COLUMN_ID to INTEGER + PRIMARY_KEY + UNIQUE + AUTOINCREMENT,
                COLUMN_TYPE to TEXT,
                COLUMN_DIRECTION to INTEGER,
                COLUMN_TIMESTAMP to INTEGER
                )*/

        db.execSQL("CREATE TABLE ${TABLE_SYNC_ENTRY}(${COLUMN_ID} INTEGER PRIMARY KEY AUTOINCREMENT, ${COLUMN_TYPE} TEXT, ${COLUMN_DIRECTION} INTEGER, ${COLUMN_TIMESTAMP} INTEGER)")
    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {

    }

    @Synchronized
    private fun dumpEntries(): List<SyncQueueEntry> {
        return use{
            return@use this.select(TABLE_SYNC_ENTRY).parseList(rowParser)
        }
    }

    @Synchronized
    fun getAggregatedData(): AggregatedSyncQueue?
    {
        return use{
            val dumped = dumpEntries()
            if(dumped.isEmpty())
            {
                return@use null
            }
            val map = dumped.groupBy { it.type }.toSortedMap(object : Comparator<ESyncDataType> {
                override fun compare(p0: ESyncDataType, p1: ESyncDataType): Int {
                    return -1 * p0.syncPriority.compareTo(p1.syncPriority)
                }

            })
            return@use AggregatedSyncQueue(dumped.map { it.id!! }.toIntArray(), map.map { Pair(it.key, SyncDirection.union(it.value.map { it.direction })) }.toTypedArray())
        }
    }

    @Synchronized
    fun purgeEntries(ids: IntArray){
        if(ids.isNotEmpty()) {
            use {
                transaction{
                    this.delete(TABLE_SYNC_ENTRY, "${COLUMN_ID} in (${ids.joinToString(",")})")
                }
            }
        }
    }

    @Synchronized
    fun insertNewEntry(type: ESyncDataType, direction: SyncDirection, timestamp: Long) {
        use {
            transaction {
                this.insert(TABLE_SYNC_ENTRY,
                        COLUMN_TYPE to type.name,
                        COLUMN_DIRECTION to direction.code,
                        COLUMN_TIMESTAMP to timestamp
                        )
            }
        }
    }

}