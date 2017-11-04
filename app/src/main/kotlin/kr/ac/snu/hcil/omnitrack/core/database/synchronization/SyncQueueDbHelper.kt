package kr.ac.snu.hcil.omnitrack.core.database.synchronization

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kr.ac.snu.hcil.omnitrack.utils.toInt
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

        val rowParser: RowParser<SyncQueueEntry> by lazy{
            rowParser{id: Int, typeText: String, direction: Short, timestamp: Long ->
                SyncQueueEntry(id, ESyncDataType.valueOf(typeText.toUpperCase()), SyncDirection.fromCode(direction), timestamp)
            }
        }
    }

    data class SyncQueueEntry(val id: Int?=null, val type: ESyncDataType, val direction: SyncDirection, val timestamp: Long)

    data class AggregatedSyncQueue(val ids: IntArray, val data: Array<Pair<ESyncDataType, SyncDirection>>)

    override fun onCreate(db: SQLiteDatabase) {
        db.createTable(TABLE_SYNC_ENTRY,
                true,
                COLUMN_ID to INTEGER + PRIMARY_KEY + UNIQUE + AUTOINCREMENT,
                COLUMN_TYPE to TEXT,
                COLUMN_DIRECTION to INTEGER,
                COLUMN_TIMESTAMP to INTEGER
                )
    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {

    }

    @Synchronized
    fun dumpEntries(): List<SyncQueueEntry>{
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
            val map = dumped.groupBy { it.type }
            return@use AggregatedSyncQueue(dumped.map { it.id!! }.toIntArray(), map.map { Pair(it.key, SyncDirection.union(it.value.map { it.direction })) }.toTypedArray())
        }
    }

    fun purgeEntries(ids: IntArray){
        if(ids.isNotEmpty()) {
            use {
                transaction{
                    this.delete(TABLE_SYNC_ENTRY, "${COLUMN_ID} in (${ids.joinToString(",")})")
                }
            }
        }
    }

    fun insertNewEntry(entry: SyncQueueEntry){
        use {
            transaction {
                this.insert(TABLE_SYNC_ENTRY,
                        COLUMN_TYPE to entry.type.name,
                        COLUMN_DIRECTION to entry.direction.code,
                        COLUMN_TIMESTAMP to entry.timestamp
                        )
            }
        }
    }

}