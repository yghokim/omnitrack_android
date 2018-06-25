package kr.ac.snu.hcil.omnitrack.core.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat

/**
 * Created by Young-Ho on 10/12/2016.
 */
class LoggingDbHelper(context: Context) : SQLiteOpenHelper(context, "logging.db", null, 1) {

    companion object {
        val TIMESTAMP_FORMAT = SimpleDateFormat("yy/MM/dd hh:mm:ss")
    }

    data class OTLog(val id: Long, val log: String, val tag: String, val timestamp: Long)

    object SystemLogScheme : TableScheme() {

        val LOG = "log"
        val TAG = "tag"

        override val creationColumnContentString: String = "$LOG TEXT, $TAG TEXT"

        override val intrinsicColumnNames: Array<String> = arrayOf(LOG, TAG)

        override val tableName: String = "system_logs"
    }

    object SessionLogScheme : TableScheme() {
        val ACTIVITY = "activity"
        val ELAPSED_TIME = "elapsed_time"
        val FROM = "opened_from"
        val CONTENT = "content"

        override val creationColumnContentString: String = "$ACTIVITY TEXT, $ELAPSED_TIME INTEGER, $FROM TEXT, $CONTENT TEXT"

        override val intrinsicColumnNames: Array<String> = arrayOf(ACTIVITY, ELAPSED_TIME, FROM, CONTENT)

        override val tableName: String = "session_logs"

        init {
            appendIndexQueryString(false, "timestamp_index", LOGGED_AT)
        }
    }


    override fun onCreate(db: SQLiteDatabase) {
        val tables = arrayOf(SystemLogScheme, SessionLogScheme)

        for (scheme in tables) {
            db.execSQL(scheme.creationQueryString)
            scheme.indexCreationQueries.forEach {
                db.execSQL(it)
            }
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun writeSystemLog(log: String, tag: String) {
        val values = ContentValues()
        val now = System.currentTimeMillis()
        values.put(SystemLogScheme.UPDATED_AT, now)
        values.put(SystemLogScheme.LOGGED_AT, now)
        values.put(SystemLogScheme.LOG, log)
        values.put(SystemLogScheme.TAG, tag)

        val newRowId = writableDatabase.insert(SystemLogScheme.tableName, null, values)
    }

    fun readSystemLogs(out: MutableList<OTLog>, append: Boolean = false) {
        if (!append) {
            out.clear()
        }

        val query = readableDatabase.query(SystemLogScheme.tableName, SystemLogScheme.columnNames, null, null, null, null, "${SystemLogScheme.LOGGED_AT} DESC")

        if (query.moveToFirst()) {
            do {
                val new = OTLog(query.getLong(query.getColumnIndex(SystemLogScheme._ID)), query.getString(query.getColumnIndex(SystemLogScheme.LOG)), query.getString(query.getColumnIndex(SystemLogScheme.TAG)), query.getLong(query.getColumnIndex(SystemLogScheme.LOGGED_AT)))
                out.add(new)
            } while (query.moveToNext())
        }

        query.close()
    }


}