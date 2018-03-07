package kr.ac.snu.hcil.omnitrack.core.synchronization

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kr.ac.snu.hcil.omnitrack.utils.toBoolean
import kr.ac.snu.hcil.omnitrack.utils.toInt
import org.jetbrains.anko.db.*

/**
 * Created by younghokim on 2017. 11. 4..
 */
class SyncQueueDbHelper(context: Context, dbName: String) : ManagedSQLiteOpenHelper(context, dbName, null, 3) {
    companion object {
        const val TABLE_SYNC_ENTRY = "SyncEntry"
        const val COLUMN_ID = "id"
        const val COLUMN_TYPE = "type"
        const val COLUMN_DIRECTION = "direction"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_IGNORE_FLAGS = "ignore_flags"

        private val rowParser: RowParser<SyncQueueEntry> by lazy {
            rowParser { id: Int, typeText: String, direction: Short, timestamp: Long, ignoreFlags: Int ->
                SyncQueueEntry(id, ESyncDataType.valueOf(typeText.toUpperCase()), SyncDirection.fromCode(direction), timestamp, ignoreFlags.toBoolean())
            }
        }
    }

    private data class SyncQueueEntry(val id: Int? = null, val type: ESyncDataType, val direction: SyncDirection, val timestamp: Long, val ignoreFlags: Boolean)

    data class AggregatedSyncQueue(val ids: IntArray, val data: Array<Triple<ESyncDataType, SyncDirection, Boolean>>)

    override fun onCreate(db: SQLiteDatabase) {
        /*
        db.createTable(TABLE_SYNC_ENTRY,
                true,
                COLUMN_ID to INTEGER + PRIMARY_KEY + UNIQUE + AUTOINCREMENT,
                COLUMN_TYPE to TEXT,
                COLUMN_DIRECTION to INTEGER,
                COLUMN_TIMESTAMP to INTEGER
                )*/
        println("create syncQueue db table")

        db.execSQL("CREATE TABLE ${TABLE_SYNC_ENTRY}(${COLUMN_ID} INTEGER PRIMARY KEY AUTOINCREMENT, ${COLUMN_TYPE} TEXT, ${COLUMN_DIRECTION} INTEGER, ${COLUMN_TIMESTAMP} INTEGER, ${COLUMN_IGNORE_FLAGS} INTEGER)")
    }

    override fun onUpgrade(p0: SQLiteDatabase, p1: Int, p2: Int) {
        p0.dropTable(TABLE_SYNC_ENTRY, true)
        onCreate(p0)
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
            return@use AggregatedSyncQueue(dumped.map { it.id!! }.toIntArray(), map.map { Triple(it.key, SyncDirection.union(it.value.map { it.direction }), it.value.find { it.ignoreFlags == true } != null) }.toTypedArray())
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
    fun insertNewEntry(type: ESyncDataType, direction: SyncDirection, timestamp: Long, ignoreFlags: Boolean) {
        use {
            transaction {
                this.insert(TABLE_SYNC_ENTRY,
                        COLUMN_TYPE to type.name,
                        COLUMN_DIRECTION to direction.code,
                        COLUMN_TIMESTAMP to timestamp,
                        COLUMN_IGNORE_FLAGS to ignoreFlags.toInt()
                        )
            }
        }
    }

}